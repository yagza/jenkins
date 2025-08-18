
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


  


                def credsMap = [:] // Здесь будут храниться все credentials

                // Определяем все возможные credentials и их параметры
                def possibleCredentials = [
                    'DATABASE_CREDS': [type: 'usernamePassword', vars: ['db_user', 'db_pass']],
                    'FTP_CREDS': [type: 'usernamePassword', vars: ['ftp_user', 'ftp_pass']],
                    'API_KEY': [type: 'string', vars: ['api_key']]
                ]

                // Загрузка credentials
                possibleCredentials.each { credId, params ->
                    try {
                        if(params.type == 'usernamePassword') {
                            withCredentials([usernamePassword(
                                credentialsId: credId,
                                usernameVariable: params.vars[0],
                                passwordVariable: params.vars[1]
                            )]) {
                                credsMap[params.vars[0]] = env[params.vars[0]]
                                credsMap[params.vars[1]] = env[params.vars[1]]
                                echo "✅ Успешно загружен ${credId}"
                            }
                        } else if(params.type == 'string') {
                            withCredentials([string(
                                credentialsId: credId,
                                variable: params.vars[0]
                            )]) {
                                credsMap[params.vars[0]] = env[params.vars[0]]
                                echo "✅ Успешно загружен ${credId}"
                            }
                        }
                    } catch(Exception e) {
                        echo "⚠️ Credential ${credId} не найден, пропускаем"
                    }
                }

                def varsFile = "ansible_vars_${BUILD_ID}.yml"
                writeYaml file: varsFile, data: credsMap
              
                sh """
                    ansible-playbook -i hosts/psi -e @${varsFile} deploy-book-01.yml
                    rm -f ${varsFile}
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
