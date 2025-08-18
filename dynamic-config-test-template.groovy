timestamps {
    node(JenkinsSlaveNode) {

        try {

            stage ('Download Project') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
                sh "ls -la && pwd"
            }

            stage('Upload Project to Deploy servers') {
                def remoteServer = 'jenkins@10.0.0.125:~/dynamic-config-test'
                sh "scp -i /home/jenkins/.ssh/id_rsa_deploy ${env.WORKSPACE}/DnsLookupApp.java ${remoteServer}"
                echo "Main java uploaded to 10.0.0.125 successfully!"
                remoteServer = 'jenkins@10.0.0.126:~/dynamic-config-test'
                sh "scp -i /home/jenkins/.ssh/id_rsa_deploy ${env.WORKSPACE}/DnsLookupApp.java ${remoteServer}"
                echo "Main java uploaded to 10.0.0.126 successfully!"
                sh "ls -la && pwd"
            }

            stage('Prepare Config Properties') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlProps]])
                sh "ls -la && pwd"
                sh "mkdir /tmp/${PROJECT_NAME}"
                sh "mv $PROJECT_NAME/$SEGMENT/* /tmp/${PROJECT_NAME}"
            }

            stage ('Download ansible Project') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlAnsible]])
                sh "ls -la && pwd"
            }

           
            dir("${env.WORKSPACE}/ans") {
              stage('Upload properties to both deploy servers') {
                sh "mv /tmp/${PROJECT_NAME}/* hosts"
                sh "rmdir /tmp/${PROJECT_NAME}"

                // Создаем Map для хранения credentials
                def creds = [:]
          
                // Проверяем и добавляем credentials только если они существуют
try {
  withCredentials([usernamePassword(
    credentialsId: 'DATABASE_CREDS',
    usernameVariable: 'DB_USER',
    passwordVariable: 'DB_PASS'
  )]) {
    creds['db_user'] = env.DB_USER
    creds['db_pass'] = env.DB_PASS
    echo "Found DATABASE_CREDS credentials"
  }
} catch (Exception e) {
  echo "DATABASE_CREDS not found, skipping"
}

// Проверяем FTP_CREDS
try {
  withCredentials([usernamePassword(
    credentialsId: 'FTP_CREDS',
    usernameVariable: 'FTP_USER',
    passwordVariable: 'FTP_PASS'
  )]) {
    creds['ftp_user'] = env.FTP_USER
    creds['ftp_pass'] = env.FTP_PASS
    echo "Found FTP_CREDS credentials"
  }
} catch (Exception e) {
  echo "FTP_CREDS not found, skipping"
}
          
              // Формируем команду с только существующими переменными
              def extraVars = creds.collect { k, v -> "-e '${k}=${v}'" }.join(' ')

              sh """
                  ansible-playbook -i hosts/psi ${extraVars} deploy-book-01.yml
              """
              }
            }

            stage('Run app') {
              sh "ssh -i /home/jenkins/.ssh/id_rsa_deploy 10.0.0.125 'cd ~/dynamic-config-test && javac DnsLookupApp.java && java DnsLookupApp'"
              sh "ssh -i /home/jenkins/.ssh/id_rsa_deploy 10.0.0.126 'cd ~/dynamic-config-test && javac DnsLookupApp.java && java DnsLookupApp'"
            }            

        } catch (Exception e) {
          echo "Pipeline failed: ${e.getMessage()}"
          currentBuild.result = 'FAILURE'
          throw e 
        } finally {
          println("Очистка Jenkins Slave Node")
          cleanWs()
        }
    }
}
