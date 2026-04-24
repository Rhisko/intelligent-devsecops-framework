<!-- Analyze these critical findings in depth for {{ project_name }}. -->
Analyze the following critical findings for project {{ project_name }}.

Return a structured assessment for each finding so the result can be converted into a developer action table.

For each finding, determine:
- short title
- exploitability
- blast radius
- confidence
- remediation priority
- whether it should block release
- concrete fix recommendation
- developer next action
- short rationale

Rules:
- do not merge unrelated findings
- do not omit any critical finding from the provided input
- keep each field concise and operational
- if the same root cause appears multiple times, keep the findings separate unless the input already groups them
- if the evidence is insufficient, say so explicitly and reduce confidence

