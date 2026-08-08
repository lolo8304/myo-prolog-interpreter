married(doris,lorenz).

child(silvan, lorenz).
child(yannick, lorenz).
child(silvan, doris).
child(yannick, doris).

child(philipp,roman).
child(michael,roman).
child(nicole,roman).

is_child(X, M) :- child(X, Y), (married(Y, M); married(M, Y)).
sibling(X,Y) :- X \= Y, X @< Y, child(X,A), child(Y,A).

list_length([], 0).
list_length([_|Ls], L) :- L #> 0, L #= L0 + 1, list_length(Ls, L0).

list_length([a], X).

