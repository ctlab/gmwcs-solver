package ru.ifmo.ctddev.gmwcs.solver;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ru.ifmo.ctddev.gmwcs.Pair;
import ru.ifmo.ctddev.gmwcs.TimeLimit;
import ru.ifmo.ctddev.gmwcs.graph.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static java.util.Arrays.*;

public class RLTSolver extends IloVarHolder implements RootedSolver {
    public static final double EPS = 0.01;
    private IloCplex cplex;
    private Map<Node, IloNumVar> y;
    private Map<Edge, IloNumVar> w;
    private Map<IloNumVar, Double> mstWeights;
    private Map<Edge, Pair<IloNumVar, IloNumVar>> x;
    private Map<Node, IloNumVar> d;
    private Map<Node, IloNumVar> x0;
    private TimeLimit tl;
    private int threads;
    private boolean suppressOutput;
    private Graph graph;
    private double minimum;
    private Node root;
    private boolean isSolvedToOptimality;
    private int maxToAddCuts;
    private int considerCuts;

    public RLTSolver() {
        tl = new TimeLimit(Double.POSITIVE_INFINITY);
        threads = 1;
        this.minimum = -Double.MAX_VALUE;
        maxToAddCuts = considerCuts = Integer.MAX_VALUE;
    }

    public void setMaxToAddCuts(int num) {
        maxToAddCuts = num;
    }

    public void setConsideringCuts(int num) {
        considerCuts = num;
    }

    public void setTimeLimit(TimeLimit tl) {
        this.tl = tl;
    }

    public void setThreadsNum(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException();
        }
        this.threads = threads;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public void initMstWeights() {
        mstWeights = new HashMap<>();
        for (Edge e: graph.edgeSet()) {
            mstWeights.put(w.get(e), e.getWeight() > 0 ? 1.0 : 0);
        }
        for (Node n: graph.vertexSet()) {
            mstWeights.put(y.get(n), n.getWeight() > 0 ? 1.0 : 0);
        }
    }

    @Override
    public List<Elem> solve(Graph graph) throws SolverException {
        try {
            cplex = new IloCplex();
            this.graph = graph;
            initVariables();
            addConstraints();
            addObjective();
            maxSizeConstraints();
            initMstWeights();
            long timeBefore = System.currentTimeMillis();
            if (root == null) {
                breakRootSymmetry();
            } else {
                tighten();
            }
            breakTreeSymmetries();
            tuning(cplex);
            cplex.use(new MstCallback());
            cplex.use(new LogCallback());
            if (!graph.vertexSet().isEmpty()) tryMst(this);
            boolean solFound = cplex.solve();
            tl.spend(Math.min(tl.getRemainingTime(),
                    (System.currentTimeMillis() - timeBefore) / 1000.0));
            if (solFound) {
                return getResult();
            }
            return Collections.emptyList();
        } catch (IloException e) {
            throw new SolverException(e.getMessage());
        } finally {
            cplex.end();
        }
    }

    @Override
    protected void setSolution(IloNumVar[] v, double[] d) throws IloException {
        cplex.addMIPStart(v, d);
    }

    @Override
    protected double getValue(IloNumVar v) throws IloException {
        return mstWeights.get(v);
    }

    private class MstCallback extends IloCplex.HeuristicCallback {
        private int counter = 0;
        private IloVarHolder hld;

        MstCallback() {
            super();
            hld = new IloVarHolder() {
                @Override
                protected void setSolution(IloNumVar[] v, double[] d) throws IloException {
                    MstCallback.this.setSolution(v, d);
                }

                @Override
                protected double getValue(IloNumVar v) throws IloException {
                    return MstCallback.this.getValue(v);
                }
            };
        }

        protected void main() throws IloException {
            if (counter % 1000 == 0 && counter / 1000 < 100) {
                tryMst(this.hld);
            }
            counter++;
        }
    }

    void newSolution(D solution, Graph g, Node root, IloVarHolder hld) throws IloException {
        List<IloNumVar> vars = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        final Double[] w = new Double[this.w.size()];
        final Double[] y = new Double[this.y.size()];
        final Double[] d = new Double[this.d.size()];
        final Double[] x0 = new Double[this.x0.size()];
        final IloNumVar[] w_n = new IloNumVar[this.w.size()];
        final IloNumVar[] y_n = new IloNumVar[this.y.size()];
        final IloNumVar[] d_n = new IloNumVar[this.d.size()];
        final IloNumVar[] x0_n = new IloNumVar[this.x0.size()];
        Node cur;
        final Set<Edge> zeroEdges = new HashSet<>(this.graph.edgeSet());
        final Set<Node> nodes = new HashSet<>(solution.getWithRoot());
        final Set<Node> zeroNodes = new HashSet<>(this.graph.vertexSet());
        zeroNodes.removeAll(nodes);
        final Deque<Node> deque = new ArrayDeque<>();
        deque.add(root != null ? root
                : nodes.stream().max(Comparator.comparingDouble(Node::getWeight))
                .get()
        );
        int n = 0;
        List<IloNumVar> arcs = new ArrayList<>();
        List<Double> arcs_w = new ArrayList<>();
        d[0] = 0.0;
        d_n[0] = this.d.get(deque.getFirst());
        fill(x0, 0.0);
        x0[0] = 1.0;
        int dist = 0;
        while (!deque.isEmpty()) {
            cur = deque.pollFirst();
            x0_n[n] = this.x0.get(cur);
            y_n[n] = this.y.get(cur);
            y[n] = 1.0;
            n++;
            nodes.remove(cur);
            int l = deque.size();
            List<Node> neighbors = g.neighborListOf(cur)
                    .stream().filter(nodes::contains).collect(Collectors.toList());
            if (!neighbors.isEmpty()) {
                dist++;
            }
            for (Node node : neighbors) {
                d_n[n + l] = this.d.get(node);
                d[n + l] = (double) dist;
                l++;
                Edge e = g.getEdge(node, cur);
                arcs.add(getX(e, node));
                arcs.add(getX(e, cur));
                arcs_w.add(1.0);
                arcs_w.add(0.0);
                zeroEdges.remove(e);
                vars.add(RLTSolver.this.w.get(e));
                weights.add(1.0);
                deque.add(node);
            }
        }
        for (Edge e : zeroEdges) {
            vars.add(RLTSolver.this.w.get(e));
            weights.add(0.0);
            Pair<IloNumVar, IloNumVar> p = this.x.get(e);
            arcs.add(p.first);
            arcs.add(p.second);
            arcs_w.add(0.0);
            arcs_w.add(0.0);
        }
        final Double[] x = new Double[arcs.size()];
        final IloNumVar[] x_n = new IloNumVar[arcs.size()];
        for (Node node : zeroNodes) {
            x0_n[n] = RLTSolver.this.x0.get(node);
            d_n[n] = RLTSolver.this.d.get(node);
            d[n] = 0.0;
            y[n] = 0.0;
            y_n[n] = RLTSolver.this.y.get(node);
            n++;
        }
        for (int i = 0; i < arcs.size(); ++i) {
            x_n[i] = arcs.get(i);
            x[i] = arcs_w.get(i);
        }
        for (int i = 0; i < weights.size(); ++i) {
            w[i] = weights.get(i);
            w_n[i] = vars.get(i);
        }
        List<IloNumVar> sol_n = new ArrayList<>(asList(w_n));
        sol_n.addAll(asList(y_n));
        sol_n.addAll(asList(d_n));
        sol_n.addAll(asList(x_n));
        sol_n.addAll(asList(x0_n));
        List<Double> sol = new ArrayList<>(asList(w));
        sol.addAll(asList(y));
        sol.addAll(asList(d));
        sol.addAll(asList(x));
        sol.addAll(asList(x0));
        double[] solD = new double[sol.size()];
        for (int i = 0; i < sol.size(); ++i) {
            solD[i] = sol.get(i);
        }
        hld.setSolution(sol_n.toArray(new IloNumVar[0]), solD);
    }

    private void tryMst(IloVarHolder hld) throws IloException {
        Map<Edge, Double> ews = hld.buildVarGraph(graph, this.y, this.w);
        D solution = null;
        Graph gr = null;
        for (Set<Node> set : graph.connectedSets()) {
            final Node root = Optional.ofNullable(this.root).orElse(
                    set.stream().max(
                            Comparator.comparingDouble(Node::getWeight)
                    ).get() // Assuming that connected set is not empty
            );
            Graph g = graph.subgraph(set);
            if (!g.containsVertex(root)) {
                continue;
            }
            MSTSolver mst = new MSTSolver(g, ews, root);
            mst.solve();
            g = graph.subgraph(g.vertexSet(), new HashSet<>(mst.getEdges()));
            D sol = TreeSolverKt.solve(g, root, null);
            if (solution == null || solution.getBestD() < sol.getBestD()) {
                solution = sol;
                gr = g;
            }
        }
        final double best = solution.getBestD();
        System.err.println("mst found solution with score " + best);
        if (cplex.getParam(IloCplex.DoubleParam.CutLo) < best) {
            newSolution(solution, gr, this.root, hld);
        }
    }

    private void breakTreeSymmetries() throws IloException {
        int n = graph.vertexSet().size();
        for (Edge e : graph.edgeSet()) {
            Node from = graph.getEdgeSource(e);
            Node to = graph.getEdgeTarget(e);
            cplex.addLe(cplex.sum(d.get(from), cplex.prod(n - 1, w.get(e))), cplex.sum(n, d.get(to)));
            cplex.addLe(cplex.sum(d.get(to), cplex.prod(n - 1, w.get(e))), cplex.sum(n, d.get(from)));
        }
    }

    private void tighten() throws IloException {
        Blocks blocks = new Blocks(graph);
        Separator separator = new Separator(y, w, cplex, graph);
        separator.setMaxToAdd(maxToAddCuts);
        separator.setMinToConsider(considerCuts);
        if (blocks.cutpoints().contains(root)) {
            for (Set<Node> component : blocks.incidentBlocks(root)) {
                dfs(root, component, true, blocks, separator);
            }
        } else {
            dfs(root, blocks.componentOf(root), true, blocks, separator);
        }
        cplex.use(separator);
    }

    private void dfs(Node root, Set<Node> component, boolean fake, Blocks blocks, Separator separator) throws IloException {
        separator.addComponent(graph.subgraph(component), root);
        if (!fake) {
            for (Node node : component) {
                cplex.addLe(cplex.diff(y.get(node), y.get(root)), 0);
            }
        }
        for (Edge e : graph.edgesOf(root)) {
            if (!component.contains(graph.opposite(root, e))) {
                continue;
            }
            cplex.addEq(getX(e, root), 0);
        }
        for (Node cp : blocks.cutpointsOf(component)) {
            if (root != cp) {
                for (Set<Node> comp : blocks.incidentBlocks(cp)) {
                    if (comp != component) {
                        dfs(cp, comp, false, blocks, separator);
                    }
                }
            }
        }
    }

    public boolean isSolvedToOptimality() {
        return isSolvedToOptimality;
    }

    private List<Elem> getResult() throws IloException {
        isSolvedToOptimality = false;
        List<Elem> result = new ArrayList<>();
        for (Node node : graph.vertexSet()) {
            if (cplex.getValue(y.get(node)) > EPS) {
                result.add(node);
            }
        }
        for (Edge edge : graph.edgeSet()) {
            if (cplex.getValue(w.get(edge)) > EPS) {
                result.add(edge);
            }
        }
        if (cplex.getStatus() == IloCplex.Status.Optimal) {
            isSolvedToOptimality = true;
        }
        return result;
    }

    private void initVariables() throws IloException {
        y = new LinkedHashMap<>();
        w = new LinkedHashMap<>();
        d = new LinkedHashMap<>();
        x = new LinkedHashMap<>();
        x0 = new LinkedHashMap<>();
        for (Node node : graph.vertexSet()) {
            String nodeName = Integer.toString(node.getNum() + 1);
            d.put(node, cplex.numVar(0, Double.MAX_VALUE, "d" + nodeName));
            y.put(node, cplex.boolVar("y" + nodeName));
            x0.put(node, cplex.boolVar("x_0_" + (node.getNum() + 1)));
        }
        for (Edge edge : graph.edgeSet()) {
            Node from = graph.getEdgeSource(edge);
            Node to = graph.getEdgeTarget(edge);
            String edgeName = (from.getNum() + 1) + "_" + (to.getNum() + 1);
            w.put(edge, cplex.boolVar("w_" + edgeName));
            IloNumVar in = cplex.boolVar("x_" + edgeName + "_in");
            IloNumVar out = cplex.boolVar("x_" + edgeName + "_out");
            x.put(edge, new Pair<>(in, out));
        }
    }

    private void tuning(IloCplex cplex) throws IloException {
        if (suppressOutput) {
            cplex.setOut(null);
            cplex.setWarning(null);
        }
        cplex.setParam(IloCplex.IntParam.Threads, threads);
        cplex.setParam(IloCplex.IntParam.ParallelMode, -1);
        cplex.setParam(IloCplex.IntParam.MIPOrdType, 3);
        if (tl.getRemainingTime() <= 0) {
            cplex.setParam(IloCplex.DoubleParam.TiLim, EPS);
        } else if (tl.getRemainingTime() != Double.POSITIVE_INFINITY) {
            cplex.setParam(IloCplex.DoubleParam.TiLim, tl.getRemainingTime());
        }
    }

    private void breakRootSymmetry() throws IloException {
        int n = graph.vertexSet().size();
        PriorityQueue<Node> nodes = new PriorityQueue<>();
        nodes.addAll(graph.vertexSet());
        int k = n;
        IloNumExpr[] terms = new IloNumExpr[n];
        IloNumExpr[] rs = new IloNumExpr[n];
        while (!nodes.isEmpty()) {
            Node node = nodes.poll();
            terms[k - 1] = cplex.prod(k, x0.get(node));
            rs[k - 1] = cplex.prod(k, y.get(node));
            k--;
        }
        IloNumVar sum = cplex.numVar(0, n, "prSum");
        cplex.addEq(sum, cplex.sum(terms));
        for (int i = 0; i < n; i++) {
            cplex.addGe(sum, rs[i]);
        }
    }

    private void addObjective() throws IloException {
        Map<Elem, IloNumVar> summands = new LinkedHashMap<>();
        Set<Elem> toConsider = new LinkedHashSet<>();
        toConsider.addAll(graph.vertexSet());
        toConsider.addAll(graph.edgeSet());
        for (Elem elem : toConsider) {
            summands.put(elem, getVar(elem));
        }
        IloNumExpr sum = unitScalProd(summands.keySet(), summands);
        cplex.addGe(sum, minimum);
        cplex.addMaximize(sum);
    }

    private IloNumVar getVar(Elem elem) {
        return elem instanceof Node ? y.get(elem) : w.get(elem);
    }

    @Override
    public void suppressOutput() {
        suppressOutput = true;
    }

    private void addConstraints() throws IloException {
        sumConstraints();
        otherConstraints();
        distanceConstraints();
    }

    private void distanceConstraints() throws IloException {
        int n = graph.vertexSet().size();
        for (Node v : graph.vertexSet()) {
            cplex.addLe(d.get(v), cplex.diff(n, cplex.prod(n, x0.get(v))));
        }
        for (Edge e : graph.edgeSet()) {
            Node from = graph.getEdgeSource(e);
            Node to = graph.getEdgeTarget(e);
            addEdgeConstraints(e, from, to);
            addEdgeConstraints(e, to, from);
        }
    }

    private void addEdgeConstraints(Edge e, Node from, Node to) throws IloException {
        int n = graph.vertexSet().size();
        IloNumVar z = getX(e, to);
        cplex.addGe(cplex.sum(n, d.get(to)), cplex.sum(d.get(from), cplex.prod(n + 1, z)));
        cplex.addLe(cplex.sum(d.get(to), cplex.prod(n - 1, z)), cplex.sum(d.get(from), n));
    }

    private void maxSizeConstraints() throws IloException {
        for (Node v : graph.vertexSet()) {
            for (Node u : graph.neighborListOf(v)) {
                if (u.getWeight() >= 0) {
                    Edge e = graph.getEdge(v, u);
                    if (e != null && e.getWeight() >= 0) {
                        cplex.addLe(y.get(v), w.get(e));
                    }
                }
            }
        }
    }

    private void otherConstraints() throws IloException {
        // (36), (39)
        for (Edge edge : graph.edgeSet()) {
            Pair<IloNumVar, IloNumVar> arcs = x.get(edge);
            Node from = graph.getEdgeSource(edge);
            Node to = graph.getEdgeTarget(edge);
            cplex.addLe(cplex.sum(arcs.first, arcs.second), w.get(edge));
            cplex.addLe(w.get(edge), y.get(from));
            cplex.addLe(w.get(edge), y.get(to));
        }
    }

    private void sumConstraints() throws IloException {
        // (31)
        cplex.addLe(cplex.sum(graph.vertexSet().stream().map(x -> x0.get(x)).toArray(IloNumVar[]::new)), 1);
        if (root != null) {
            cplex.addEq(x0.get(root), 1);
        }
        // (32)
        for (Node node : graph.vertexSet()) {
            Set<Edge> edges = graph.edgesOf(node);
            IloNumVar xSum[] = new IloNumVar[edges.size() + 1];
            int i = 0;
            for (Edge edge : edges) {
                xSum[i++] = getX(edge, node);
            }
            xSum[xSum.length - 1] = x0.get(node);
            cplex.addEq(cplex.sum(xSum), y.get(node));
        }
    }

    private IloNumVar getX(Edge e, Node to) {
        if (graph.getEdgeSource(e) == to) {
            return x.get(e).first;
        } else {
            return x.get(e).second;
        }
    }

    private IloLinearNumExpr unitScalProd(Set<? extends Elem> units, Map<? extends Elem, IloNumVar> vars) throws IloException {
        int n = units.size();
        double[] coef = new double[n];
        IloNumVar[] variables = new IloNumVar[n];
        int i = 0;
        for (Elem elem : units) {
            coef[i] = elem.getWeight();
            variables[i++] = vars.get(elem);
        }
        return cplex.scalProd(coef, variables);
    }

    public void setLB(double lb) {
        this.minimum = lb;
    }

    private class LogCallback extends IloCplex.IncumbentCallback {

        @Override
        protected void main() throws IloException {
            System.err.println(this.getSolutionSource() == 118);
            System.err.println("Value " + this.getIncumbentObjValue());
        }
    }
}
