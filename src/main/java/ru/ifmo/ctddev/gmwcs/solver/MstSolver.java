package ru.ifmo.ctddev.gmwcs.solver;

import ru.ifmo.ctddev.gmwcs.graph.Edge;
import ru.ifmo.ctddev.gmwcs.graph.Graph;
import ru.ifmo.ctddev.gmwcs.graph.Node;

import java.util.*;

/**
 * Created by Nikolay Poperechnyi on 30/01/2018.
 */
public class MstSolver {
    private final Graph g;
    private final Map<Edge, Double> ws;
    private final Node root;
    private double cost;
    private List<Edge> res;

    MstSolver(Graph g, Map<Edge, Double> edgeWeights, Node root) {
        this.g = g;
        this.ws = edgeWeights;
        this.root = root;
    }

    public double getCost() {
        return cost;
    }
    public List<Edge> getEdges() {
        return res;
    }

    public void solve() {
        Double cost = 0.0;
        List<Edge> res = new ArrayList<>();
        Set<Node> unvisited = new HashSet<>(g.vertexSet());
        unvisited.remove(root);
        PriorityQueue<Edge> q =
                new PriorityQueue<>(Comparator.comparingDouble(ws::get));
        Node cur = root;
        while (!unvisited.isEmpty()) {
            for (Edge e : g.edgesOf(cur)) {
                if (unvisited.contains(g.opposite(cur, e))) {
                    q.add(e);
                }
            }
            final Edge e = q.remove();
            cost += ws.get(e);
            cur = g.opposite(cur, e);
            res.add(e);
            unvisited.remove(cur);
        }
        this.cost = cost;
        this.res = res;
    }

}
