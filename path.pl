% Define the successor facts
successor(a, b).
successor(b, c).
% Add more successor facts as needed

% Helper predicate for path
path_helper(X, Y, [Y]) :- successor(X, Y).
path_helper(X, Y, [Z|Path]) :- successor(X, Z), path_helper(Z, Y, Path).

% Define the path predicate
path(X, Y, [X|Path]) :- path_helper(X, Y, Path).
