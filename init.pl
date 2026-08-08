married(doris,lorenz).

child(silvan, lorenz).
child(yannick, lorenz).

list_length([], 0).
list_length([_|Ls], L) :- L #= L0 + 1, L #> 0, list_length(Ls, L0).

list_length([a], X).

