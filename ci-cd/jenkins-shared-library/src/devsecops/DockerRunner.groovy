// package devsecops

// class DockerRunner implements Serializable {

//     def steps
//     def configLoader
//     def dockerConfig   // lazy-loaded

//     DockerRunner(steps) {
//         this.steps = steps
//         this.configLoader = new ConfigLoader(steps)
//     }

//     /**
//      * Load docker config from resources (CPS-safe)
//      */
//     private void ensureDockerConfigLoaded() {
//         if (dockerConfig) return

//         dockerConfig = configLoader.load("docker-config").docker

//         if (!dockerConfig?.base_host_path) {
//             steps.error("docker-config.yaml missing docker.base_host_path")
//         }
//         if (!dockerConfig.base_host_path.startsWith('/')) {
//             steps.error("docker.base_host_path must be absolute")
//         }
//         if (!dockerConfig?.workspace_mount?.container_path) {
//             steps.error("docker-config.yaml missing docker.workspace_mount.container_path")
//         }
//     }
//     /**
//      * Run a command inside a Docker container
//      *
//      * @param workDir   Path inside host machine to mount into container
//      * @param image     Docker image to use
//      * @param command   Command to run inside container
//      * @param env       Map<String, String> of environment variables to set inside container
//      * @param volumes   List<String> of additional volume mounts (host_path:container_path:mode)
//      *
//      * @return Exit code of docker run command
//      */
//     int run(
//         String workDir,
//         String image,
//         String command,
//         Map<String, String> env = [:],
//         List<String> volumes = []
//     ) {
//         ensureDockerConfigLoaded()

//         // === HOST PATH (ABSOLUTE) ===
//         def hostDir = "${dockerConfig.base_host_path}${workDir}"

//         // === CONTAINER PATH ===
//         def networkName = dockerConfig.network ?: "default"
//         def containerPath = dockerConfig.workspace_mount.container_path
//         def mountMode     = dockerConfig.workspace_mount.mode ?: "rw"

//         def envArgs = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
//         def volArgs = volumes.collect { v -> "-v ${v}" }.join(' ')

//         return steps.sh(
//             script: """
//               docker run --rm \
//                 --network ${networkName} \
//                 -v "${hostDir}:${containerPath}:${mountMode}" \
//                 ${volArgs} \
//                 ${envArgs} \
//                 -w ${containerPath} \
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
    def dockerConfig

    DockerRunner(steps) {
        this.steps = steps
        this.configLoader = new ConfigLoader(steps)
    }

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

    private String buildDockerCommand(
        String workDir,
        String image,
        String command,
        Map<String, String> env = [:],
        List<String> volumes = [],
        boolean mountWorkspace = true,
        String workingDir = null,
        String network = null
    ) {
        ensureDockerConfigLoaded()

        def networkName = network ?: dockerConfig.network ?: "default"
        def containerPath = dockerConfig.workspace_mount.container_path
        def mountMode = dockerConfig.workspace_mount.mode ?: "rw"

        def mountArgs = []

        if (mountWorkspace) {
            if (!workDir?.trim()) {
                steps.error("DockerRunner requires workDir when mountWorkspace=true")
            }

            def hostDir = "${dockerConfig.base_host_path}${workDir}"
            mountArgs << "-v \"${hostDir}:${containerPath}:${mountMode}\""
        }

        if (volumes) {
            mountArgs.addAll(volumes.collect { v -> "-v ${v}" })
        }

        def envArgs = env.collect { k, v -> "-e ${k}='${escapeSingleQuotes(v)}'" }.join(' ')
        def volArgs = mountArgs.join(' ')
        def finalWorkingDir = workingDir ?: (mountWorkspace ? containerPath : null)
        def workDirArg = finalWorkingDir ? "-w ${finalWorkingDir}" : ""

        return """
          docker run --rm \
            --network ${networkName} \
            ${volArgs} \
            ${envArgs} \
            ${workDirArg} \
            ${image} ${command}
        """.stripIndent().trim()
    }

    int run(
        String workDir,
        String image,
        String command,
        Map<String, String> env = [:],
        List<String> volumes = [],
        boolean mountWorkspace = true,
        String workingDir = null,
        String network = null
    ) {
        def dockerCommand = buildDockerCommand(
            workDir,
            image,
            command,
            env,
            volumes,
            mountWorkspace,
            workingDir,
            network
        )

        return steps.sh(
            script: dockerCommand,
            returnStatus: true
        )
    }

    String runAndCapture(
        String workDir,
        String image,
        String command,
        Map<String, String> env = [:],
        List<String> volumes = [],
        boolean mountWorkspace = true,
        String workingDir = null,
        String network = null
    ) {
        def dockerCommand = buildDockerCommand(
            workDir,
            image,
            command,
            env,
            volumes,
            mountWorkspace,
            workingDir,
            network
        )

        return steps.sh(
            script: dockerCommand,
            returnStdout: true
        ).trim()
    }

    private static String escapeSingleQuotes(String value) {
        return (value ?: "").replace("'", "'\"'\"'")
    }
}