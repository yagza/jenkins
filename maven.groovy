timestamps {
    node(JenkinsSlaveNode) {

        def MAVEN_VERSION = "${env.MAVEN_VERSION}"
        def JDK_VERSION = "${env.JDK_VERSION}"

            try {

                stage ('Git Checkout') {
                    checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
                }

                stage('Build') {
                    tool name: '3.8.8', type: 'maven'
                    withMaven(maven: "${MAVEN_VERSION}", jdk: "${JDK_VERSION}") {
                            sh 'mvn -B -DskipTests clean package'
                    }
                }
                stage('Test') {
                    withMaven(maven: "${MAVEN_VERSION}", jdk: "${JDK_VERSION}") {
                        sh 'mvn test'
                        /*post {
                            always {
                                junit 'target/surefire-reports/*.xml'
                            }
                        }*/
                    }
                }
                stage('Deliver') {
                        sh './jenkins/scripts/deliver.sh'
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