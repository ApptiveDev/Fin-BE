import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from code_health_report import build_metrics, main, render_report  # noqa: E402

JACOCO = """<?xml version="1.0" encoding="UTF-8"?>
<report name="fin">
  <counter type="LINE" missed="20" covered="80"/>
  <counter type="BRANCH" missed="3" covered="7"/>
</report>
"""

CODELENS = {
    "analysis": {"summary": {"lines": {"total": 100}, "complexity": {"cognitive": 231}}},
    "health": {"score": 84.2, "grade": "B"},
}

PMD = """<?xml version="1.0" encoding="UTF-8"?>
<pmd xmlns="http://pmd.sourceforge.net/report/2.0.0">
  <file name="src/main/java/Foo.java">
    <violation beginline="1" rule="CyclomaticComplexity" priority="3">The class 'Foo' has a total cyclomatic complexity of 133.</violation>
    <violation beginline="10" rule="CognitiveComplexity" priority="3" method="run">The method 'run' has a cognitive complexity of 17.</violation>
    <violation beginline="10" rule="CyclomaticComplexity" priority="3" method="run">The method 'run' has a cyclomatic complexity of 12.</violation>
    <violation beginline="30" rule="EmptyCatchBlock" priority="3" method="load">Avoid empty catch blocks.</violation>
  </file>
</pmd>
"""

# Foo.java 10-14 와 12-14 는 겹치므로 5줄로 병합되어야 한다: 5 + 5(Bar) + 3(Baz) = 13
CPD = """<?xml version="1.0" encoding="UTF-8"?>
<pmd-cpd xmlns="https://pmd-code.org/schema/cpd-report">
  <duplication lines="5" tokens="100">
    <file path="src/main/java/Foo.java" line="10" endline="14"/>
    <file path="src/main/java/Bar.java" line="20" endline="24"/>
  </duplication>
  <duplication lines="3" tokens="60">
    <file path="src/main/java/Foo.java" line="12" endline="14"/>
    <file path="src/main/java/Baz.java" line="1" endline="3"/>
  </duplication>
</pmd-cpd>
"""


class BuildMetricsTest(unittest.TestCase):
    def setUp(self):
        self._dir = tempfile.TemporaryDirectory()
        self.tmp = Path(self._dir.name)

    def tearDown(self):
        self._dir.cleanup()

    def write(self, name: str, content: str) -> Path:
        path = self.tmp / name
        path.write_text(content, encoding="utf-8")
        return path

    def test_combines_all_reports(self):
        metrics = build_metrics(
            self.write("jacoco.xml", JACOCO),
            self.write("codelens.json", json.dumps(CODELENS)),
            self.write("pmd.xml", PMD),
            self.write("cpd.xml", CPD),
            commit="abc1234",
        )

        self.assertAlmostEqual(metrics["line_coverage"], 80.0)
        self.assertAlmostEqual(metrics["branch_coverage"], 70.0)
        self.assertAlmostEqual(metrics["health_score"], 84.2)
        self.assertEqual(metrics["health_grade"], "B")
        self.assertEqual(metrics["duplicated_lines"], 13)
        self.assertAlmostEqual(metrics["duplication_percent"], 13.0)
        # 클래스 단위 위반(method 없음)은 제외하고, 같은 메서드의 두 규칙은 하나로 센다
        self.assertEqual(metrics["high_complexity_methods"], 1)
        self.assertEqual(metrics["pmd_issue_count"], 4)
        self.assertEqual(metrics["warnings"], [])

    def test_missing_reports_become_warnings(self):
        missing = self.tmp / "missing"
        metrics = build_metrics(missing, missing, missing, missing, commit="abc1234")

        self.assertIsNone(metrics["line_coverage"])
        self.assertIsNone(metrics["health_score"])
        self.assertIsNone(metrics["pmd_issue_count"])
        self.assertEqual(len(metrics["warnings"]), 4)
        report = render_report(metrics)
        self.assertIn("| Line Coverage | N/A | N/A | — |", report)
        self.assertIn("### Warnings", report)


class RenderReportTest(unittest.TestCase):
    def test_compares_with_baseline(self):
        current = {
            "commit": "abc123456789",
            "line_coverage": 81.3,
            "health_score": 84.2,
            "health_grade": "B",
            "duplication_percent": 4.1,
            "high_complexity_methods": 7,
        }
        baseline = {
            "line_coverage": 79.8,
            "health_score": 82.7,
            "health_grade": "B",
            "duplication_percent": 3.8,
            "high_complexity_methods": 6,
        }

        report = render_report(current, baseline)

        self.assertTrue(report.startswith("<!-- backend-code-health-report -->"))
        self.assertIn("Commit: `abc1234`", report)
        self.assertIn("| Line Coverage | 81.3% | 79.8% | +1.5 pp ✅ |", report)
        self.assertIn("| Code Health | 84.2 (B) | 82.7 (B) | +1.5 ✅ |", report)
        self.assertIn("| Duplicated Lines | 4.1% | 3.8% | +0.3 pp ⚠️ |", report)
        self.assertIn("| High-complexity Methods | 7 | 6 | +1 ⚠️ |", report)

    def test_lists_attention_and_code_issues(self):
        current = {
            "commit": "abc1234",
            "complexity_issues": [
                {"path": "src/main/java/Foo.java", "line": 10, "method": "run",
                 "rule": "CognitiveComplexity", "value": 17},
            ],
            "pmd_issues": [
                {"path": "src/main/java/Foo.java", "line": 30, "rule": "EmptyCatchBlock",
                 "priority": 3, "message": "Avoid empty catch blocks."},
            ],
            "duplication_blocks": [
                {"lines": 12, "occurrences": [
                    {"path": "src/main/java/Foo.java", "line": 10},
                    {"path": "src/main/java/Bar.java", "line": 20},
                ]},
            ],
        }

        report = render_report(current)

        self.assertIn("- `src/main/java/Foo.java:10` — Cognitive Complexity 17", report)
        self.assertIn(
            "Duplicated block (12 lines): `src/main/java/Foo.java:10` ↔ `src/main/java/Bar.java:20`",
            report,
        )
        self.assertIn("- `src/main/java/Foo.java:30` — Empty Catch Block: Avoid empty catch blocks.", report)


class MainTest(unittest.TestCase):
    def test_unreadable_baseline_is_reported_as_warning(self):
        with tempfile.TemporaryDirectory() as directory:
            tmp = Path(directory)
            missing = str(tmp / "missing")
            exit_code = main([
                "--jacoco", missing, "--codelens", missing, "--pmd", missing, "--cpd", missing,
                "--baseline", missing,
                "--commit", "abc1234",
                "--metrics-output", str(tmp / "out" / "metrics.json"),
                "--report-output", str(tmp / "out" / "report.md"),
            ])

            self.assertEqual(exit_code, 0)
            metrics = json.loads((tmp / "out" / "metrics.json").read_text(encoding="utf-8"))
            self.assertTrue(any("Base branch snapshot unavailable" in w for w in metrics["warnings"]))
            self.assertTrue((tmp / "out" / "report.md").exists())


if __name__ == "__main__":
    unittest.main()
