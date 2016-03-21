package ru.ifmo.ctddev.gmwcs.solver;

import ru.ifmo.ctddev.gmwcs.Pair;
import ru.ifmo.ctddev.gmwcs.TimeLimit;
import ru.ifmo.ctddev.gmwcs.graph.*;

import java.util.*;

public class BicomponentSolver implements Solver {
    private TimeLimit rooted;
    private TimeLimit biggest;
    private TimeLimit unrooted;
    private RLTSolver solver;
    private boolean isSolvedToOptimality;
    private double lb;
    private boolean silence;

    public BicomponentSolver(RLTSolver solver) {
        rooted = new TimeLimit(Double.POSITIVE_INFINITY);
        unrooted = biggest = rooted;
        this.solver = solver;
        lb = 0;
    }

    public void setRootedTL(TimeLimit tl) {
        this.rooted = tl;
    }

    public void setUnrootedTL(TimeLimit tl) {
        this.unrooted = tl;
    }

    public void setTLForBiggest(TimeLimit tl) {
        this.biggest = tl;
    }

    public List<Unit> solve(Graph graph) throws SolverException {
        Graph g = graph;
        graph = graph.subgraph(graph.vertexSet());
        Preprocessor.preprocess(graph);
        if (!silence) {
            System.out.print("Preprocessing deleted " + (g.vertexSet().size() - graph.vertexSet().size()) + " nodes ");
            System.out.println("and " + (g.edgeSet().size() - graph.edgeSet().size()) + " edges.");
        }
        isSolvedToOptimality = true;
        solver.setLB(-Double.MAX_VALUE);
        if (graph.vertexSet().size() == 0) {
            return null;
        }
        long timeBefore = System.currentTimeMillis();
        Decomposition decomposition = new Decomposition(graph);
        double duration = (System.currentTimeMillis() - timeBefore) / 1000.0;
        if (!silence) {
            System.out.println("Graph decomposing takes " + duration + " seconds.");
        }
        List<Unit> bestBiggest = solveBiggest(graph, decomposition);
        List<Unit> bestUnrooted = extract(solveUnrooted(graph, decomposition));
        graph.vertexSet().forEach(Node::clear);
        graph.edgeSet().forEach(Edge::clear);
        List<Unit> best = Utils.sum(bestBiggest) > Utils.sum(bestUnrooted) ? bestBiggest : bestUnrooted;
        solver.setLB(-Double.MAX_VALUE);
        if (Utils.sum(best) < 0) {
            return null;
        }
        return best;
    }

    private List<Unit> extract(List<Unit> sol) {
        List<Unit> res = new ArrayList<>();
        for (Unit u : sol) {
            for (Unit a : u.getAbsorbed()) {
                res.add(a);
            }
            res.add(u);
        }
        return res;
    }

    @Override
    public void setTimeLimit(TimeLimit tl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSolvedToOptimality() {
        return isSolvedToOptimality;
    }

    @Override
    public void suppressOutput() {
        solver.suppressOutput();
        silence = true;
    }

    @Override
    public void setLB(double lb) {
        this.lb = lb;
    }

    private Node getRoot(Graph graph) {
        Set<Node> rootCandidates = new LinkedHashSet<>();
        for (int i = -1; i < graph.vertexSet().size(); i++) {
            rootCandidates.add(new Node(i, 0.0));
        }
        graph.vertexSet().stream().forEach(v -> rootCandidates.removeAll(v.getAbsorbed()));
        rootCandidates.removeAll(graph.vertexSet());
        return rootCandidates.iterator().next();
    }

    private List<Unit> solveBiggest(Graph graph, Decomposition decomposition) throws SolverException {
        Graph tree = new Graph();
        Map<Unit, List<Unit>> history = new HashMap<>();
        graph.vertexSet().forEach(v -> history.put(v, v.getAbsorbed()));
        graph.edgeSet().forEach(e -> history.put(e, e.getAbsorbed()));
        Node root = getRoot(graph);
        tree.addVertex(root);
        Map<Unit, Node> itsCutpoints = new LinkedHashMap<>();
        for (Pair<Set<Node>, Node> p : decomposition.getRootedComponents()) {
            for (Node node : p.first) {
                for (Edge edge : graph.edgesOf(node)) {
                    itsCutpoints.put(edge, p.second);
                }
                itsCutpoints.put(node, p.second);
            }
            tree.addGraph(graph.subgraph(p.first));
            addAsChild(tree, p.first, p.second, root);
        }
        solver.setRoot(root);
        List<Unit> rootedRes = solve(tree, rooted);
        solver.setRoot(null);
        Graph main = graph.subgraph(decomposition.getBiggestComponent());
        if (rootedRes != null) {
            rootedRes.stream().filter(unit -> unit != root).forEach(unit -> {
                Node cutpoint = itsCutpoints.get(unit);
                cutpoint.absorb(unit);
            });
        }
        solver.setLB(lb);
        List<Unit> solution = solve(main, biggest);
        List<Unit> result = new ArrayList<>();
        result.addAll(solution);
        solver.setLB(Utils.sum(result));
        solution.stream().forEach(u -> result.addAll(u.getAbsorbed()));
        repairCutpoints(history);
        return result;
    }

    private void repairCutpoints(Map<Unit, List<Unit>> history) {
        history.keySet().forEach(Unit::clear);
        for (Unit u : history.keySet()) {
            for (Unit a : history.get(u)) {
                u.absorb(a);
            }
        }
    }

    private void addAsChild(Graph tree, Set<Node> component, Node cp, Node root) {
        for (Node neighbour : tree.neighborListOf(cp)) {
            if (!component.contains(neighbour)) {
                continue;
            }
            Edge edge = tree.getEdge(cp, neighbour);
            tree.removeEdge(edge);
            tree.addEdge(root, neighbour, edge);
        }
        tree.removeVertex(cp);
    }

    private List<Unit> solveUnrooted(Graph graph, Decomposition decomposition) throws SolverException {
        Set<Node> union = new LinkedHashSet<>();
        decomposition.getUnrootedComponents().forEach(union::addAll);
        return solve(graph.subgraph(union), unrooted);
    }

    private List<Unit> solve(Graph graph, TimeLimit tl) throws SolverException {
        solver.setTimeLimit(tl);
        List<Unit> result = solver.solve(graph);
        if (!solver.isSolvedToOptimality()) {
            isSolvedToOptimality = false;
        }
        return result;
    }
}
