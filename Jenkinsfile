def getSecretsFromKeyVault(stage, vaultSuffix) {
    def vaultName = "jfsandbox"

    
    def secretNamesRaw = sh(
        script: """
        az keyvault secret list --vault-name ${vaultName} --query '[].name' --output tsv |
        grep '^aks-' |
        grep -v 'tls-crt' |
        grep -v 'tls-key' || true
        """,
        returnStdout: true
    ).trim()

    def secretNames = secretNamesRaw ? secretNamesRaw.split("\n") : []
    def secretValues = [:]

    for (secretName in secretNames) {
        def rawValue = sh(
            script: "az keyvault secret show --vault-name ${vaultName} --name ${secretName} --query value --output tsv",
            returnStdout: true
        ).trim()

        def parsedAny = false
        def lines = rawValue.split("\\r?\\n")

        for (line in lines) {
            line = line.trim()
            if (!line || line.startsWith("#")) {
                continue
            }
            def parts = line.split("=", 2)
            if (parts.length == 2) {
                def actualKey = parts[0].trim()
                def actualValue = parts[1].trim()
                secretValues[actualKey] = actualValue
                parsedAny = true
            }
        }

        if (!parsedAny) {
            // fallback: stary styl – klucz na podstawie nazwy secreta
            def helmSecretName = secretName.replaceFirst('^aks-', '').replaceAll('-', '_')
            secretValues[helmSecretName] = rawValue
        }
    }

    return secretValues
}


pipeline {
  agent { label 'terraform' }

  parameters {
    string(name: 'PROJECT_NAME', defaultValue: 'external', description: 'PROJECT_NAME')
    string(name: 'CHART_PATH', defaultValue: 'external', description: 'Podkatalog z helm chart')
    string(name: 'CHART_VERSION', defaultValue: '0.1.0', description: 'Wersja chartu do wypchnięcia')
  }

  environment {
    KUBECONFIG_CRED_ID = 'k3s-kubeconfig'
    AZURE_TENANT_ID = 'b8d5cd0a-97e5-4064-a2d0-ceff502aef62'
    ACR_NAME = 'jfsandbox'
    REPO_URL = 'https://github.com/frankja2/helm-charts.git'
    CHART_PATH = "${params.CHART_PATH}"     // <--- param
    CHART_VERSION = "${params.CHART_VERSION}" // <--- param
  }

  stages {
    stage('Login to ACR') {
      steps {
        withCredentials([
          file(credentialsId: env.KUBECONFIG_CRED_ID, variable: 'KUBECONFIG'),
          usernamePassword(credentialsId: 'jenkins-serviceprincipal', usernameVariable: 'AZURE_CLIENT_ID', passwordVariable: 'AZURE_CLIENT_SECRET')
        ]) {
          sh '''
            export KUBECONFIG=$KUBECONFIG
            az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET --tenant $AZURE_TENANT_ID
            az acr repository list --name $ACR_NAME --output table
          '''
        }
      }
    }

    stage('Clone Helm Charts') {
      steps {
        git url: env.REPO_URL, branch: 'main'
      }
    }

    stage('Build Helm Chart') {
      steps {
        dir(env.CHART_PATH) {
          sh 'helm package .'
        }
      }
    }

stage('Push Helm Chart to ACR') {
  steps {
    withCredentials([usernamePassword(credentialsId: 'jenkins-serviceprincipal', usernameVariable: 'AZURE_CLIENT_ID', passwordVariable: 'AZURE_CLIENT_SECRET')]) {
     script {
    def secrets = getSecretsFromKeyVault('dev', 'test')

    def secretsList = secrets.collect { k, v -> [ name: k, value: v ] }
    def yamlContent = "secrets:\n" +
        secretsList.collect { "  - name: \"${it.name}\"\n    value: \"${it.value}\"" }.join('\n')

    writeFile file: 'secrets.yaml', text: yamlContent
}
      dir(env.CHART_PATH) {

        sh '''
          CHART_TGZ=$(ls *.tgz | head -n1)
          helm registry login $ACR_NAME.azurecr.io --username $AZURE_CLIENT_ID --password $AZURE_CLIENT_SECRET
          helm push $CHART_TGZ oci://$ACR_NAME.azurecr.io/helm
        '''
      }
    }
  }
}

stage('Template Helm Chart from ACR') {
  steps {
    withCredentials([usernamePassword(credentialsId: 'jenkins-serviceprincipal', usernameVariable: 'AZURE_CLIENT_ID', passwordVariable: 'AZURE_CLIENT_SECRET')]) {
      script {
        // Zakładam, że plik values.yaml jest w katalogu głównym workspace (albo podaj pełną ścieżkę)
        def chartVersion = sh(
          script: "yq .chart-version values.yaml",
          returnStdout: true
        ).trim()
      echo "Wersja chartu z values.yaml: ${chartVersion}"
      
        sh """
          helm registry login \$ACR_NAME.azurecr.io --username \$AZURE_CLIENT_ID --password \$AZURE_CLIENT_SECRET
          helm upgrade -i \$PROJECT_NAME oci://\$ACR_NAME.azurecr.io/helm/\$CHART_PATH --version ${chartVersion} -f secrets.yaml -n default
        """
      }
    }
  }
}
}
}
