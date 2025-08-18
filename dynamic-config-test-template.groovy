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


def credsMap = [:]  // Будет хранить все найденные credentials

// Список всех возможных credentials с их параметрами
def possibleCredentials = [
    'DATABASE_CREDS': [usernameVar: 'db_user', passwordVar: 'db_pass'],
    'FTP_CREDS': [usernameVar: 'ftp_user', passwordVar: 'ftp_pass'],
    'API_CREDS': [apiKeyVar: 'api_key']  // Пример для single-значения
]

possibleCredentials.each { credId, vars ->
    try {
        if (vars.passwordVar) {  // Для username/password
            withCredentials([usernamePassword(
                credentialsId: credId,
                usernameVariable: vars.usernameVar,
                passwordVariable: vars.passwordVar
            )]) {
                credsMap[vars.usernameVar] = env[vars.usernameVar]
                credsMap[vars.passwordVar] = env[vars.passwordVar]
                echo "✅ Успешно загружен ${credId}"
            }
        } else {  // Для single-значения (например, API key)
            withCredentials([string(
                credentialsId: credId,
                variable: vars.apiKeyVar
            )]) {
                credsMap[vars.apiKeyVar] = env[vars.apiKeyVar]
                echo "✅ Успешно загружен ${credId}"
            }
        }
    } catch (Exception e) {
        echo "⚠️ Credential ${credId} не найден, пропускаем"
    }
}

// Создаем временный YAML-файл
def varsFile = "/tmp/ansible_vars_${UUID.randomUUID()}.yml"
writeYaml file: varsFile, data: credsMap, overwrite: true
                  

              sh """
                  ansible-playbook -i hosts/psi -e @${varsFile} deploy-book-01.yml
                  rm -f ${varsFile}  # Важно: удаляем файл сразу после использования
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
