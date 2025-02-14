import groovy.json.JsonSlurper

timestamps {
    ansiColor('xterm') {
        node(JenkinsSlaveNode) {

            env.PROJECT_NAME = "project_pso"
            env.PROJECT_VERSION = "0.0.1"

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
// with plugin Pipeline Utility Steps
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
                    MyApp = docker.build("yagza/${env.PROJECT_NAME}:${env.PROJECT_VERSION}")
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
                    if (env.FIRST_DEPLOY == 'false') {
                        try {
                            // Получаем тег текущего запущенного контейнера
                            OLD_TAG = sh(
                                script: "docker inspect --format='{{.Config.Image}}' ${env.PROJECT_NAME} | awk -F':' '{print \$2}'",
                                returnStdout: true
                            ).trim()
                            echo "Current running container tag: ${OLD_TAG}"
                            if (OLD_TAG == null || OLD_TAG == "") {
                                throw new Exception("Container not found or tag is empty")
                            }
                        } catch (Exception e) {
                            echo "No running container found or failed to get old tag: ${e}"
                            OLD_TAG = '1.0.1' // Fallback, если контейнер не найден
                            echo "Using fallback tag: ${OLD_TAG}"
                        }
                    } else {
                        echo "Skipping 'Get old tag' stage because FIRST_DEPLOY is ${env.FIRST_DEPLOY}"
                    }
                }
        
                stage('Stop old container') {
                    if (env.FIRST_DEPLOY == 'false') {
                        try {
                            sh "docker rm ${env.PROJECT_NAME} -f"
                        } catch (Exception e) {
                            echo "No running container found or failed to stop/remove: ${e}"
                        }
                    } else {
                        echo "Skipping 'Stop old Container' stage because FIRST_DEPLOY is ${env.FIRST_DEPLOY}"
                    }
                }

                stage('Run New Version') {
                    try {
                        MyApp.run("--name ${env.PROJECT_NAME} -p 8080:8080")
                        echo 'you may try to connect http://10.0.0.146:8080'
                    } catch (Exception e) {
                        echo "Failed to start new container: ${e}"
                        currentBuild.result = 'FAILURE'
                    }
                }

                stage('Health check') {
                    if (currentBuild.result != 'FAILURE') {
                        try {
                            // Пример проверки здоровья через curl
                            sh "curl --fail http://localhost:8080"
                        } catch (Exception e) {
                            echo "Health check failed: ${e}"
                            currentBuild.result = 'FAILURE' // Отмечаем сборку как неудачную
                            // Не используем error, чтобы pipeline продолжал выполнение
                        }
                    }
                }            
        
                stage('Rollback if failed') {
                    if (env.FIRST_DEPLOY == 'false') {
                        if (currentBuild.result == 'FAILURE') {
                            echo "Rolling back to previous version: ${OLD_TAG}"
                            try {
                                sh "docker rm ${env.PROJECT_NAME} -f"
                                sh "docker run -d --name ${env.PROJECT_NAME} -p 8080:8080 ${env.PROJECT_NAME}:${OLD_TAG}"
                            } catch (Exception e) {
                                echo "Failed to rollback: ${e}"
                                error "Failed to rollback"
                            }
                        }
                    } else {
                        echo "Skipping 'Rollback' stage because FIRST_DEPLOY is ${env.FIRST_DEPLOY}"
                    }
                }
            }
        }
    }
