timestamps {
  node(JenkinsSlaveNode) {

    try {

        def secrets = [
          [path: 'team/btccpl', engineVersion: 2, secretValues: [
            [envVar: 'SECRET_1', vaultKey: 'some_name'],
            [envVar: 'SECRET_2', vaultKey: 'team_name'],
            [envVar: 'SECRET_3', vaultKey: 'ssh_name']]
          ],
          [path: 'secret/ssh-key', engineVersion: 2, secretValues: [
            [envVar: 'githubcreads', vaultKey: 'github']]
          ]
        ]

        def vault_configuration = [vaultUrl: 'http://10.0.0.150:8200', vaultCredentialId: 'btccpl-read-vault', engineVersion: 2]      
      
      stage ("vault secret print") {
          
        withVault([configuration: vault_configuration, vaultSecrets: secrets]) {
          sh 'echo $SECRET_1'
          sh 'echo $SECRET_2'
          sh 'echo $SECRET_3'
        }
      }
    
      stage ('Git Checkout using secret from vault') {
        withVault([configuration: vault_configuration, vaultSecrets: secrets]) {
          writeFile(file: 'id_rsa', text: githubcreads)
            sh '''
            mkdir -p ~/.ssh
            mv id_rsa ~/.ssh/id_rsa
            chmod 600 ~/.ssh/id_rsa
            ssh-keyscan github.com >> ~/.ssh/known_hosts
            chmod 600 ~/.ssh/known_hosts
            git clone git@github.com:yagza/simple-php-website.git
            ls -la simple-php-website
            '''
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
