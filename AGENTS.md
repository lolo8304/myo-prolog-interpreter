# AGENTS.md

Guidance for coding agents working in this repository.

## Repository Shape

This is a small Java/Gradle Prolog interpreter. The root project is `prolog`,
with the application module in `app`.

- Main sources live in `app/src/main/java/prolog`.
- Tests live in `app/src/test/java` and fixtures in `app/src/test/resources/tests`.
- The CLI entry point is `prolog.Prolog`, configured in `app/build.gradle`.
- The REPL bootstrap file is root-level `init.pl`.

There is no `src/main` directory at the repository root. When a request refers
to `src/main`, read and edit `app/src/main` unless the user explicitly says
otherwise.

## Prolog Startup Contract

Use `init.pl` as the canonical foundation program for interactive Prolog work.
`PrologCli.execute()` checks for `init.pl` in the current working directory and
consults it before reading REPL input.

Run the application from the repository root so this lookup resolves correctly:

```sh
./gradlew run
```

When changing default facts, rules, or examples that should be available at
startup, update `init.pl`. Keep it valid Prolog syntax with one clause per
terminating `.`. Current examples include facts such as `married/2`, `child/2`,
and `list_length/2`.

If tests or scripts need the same baseline knowledge base, load or parse
`init.pl` instead of duplicating its clauses inline.

## Architecture Notes

The interpreter has three main layers:

- `prolog.Lexer`, `Token`, `TokenValue`, and related token classes handle
  lexical analysis.
- `prolog.Parser` converts token streams into AST nodes under `prolog.nodes`.
- `prolog.interpreter` contains runtime state, terms, substitutions, unification,
  clause memory, and query solving.

Important behavior boundaries:

- `Prolog` is only the Picocli shell and verbosity configuration.
- `PrologCli` owns REPL flow, file consultation, and string/reader parsing.
- `ProgramNode.consult()` and `ClauseNode.consult()` decide whether clauses
  become database facts/rules or execute as queries.
- `PrologRuntime` owns contexts, memory access, and recursive solving.
- `Memory` stores facts and rules by predicate indicator. Prefer going through
  existing node/runtime APIs instead of bypassing it.

## Development Commands

Use the Gradle wrapper.

```sh
./gradlew test
./gradlew run
./gradlew test --tests prolog.ParserTest
```

Verbose interpreter modes are exposed as CLI flags:

```sh
./gradlew run --args="-v"
./gradlew run --args="-vv"
./gradlew run --args="-vvv"
```

## Testing Guidance

Add focused JUnit 5 tests for parser, lexer, term, unification, and runtime
changes. Existing test classes are good placement guides:

- Lexer/token changes: `app/src/test/java/prolog/LexerTest.java`
- Parser/AST changes: `app/src/test/java/prolog/ParserTest.java`
- List syntax: `app/src/test/java/prolog/ListsTest.java`
- Runtime solving: `app/src/test/java/prolog/PrologRuntimeTest.java`
- Term and unification behavior: `TermTest.java`, `UnifyTest.java`,
  `TermStatusTest.java`
- ISO syntax compatibility data: `app/src/test/resources/tests/iso-conformity-testing.csv`

Prefer small fixture programs in test resources for multi-clause examples.
Use `Tester` helpers where they already fit; do not introduce a second parsing
helper style without a clear reason.

## Coding Conventions

- Preserve the existing Java style: simple classes, explicit methods, and
  package-level organization by interpreter layer.
- Keep parser grammar comments close to parser code when changing grammar.
- Keep AST node conversion methods (`asTerm`, `asTerms`, `asConstr`,
  `freevars`, term status methods) consistent across node types.
- Avoid broad refactors while fixing narrow parser or runtime behavior.
- Do not introduce a new Prolog bootstrap path unless `init.pl` loading is kept
  working from the repository root.

## Safety Notes

The worktree may contain user changes. Check `git status --short` before edits
and do not revert unrelated files. At the time this file was written, there were
already local modifications in IDE metadata, Java sources, and `init.pl`; treat
such changes as user-owned unless you made them in the current task.

## Overall rules
- if you new features, commands are implemented, also mention them in the existing sections of the readme.md file
- whenever you make changes, accept that i also make changes myself and always use all changes in git. 
