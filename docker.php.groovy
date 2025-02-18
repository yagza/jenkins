/*
    # The following variables must be set in "Prepare an environment for the run"

    JenkinsSlaveNode 
    GithubCreds 
    DockerRegistry 
    DockerhubCreds 
    FIRST_DEPLOY 
*/

// import groovy.json.JsonSlurper

def SlaveNodes = ['vm-01', 'vm-02'] 

timestamps {
    ansiColor('xterm') {
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
/*
                    def version = readFile "composer.json"
                    echo version
*/
// Nailed it with the plugin Pipeline Utility Steps
                    def object = readJSON file: "${WORKSPACE}/composer.json"

                    assert object instanceof Map
                    if (object.name != null ) {
                        env.PROJECT_NAME = object.name.replaceAll("/","_")
                    }

                    if (object.version != null ) {
                        env.PROJECT_VERSION = object.version
                    }

                }

                stage('Build image') {
                    MyApp = docker.build("yagza/${PROJECT_NAME}:${PROJECT_VERSION}")
                }

                stage('Push image') {
                    docker.withRegistry(DockerRegistry, DockerhubCreds) {
                    MyApp.push()
                    }
                }

                stage ('Clear local registry') {
                    sh "docker system prune -a -f"
                }

                stage('Get old tag') {
                // for rollback purpose
                    if (env.FIRST_DEPLOY == 'false') {
                        try {
                            OLD_TAG = sh(
                                script: "docker inspect --format='{{.Config.Image}}' ${PROJECT_NAME} | awk -F':' '{print \$2}'",
                                returnStdout: true
                            ).trim()
                            echo "Current running container tag: ${OLD_TAG}"
                            if (OLD_TAG == null || OLD_TAG == "") {
                                throw new Exception("Container not found or tag is empty")
                            }
                        } catch (Exception e) {
                            echo "No running container found or failed to get old tag: ${e}"
                            OLD_TAG = '1.0.1'
                            echo "Using fallback tag: ${OLD_TAG}"
                        }
                    } else {
                        echo "Skipping 'Get old tag' stage because FIRST_DEPLOY is ${FIRST_DEPLOY}"
                    }
                }

                stage('Stop old container') {
                    if (env.FIRST_DEPLOY == 'false') {
                        try {
                            sh "docker rm ${PROJECT_NAME} -f"
                        } catch (Exception e) {
                            echo "No running container found or failed to stop/remove: ${e}"
                        }
                    } else {
                        echo "Skipping 'Stop old Container' stage because FIRST_DEPLOY is ${FIRST_DEPLOY}"
                    }
                }


                stage('Run New Version on every nodes') {
                    def parallelTasks = [:]

                    SlaveNodes.each { slave ->
                        parallelTasks["Run on ${slave}"] = {
                            node(slave) {
                                try {
                                    MyApp.run("--name ${PROJECT_NAME} -p 8080:8080")
                                    echo 'you may try to connect https://udemy.my.home'
                                } catch (Exception e) {
                                    echo "Failed to start new container: ${e}"
                                    currentBuild.result = 'FAILURE'
                                }
                            }
                        }
                    }

                    parallel parallelTasks
                }

                stage('Health check') {
                    if (currentBuild.result != 'FAILURE') {
                        try {
                            sh "curl --fail http://localhost:8080"
                        } catch (Exception e) {
                            echo "Health check failed: ${e}"
                            currentBuild.result = 'FAILURE'
                        }
                    }
                }            

                stage('Rollback if failed') {
                    if (env.FIRST_DEPLOY == 'false') {
                        if (currentBuild.result == 'FAILURE') {
                            echo "Rolling back to previous version: ${OLD_TAG}"
                            try {
                                sh "docker rm ${PROJECT_NAME} -f"
                                sh "docker run -d --name ${PROJECT_NAME} -p 8080:8080 yagza/${PROJECT_NAME}:${OLD_TAG}"
                            } catch (Exception e) {
                                echo "Failed to rollback: ${e}"
                                error "Failed to rollback"
                            }
                        }
                    } else {
                        echo "Skipping 'Rollback' stage because FIRST_DEPLOY is ${FIRST_DEPLOY}"
                    }
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
}