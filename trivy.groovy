timestamps {
    node(JenkinsSlaveNode) {

        def imageName = "yagza/shadowsocks_v2ray"
        //def trivyReport = "${env.imageName}.replaceAll('/','_')"
        def trivyReport = "shadowsocks_v2ray.html"

        try {
            stage ("trivy scan") {
                sh "trivy image --format template --template \"@/usr/local/bin/contrib/html.tpl\" -o ${trivyReport} ${imageName}"
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