#!/usr/bin/env bash
# Find git merge conflict markers in a file.
#
# Prints the line number and the marker for each of:
#   <<<<<<< (ours start)
#   ||||||| (base / diff3 separator)
#   ======= (separator)
#   >>>>>>> (theirs end)
#
# Usage:
#   script/upstream/find-conflict-markers.sh <file>
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 <file>" >&2
  exit 2
fi

file=$1

if [ ! -f "$file" ]; then
  echo "error: not a file: $file" >&2
  exit 2
fi

# Match the four conflict marker line shapes. `=======` must be the whole line;
# the others may have trailing content (branch name, commit hash, etc.).
#
# Prefer ripgrep when available (matches the historical invocation), fall back
# to POSIX grep so the script works in minimal environments.
if command -v rg >/dev/null 2>&1; then
  rg -n '^(<{7}|\|{7}|={7}$|>{7})' "$file" || {
    status=$?
    # rg exits 1 when no matches are found; treat that as success (clean file).
    if [ "$status" -eq 1 ]; then
      exit 0
    fi
    exit "$status"
  }
else
  grep -nE '^(<{7}|\|{7}|={7}$|>{7})' "$file" || {
    status=$?
    if [ "$status" -eq 1 ]; then
      exit 0
    fi
    exit "$status"
  }
fi
