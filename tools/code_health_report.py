import argparse
import json
import re
from collections import defaultdict
from pathlib import Path
from xml.etree import ElementTree as ET

COMPLEXITY_RULES = {"CognitiveComplexity", "CyclomaticComplexity"}


def _local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def _display_path(raw_path: str) -> str:
    path = Path(raw_path)
    try:
        return path.resolve().relative_to(Path.cwd().resolve()).as_posix()
    except (OSError, ValueError):
        return raw_path.replace("\\", "/")


def _rule_label(rule: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", " ", rule)


def _coverage(root: ET.Element, counter_type: str) -> float | None:
    for counter in root:
        if _local_name(counter.tag) == "counter" and counter.get("type") == counter_type:
            missed = int(counter.get("missed", "0"))
            covered = int(counter.get("covered", "0"))
            total = missed + covered
            return covered * 100 / total if total else None
    return None


def _pmd_issues(root: ET.Element) -> list[dict]:
    issues = []
    for file_element in root.iter():
        if _local_name(file_element.tag) != "file":
            continue
        path = _display_path(file_element.get("name", ""))
        for violation in file_element:
            if _local_name(violation.tag) != "violation":
                continue
            message = " ".join((violation.text or "").split())
            match = re.search(r"complexity of (\d+)", message)
            issues.append(
                {
                    "path": path,
                    "line": int(violation.get("beginline", "0")),
                    "rule": violation.get("rule", ""),
                    "method": violation.get("method"),
                    "priority": int(violation.get("priority", "5")),
                    "message": message,
                    "value": int(match.group(1)) if match else None,
                }
            )
    return issues


def _cpd_metrics(root: ET.Element) -> tuple[int, list[dict]]:
    ranges_by_file = defaultdict(list)
    blocks = []
    for duplication in root.iter():
        if _local_name(duplication.tag) != "duplication":
            continue
        occurrences = []
        for file_element in duplication:
            if _local_name(file_element.tag) != "file":
                continue
            path = _display_path(file_element.get("path", ""))
            start = int(file_element.get("line", "0"))
            end = int(file_element.get("endline", str(start)))
            ranges_by_file[path].append((start, end))
            occurrences.append({"path": path, "line": start, "endline": end})
        blocks.append(
            {
                "lines": int(duplication.get("lines", "0")),
                "tokens": int(duplication.get("tokens", "0")),
                "occurrences": occurrences,
            }
        )

    duplicated_lines = 0
    for ranges in ranges_by_file.values():
        current_start = current_end = None
        for start, end in sorted(ranges):
            if current_start is None:
                current_start, current_end = start, end
            elif start <= current_end + 1:
                current_end = max(current_end, end)
            else:
                duplicated_lines += current_end - current_start + 1
                current_start, current_end = start, end
        if current_start is not None:
            duplicated_lines += current_end - current_start + 1

    return duplicated_lines, blocks


def build_metrics(
    jacoco_path: Path,
    codelens_path: Path,
    pmd_path: Path,
    cpd_path: Path,
    commit: str,
) -> dict:
    warnings = []
    line_coverage = branch_coverage = None
    health_score = health_grade = cognitive_complexity = total_lines = None
    duplicated_lines = None
    issues = []
    pmd_issue_count = None
    duplication_blocks = []
    high_complexity_methods = None

    try:
        jacoco = ET.parse(jacoco_path).getroot()
        line_coverage = _coverage(jacoco, "LINE")
        branch_coverage = _coverage(jacoco, "BRANCH")
    except (OSError, ET.ParseError) as error:
        warnings.append(f"JaCoCo report unavailable: {error}")

    try:
        codelens = json.loads(codelens_path.read_text(encoding="utf-8"))
        summary = codelens["analysis"]["summary"]
        health = codelens["health"]
        health_score = health["score"]
        health_grade = health["grade"]
        cognitive_complexity = summary["complexity"]["cognitive"]
        total_lines = summary["lines"]["total"]
    except (OSError, ValueError, KeyError, TypeError) as error:
        warnings.append(f"Codelens report unavailable: {error}")

    try:
        issues = _pmd_issues(ET.parse(pmd_path).getroot())
        pmd_issue_count = len(issues)
        complex_methods = {
            (issue["path"], issue["line"])
            for issue in issues
            if issue["rule"] in COMPLEXITY_RULES and issue["method"]
        }
        high_complexity_methods = len(complex_methods)
    except (OSError, ET.ParseError, ValueError) as error:
        warnings.append(f"PMD report unavailable: {error}")

    try:
        duplicated_lines, duplication_blocks = _cpd_metrics(ET.parse(cpd_path).getroot())
    except (OSError, ET.ParseError, ValueError) as error:
        warnings.append(f"CPD report unavailable: {error}")

    return {
        "schema_version": 1,
        "commit": commit,
        "line_coverage": line_coverage,
        "branch_coverage": branch_coverage,
        "health_score": health_score,
        "health_grade": health_grade,
        "cognitive_complexity": cognitive_complexity,
        "duplicated_lines": duplicated_lines,
        "duplication_percent": (
            duplicated_lines * 100 / total_lines
            if duplicated_lines is not None and total_lines
            else None
        ),
        "high_complexity_methods": high_complexity_methods,
        "pmd_issue_count": pmd_issue_count,
        "complexity_issues": [
            issue
            for issue in issues
            if issue["rule"] in COMPLEXITY_RULES and issue["method"]
        ],
        "pmd_issues": issues,
        "duplication_blocks": duplication_blocks,
        "warnings": warnings,
    }


def render_report(current: dict, baseline: dict | None = None) -> str:
    baseline = baseline or {}

    def number(value, suffix=""):
        return "N/A" if value is None else f"{value:.1f}{suffix}"

    def integer(value):
        return "N/A" if value is None else str(value)

    def change(key: str, higher_is_better: bool, suffix: str = "") -> str:
        value = current.get(key)
        previous = baseline.get(key)
        if value is None or previous is None:
            return "—"
        delta = value - previous
        good = delta >= 0 if higher_is_better else delta <= 0
        marker = "✅" if good else "⚠️"
        sign = "+" if delta > 0 else ""
        if suffix:
            return f"{sign}{delta:.1f} {suffix} {marker}"
        if isinstance(value, int) and isinstance(previous, int):
            return f"{sign}{delta:d} {marker}"
        return f"{sign}{delta:.1f} {marker}"

    current_health = (
        "N/A"
        if current.get("health_score") is None
        else f"{current['health_score']:.1f} ({current.get('health_grade', '—')})"
    )
    baseline_health = (
        "N/A"
        if baseline.get("health_score") is None
        else f"{baseline['health_score']:.1f} ({baseline.get('health_grade', '—')})"
    )
    rows = [
        (
            "Line Coverage",
            number(current.get("line_coverage"), "%"),
            number(baseline.get("line_coverage"), "%"),
            change("line_coverage", True, "pp"),
        ),
        (
            "Branch Coverage",
            number(current.get("branch_coverage"), "%"),
            number(baseline.get("branch_coverage"), "%"),
            change("branch_coverage", True, "pp"),
        ),
        (
            "Code Health",
            current_health,
            baseline_health,
            change("health_score", True),
        ),
        (
            "Duplicated Lines",
            number(current.get("duplication_percent"), "%"),
            number(baseline.get("duplication_percent"), "%"),
            change("duplication_percent", False, "pp"),
        ),
        (
            "High-complexity Methods",
            integer(current.get("high_complexity_methods")),
            integer(baseline.get("high_complexity_methods")),
            change("high_complexity_methods", False),
        ),
        (
            "PMD Issues",
            integer(current.get("pmd_issue_count")),
            integer(baseline.get("pmd_issue_count")),
            change("pmd_issue_count", False),
        ),
    ]

    lines = [
        "<!-- backend-code-health-report -->",
        "## Backend Code Health Report",
        "",
        f"Commit: `{current.get('commit', 'unknown')[:7]}`",
        "",
        "| Metric | Current | Base | Change |",
        "|---|---:|---:|---:|",
    ]
    lines.extend(f"| {label} | {value} | {previous} | {delta} |" for label, value, previous, delta in rows)

    attention = []
    complexity_by_method = defaultdict(list)
    for issue in current.get("complexity_issues", []):
        key = (issue.get("path"), issue.get("line"), issue.get("method"))
        complexity_by_method[key].append(issue)
    complexity_methods = sorted(
        complexity_by_method.items(),
        key=lambda item: max(issue.get("value") or 0 for issue in item[1]),
        reverse=True,
    )
    for (path, line, _method), issues in complexity_methods[:3]:
        details = []
        for issue in sorted(issues, key=lambda item: item["rule"]):
            rule = _rule_label(issue["rule"])
            value = f" {issue['value']}" if issue.get("value") is not None else ""
            details.append(f"{rule}{value}")
        attention.append(f"- `{path}:{line}` — {' / '.join(details)}")

    remaining = 5 - len(attention)
    duplicate_blocks = sorted(
        current.get("duplication_blocks", []),
        key=lambda block: block.get("lines", 0),
        reverse=True,
    )
    for block in duplicate_blocks[:remaining]:
        occurrences = block.get("occurrences", [])
        if len(occurrences) < 2:
            continue
        left, right = occurrences[:2]
        attention.append(
            f"- Duplicated block ({block['lines']} lines): "
            f"`{left['path']}:{left['line']}` ↔ `{right['path']}:{right['line']}`"
        )

    if attention:
        lines.extend(["", "### Attention", "", *attention])

    code_issues = [
        issue
        for issue in current.get("pmd_issues", [])
        if issue.get("rule") not in COMPLEXITY_RULES
    ]
    if code_issues:
        lines.extend(["", "### Code Issues", ""])
        for issue in sorted(
            code_issues,
            key=lambda item: (
                item.get("priority", 5),
                item.get("path", ""),
                item.get("line", 0),
            ),
        )[:5]:
            rule = _rule_label(issue["rule"])
            lines.append(
                f"- `{issue['path']}:{issue['line']}` — {rule}: {issue['message']}"
            )

    warnings = current.get("warnings", [])
    if warnings:
        lines.extend(["", "### Warnings", ""])
        lines.extend(f"- {warning}" for warning in warnings)

    lines.extend(
        [
            "",
            "> Informational report only — these metrics do not block merging.",
            "",
        ]
    )
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Build the backend code health report.")
    parser.add_argument("--jacoco", type=Path, required=True)
    parser.add_argument("--codelens", type=Path, required=True)
    parser.add_argument("--pmd", type=Path, required=True)
    parser.add_argument("--cpd", type=Path, required=True)
    parser.add_argument("--baseline", type=Path)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--metrics-output", type=Path, required=True)
    parser.add_argument("--report-output", type=Path, required=True)
    args = parser.parse_args(argv)

    metrics = build_metrics(
        args.jacoco,
        args.codelens,
        args.pmd,
        args.cpd,
        args.commit,
    )
    baseline = None
    if args.baseline:
        try:
            baseline = json.loads(args.baseline.read_text(encoding="utf-8"))
        except (OSError, ValueError) as error:
            metrics["warnings"].append(f"Base branch snapshot unavailable: {error}")

    args.metrics_output.parent.mkdir(parents=True, exist_ok=True)
    args.report_output.parent.mkdir(parents=True, exist_ok=True)
    args.metrics_output.write_text(
        json.dumps(metrics, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    args.report_output.write_text(render_report(metrics, baseline), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
