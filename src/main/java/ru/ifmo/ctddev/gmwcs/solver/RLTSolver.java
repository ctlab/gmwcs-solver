package ru.ifmo.ctddev.gmwcs.solver;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import ru.ifmo.ctddev.gmwcs.LDSU;
import ru.ifmo.ctddev.gmwcs.Pair;
import ru.ifmo.ctddev.gmwcs.TimeLimit;
import ru.ifmo.ctddev.gmwcs.graph.Blocks;
import ru.ifmo.ctddev.gmwcs.graph.Edge;
import ru.ifmo.ctddev.gmwcs.graph.Node;
import ru.ifmo.ctddev.gmwcs.graph.Unit;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RLTSolver implements RootedSolver {
    public static final double EPS = 0.01;
    private IloCplex cplex;
    private Map<Node, IloNumVar> y;
    private Map<Edge, IloNumVar> w;
    private Map<Edge, Pair<IloNumVar, IloNumVar>> x;
    private Map<Node, IloNumVar> d;
    private Map<Node, IloNumVar> x0;
    private TimeLimit tl;
    private int threads;
    private boolean suppressOutput;
    private UndirectedGraph<Node, Edge> graph;
    private double minimum;
    private Node root;
    private SolutionCallback solutionCallback;
    private boolean isSolvedToOptimality;
    private int maxToAddCuts;
    private int considerCuts;

    public RLTSolver() {
        tl = new TimeLimit(Double.POSITIVE_INFINITY);
        threads = 1;
        this.minimum = -Double.MAX_VALUE;
        maxToAddCuts = considerCuts = Integer.MAX_VALUE;
    }

    static IloNumVar[] getVars(Set<? extends Unit> units, Map<? extends Unit, IloNumVar> vars) {
        IloNumVar[] result = new IloNumVar[units.size()];
        int i = 0;
        for (Unit unit : units) {
            result[i++] = vars.get(unit);
        }
        return result;
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

    @Override
    public List<Unit> solve(UndirectedGraph<Node, Edge> graph, LDSU<Unit> synonyms) throws SolverException {
        try {
            cplex = new IloCplex();
            this.graph = graph;
            initVariables();
            addConstraints();
            addObjective(synonyms);
            maxSizeConstraints();
            long timeBefore = System.currentTimeMillis();
            if (root == null) {
                breakRootSymmetry();
            } else {
                tighten();
            }
            breakTreeSymmetries();
            tuning(cplex);
            boolean solFound = cplex.solve();
            tl.spend(Math.min(tl.getRemainingTime(), (System.currentTimeMillis() - timeBefore) / 1000.0));
            if (solFound) {
                return getResult();
            }
            return Collections.emptyList();
        } catch (IloException e) {
            throw new SolverException();
        } finally {
            cplex.end();
        }
    }

    private void breakTreeSymmetries() throws IloException {
        int n = graph.vertexSet().size();
        for(Edge e : graph.edgeSet()){
            Node from = graph.getEdgeSource(e);
            Node to = graph.getEdgeTarget(e);
            cplex.addLe(cplex.sum(d.get(from), w.get(e)), cplex.sum(n, d.get(to)));
            cplex.addLe(cplex.sum(d.get(to), w.get(e)), cplex.sum(n, d.get(from)));
        }
    }

    private void tighten() throws IloException {
        Blocks blocks = new Blocks(graph);
        if (!blocks.cutpoints().contains(root)) {
            throw new IllegalArgumentException();
        }
        Separator separator = new Separator(y, w, cplex, graph);
        separator.setMaxToAdd(maxToAddCuts);
        separator.setMinToConsider(considerCuts);
        for (Set<Node> component : blocks.incidentBlocks(root)) {
            dfs(root, component, true, blocks, separator);
        }
        cplex.use(separator);
    }

    private void dfs(Node root, Set<Node> component, boolean fake, Blocks blocks, Separator separator) throws IloException {
        separator.addComponent(Utils.subgraph(graph, component), root);
        if (!fake) {
            for (Node node : component) {
                cplex.addLe(cplex.diff(y.get(node), y.get(root)), 0);
            }
        }
        for (Edge e : graph.edgesOf(root)) {
            if (!component.contains(Graphs.getOppositeVertex(graph, e, root))) {
                continue;
            }
            if (root == graph.getEdgeSource(e)) {
                cplex.addEq(x.get(e).first, 0);
            } else {
                cplex.addEq(x.get(e).second, 0);
            }
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

    private List<Unit> getResult() throws IloException {
        isSolvedToOptimality = false;
        List<Unit> result = new ArrayList<>();
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
            IloNumVar in = cplex.boolVar();
            IloNumVar out = cplex.boolVar();
            x.put(edge, new Pair<>(in, out));
        }
    }

    private void tuning(IloCplex cplex) throws IloException {
        if (suppressOutput) {
            cplex.setOut(null);
            cplex.setWarning(null);
        }
        if (solutionCallback != null) {
            cplex.use(new MIPCallback());
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
        IloNumVar sum = cplex.numVar(0, n);
        cplex.addEq(sum, cplex.sum(terms));
        for(int i = 0; i < n; i++){
            cplex.addGe(sum, rs[i]);
        }
    }

    public void setCallback(SolutionCallback callback) {
        this.solutionCallback = callback;
    }

    private void addObjective(LDSU<Unit> synonyms) throws IloException {
        List<Double> ks = new ArrayList<>();
        List<IloNumVar> vs = new ArrayList<>();
        for (int i = 0; i < synonyms.size(); i++) {
            double weight = synonyms.weight(i);
            List<Unit> set = synonyms.set(i);
            if(set.size() == 0 || weight == 0.0){
                continue;
            }
            ks.add(synonyms.weight(i));
            if(set.size() == 1){
                vs.add(getVar(set.get(0)));
                continue;
            }
            IloNumVar x = cplex.boolVar("s" + i);
            vs.add(x);
            IloNumExpr[] vars = set.stream().map(this::getVar).toArray(IloNumExpr[]::new);
            if(weight > 0){
                cplex.addLe(x, cplex.sum(vars));
            } else {
                cplex.addGe(cplex.prod(vars.length, x), cplex.sum(vars));
            }
        }
        IloNumExpr sum = cplex.scalProd(ks.stream().mapToDouble(d -> d).toArray(),
                                        vs.toArray(new IloNumVar[vs.size()]));
        cplex.addGe(sum, minimum);
        cplex.addMaximize(sum);
    }

    private IloNumVar getVar(Unit unit) {
        return unit instanceof Node ? y.get(unit) : w.get(unit);
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
        for(Node v : graph.vertexSet()){
            cplex.addLe(d.get(v), cplex.diff(n, cplex.prod(n, x0.get(v))));
        }
        for(Edge e : graph.edgeSet()){
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
            for (Node u : Graphs.neighborListOf(graph, v)) {
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
        cplex.addEq(cplex.sum(getVars(graph.vertexSet(), x0)), 1);
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

    private IloNumVar getX(Edge e, Node to){
        if(graph.getEdgeSource(e) == to){
            return x.get(e).first;
        } else {
            return x.get(e).second;
        }
    }

    private IloLinearNumExpr unitScalProd(Set<? extends Unit> units, Map<? extends Unit, IloNumVar> vars) throws IloException {
        int n = units.size();
        double[] coef = new double[n];
        IloNumVar[] variables = new IloNumVar[n];
        int i = 0;
        for (Unit unit : units) {
            coef[i] = unit.getWeight();
            variables[i++] = vars.get(unit);
        }
        return cplex.scalProd(coef, variables);
    }

    public void setLB(double lb) {
        this.minimum = lb;
    }

    public static abstract class SolutionCallback {
        public abstract void main(List<Unit> solution);
    }

    private class MIPCallback extends IloCplex.IncumbentCallback {

        @Override
        protected void main() throws IloException {
            if (solutionCallback == null) {
                return;
            }
            List<Unit> result = new ArrayList<>();
            for (Node node : graph.vertexSet()) {
                if (getValue(y.get(node)) > EPS) {
                    result.add(node);
                }
            }
            for (Edge edge : graph.edgeSet()) {
                if (getValue(w.get(edge)) > EPS) {
                    result.add(edge);
                }
            }
            solutionCallback.main(result);
        }
    }
}
