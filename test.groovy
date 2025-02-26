timestamps {
    node(JenkinsSlaveNode) {

        withEnv(['ENV_INSIDE_NODE_1="This is the value of ENV_INSIDE_NODE_1"',
             "ENV_INSIDE_NODE_2=${env.ENV_FROM_PIPE_2}"]) {

            try {

                stage ("Print external defined envs") {
                    println(ENV_FROM_PIPE_1)
                    println(env.ENV_FROM_PIPE_2)
                }

                stage ("Print inside defined envs") {
                    echo "${ENV_INSIDE_NODE_1}"
                    echo "${ENV_INSIDE_NODE_2}"
                }

                stage ("Print active choise parameters") {
                    echo "the zone is the following ${ZONE}"
                    echo "the food will be ${FOOD}"
                }

                stage ("Chto tam po tely") {
                    echo "${ZONE} with the ${BODY}"
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