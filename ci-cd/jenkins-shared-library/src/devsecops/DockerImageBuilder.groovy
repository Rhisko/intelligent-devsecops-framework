package devsecops

class DockerImageBuilder implements Serializable {

    def steps
    def cfgLoader
    def dockerCfg

    DockerImageBuilder(steps) {
        this.steps = steps
        this.cfgLoader = new ConfigLoader(steps)
        this.dockerCfg = cfgLoader.load("docker-build").docker
    }

    /**
     * buildAndPush
     * - Registry-agnostic
     * - No hardcoded registry URL
     * - Uses access token via Jenkins credentials
     */
    void buildAndPush(Map cfg = [:]) {

        if (!cfg.image) {
            steps.error("buildAndPush requires 'image' (e.g. payment-service)")
        }

        // === Resolve registry ===
        def registryKey = cfg.registry ?: dockerCfg.registry.default
        def registryCfg = dockerCfg.registry[registryKey]

        if (!registryCfg) {
            steps.error("Registry '${registryKey}' not defined in docker-build.yaml")
        }
        if (!registryCfg.server || !registryCfg.image_prefix || !registryCfg.credential_id) {
            steps.error("Registry '${registryKey}' config incomplete (server, image_prefix, credential_id required)")
        }

        // === Image naming ===
        def tag        = cfg.tag ?: "latest"
        def imagePath  = registryCfg.image_prefix.replace('{image}', cfg.image)
        def fullImage  = "${registryCfg.server}/${imagePath}:${tag}"

        // === Build options (override > default) ===
        def context    = cfg.context    ?: dockerCfg.defaults.context
        def dockerfile = cfg.dockerfile ?: dockerCfg.defaults.dockerfile
        def target     = cfg.target
        def buildArgs  = cfg.buildArgs  ?: [:]
        def labels     = cfg.labels     ?: dockerCfg.defaults.labels ?: [:]

        // === Compose CLI args ===
        def buildArgStr = buildArgs.collect { k, v -> "--build-arg ${k}=${v}" }.join(' ')
        def targetStr   = target ? "--target ${target}" : ""
        def labelStr    = labels.collect { k, v -> "--label ${k}=${v}" }.join(' ')

        steps.echo "[BUILD] Registry   : ${registryKey}"
        steps.echo "[BUILD] Image      : ${fullImage}"
        steps.echo "[BUILD] Context    : ${context}"
        steps.echo "[BUILD] Dockerfile : ${dockerfile}"

        // === Login (registry-agnostic) ===
        steps.withCredentials([
            steps.usernamePassword(
                credentialsId: registryCfg.credential_id,
                usernameVariable: 'REG_USER',
                passwordVariable: 'REG_TOKEN'
            )
        ]) {
            steps.sh """
              echo "\$REG_TOKEN" | docker login ${registryCfg.server} \
                -u "\$REG_USER" --password-stdin
            """
        }

        // === Build ===
        steps.sh """
          docker build \
            -f ${dockerfile} \
            ${targetStr} \
            ${buildArgStr} \
            ${labelStr} \
            -t ${fullImage} \
            ${context}
        """

        // === Push ===
        steps.sh "docker push ${fullImage}"

        steps.echo "[BUILD] Image pushed successfully: ${fullImage}"

        // === Export reference ===
        steps.env.PUBLISHED_IMAGE = fullImage
    }
}
