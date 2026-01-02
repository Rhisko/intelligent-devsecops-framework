package devsecops

class DockerRunner implements Serializable {

    def steps

    DockerRunner(steps) {
        this.steps = steps
    }

    /**
     * Generic container runner
     *
     * @param image Docker image
     * @param command Command executed inside container
     * @param env Environment variables (optional)
     * @param volumes Extra volume mounts (optional)
     */
    int run(
        String image,
        String command,
        Map<String, String> env = [:],
        List<String> volumes = []
        boolean verify = false
    ) {

        def envArgs = env.collect { k, v -> "-e ${k}=${v}" }.join(' ')
        def volArgs = volumes.collect { v -> "-v ${v}" }.join(' ')
        // Optional verification block (make sure mount is correct)
        def verifyCmd = verify ? """
          echo "[VERIFY] PWD: \$(pwd)";
          echo "[VERIFY] Listing:";
          ls -la;
          echo "[VERIFY] Python files:";
          find . -name '*.py' -print || true;
        """ : ""


        return steps.sh(
            script: """
              docker run --rm \
                -v "\${WORKSPACE}:/workspace:ro" \
                ${volArgs} \
                ${envArgs} \
                -w /workspace \
                ${image} \
                sh -c '${verifyCmd} ${command}'
            """.stripIndent(),
            returnStatus: true
        )
    }
}
