from __future__ import annotations

from app.domain.enums.issue_family import IssueFamily


FAMILY_RULES = {
    'external_ruff:ruff:F841': IssueFamily.UNUSED_VARIABLE,
    'python:S1481': IssueFamily.UNUSED_VARIABLE,
    'external_ruff:ruff:F401': IssueFamily.UNUSED_IMPORT,
    'external_ruff:ruff:S324': IssueFamily.INSECURE_HASH,
    'external_semgrep:semgrep:python.lang.security.insecure-hash-algorithms-md5.insecure-hash-algorithm-md5': IssueFamily.INSECURE_HASH,
    'external_ruff:ruff:S608': IssueFamily.SQL_INJECTION,
    'external_ruff:ruff:S105': IssueFamily.HARDCODED_SECRET,
    'external_ruff:ruff:S307': IssueFamily.UNSAFE_EVAL,
    'external_semgrep:semgrep:python.lang.security.audit.eval-detected.eval-detected': IssueFamily.UNSAFE_EVAL,
    'external_ruff:ruff:S113': IssueFamily.REQUEST_WITHOUT_TIMEOUT,
    'external_semgrep:semgrep:dockerfile.security.missing-user.missing-user': IssueFamily.CONTAINER_RUNS_AS_ROOT,
    'external_ruff:ruff:I001': IssueFamily.IMPORT_FORMATTING,
    'external_ruff:ruff:E501': IssueFamily.STYLE,
    'external_ruff:ruff:W291': IssueFamily.STYLE,
    'external_ruff:ruff:W292': IssueFamily.STYLE,
}


def map_issue_family(rule: str, engine: str, component: str) -> IssueFamily:
    if rule in FAMILY_RULES:
        return FAMILY_RULES[rule]
    if engine == 'trivy':
        return IssueFamily.DEPENDENCY_VULNERABILITY
    return IssueFamily.UNKNOWN
