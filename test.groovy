timestamps {
    node(JenkinsSlaveNode) {
    environment {
        ENV_INSIDE_NODE_1 = "This is the value of ENV_INSIDE_NODE_1"
        ENV_INSIDE_NODE_2 = env.ENV_FROM_PIPE_2
    }
        try {
            println("These are external defined envs")
            println(ENV_FROM_PIPE_1)
            println(env.ENV_FROM_PIPE_2)
            println("These are inside defined envs")
            echo"${ENV_INSIDE_NODE_1}"
            echo"${ENV_INSIDE_NODE_2}"
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