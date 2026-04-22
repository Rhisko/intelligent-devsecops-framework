# from __future__ import annotations

# from app.domain.enums.analysis_mode import AnalysisMode


# class RoutingService:
#     def select_task(self, analysis_mode: str, has_critical: bool, has_dependency_findings: bool) -> str:
#         mode = AnalysisMode(analysis_mode)
#         print(f'RoutingService: mode={mode}, has_critical={has_critical}, has_dependency_findings={has_dependency_findings}')
#         if mode == AnalysisMode.RELEASE_GATE:
#             return 'release_gate'
#         if mode == AnalysisMode.CRITICAL_ANALYSIS or has_critical:
#             return 'critical_analysis'
#         if mode == AnalysisMode.DEPENDENCY_ANALYSIS or has_dependency_findings:
#             return 'dependency_analysis'
#         return 'summary'

from __future__ import annotations

from app.domain.enums.analysis_mode import AnalysisMode


class RoutingService:
    def select_task(
        self,
        analysis_mode: str,
        has_critical: bool,
        has_dependency_findings: bool,
    ) -> AnalysisMode:
        try:
            mode = AnalysisMode(analysis_mode)
        except ValueError:
            mode = AnalysisMode.SUMMARY

        if mode == AnalysisMode.RELEASE_GATE:
            return AnalysisMode.RELEASE_GATE

        if mode == AnalysisMode.CRITICAL_ANALYSIS:
            return AnalysisMode.CRITICAL_ANALYSIS

        if mode == AnalysisMode.DEPENDENCY_ANALYSIS:
            return AnalysisMode.DEPENDENCY_ANALYSIS

        return AnalysisMode.SUMMARY