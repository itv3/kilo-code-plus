---
"kilo-code": patch
---

Prevent the CLI from freezing when a changed file has tens of thousands of lines. Large-file diffs now skip the full patch body but still report additions/deletions, and the diff computation no longer blocks ESC/interrupt or live updates.
