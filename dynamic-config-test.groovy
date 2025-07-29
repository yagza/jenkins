timestamps {
    node(JenkinsSlaveNode) {

        try {

            stage ('Git Checkout') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
            }

            stage('Prepare Config') {
                // Указываем ID конфига, заданный в Jenkins Config File Provider
                // def configFileId = 'java-properties-config-id'  // Замените на реальный ID вашего конфига
        
                // Загружаем конфиг в workspace
                def configFile = configFileProvider([
                    configFile(
                        fileId: "${configFileId}",
                        targetLocation: "${env.WORKSPACE}/DnsLookupApp.properties",
                        variable: 'JAVA_PROPS'
                    )
                ])[0]
        
                echo "Config file loaded at ${configFile.path}"
            }

            // Шаг 3: Деплой на виртуальную машину (копирование)
            stage('Deploy') {
                def remoteServer = 'jenkins@10.0.0.125:/deploy_simple-java-1/'
                def sshCommand = "scp -r ${env.WORKSPACE} ${remoteServer}"
        
                // Используем ssh-agent для аутентификации (если нужно)
                sshagent(['jenkins_deploy']) {  // Укажите ID ваших SSH-ключей в Jenkins
                    sh sshCommand
                }
        
                echo "Deployed to VM successfully!"
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
