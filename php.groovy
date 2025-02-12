timestamps {
    ansiColor('xterm') {
        node(env.JenkinsSlaveNode) {

            env.PROJECT_NAME = "project_pso"
            env.PROJECT_VERSION = "0.0.1"

            stage ('Git Checkout') {
                git credentialsId: 'jenkins-to-github', url: 'git@github.com:yagza/simple-php-website.git'
                // checkout scmGit(branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[credentialsId: 'jenkins-to-github', url: 'git@github.com:yagza/simple-php-website.git']])
            }
        }
    }
}