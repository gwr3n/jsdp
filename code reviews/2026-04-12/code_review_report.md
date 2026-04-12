# Code review — 2026-04-12

Summary
-------

A quick automated/manual pass over the codebase focusing on common code-smells and runtime prints. The scan highlights frequent uses of direct console output, a few inconsistent logging patterns, and a couple of minor maintainability notes.

Findings
--------

- **Console output:** Many classes write to standard output and standard error (e.g., System.out.println / System.err.println) rather than using a logging framework. Examples:
  - [jsdp/src/main/java/jsdp/app/control/clqg/univariate/CLQG.java](jsdp/src/main/java/jsdp/app/control/clqg/univariate/CLQG.java#L184)
  - [jsdp/src/main/java/jsdp/app/skeleton/StochasticDynamicProgram.java](jsdp/src/main/java/jsdp/app/skeleton/StochasticDynamicProgram.java#L171)
  - [jsdp/src/main/java/jsdp/app/routing/stochastic/fuel/BowserRoutingFuel.java](jsdp/src/main/java/jsdp/app/routing/stochastic/fuel/BowserRoutingFuel.java#L935)
  - [jsdp/src/main/java/jsdp/app/inventory/univariate/CapacitatedStochasticLotSizing.java](jsdp/src/main/java/jsdp/app/inventory/univariate/CapacitatedStochasticLotSizing.java#L94)

- **Mixed logging:** Some classes already use a logger but still contain System.out calls (example: BowserRoutingFuel prints initialState then also uses `logger.info`). Prefer consistent logging at appropriate levels.

- **Debug/diagnostic prints in library code:** Several algorithmic classes print timing/diagnostic output directly; these can pollute user output and hinder automated runs. Replace with logger calls at `INFO` or `DEBUG` levels or gate behind a verbose flag.

- **Intentional reproducibility:** At least one application sets a fixed seed (`rnd.setSeed(1234)`) — note this is fine if reproducibility is desired, but document it in the README or command-line usage.

- **No high-severity issues found in quick scan:** Unit tests were exercised previously in this workspace; no failing tests were obvious from the quick scan. (I ran a pattern search focused on prints and common keywords; a deeper static analysis or linter run can surface more issues.)

Recommendations
---------------

- Replace direct `System.out` / `System.err` calls with a logging framework (SLF4J + logback or java.util.logging). Use appropriate levels (`DEBUG`, `INFO`, `WARN`, `ERROR`).
- Remove stray debug prints from core library code; keep console output only in CLI/front-end runner classes, and make it configurable via logging or a `--quiet/--verbose` flag.
- Standardize logging across the codebase (single logger convention per class). Where `logger` exists, remove parallel `System.out` statements.
- Consider adding a lightweight static analysis step (SpotBugs, Checkstyle, or PMD) to CI to catch prints and other issues early.

Actions taken
-------------

- Created this review file at: [code reviews/2026-04-12/code_review_report.md](code%20reviews/2026-04-12/code_review_report.md)
- Performed a quick scan for `System.out`, `System.err`, and common debug patterns and inspected representative files.

Next steps (optional)
---------------------

- I can open a PR that replaces console prints with SLF4J `logger` calls for the small set of runner classes you prefer to keep outputting to console.
- I can run a deeper static analysis (SpotBugs/Checkstyle) and add a CI job to block prints in library code.

If you want one of the next steps, tell me which and I will implement it.

Static analysis run — 2026-04-12
--------------------------------

I ran Checkstyle, PMD and attempted SpotBugs on the `jsdp` Maven module to gather more actionable issues.

- **Checkstyle:** Completed. The run produced many style issues across `src/main/java/jsdp` including:
  - Line-length violations (many lines > 80 chars)
  - Trailing whitespace and tab characters
  - Missing Javadoc comments on public fields/methods
  - Visibility/encapsulation issues (non-private fields)
  - Naming and whitespace rules (logger constant names, brace/whitespace placement)

  These are detailed in the Checkstyle output captured during the run; fixing these will improve readability and enable CI enforcement.

- **SpotBugs:** Failed to run — Maven could not resolve the SpotBugs Maven plugin in this environment when invoked directly. We can either add SpotBugs configuration to the `pom.xml` (recommended) or I can try alternative plugin coordinates/versions if you want me to retry.

- **PMD:** Failed due to plugin JDK-target compatibility. The PMD plugin rejected the configured `targetJdk` value (project compiles with a recent JDK that the PMD version used does not support). Options:
  - Run PMD configured with an older `targetJdk` (may produce false positives/negatives), or
  - Upgrade PMD plugin to a version that supports the project's Java version, or
  - Temporarily set the Maven `maven.compiler.target`/`release` to a supported value for analysis runs in CI.

Recommendations from static analysis
-----------------------------------

- Address Checkstyle issues incrementally: start with trailing whitespace, tab removal, and line-length trimming. Then add missing Javadoc for public APIs and fix visibility modifiers where appropriate.
- Add Checkstyle configuration to the project root (if not present) and enable the plugin in the `pom.xml` so CI can fail the build on new violations.
- Add/upgrade PMD to a version compatible with your Java toolchain and run it as part of CI.
- Add SpotBugs to the `pom.xml` with a known working plugin version so the plugin can be run consistently in CI and locally.

Log artifacts
-------------

- Checkstyle output excerpt captured during the run. (Full console log available in terminal history.)
- PMD run failed with an "Unsupported targetJdk" error — see terminal output for details.
- SpotBugs plugin resolution failed in this environment; adding plugin configuration to `pom.xml` will make runs reproducible.

- PMD run (3.24.0): succeeded with findings — PMD 7.3.0 reported 180 violations and wrote results to `jsdp/target/pmd.xml` (and site pages under `jsdp/target/site/`).


If you want, I can:
- open a PR that adds Checkstyle/PMD/SpotBugs plugin configurations to `pom.xml`, or
- create a branch and start fixing the highest-value Checkstyle issues (trailing whitespace, tabs, and long lines) across the `jsdp` package.

