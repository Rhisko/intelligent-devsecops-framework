// package devsecops

// class DockerRunner implements Serializable {

//     def steps
    
    
//  // === KONSTANTA BASE PATH (ABSOLUTE) ===
//     static final String BASE_HOST_PATH =
//         "/Users/risko/Data/tools/jenkins_data"
//     DockerRunner(steps) {
//         this.steps = steps
//     }

//     /**
//      * Generic container runner
//      *
//      * @param image Docker image
//      * @param command Command executed inside container
//      * @param env Environment variables (optional)
//      * @param volumes Extra volume mounts (optional)
//      */
//     int run(
//         String workDir,
//         String image,
//         String command,
//         Map<String, String> env = [:],
//         List<String> volumes = []
   
//     ) {
//         def envArgs = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
//         def volArgs = volumes.collect { v -> "-v ${v}" }.join(' ')
//         def hostDir = "${BASE_HOST_PATH}/${workDir}"

//         return steps.sh(
//             script: """
//               docker run --rm \
//                 -v "${hostDir}:/ci-workspace:ro" \
//                 ${volArgs} \
//                 ${envArgs} \
//                 -w /ci-workspace \
//                 ${image} ${command}
//             """.stripIndent(),
//             returnStatus: true
//         )
//     }
// }

package devsecops

class DockerRunner implements Serializable {

    def steps
    def configLoader
    def dockerConfig   // lazy-loaded

    DockerRunner(steps) {
        this.steps = steps
        this.configLoader = new ConfigLoader(steps)
    }

    /**
     * Load docker config from resources (CPS-safe)
     */
    private void ensureDockerConfigLoaded() {
        if (dockerConfig) return

        dockerConfig = configLoader.load("docker-config").docker

        if (!dockerConfig?.base_host_path) {
            steps.error("docker-config.yaml missing docker.base_host_path")
        }
        if (!dockerConfig.base_host_path.startsWith('/')) {
            steps.error("docker.base_host_path must be absolute")
        }
        if (!dockerConfig?.workspace_mount?.container_path) {
            steps.error("docker-config.yaml missing docker.workspace_mount.container_path")
        }
    }

    /**
     * Generic container runner
     *
     * @param workDir   Relative directory under base_host_path
     * @param image     Docker image
     * @param command   Command executed inside container
     * @param env       Environment variables (optional)
     * @param volumes   Extra volume mounts (optional)
     */
    int run(
        String workDir,
        String image,
        String command,
        Map<String, String> env = [:],
        List<String> volumes = []
    ) {
        ensureDockerConfigLoaded()

        // === HOST PATH (ABSOLUTE) ===
        def hostDir = "${dockerConfig.base_host_path}/${workDir}"

        // === CONTAINER PATH ===
        def containerPath = dockerConfig.workspace_mount.container_path
        def mountMode     = dockerConfig.workspace_mount.mode ?: "rw"

        def envArgs = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
        def volArgs = volumes.collect { v -> "-v ${v}" }.join(' ')

        return steps.sh(
            script: """
              docker run --rm \
                -v "${hostDir}:${containerPath}:${mountMode}" \
                ${volArgs} \
                ${envArgs} \
                -w ${containerPath} \
                ${image} ${command}
            """.stripIndent(),
            returnStatus: true
        )
    }
}
