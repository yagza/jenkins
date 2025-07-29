timestamps {
    node(JenkinsSlaveNode) {

        try {

            stage ('Git Checkout') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
            }

            stage('Prepare Config') {
                configFileProvider([configFile(fileId: "${configFileId}", targetLocation: "${env.WORKSPACE}/DnsLookupApp.properties")]) {
                    echo "Config file applied successfully!"
                }
            }

            stage('Deploy') {
                def remoteServer = 'jenkins@10.0.0.125:~'
                def sshCommand = "scp -i /home/jenkins/.ssh/id_rsa_deploy ${env.WORKSPACE}/DnsLookupApp.java ${remoteServer}"
                sh "${sshCommand}"
                def sshCommand = "scp -i /home/jenkins/.ssh/id_rsa_deploy ${env.WORKSPACE}/DnsLookupApp.properties ${remoteServer}"
                sh "${sshCommand}"
                echo "Deployed to VM successfully!"
            }

            stage('Run app') {
                sh "ssh -i /home/jenkins/.ssh/id_rsa_deploy 10.0.0.125 'cd ~/dynamic-config-test && javac DnsLookupApp.java && java DnsLookupApp'"
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
