class AdvisoryError(Exception):
    """Base exception for the advisory platform."""


class ConfigurationError(AdvisoryError):
    """Raised when required configuration is missing or invalid."""


class SonarAPIError(AdvisoryError):
    """Raised when SonarQube API calls fail."""


class ValidationError(AdvisoryError):
    """Raised when CLI or payload validation fails."""
