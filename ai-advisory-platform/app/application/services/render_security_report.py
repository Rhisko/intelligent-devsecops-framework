from __future__ import annotations

from collections import Counter
from datetime import datetime
from pathlib import Path
from typing import Any

from jinja2 import Environment, FileSystemLoader, select_autoescape


def badge_class(value: Any, kind: str = "default") -> str:
    v = str(value).upper()

    if kind == "risk":
        if v in {"CRITICAL", "HOLD"}:
            return "badge badge-critical"
        if v in {"HIGH", "P1"}:
            return "badge badge-high"
        if v in {"MEDIUM", "P2", "REVIEW"}:
            return "badge badge-medium"
        if v in {"LOW", "P3", "GO"}:
            return "badge badge-low"

    if v == "TRUE":
        return "badge badge-critical"
    if v == "FALSE":
        return "badge badge-low"

    return "badge badge-neutral"


def split_engines(engine_value: str) -> list[str]:
    return [part.strip() for part in str(engine_value).split(",") if part.strip()]


def build_breakdown(findings: list[dict[str, Any]], attr_name: str) -> list[dict[str, Any]]:
    counter = Counter()

    for finding in findings:
        value = finding.get(attr_name)

        if attr_name == "engine":
            for eng in split_engines(str(value or "")):
                counter[eng] += 1
        else:
            counter[str(value)] += 1

    return [{"label": key, "value": value} for key, value in sorted(counter.items())]


def render_report(
    report: dict[str, Any],
    output_path: str | Path,
    template_dir: str | Path = "./configs",
    template_name: str = "security_advisory_report.html.j2",
    logo_filename: str = "logo.png",
) -> Path:
    env = Environment(
        loader=FileSystemLoader(str(template_dir)),
        autoescape=select_autoescape(["html", "xml"]),
        trim_blocks=True,
        lstrip_blocks=True,
    )

    env.globals["badge_class"] = badge_class
    env.globals["split_engines"] = split_engines

    template = env.get_template(template_name)

    findings = report.get("findings", [])
    blockers = sum(1 for item in findings if item.get("release_blocker"))
    severity_breakdown = build_breakdown(findings, "severity")
    engine_breakdown = build_breakdown(findings, "engine")

    html = template.render(
        report=report,
        blockers=blockers,
        severity_breakdown=severity_breakdown,
        engine_breakdown=engine_breakdown,
        generated_at=datetime.now().astimezone().strftime("%Y-%m-%d %H:%M %Z"),
        logo_filename=logo_filename,
    )

    output_file = Path(output_path)
    output_file.write_text(html, encoding="utf-8")
    return output_file

# render_report(report={}, output_path="report.html")