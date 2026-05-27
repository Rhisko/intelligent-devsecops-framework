import java.net.URLEncoder

def call(Map config = [:]) {
    def chatId = config.chat_id ?: error("[sendToTelegram] 'chat_id' is required")
    def parseMode = config.parse_mode ?: "HTML"
    def disablePreview = config.get("disable_web_page_preview", true)
    def disableNotification = config.get("disable_notification", false)
    def credentialsId = config.credentials_id ?: "telegram-bot-token"

    def message = config.message ?: buildAiAdvisoryMessage(config)

    withCredentials([
        string(credentialsId: credentialsId, variable: 'TELEGRAM_BOT_TOKEN')
    ]) {
        def encodedMessage = URLEncoder.encode(message, "UTF-8")
        def encodedParseMode = URLEncoder.encode(parseMode, "UTF-8")
        def url = "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage"

        def response = sh(
            script: """
                set -euo pipefail

                curl -sS -X POST "${url}" \\
                  -H "Content-Type: application/x-www-form-urlencoded" \\
                  --data "chat_id=${chatId}" \\
                  --data "text=${encodedMessage}" \\
                  --data "parse_mode=${encodedParseMode}" \\
                  --data "disable_web_page_preview=${disablePreview}" \\
                  --data "disable_notification=${disableNotification}"
            """,
            returnStdout: true
        ).trim()

        return response
    }
}

private String buildAiAdvisoryMessage(Map config) {
    def project = escapeHtml(config.project ?: "-")
    def overallRiskRaw = String.valueOf(config.overall_risk ?: "-")
    def exploitationRiskRaw = String.valueOf(config.exploitation_risk ?: "-")
    def releaseRecommendationRaw = String.valueOf(config.release_recommendation ?: "-")
    def executiveSummary = escapeHtml(config.executive_summary ?: "-")

    def overallRisk = "${riskEmoji(overallRiskRaw)} ${escapeHtml(overallRiskRaw)}"
    def exploitationRisk = "${riskEmoji(exploitationRiskRaw)} ${escapeHtml(exploitationRiskRaw)}"
    def releaseRecommendation = "${releaseEmoji(releaseRecommendationRaw)} ${escapeHtml(releaseRecommendationRaw)}"

    def reportUrl = resolveReportUrl(config)
    def jobName = escapeHtml(config.job_name ?: "")
    def buildNumber = escapeHtml(config.build_number ?: "")
    def environment = escapeHtml(config.environment ?: "")
    def timestamp = escapeHtml(config.timestamp ?: "")

    def reportSection = buildReportSection(reportUrl)
    def buildSection = buildBuildSection(jobName, buildNumber, environment, timestamp)

    return """
🚨 <b>INTELLIGENT DEVSECOPS FRAMEWORK</b>
<i>AI-Driven Security Advisory</i>

━━━━━━━━━━━━━━━━━━
<b>📦 PROJECT</b>
━━━━━━━━━━━━━━━━━━
<code>${project}</code>

━━━━━━━━━━━━━━━━━━
<b>🛡️ RISK OVERVIEW</b>
━━━━━━━━━━━━━━━━━━
<b>Overall Risk</b>          : <code>${overallRisk}</code>
<b>Exploitation Risk</b>     : <code>${exploitationRisk}</code>
<b>Release Recommendation</b>: <code>${releaseRecommendation}</code>

━━━━━━━━━━━━━━━━━━
<b>📝 EXECUTIVE SUMMARY</b>
━━━━━━━━━━━━━━━━━━
${executiveSummary}

${buildSection ? buildSection + "\n\n" : ""}${reportSection ? reportSection + "\n\n" : ""}<i>Generated automatically by CI/CD AI Advisory Pipeline.</i>
""".stripIndent().trim()
}

private String buildReportSection(String reportUrl) {
    if (!reportUrl) {
        return """
━━━━━━━━━━━━━━━━━━
<b>🔗 REPORT ACCESS</b>
━━━━━━━━━━━━━━━━━━
<code>Report URL not available</code>
""".trim()
    }

    return """
━━━━━━━━━━━━━━━━━━
<b>🔗 REPORT ACCESS</b>
━━━━━━━━━━━━━━━━━━
<a href="${escapeHtmlAttribute(reportUrl)}">Click here to view the full advisory report</a>
""".trim()
}

private String buildBuildSection(String jobName, String buildNumber, String environment, String timestamp) {
    if (!(jobName || buildNumber || environment || timestamp)) {
        return ""
    }

    return """
━━━━━━━━━━━━━━━━━━
<b>🏗️ BUILD INFORMATION</b>
━━━━━━━━━━━━━━━━━━
${jobName ? "<b>Job</b>          : <code>${jobName}</code>\n" : ""}${buildNumber ? "<b>Build Number</b> : <code>${buildNumber}</code>\n" : ""}${environment ? "<b>Environment</b>  : <code>${environment}</code>\n" : ""}${timestamp ? "<b>Timestamp</b>    : <code>${timestamp}</code>" : ""}
""".trim()
}

private String normalizeUrl(Object value) {
    def raw = String.valueOf(value ?: "").trim()
    if (!raw) {
        return ""
    }

    if (!(raw.startsWith("http://") || raw.startsWith("https://"))) {
        return ""
    }

    return raw.replace(" ", "%20")
}

private String resolveReportUrl(Map config) {
    def reportUrl = normalizeUrl(config.report_url)
    def reportPath = normalizeReportPath(config.report_path ?: config.report_path_file)

    if (reportUrl && reportPath) {
        return joinUrl(reportUrl, reportPath)
    }

    if (reportUrl) {
        return reportUrl
    }

    if (reportPath) {
        return joinUrl("http://localhost", reportPath)
    }

    return ""
}

private String normalizeReportPath(Object value) {
    def raw = String.valueOf(value ?: "").trim()
    if (!raw) {
        return ""
    }

    return raw
        .replaceFirst('^/report/', '')
        .replaceFirst('^report/', '')
        .replace(" ", "%20")
}

private String joinUrl(String baseUrl, String path) {
    def cleanBase = String.valueOf(baseUrl ?: "").replaceAll('/+$', '')
    def cleanPath = String.valueOf(path ?: "").replaceAll('^/+', '')

    return "${cleanBase}/${cleanPath}"
}

private String riskEmoji(String value) {
    switch ((value ?: "").toUpperCase()) {
        case "CRITICAL":
            return "🔴"
        case "HIGH":
            return "🟠"
        case "MEDIUM":
            return "🟡"
        case "LOW":
            return "🟢"
        default:
            return "⚪"
    }
}

private String releaseEmoji(String value) {
    switch ((value ?: "").toUpperCase()) {
        case "HOLD":
            return "⛔"
        case "BLOCK":
            return "🚫"
        case "GO":
            return "✅"
        case "REVIEW":
            return "🧐"
        default:
            return "⚪"
    }
}

private String escapeHtml(Object value) {
    return String.valueOf(value ?: "")
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}

private String escapeHtmlAttribute(Object value) {
    return escapeHtml(value).replace("\"", "&quot;")
}
