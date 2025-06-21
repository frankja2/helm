pipeline {
  agent { label 'terraform' }

  environment {
    KUBECONFIG_CRED_ID = 'k3s-kubeconfig'
    AZURE_TENANT_ID = 'b8d5cd0a-97e5-4064-a2d0-ceff502aef62'
    ACR_NAME = 'jfsanbox'
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
            az acr login --name $ACR_NAME
          '''
        }
      }
    }
  }
}
