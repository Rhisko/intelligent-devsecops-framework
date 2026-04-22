from __future__ import annotations

import re
from typing import Any

from app.domain.models.issue import Issue
from app.domain.rules.family_mapper import map_issue_family
from app.domain.rules.normalization_rules import extract_engine, normalize_issue_type, normalize_severity
from app.infrastructure.clients.http_client import HTTPClient
from app.shared.exceptions import SonarAPIError
from app.shared.utils.strings import component_to_path, normalize_whitespace


def _effort_to_minutes(value: str | None) -> int:
    if not value:
        return 0
    total = 0
    for qty, unit in re.findall(r"(\d+)(h|min)", value):
        n = int(qty)
        total += n * 60 if unit == 'h' else n
    return total


class SonarClient:
    def __init__(self, sonar_url: str, token: str, timeout_seconds: int = 30):
        self.sonar_url = sonar_url.rstrip('/')
        self.token = token
        self.http = HTTPClient(timeout_seconds=timeout_seconds)

    def fetch_project_issues(self, project_key: str, page_size: int = 200) -> list[Issue]:
        page = 1
        issues: list[Issue] = []
        headers = {'Authorization': f'Bearer {self.token}'}
        with self.http.build_client(headers=headers) as client:
            while True:
                response = client.get(
                    f'{self.sonar_url}/api/issues/search',
                    params={'componentKeys': project_key, 'p': page, 'ps': page_size},
                )
                if response.status_code >= 400:
                    raise SonarAPIError(f'Sonar API error: {response.status_code} {response.text}')
                payload: dict[str, Any] = response.json()
                page_issues = payload.get('issues', [])
                issues.extend(self._normalize_issues(page_issues))
                # print(f'Fetched {len(page_issues)} issues from SonarQube after normalization (page {page})\n\n')

                paging = payload.get('paging', {})
                total = paging.get('total', len(page_issues))
                if page * page_size >= total:
                    break
                page += 1
        return issues

    def _normalize_issues(self, raw_issues: list[dict[str, Any]]) -> list[Issue]:
        normalized: list[Issue] = []
        for item in raw_issues:
            engine = extract_engine(item)
            component = item.get('component', '')
            path = component_to_path(component)
            rule = item.get('rule', '')
            normalized.append(
                Issue(
                    issue_key=item.get('key', ''),
                    project=item.get('project', ''),
                    component=component,
                    path=path,
                    line=item.get('line') or item.get('textRange', {}).get('startLine', 1) or 1,
                    rule=rule,
                    engine=engine,
                    severity=normalize_severity(item.get('severity', 'INFO')),
                    type=normalize_issue_type(item.get('type', 'UNKNOWN')),
                    message=normalize_whitespace(item.get('message', '')),
                    effort_minutes=_effort_to_minutes(item.get('effort')),
                    impacts=item.get('impacts', []),
                    is_external=engine != 'sonar',
                    family=map_issue_family(rule=rule, engine=engine, component=component),
                )
            )
        print(f'Normalized {len(normalized)}')
        return normalized
