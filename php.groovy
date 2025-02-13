import groovy.json.JsonSlurper

timestamps {
//    ansiColor('xterm') {
        node(JenkinsSlaveNode) {

            env.PROJECT_NAME = "project_pso"
            env.PROJECT_VERSION = "0.0.1"

            try {

                stage ('Git Checkout') {
                    checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])

/* These are works normally
                    def jsonSlurper = new JsonSlurper()
                    def object = jsonSlurper.parseText('{"name": "Goblin_inc/simple-php","type": "website","version": "1.0.1"}')
                    echo object.type
                    echo object.version
*/

/* These aren't   - Caused: java.io.NotSerializableException: groovy.json.JsonSlurper
                    def jsonSlurper = new JsonSlurper()
                    def object = jsonSlurper.parseText(readFile('composer.json'))
                    echo object.type
                    echo object.version
*/


                    def version = readFile "composer.json"
                    echo version
/*
// with plugin Pipeline Utility Steps
                    def object = readJSON file: "${WORKSPACE}/composer.json"
                    echo "Parsed object: ${object}"
                    echo object.type
                    echo object.version


                    assert object instanceof Map
                    if (object.name != null ) {
                        env.PROJECT_NAME = object.name.replaceAll("/","_")
                    }

                    if (object.version != null ) {
                        env.PROJECT_VERSION = object.version
                    }
*/
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