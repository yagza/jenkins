timestamps {
  node(JenkinsSlaveNode) {

    try {
      stage ("vault secret print") {
        def secrets = [
          [path: '/v1/team/btccpl/data/some_secret', engineVersion: 2, secretValues: [
            [envVar: 'SECRET_1', vaultKey: 'some_name'],
            [envVar: 'SECRET_2', vaultKey: 'team_name']]
          ]
        ]

        def vault_configuration = [vaultUrl: 'http://10.0.0.150:8200', vaultCredentialId: 'token-btcpl-read-vault', engineVersion: 2]
    
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
