timestamps {
    node(JenkinsSlaveNode) {

        try {
            stage ("trivy scan") {
                sh "trivy image ${trivyOptions} -o ${trivyReport} ${imageName}"
            }

            stage('Archive Report') {
                archiveArtifacts artifacts: trivyReport, fingerprint: true
            }

            stage('Notify') {
                echo "Trivy scan completed. Report is available as an artifact."
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