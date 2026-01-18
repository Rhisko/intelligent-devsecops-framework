#!/usr/bin/env groovy
import groovy.json.JsonOutput

/**
 * ExternalIssuesPublisher
 *
 * Jenkins Shared Library entry point
 * Publishes external issues (Ruff, Semgrep, Trivy) to SonarQube format
 *
 * Usage example:
 *   ExternalIssuesPublisher(
 *     tool: "ruff",
 *     input: "ruff.json"
 *   )
 */
def call(Map args = [:]) {

    // -------------------------------
    // 1. Validate arguments
    // -------------------------------
    String tool      = args.tool ?: error("Parameter 'tool' is required (ruff | semgrep | trivy)")
    String inputFile = args.input ?: error("Parameter 'input' (json file) is required")

    if (!fileExists(inputFile)) {
        error "[ExternalIssuesPublisher] Input file not found: ${inputFile}"
    }

    echo "[ExternalIssuesPublisher] Tool  : ${tool}"
    echo "[ExternalIssuesPublisher] Input : ${inputFile}"

    // -------------------------------
    // 2. Load findings
    // -------------------------------
    def raw = readJSON(file: inputFile)

    // Normalize findings list (tool-specific)
    def findings = normalizeFindings(tool, raw)

    echo "[ExternalIssuesPublisher] Findings count: ${findings.size()}"

    // -------------------------------
    // 3. Dispatch to mapper
    // -------------------------------
    Map sonarPayload

    switch (tool) {

        case "ruff":
            sonarPayload = devsecops.RuffExternalSonarMapper
                .toSonar(findings)
            break

        case "semgrep":
            sonarPayload = devsecops.SemgrepExternalSonarMapper
                .toSonar(findings)
            break

        // case "trivy":
        //     sonarPayload = devsecops.trivy.TrivyExternalSonarMapper
        //         .toSonar(findings)
        //     break

        default:
            error "[ExternalIssuesPublisher] Unsupported tool: ${tool}"
    }

    echo "[ExternalIssuesPublisher] Sonar rules : ${sonarPayload.rules.size()}"
    echo "[ExternalIssuesPublisher] Sonar issues: ${sonarPayload.issues.size()}"

    return sonarPayload
}

/**
 * Normalize findings structure per tool
 * So mapper always receives List<Map>
 */
private List normalizeFindings(String tool, def raw) {

    switch (tool) {

        case "ruff":
            // Ruff JSON is already a list
            return raw instanceof List ? raw : []

        case "semgrep":
            // Semgrep wraps findings under "results"
            return raw?.results ?: []

        case "trivy":
            // Trivy vulnerabilities usually nested
            return raw?.Results ?: []

        default:
            return []
    }
}
