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

           
            /*dir("${env.WORKSPACE}/ans") {
              stage('Upload properties to both deploy servers') {
                sh "mv /tmp/${PROJECT_NAME}/* hosts"
                sh "rmdir /tmp/${PROJECT_NAME}"
                withCredentials([
                    usernamePassword(credentialsId: 'DATABASE_CREDS', usernameVariable: 'db_user', passwordVariable: 'db_password'),
                    usernamePassword(credentialsId: 'FTP_CREDS', usernameVariable: 'ftp_user', passwordVariable: 'ftp_password')
                ]) {
                    sh """
                    ansible-playbook -i hosts/psi -e 'DB_USERNAME=$db_user' -e 'DB_PASSWORD=$db_password' -e 'FTP_USERNAME=$ftp_user' -e 'FTP_PASSWORD=$ftp_password' deploy-book-01.yml
                    """
                }
              }
            }*/


            stage('Something strange') {

                sh "mv /tmp/${PROJECT_NAME}/* hosts"
                sh "rmdir /tmp/${PROJECT_NAME}"
                
                    // Читаем файл all.yml
                    def allYml = readYaml file: 'ans/hosts/group_vars/all.yml'
                    
                    // Получаем список credentials
                    def credsList = allYml.Credentials
                    
                    // Создаем map для хранения переменных
                    def ansibleVars = [:]
                    
                    // Обрабатываем каждый credential
                    credsList.each { credId ->
                        // Извлекаем компонент из имени credential (удаляем _CREDS)
                        def component = credId.replace('_CREDS', '')
                        
                        // Используем withCredentials для получения значений
                        withCredentials([usernamePassword(
                            credentialsId: credId,
                            usernameVariable: 'USERNAME',
                            passwordVariable: 'PASSWORD'
                        )]) {
                            // Сохраняем значения в map
                            ansibleVars["${component}_USERNAME"] = env.USERNAME
                            ansibleVars["${component}_PASSWORD"] = env.PASSWORD
                        }
                    }
                    
                    // Записываем переменные в файл
                    writeYaml file: 'ansible_cred_vars.yml', data: ansibleVars
                    
                    // Для отладки можно вывести содержимое файла
                    sh 'cat ansible_cred_vars.yml'

                    sh """
                    ansible-playbook -i hosts/psi -e @ansible_cred_vars.yml deploy-book-01.yml
                    rm -f ansible_cred_vars.yml
                    """
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
