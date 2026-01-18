/**
 * Consolidate multiple Sonar external payloads
 * Each payload must be: [rules: List, issues: List]
 */
Map call(List<Map> payloads) {

    Map<String, Map> rulesIndex = [:]
    List<Map> issues = []

    payloads.each { payload ->

        if (!payload?.rules || !payload?.issues) {
            return
        }

        // ---- RULES (deduplicate by id) ----
        payload.rules.each { r ->
            if (r?.id && !rulesIndex.containsKey(r.id)) {
                rulesIndex[r.id] = r
            }
        }

        // ---- ISSUES (append as-is) ----
        payload.issues.each { i ->
            issues << i
        }
    }

    return [
        rules : rulesIndex.values().toList(),
        issues: issues
    ]
}
