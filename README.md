# myo-prolog-interpreter

A small Prolog interpreter written in Java. It supports facts, rules, recursive
queries, list notation, dynamic assertions, `consult/1`, a categorized ISO
Prolog built-in registry, and a REPL with command history.

This is a learning interpreter, not a complete ISO Prolog implementation. The
focused project tests pass; the broad CSV-based ISO conformance fixture is kept
as a reference and currently fails many cases.

## Acknowledgements

Thanks to [The Power of Prolog](https://www.youtube.com/@ThePowerOfProlog) and
[Markus Triska](https://www.metalevel.at/) for excellent Prolog documentation,
learning material, depth, and attention to detail. The
[metalevel.at Prolog learning path](https://www.metalevel.at/prolog) and the
accompanying videos are especially useful for working through Prolog step by
step.

## Prerequisites

- Java JDK 17 or newer
- A POSIX-like shell for the examples below
- Network access the first time Gradle downloads dependencies

The checked-in Gradle wrapper is used for builds, so a separate Gradle install
is not required.

Runtime dependencies are declared in `app/build.gradle`:

- `picocli` for command-line option parsing
- `jline` for interactive REPL editing and command history

## Build And Install

Compile:

```sh
./gradlew compileJava
```

Build the runnable distribution:

```sh
./gradlew installDist
```

Run through Gradle:

```sh
./gradlew -q run
```

Run the installed script:

```sh
./prolog.sh
```

## Command Line

By default the REPL tries to preload an `init.pl` file before showing the first
prompt. The lookup order is:

1. `./init.pl`
2. `../init.pl`
3. no preload if neither file exists

`init.pl` is consulted with solution continuation forced to `.`, so startup
queries do not block waiting for `;`, space, or `.` input.

Start the REPL with the default preload behavior:

```sh
./gradlew -q run
```

or, after `./gradlew installDist`:

```sh
./prolog.sh
```

Skip loading `init.pl`:

```sh
./gradlew -q run --args='--no-load'
```

or:

```sh
./prolog.sh --no-load
```

Show help:

```sh
./gradlew -q run --args='--help'
```

or:

```sh
./prolog.sh --help
```

Verbose modes:

```sh
./gradlew -q run --args='-v'
./gradlew -q run --args='-vv'
./gradlew -q run --args='-vvv'
```

Installed-script equivalents:

```sh
./prolog.sh -v
./prolog.sh -vv
./prolog.sh -vvv
```

## REPL Usage

At the prompt, enter a fact, rule, directive-like built-in call, or query:

```prolog
?- parent(toni, lolo).
?- parent(toni, X).
```

Queries may be written with or without `?-`:

```prolog
parent(toni, X).
?- parent(toni, X).
```

Interactive behavior:

- Cursor up recalls the previous command from the in-memory history.
- Cursor down moves forward through the in-memory history.
- Editing and history are powered by JLine in an interactive terminal.
- Piped input falls back to line-based `Scanner` input and does not provide
  cursor-key editing.
- After a solution, press space or `;` to request the next solution. In an
  interactive terminal this is a single-key action; Enter is not required.
- Press `.` to stop looking for more solutions. In an interactive terminal this
  is also a single-key action.
- In piped/non-interactive input, solution continuation is line-based: provide a
  line containing `;` to continue, or any other line such as `.` to stop.
- `quit` exits the REPL.
- Failed queries print `false.`
- Ground successful queries print `true.`
- Variable bindings print on one line, for example `X = a, Y = b`.

Example interactive continuation:

```text
?- color(X).
solution: X = red
```

Press space or `;` to continue:

```text
;
solution: X = blue
```

Press `.` to stop instead of asking for another solution.

## Example

```prolog
list_length([], 0).
list_length([_|Ls], L) :- L #= L0 + 1, L #> 0, list_length(Ls, L0).

list_length([a,b], X).
```

Expected output:

```text
solution: X = 2
```

Dynamic rules can be asserted with grouped rule syntax:

```prolog
assertz((grandparent(X, Y) :- parent(X, Z), parent(Z, Y))).
```

The double parentheses are intentional: they make the rule a single argument to
`assertz/1`.

## Source Files

- `app/src/main/java/prolog/Lexer.java`: tokenizes Prolog source.
- `app/src/main/java/prolog/Parser.java`: parses clauses, expressions, terms, lists, and grouped rules.
- `app/src/main/java/prolog/interpreter/PrologRuntime.java`: query solving, built-ins, dynamic database operations, and REPL solution output.
- `app/src/main/java/prolog/PrologCli.java`: REPL, `init.pl` loading, history, and solution continuation keys.
- `app/src/test/java/prolog/IsoBestPracticeConformityTest.java`: focused ISO-style behavior tests for implemented features.
- `app/src/test/resources/tests/iso-conformity-testing.csv`: broad ISO reference fixture.

## Syntax Supported

Terms:

- atoms: `foo`, `hello`
- quoted atoms: `'tmp.pl'`
- variables: `X`, `List`, `_`
- numbers: integers and decimal numbers accepted by the lexer
- compound terms: `f(a, X)`
- callable operator terms: `=(a,b)`, `#>(2,1)`, `=..(Term, List)`
- grouped rule terms for assertion: `(Head :- Body)`

Lists:

- empty list: `[]`
- list notation: `[a,b,c]`
- head-tail notation: `[Head|Tail]`
- strings are treated as character lists: `"ab"`
- character code literal examples such as `0'a`

Clauses:

- facts: `parent(toni,lolo).`
- rules: `parent(X,Y) :- father(X,Y).`
- queries: `parent(toni, X).` or `?- parent(toni, X).`

## Operators

The runtime exposes these default operators through `current_op/3`:

| Priority | Specifier | Operators |
| --- | --- | --- |
| 1200 | `xfx` | `:-` |
| 1200 | `fx` | `:-`, `?-` |
| 1100 | `xfy` | `;` |
| 1050 | `xfy` | `->` |
| 1000 | `xfy` | `,` |
| 700 | `xfx` | `=`, `\=`, `==`, `\==`, `@<`, `@<=`, `@>`, `@>=`, `=..`, `is`, `=:=`, `=\=`, `<`, `=<`, `>`, `>=` |
| 500 | `yfx` | `+`, `-` |
| 400 | `yfx` | `*`, `/`, `//`, `mod` |
| 200 | `fy` | `\+` |

CLPZ-style comparison operators are implemented as built-ins:

```prolog
#=  #>  #>=  #<  #=<  #\=
```

The parser is still simpler than a full ISO operator parser. When in doubt, use
callable operator form:

```prolog
=(a,b).
#>(2,1).
/(parent, 2).
=..(f(a,b), Parts).
```

## ISO Prolog Built-ins Implemented

The runtime registers built-ins by category in `PrologRuntime`.

Term unification and comparison:

- `=/2`
- `\=/2`
- `==/2`
- `\==/2`
- `@</2`
- `@<=/2`
- `@>/2`
- `@>=/2`
- `unify_with_occurs_check/2`

Type testing:

- `var/1`
- `nonvar/1`
- `atom/1`
- `integer/1`
- `float/1`
- `number/1`
- `atomic/1`
- `compound/1`
- `callable/1`

Term creation and decomposition:

- `functor/3`
- `arg/3`
- `=../2`
- `copy_term/2`

Arithmetic and CLPZ-style comparison:

- `is/2`
- `>/2`
- `>=/2`
- `</2`
- `=</2`
- `=:=/2`
- `=\=/2`
- `#=/2`
- `#>/2`
- `#>=/2`
- `#</2`
- `#=</2`
- `#\=/2`

Arithmetic expressions currently support:

- unary `+`
- unary `-`
- binary `+`
- binary `-`
- binary `*`
- binary `/`
- binary `//`

Control:

- `true/0`
- `false/0`
- `fail/0`
- `!/0` as deterministic success placeholder
- `,/2`
- `;/2`
- `->/2`
- `\+/1`
- `once/1`
- `repeat/0` as success placeholder
- `call/1` through `call/8`
- `catch/3`
- `throw/1`

Streams, files, and term IO:

- `open/3`
- `open/4`
- `close/1`
- `close/2`
- `current_input/1`
- `current_output/1`
- `set_input/1`
- `set_output/1`
- `flush_output/0`
- `flush_output/1`
- `read/1`
- `read/2`
- `read_term/2`
- `read_term/3`
- `write/1`
- `write/2`
- `writeq/1`
- `writeq/2`
- `write_canonical/1`
- `write_canonical/2`
- `get_char/1`
- `get_char/2`
- `get_code/1`
- `get_code/2`
- `peek_char/1`
- `peek_char/2`
- `peek_code/1`
- `peek_code/2`
- `put_char/1`
- `put_char/2`
- `put_code/1`
- `put_code/2`
- `nl/0`
- `nl/1`

Knowledge base:

- `dynamic/1`
- `asserta/1`
- `assertz/1`
- `retract/1`
- `retractall/1`
- `clause/2`
- `abolish/1`
- `consult/1`

All-solutions predicates:

- `findall/3`
- `bagof/3`
- `setof/3`

Atom, character, and number conversion:

- `atom_length/2`
- `atom_chars/2`
- `atom_codes/2`
- `char_code/2`
- `number_chars/2`
- `number_codes/2`
- `sub_atom/5`

Listing and metadata:

- `current_predicate/1`
- `current_op/3`
- `current_char_conversion/2`

Environment and flags:

- `current_prolog_flag/2`
- `set_prolog_flag/2`
- `op/3`
- `halt/0`
- `halt/1`

Default flags:

- `bounded = false`
- `integer_rounding_function = toward_zero`
- `max_arity = unbounded`
- `unknown = fail`
- `double_quotes = chars`

## Tests

Run the focused tests for implemented lexer, parser, runtime, lists, unification,
and ISO-style behavior:

```sh
./gradlew test --tests prolog.IsoBestPracticeConformityTest --tests prolog.PrologRuntimeTest --tests prolog.ParserTest --tests prolog.LexerTest --tests prolog.ListsTest --tests prolog.UnifyTest
```

The full test task currently includes the broad CSV-backed ISO conformance
fixture and is expected to fail:

```sh
./gradlew test
```

At the time this README was written, the focused suite passes, while the full
suite fails in `IsoConformityTest` on many ISO edge cases, especially character
escaping, advanced operator parsing, and syntax cases that this interpreter does
not yet implement.

## Known Limits

- This is not a complete ISO Prolog system.
- The parser has limited operator precedence support. Callable operator form is
  often more reliable than infix notation for edge cases.
- `retract/1`, `retractall/1`, `abolish/1`, stream predicates, and some control
  predicates are partial implementations.
- `!/0` and `repeat/0` are placeholders, not full choice-point control.
- CLPZ support is useful for simple numeric constraints but is not a complete
  constraint solver.
- Recursive queries with insufficiently bounded arguments, such as
  `list_length(X, 2).`, can still recurse heavily because solving is currently
  recursive and constraint propagation is limited.
