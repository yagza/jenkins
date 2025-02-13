import groovy.json.JsonSlurper

timestamps {
//    ansiColor('xterm') {
        node(JenkinsSlaveNode) {

            env.PROJECT_NAME = "project_pso"
            env.PROJECT_VERSION = "0.0.1"

            try {

                stage ('Git Checkout') {
                    checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])

                    def jsoncomposer = new JsonSlurper()
                    def parsedcomposer = jsoncomposer.parseText(readFile("$WORKSPACE/composer.json"))

                    assert parsedcomposer instanceof Map
                    if (parsedcomposer.name != null ) {
                        env.PROJECT_NAME = parsedcomposer.name.replaceAll("/","_")
                    }

                    if (parsedcomposer.version != null ) {
                        env.PROJECT_VERSION = parsedcomposer.version
                    }

                }

            }

            catch (exception) {
                throw exception
            }

            finally {
                println("Clean something")
                println("env.PROJECT_NAME")
                println("env.PROJECT_VERSION")
            }
            
        }
//    }
}