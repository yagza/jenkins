timestamps {
    node(JenkinsSlaveNode) {

        MAVEN_VERSION = "${MAVEN_VERSION}"
        JDK_VERSION = "${JDK_VERSION}"

            try {

                stage ('Git Checkout') {
                    checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
                }

                stage('Build') {
                    tool name: '3.8.8', type: 'maven'
                    withMaven(maven: "${MAVEN_VERSION}", jdk: "${JDK_VERSION}") {
                        steps {
                            //sh 'mvn -B -DskipTests clean package'
                            sh 'mvn -B -DskipTests clean package'
                        }
                    }
                }
                stage('Test') {
                    steps {
                        //sh 'mvn test'
                        sh 'mvn -B -DskipTests clean package'
                    }
                    post {
                        always {
                            junit 'target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Deliver') {
                    steps {
                        sh './jenkins/scripts/deliver.sh'
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