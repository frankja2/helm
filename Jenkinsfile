pipeline {
  agent { label 'terraform' }

  environment {
    KUBECONFIG_CRED_ID = 'k3s-kubeconfig'
  }

  stages {
    stage('Install Helm Chart') {
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED_ID, variable: 'KUBECONFIG')]) {
          sh '''
            export KUBECONFIG=$KUBECONFIG
            helm repo add bitnami https://charts.bitnami.com/bitnami
            helm repo update
            helm install nginx-test bitnami/nginx --wait
            helm list
          '''
        }
      }
    }
  }
}


