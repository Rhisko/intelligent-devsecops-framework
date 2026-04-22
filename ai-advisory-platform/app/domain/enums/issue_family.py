from enum import StrEnum


class IssueFamily(StrEnum):
    UNUSED_VARIABLE = "unused_variable"
    UNUSED_IMPORT = "unused_import"
    INSECURE_HASH = "insecure_hash"
    SQL_INJECTION = "sql_injection"
    HARDCODED_SECRET = "hardcoded_secret"
    UNSAFE_EVAL = "unsafe_eval"
    REQUEST_WITHOUT_TIMEOUT = "request_without_timeout"
    CONTAINER_RUNS_AS_ROOT = "container_runs_as_root"
    DEPENDENCY_VULNERABILITY = "dependency_vulnerability"
    IMPORT_FORMATTING = "import_formatting"
    STYLE = "style"
    UNKNOWN = "unknown"
