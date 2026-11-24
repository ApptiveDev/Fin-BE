"""PR이 새로 추가한 Flyway 마이그레이션 버전이 main의 최대 버전보다 큰지 검사한다.

main이 앞서 나간 뒤 그보다 작은 버전을 머지하면, 빈 DB에서 도는 CI는 통과하지만
이미 그 버전까지 적용된 dev·운영 DB에서는 Flyway validate가 실패해 앱이 뜨지 않는다.

사용법: python tools/check_migration_versions.py <base-ref> <head-ref>
출력의 마지막 줄은 커밋 상태 설명으로 쓰므로 짧게 유지한다.
"""

import argparse
import re
import subprocess
import sys

MIGRATION_DIR = "src/main/resources/db/migration"
VERSIONED = re.compile(r"V(\d+)__.+\.sql")


def _version(name: str) -> int | None:
    match = VERSIONED.fullmatch(name)
    return int(match.group(1)) if match else None


def find_violations(base_names: list[str], added_names: list[str]) -> tuple[list[str], int]:
    """base(main)의 최대 버전과, 그 이하이거나 형식이 맞지 않는 추가 파일 목록을 돌려준다."""
    max_version = max((v for name in base_names if (v := _version(name)) is not None), default=0)
    problems = []
    for name in sorted(added_names):
        if not name.startswith("V"):
            continue  # R__ 반복 마이그레이션처럼 버전이 없는 파일은 대상이 아니다
        version = _version(name)
        if version is None:
            problems.append(f"{name}: V<숫자>__<설명>.sql 형식이 아닙니다.")
        elif version <= max_version:
            problems.append(f"{name}: main의 최대 버전 V{max_version} 이하입니다.")
    return problems, max_version


def _git_names(repo: str, *args: str) -> list[str]:
    out = subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True, text=True).stdout
    return [path.rsplit("/", 1)[-1] for path in out.splitlines()]


def migration_names(base: str, head: str, repo: str = ".") -> tuple[list[str], list[str]]:
    """base에 있는 마이그레이션과, head가 base에 없던 것을 새로 추가한 마이그레이션 파일 이름."""
    base_names = _git_names(repo, "ls-tree", "--name-only", base, f"{MIGRATION_DIR}/")
    # 두 트리를 바로 비교하므로, head가 뒤처져 있어 main에만 있는 파일은 삭제(D)로 잡혀 대상에서 빠진다.
    added_names = _git_names(
        repo, "diff", "--no-renames", "--diff-filter=A", "--name-only", base, head, "--", MIGRATION_DIR
    )
    return base_names, added_names


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("base")
    parser.add_argument("head")
    args = parser.parse_args(argv)

    base_names, added_names = migration_names(args.base, args.head)
    problems, max_version = find_violations(base_names, added_names)
    for problem in problems:
        print(problem)
    if problems:
        print(f"추가한 마이그레이션을 V{max_version + 1}부터 순서대로 다시 매겨 주세요 (main 최대 V{max_version})")
        return 1
    print(f"OK (main 최대 V{max_version}, 추가 {len(added_names)}개)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
