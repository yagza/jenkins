timestamps {
    node(JenkinsSlaveNode) {

        try {

            stage ('Download Project') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
                sh "ls -la && pwd"
            }

            stage('Upload Project to Deploy servers') {
                def remoteServer = 'jenkins@10.0.0.125:~/dynamic-config-test'
                def sshCommand = "scp -i /home/jenkins/.ssh/id_rsa_deploy ${env.WORKSPACE}/DnsLookupApp.java ${remoteServer}"
                sh "${sshCommand}"
                echo "Main java uploaded to 10.0.0.125 successfully!"
                remoteServer = 'jenkins@10.0.0.126:~/dynamic-config-test'
                sh "${sshCommand}"
                echo "Main java uploaded to 10.0.0.126 successfully!"
                sh "ls -la && pwd"
            }

            stage ('Download ansible Project') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlAnsible]])
                sh "ls -la && pwd"
            }

            stage('Prepare Config Properties') {
                checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlProps]])
                sh "ls -la && pwd"
                sh "cp -a $PROJECT_NAME/$SEGMENT/* ../ans/hosts"
            }

            dir("${env.WORKSPACE}/ans") {
              stage('Upload properties to both deploy servers') {
                ansible-playbook -i hosts/psi deploy-book-01.yml
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
