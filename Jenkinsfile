pipeline {
  agent { label 'terraform' }

  environment {
    KUBECONFIG_CRED_ID = 'k3s-kubeconfig'
  }

  stages {
    stage('Validate Helm Chart') {
steps {
  sh 'which helm || echo "brak helma"'
  sh 'ls -l /usr/local/bin/'
  sh 'env'
}
    }
    stage('Install Helm Chart') {
steps {
  sh 'sleep 120'
}
    }
  }
}
