timestamps {
    node(JenkinsSlaveNode) {

        try {

            stage ('Git Checkout') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
            }

            stage('Prepare Config') {
                // Указываем ID конфига, заданный в Jenkins Config File Provider
                // def configFileId = 'java-properties-config-id'  // Замените на реальный ID вашего конфига

                configFileProvider([configFile(fileId: "${configFileId}", targetLocation: "${env.WORKSPACE}/DnsLookupApp.properties")]) {
                    echo "Config file applied successfully!"
                }
            }

            // Шаг 3: Деплой на виртуальную машину (копирование)
            stage('Deploy') {
                def remoteServer = 'jenkins@10.0.0.125:/deploy_simple-java-1/'
                def sshCommand = "scp -i .ssh/id_rsa_deploy -r ${env.WORKSPACE} ${remoteServer}"
        
                sh "${sshCommand}"
        
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
