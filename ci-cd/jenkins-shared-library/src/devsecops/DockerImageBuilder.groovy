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

    void buildAndPush(Map cfg = [:]) {

        if (!cfg.image) {
            steps.error("buildAndPush requires 'image' (e.g. org/service)")
        }

        // === Registry ===
        def registryKey = cfg.registry ?: dockerCfg.registry.default
        def registryCfg = dockerCfg.registry[registryKey]
        if (!registryCfg) {
            steps.error("Registry '${registryKey}' not defined in docker-build.yaml")
        }

        def tag = cfg.tag ?: "latest"
        def fullImage = "${registryCfg.url}/${cfg.image}:${tag}"

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

        steps.echo "[BUILD] Image      : ${fullImage}"
        steps.echo "[BUILD] Context    : ${context}"
        steps.echo "[BUILD] Dockerfile : ${dockerfile}"

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
        steps.withCredentials([
            steps.usernamePassword(
                credentialsId: registryCfg.credential_id,
                usernameVariable: 'REG_USER',
                passwordVariable: 'REG_PASS'
            )
        ]) {
            steps.sh """
              echo \$REG_PASS | docker login ${registryCfg.url} \
                -u \$REG_USER --password-stdin
              docker push ${fullImage}
            """
        }

        steps.echo "[BUILD] Image pushed successfully: ${fullImage}"

        // === Optional: return reference ===
        steps.env.PUBLISHED_IMAGE = fullImage
    }
}
