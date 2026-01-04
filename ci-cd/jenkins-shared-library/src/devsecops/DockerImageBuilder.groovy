package devsecops

class DockerImageBuilder implements Serializable {

    def steps
    def cfgLoader
    def dockerCfg   // lazy-loaded

    DockerImageBuilder(steps) {
        this.steps = steps
        this.cfgLoader = new ConfigLoader(steps)
    }

    private void ensureConfigLoaded() {
        if (dockerCfg != null) {
            return
        }

        dockerCfg = cfgLoader.load("docker-build").docker

        if (!dockerCfg?.registry) {
            steps.error("docker-build.yaml missing 'docker.registry'")
        }
        if (!dockerCfg.registry?.default) {
            steps.error("docker-build.yaml missing 'docker.registry.default'")
        }
    }

    void buildAndPush(Map cfg = [:]) {

        // === ENSURE CONFIG LOADED ===
        ensureConfigLoaded()

        if (!cfg.image) {
            steps.error("buildAndPush requires 'image'")
        }

        // === Resolve registry ===
        def registryKey = cfg.registry ?: dockerCfg.registry.default
        def registryCfg = dockerCfg.registry[registryKey]

        if (!registryCfg) {
            steps.error("Registry '${registryKey}' not defined in docker-build.yaml")
        }

        def tag = cfg.tag ?: "latest"
        def imagePath = registryCfg.image_prefix
            .replace('{image}', cfg.image)
            .replace('{username}', steps.env.DOCKERHUB_USER ?: '')
            .replace('{owner}', steps.env.GITHUB_OWNER ?: '')

        def fullImage = "${registryCfg.server}/${imagePath}:${tag}"

        // === Build options ===
        def context    = cfg.context    ?: dockerCfg.defaults.context
        def dockerfile = cfg.dockerfile ?: dockerCfg.defaults.dockerfile
        def target     = cfg.target
        def buildArgs  = cfg.buildArgs  ?: [:]
        def labels     = cfg.labels     ?: dockerCfg.defaults.labels ?: [:]

        def buildArgStr = buildArgs.collect { k, v -> "--build-arg ${k}=${v}" }.join(' ')
        def targetStr   = target ? "--target ${target}" : ""
        def labelStr    = labels.collect { k, v -> "--label ${k}=${v}" }.join(' ')

        steps.echo "[BUILD] Image: ${fullImage}"

        // === Login ===
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

        // === Build & Push ===
        steps.sh """
          docker build \
            -f ${dockerfile} \
            ${targetStr} \
            ${buildArgStr} \
            ${labelStr} \
            -t ${fullImage} \
            ${context}

          docker push ${fullImage}
        """

        steps.env.PUBLISHED_IMAGE = fullImage
    }
}
