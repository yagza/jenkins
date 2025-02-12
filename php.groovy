timestamps {
    ansiColor('xterm') {
        node(env.JenkinsSlaveNode) {

            env.PROJECT_NAME = "project_pso"
            env.PROJECT_VERSION = "0.0.1"

            try {
                stage ("Git Checkout") {
                    checkout scmGit(branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[credentialsId: 'jenkins-to-github', url: 'https://github.com/yagza/simple-php-website.git']])
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