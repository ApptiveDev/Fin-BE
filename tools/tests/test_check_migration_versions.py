import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from check_migration_versions import MIGRATION_DIR, find_violations, migration_names  # noqa: E402

MAIN = ["V1__init.sql", "V2__data.sql", "V14__latest.sql"]


class FindViolationsTest(unittest.TestCase):
    def test_versions_above_main_max_pass(self):
        problems, max_version = find_violations(MAIN, ["V15__a.sql", "V16__b.sql"])
        self.assertEqual([], problems)
        self.assertEqual(14, max_version)

    def test_version_equal_to_or_below_main_max_fails(self):
        problems, _ = find_violations(MAIN, ["V14__dup.sql", "V12__gap.sql", "V15__ok.sql"])
        self.assertEqual(
            ["V12__gap.sql: main의 최대 버전 V14 이하입니다.", "V14__dup.sql: main의 최대 버전 V14 이하입니다."],
            problems,
        )

    def test_versions_compare_numerically(self):
        problems, max_version = find_violations(["V9__a.sql", "V10__b.sql"], ["V011__c.sql"])
        self.assertEqual(10, max_version)
        self.assertEqual([], problems)

    def test_malformed_versioned_name_fails(self):
        problems, _ = find_violations(MAIN, ["V15_missing_double_underscore.sql"])
        self.assertEqual(["V15_missing_double_underscore.sql: V<숫자>__<설명>.sql 형식이 아닙니다."], problems)

    def test_files_without_version_are_ignored(self):
        problems, _ = find_violations(MAIN, ["R__views.sql"])
        self.assertEqual([], problems)

    def test_empty_main_starts_from_zero(self):
        problems, max_version = find_violations([], ["V1__init.sql"])
        self.assertEqual(([], 0), (problems, max_version))


class MigrationNamesTest(unittest.TestCase):
    """PR 브랜치가 main보다 뒤처져 있을 때도 PR이 추가한 파일만 골라내는지 실제 git으로 확인한다."""

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.repo = self._tmp.name
        self.git("init", "-q", "-b", "main")
        self.commit("V1__init.sql")

    def tearDown(self):
        self._tmp.cleanup()

    def git(self, *args):
        subprocess.run(
            ["git", "-c", "user.name=t", "-c", "user.email=t@t", *args],
            cwd=self.repo, check=True, capture_output=True,
        )

    def commit(self, *names):
        directory = Path(self.repo, MIGRATION_DIR)
        directory.mkdir(parents=True, exist_ok=True)
        for name in names:
            (directory / name).write_text("select 1;\n")
        self.git("add", ".")
        self.git("commit", "-q", "-m", "add " + " ".join(names))

    def test_behind_branch_reports_only_its_own_files(self):
        self.git("switch", "-q", "-c", "pr")
        self.commit("V2__pr.sql")
        self.git("switch", "-q", "main")
        self.commit("V2__main.sql", "V3__main.sql")

        base_names, added_names = migration_names("main", "pr", self.repo)

        self.assertEqual(["V1__init.sql", "V2__main.sql", "V3__main.sql"], base_names)
        self.assertEqual(["V2__pr.sql"], added_names)
        self.assertEqual(
            ["V2__pr.sql: main의 최대 버전 V3 이하입니다."], find_violations(base_names, added_names)[0]
        )

    def test_branch_that_merged_main_is_not_blamed_for_main_files(self):
        self.git("switch", "-q", "-c", "pr")
        self.git("switch", "-q", "main")
        self.commit("V2__main.sql")
        self.git("switch", "-q", "pr")
        self.git("merge", "-q", "--no-edit", "main")
        self.commit("V3__pr.sql")

        self.assertEqual(["V3__pr.sql"], migration_names("main", "pr", self.repo)[1])


if __name__ == "__main__":
    unittest.main()
