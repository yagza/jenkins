timestamps {
  node(JenkinsSlaveNode) {

    try {
      stage ("vault secret print") {
        def secrets = [
          [path: 'secret/testing', engineVersion: 2, secretValues: [
            [envVar: 'SECRET_1', vaultKey: 'secret1'],
            [envVar: 'SECRET_2', vaultKey: 'secret2']]
          ]
        ]

        def vault_configuration = [vaultUrl: 'http://10.0.0.150:8200', vaultCredentialId: 'btccpl-read-vault', engineVersion: 2]
    
        withVault([configuration: vault_configuration, vaultSecrets: secrets]) {
          sh 'echo $SECRET_1'
          sh 'echo $SECRET_2'
        }
      }
    }

    catch (Exception e) {
      echo "Pipeline failed: ${e.getMessage()}"
      currentBuild.result = 'FAILURE'
      throw e 
    } finally {
      println("Очистка Jenkins Slave Node")
      cleanWs()
    }
  }
}
