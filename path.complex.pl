% Define the successor facts
successor(a, b).
successor(b, c).
successor(b, d).
successor(c, e).
successor(d, e).
successor(e, f).
successor(f, g).
successor(g, h).
successor(h, i).
successor(i, j).
successor(j, k).
successor(k, l).
successor(l, m).
successor(m, n).
successor(n, o).
successor(o, p).
successor(p, q).
successor(q, r).
successor(r, s).
successor(s, t).
successor(t, u).
successor(u, v).
successor(v, w).
successor(w, x).
successor(x, y).
successor(y, z).

% extra branches (to make it "complex")
successor(c, x).
successor(d, k).
successor(f, n).
successor(h, r).

% Add more successor facts as needed


% Helper predicate for path
path_helper(X, Y, [Y]) :- successor(X, Y).
path_helper(X, Y, [Z|Path]) :- successor(X, Z), path_helper(Z, Y, Path).

% Define the path predicate
path(X, Y, [X|Path]) :- path_helper(X, Y, Path).
