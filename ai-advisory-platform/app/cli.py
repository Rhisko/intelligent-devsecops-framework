from __future__ import annotations

import argparse

from app.shared.constants import DEFAULT_ANALYSIS_MODE, DEFAULT_OUTPUT_FORMAT, SUPPORTED_ANALYSIS_MODES


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description='AI advisory CLI for SonarQube findings')
    parser.add_argument('--project-key', required=True, help='SonarQube project key')
    parser.add_argument('--analysis-mode', default=DEFAULT_ANALYSIS_MODE, choices=sorted(SUPPORTED_ANALYSIS_MODES))
    parser.add_argument('--sonar-url', default=None, help='Override SonarQube URL')
    parser.add_argument('--report-dir', default=".", help='Override directory for report output')
    parser.add_argument('--output-file', default=None, help='Write output JSON to file')
    parser.add_argument('--format', default=DEFAULT_OUTPUT_FORMAT, choices=['json'])

    return parser

