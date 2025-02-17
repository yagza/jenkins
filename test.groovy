timestamps {
    node(JenkinsSlaveNode) {
/*
    environment {
        ENV_INSIDE_NODE_1 = 'This is the value of ENV_INSIDE_NODE_1'
        ENV_INSIDE_NODE_2 = env.ENV_FROM_PIPE_2
    }
*/
        try {

            script {
                env.ENV_INSIDE_NODE_1 = 'This is the value of ENV_INSIDE_NODE_1'
                env.ENV_INSIDE_NODE_2 = env.ENV_FROM_PIPE_2
            }

            stage ("Print external defined envs") {
                println(ENV_FROM_PIPE_1)
                println(env.ENV_FROM_PIPE_2)
            }

            stage ("Print inside defined envs") {
                echo "${ENV_INSIDE_NODE_2}"
                echo "${ENV_INSIDE_NODE_1}"
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