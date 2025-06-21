def getSecretsFromKeyVault(stage, vaultSuffix) {
    def vaultName = "jfsandbox"

    def secretNamesRaw = sh(
        script: """
        az keyvault secret list --vault-name ${vaultName} --query '[].name' --output tsv |
        grep '^aks-' |
        grep -v 'tls-crt' |
        grep -v 'tls-key' || true
        """,
        returnStdout: true
    ).trim()

    def secretNames = secretNamesRaw ? secretNamesRaw.split("\n") : []
    def secretValues = [:]

    for (secretName in secretNames) {
        def rawValue = sh(
            script: "az keyvault secret show --vault-name ${vaultName} --name ${secretName} --query value --output tsv",
            returnStdout: true
        ).trim()

        // Szukamy nowego formatu "key: value"
        def parts = rawValue.split(":", 2)
        if (parts.length == 2) {
            def actualKey = parts[0].trim()
            def actualValue = parts[1].trim()
            secretValues[actualKey] = actualValue
        } else {
            // fallback: stary styl – klucz na podstawie nazwy secreta
            def helmSecretName = secretName.replaceFirst('^aks-', '').replaceAll('-', '_')
            secretValues[helmSecretName] = rawValue
        }
    }

    return secretValues
}
