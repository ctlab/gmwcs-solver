package ru.ifmo.ctddev.gmwcs.solver;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ru.ifmo.ctddev.gmwcs.Pair;
import ru.ifmo.ctddev.gmwcs.graph.Edge;
import ru.ifmo.ctddev.gmwcs.graph.Graph;
import ru.ifmo.ctddev.gmwcs.graph.Node;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.fill;

/**
 * Created by Nikolay Poperechnyi on 21.02.18.
 */
public abstract class IloVarHolder {

    protected abstract void setSolution(IloNumVar[] v, double[] d) throws IloException;

    protected abstract double getValue(IloNumVar v) throws IloException;

    public Map<Edge, Double> buildVarGraph(Graph graph,
                                           Map<Node, IloNumVar> y,
                                           Map<Edge, IloNumVar> w) throws IloException {
        Map<Edge, Double> result = new HashMap<>();
        for (Edge e : graph.edgeSet()) {
            Node u = graph.getEdgeSource(e);
            Node v = graph.getEdgeTarget(e);
            Double uw = getValue(y.get(u));
            Double vw = getValue(y.get(v));
            Double ew = getValue(w.get(e));
            result.put(e, 3 - uw - vw - ew);
        }
        return result;
    }


}
