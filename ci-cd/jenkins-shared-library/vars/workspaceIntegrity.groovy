/**
 * Workspace Integrity Guard
 * - Ensures Jenkins workspace is not modified during CI execution
 * - Uses git status porcelain for deterministic comparison
 */

def capture() {
    return sh(
        script: "git status --porcelain",
        returnStdout: true
    ).trim()
}

def assertUnchanged(String beforeState) {
    def afterState = sh(
        script: "git status --porcelain",
        returnStdout: true
    ).trim()

    if (beforeState != afterState) {
        error("""
Workspace integrity violation detected.
The following changes were introduced during CI execution:

${afterState}
""")
    }
    else {
        echo "[WORKSPACE INTEGRITY] No changes detected in workspace."
    }
}
