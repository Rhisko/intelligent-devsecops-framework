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
- do not omit any finding from payload.critical_findings
- return exactly payload.input_counts.critical_findings rows in the findings array
- do not limit the output to 10 findings
- keep each field concise and operational
- if the same root cause appears multiple times, keep the findings separate unless the input already groups them
- if the evidence is insufficient, say so explicitly and reduce confidence
