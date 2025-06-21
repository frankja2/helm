pipeline {
  agent { label 'terraform' }

  environment {
    KUBECONFIG_CRED_ID = 'k3s-kubeconfig'
    AZURE_TENANT_ID = 'b8d5cd0a-97e5-4064-a2d0-ceff502aef62'
    ACR_NAME = 'jfsandbox'
    REPO_URL = 'https://github.com/frankja2/helm-charts.git'
    CHART_PATH = 'external'
    CHART_VERSION = '0.1.0'    // Dopasuj do Chart.yaml jeśli zmieniasz wersję
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
stage('Generate secret.yaml') {
  steps {
    sh '''
      cat <<EOF > secret.yaml
secrets:
  - name: db-password
    value: supersecret123
  - name: api-key
    value: myapikey-xyz
  - name: admin-password
    value: adminP@ssw0rd
EOF
    '''
  }
}
stage('Template Helm Chart from ACR') {
  steps {
    withCredentials([usernamePassword(credentialsId: 'jenkins-serviceprincipal', usernameVariable: 'AZURE_CLIENT_ID', passwordVariable: 'AZURE_CLIENT_SECRET')]) {
      sh '''
        helm registry login $ACR_NAME.azurecr.io --username $AZURE_CLIENT_ID --password $AZURE_CLIENT_SECRET

        helm template myrelease oci://jfsandbox.azurecr.io/helm/external --version 0.1.0 -f secrets.yaml
      '''
    }
  }
}
}
}
