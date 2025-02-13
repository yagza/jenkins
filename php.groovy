import groovy.json.JsonSlurper

timestamps {
//    ansiColor('xterm') {
        node(JenkinsSlaveNode) {

            env.PROJECT_NAME = "project_pso"
            env.PROJECT_VERSION = "0.0.1"

            try {

                stage ('Git Checkout') {
                    checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])

                    def jsonSlurper = new groovy.json.JsonSlurper()
//                    def object = jsonSlurper.parseText('{"key": "value"}')
//                    echo object.key

//                    def jsonSlurper = new JsonSlurper()
                    def version = readFile "${env.WORKSPACE}/composer.json"
//                    def object = jsonSlurper.parseText(readFile("${WORKSPACE}/composer.json"))
//
//                    assert object instanceof Map
//                    if (object.name != null ) {
//                        env.PROJECT_NAME = object.name.replaceAll("/","_")
//                    }
//
//                    if (object.version != null ) {
//                        env.PROJECT_VERSION = object.version
//                    }
                    echo version

                }

            }

            catch (exception) {
                throw exception
            }

            finally {
                println("Clean something")
                println(env.PROJECT_NAME)
                println(env.PROJECT_VERSION)
            }
            
        }
    }