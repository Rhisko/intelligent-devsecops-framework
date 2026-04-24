from __future__ import annotations

import json
from pathlib import Path

from app.bootstrap import Bootstrap
from app.cli import build_parser
from app.infrastructure.observability.logger import get_logger
from app.application.services.render_security_report import render_report

logger = get_logger(__name__)


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    use_case = Bootstrap().build_analyze_sonar_project_use_case(sonar_url_override=args.sonar_url)
    result = use_case.execute(project_key=args.project_key, analysis_mode=args.analysis_mode)
    # print(json.dumps(result, ensure_ascii=False))
    render_report(
        report=result,
        output_path="advisory_report.html"
    )
    serialized = json.dumps(result, indent=2, ensure_ascii=False)
    if args.output_file:
        output_path = Path(args.output_file)
        output_path.write_text(serialized, encoding='utf-8')
        logger.info('Advisory output written to %s', output_path)
    else:
        print(serialized)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
