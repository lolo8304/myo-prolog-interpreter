married(doris,lorenz).

child(silvan, lorenz).
child(yannick, lorenz).
child(silvan, doris).
child(yannick, doris).

is_child(X, M) :- child(X, Y), (married(Y, M); married(M, Y)).

list_length([], 0).
list_length([_|Ls], L) :- L #> 0, L #= L0 + 1, list_length(Ls, L0).

list_length([a], X).

