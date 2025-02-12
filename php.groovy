timestamps {
    ansiColor('xterm') {
        node(JenkinsSlaveNode) {

            env.PROJECT_NAME = "project_pso"
            env.PROJECT_VERSION = "0.0.1"

            try {

                stage ('Git Checkout') {
                    checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
                }

            }

            catch (exception) {
                throw exception
            }

            finally {
                println("Clean something")
            }
            
        }
    }
}