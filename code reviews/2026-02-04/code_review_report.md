# Code Review Report: JSDP (Java Stochastic Dynamic Programming)

**Date**: March 4, 2026
**Scope**: Project architecture, core `sdp` logic, data structures, concurrency, and testing strategies.

## 1. Project Architecture & Organization 
The project structure cleanly separates concerns into three distinct primary packages under `jsdp`:
- `jsdp.sdp`: Contains the core modeling abstractions and the recurrence engines.
- `jsdp.app`: Contains ~67 example implementations and applications of the engine.
- `jsdp.utilities`: Reusable utility functions (e.g., hashing, sampling).

> [!TIP]
> **Positive**: The core library relies on strong OOP principles paired with Functional Interfaces (e.g., [ImmediateValueFunction](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/app/standalone/stochastic/InventoryControl.java#114-118), `RandomOutcomeFunction`). This effectively modernizes the Stochastic DP formulation in Java.

> [!NOTE] 
> **Observation**: There are "standalone" applications (e.g., `jsdp.app.standalone.stochastic.InventoryControl`) that manually implement the DP recursion (via recursive [f(State)](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/app/standalone/stochastic/InventoryControl.java#124-149)) without utilizing the `jsdp.sdp` classes. Assuming these are meant to be baseline/benchmark implementations. If not, they violate DRY principles by reinventing the DP engine logic.

## 2. Concurrency and Data Structures
Stochastic DP naturally suffers from the "Curse of Dimensionality" (state space explosion). The library handles this very well:

- **State Value Storage**: [ValueRepository](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/sdp/ValueRepository.java#44-290) employs the Strategy pattern through the `HashType` enum, allowing the user to select the backing storage mapping (`Map<StateAction, Double>` and `Map<State, Double>`) dynamically. It effectively supports high-performance collections:
  - `THashMap` (from Trove4j) for performant primitives.
  - `ConcurrentHashMap` for parallel execution.
  - `MapDB` bindings (Heap, Memory, Disk) to allow swapping large state spaces to disk to prevent `OutOfMemoryError`. 
- **Parallel Computing**: [BackwardRecursion](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/sdp/BackwardRecursion.java#46-216) embraces Java 8 streams (`parallelStream()`) intelligently to iterate through states and actions concurrently. 

> [!CAUTION] 
> In `ValueRepository.getExpectedValue()`, `ConcurrentHashMap.computeIfAbsent` is used. While thread-safe, the computation inside the lambda evaluates transition probabilities across parallel streams. This could occasionally lead to thread starvation or nested parallelism inefficiencies depending on the exact ForkJoinPool size if state generation heavily overlaps with evaluation.

## 3. General Code Quality and Patterns
- **Build System**: Maven ([pom.xml](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/pom.xml)) is correctly configured. 
- **Dependencies**: Well-chosen tools (SSJ for simulation, MapDB, Trove4j, Log4j).
- **Style and Documentation**: The code is fairly clean. Javadocs are present on abstract classes ([Recursion](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/sdp/Recursion.java#38-121), [ValueRepository](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/sdp/ValueRepository.java#44-290)), fulfilling standard expectations for an open-source library.

## 4. Testing Analysis
> [!WARNING] 
> **Critical Finding**: There is a severe lack of automated tests in the repository.

- The command `find src/test/java -name "*.java"` reveals exactly **3 test files**: `AllTests.java`, `PoissonSampleTest.java`, and `NormalSampleTest.java` (all within the `utilities.sampling` package).
- There are **ZERO unit tests** for the core engine (`jsdp/sdp/*`) or any of the applications (`jsdp/app/*`).
- The library relies entirely on terminal outputs via `public static void main` methods in the [app](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/app/standalone/stochastic/InventoryControl.java#116-117) package for verification. This makes regression testing virtually impossible when evolving the core code.

## 5. Summary and Recommendations

| Area | Rating | Notes |
| :--- | :--- | :--- |
| **Architecture** | Excellent | Very strong abstraction of Markov Decision Processes. |
| **Performance** | Excellent | Disk-backed MapDB & Trove4j integrations show highly specialized performance awareness. |
| **Code Style** | Good | Idiomatic use of Java 8 features. Clean packages. |
| **Testing** | Poor | The immediate priority should be establishing a test harness for the core [BackwardRecursion](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/sdp/BackwardRecursion.java#46-216) and [ValueRepository](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/sdp/ValueRepository.java#44-290) logic. |

**Recommended Next Steps:**
1. Migrate one of the simple `jsdp.app` examples (like [InventoryControl](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/app/standalone/stochastic/InventoryControl.java#59-218)) into an automated JUnit test case to serve as a high-level regression test for [BackwardRecursion](file:///Users/gwren/GitHub/jsdp%20copy/jsdp/src/main/java/jsdp/sdp/BackwardRecursion.java#46-216).
2. Consolidate or document the standalone scripts to clarify why they ignore the `jsdp.sdp` engine.
