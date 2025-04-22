timestamps {
    node(JenkinsSlaveNode) {

        def MAVEN_VERSION = "${env.MAVEN_VERSION}"
        def JDK_VERSION = "${env.JDK_VERSION}"
        def postgresContainer="pgcontainer"

            try {

                stage ('Git Checkout') {
                    checkout scmGit(branches: [[name: GitBranchSource]], extensions: [], userRemoteConfigs: [[credentialsId: GithubCreds, url: GitUrlSource]])
                }

                stage ('podman db') {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
                        sh """
                            podman login -u \$REGISTRY_USER -p \$REGISTRY_PASSWORD docker.io
                        """
                    }
                    sh "podman rm -f ${postgresContainer} || true"
                    env.postgresContainer=postgresContainer
                    withCredentials([string(credentialsId: 'postgresAdminPass', variable: 'PG_PASS')]) {
                        sh """
                            podman run -d \
                            --name ${postgresContainer} \
                            -e POSTGRES_PASSWORD=\$PG_PASS \
                            -p 5432:5432 \
                            postgres:16-alpine
                        """
                    }
                    def maxRetries = 4
                    def waitTime = 30
                    def retryCount = 0
                    def isReady = false
                
                    while (retryCount < maxRetries && !isReady) {
                        retryCount++
                        echo "Попытка проверки готовности PostgreSQL (#${retryCount})"
                    
                        isReady = sh(
                            script: "podman exec ${postgresContainer} pg_isready -U postgres && echo 'READY' || echo 'NOT_READY'",
                            returnStdout: true
                        ).trim() == 'READY'
                    
                        if (!isReady && retryCount < maxRetries) {
                            sleep(waitTime)
                        }
                    }
                
                    if (!isReady) {
                        error "PostgreSQL не готов после ${maxRetries} попыток проверки"
                    }
                
                    echo "PostgreSQL готов к подключениям"

                    withCredentials([string(credentialsId: 'postgresUserDb', variable: 'USER_DB'),string(credentialsId: 'postgresAdminPass', variable: 'PG_PASS')]) {
                        sh """
                            podman exec ${postgresContainer} bash -c \
                            "PGPASSWORD=\${PG_PASS} psql -U postgres -c \\"
                                CREATE USER \${USER_DB} WITH PASSWORD '\${USER_DB}';
                                CREATE DATABASE ${USER_DB} OWNER \${USER_DB};
                                GRANT ALL PRIVILEGES ON DATABASE ${USER_DB} TO \${USER_DB};
                            \\""
                        """

                        sh """
                            podman exec ${postgresContainer} bash -c \
                            "PGPASSWORD=\${PG_PASS} psql -U postgres -d ${USER_DB} -c \\"
                                GRANT ALL ON SCHEMA public TO \${USER_DB};
                            \\""
                        """
                    }
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
                //stage('Deliver') {
                //        sh './jenkins/scripts/deliver.sh'
                //}

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