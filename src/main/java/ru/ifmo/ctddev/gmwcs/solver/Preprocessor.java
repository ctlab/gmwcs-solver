package ru.ifmo.ctddev.gmwcs.solver;

import ru.ifmo.ctddev.gmwcs.graph.Edge;
import ru.ifmo.ctddev.gmwcs.graph.Graph;
import ru.ifmo.ctddev.gmwcs.graph.Node;
import ru.ifmo.ctddev.gmwcs.graph.Unit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Preprocessor {
    public static void preprocess(Graph graph) {
        for (Edge edge : new ArrayList<>(graph.edgeSet())) {
            if (!graph.containsEdge(edge)) {
                continue;
            }
            Node from = graph.getEdgeSource(edge);
            Node to = graph.getEdgeTarget(edge);
            double ew = edge.getWeight();
            if (ew >= 0 && ew + from.getWeight() >= 0 && ew + to.getWeight() >= 0) {
                merge(graph, edge, from, to);
            }
        }

        for (Node v : new ArrayList<>(graph.vertexSet())) {
            if (v.getWeight() <= 0 && graph.degreeOf(v) == 2) {
                Edge[] edges = graph.edgesOf(v).stream().toArray(Edge[]::new);
                if (edges[1].getWeight() > 0 || edges[0].getWeight() > 0) {
                    continue;
                }
                Node left = graph.getOppositeVertex(v, edges[0]);
                Node right = graph.getOppositeVertex(v, edges[1]);
                if (left == right) {
                    graph.removeVertex(v);
                } else {
                    graph.removeVertex(v);
                    edges[0].absorb(v);
                    edges[0].absorb(edges[1]);
                    graph.addEdge(left, right, edges[0]);
                }
            }
        }
    }

    private static void merge(Graph graph, Unit... units) {
        Set<Node> nodes = new HashSet<>();
        Set<Edge> edges = new HashSet<>();
        for (Unit unit : units) {
            if (unit instanceof Node) {
                nodes.add((Node) unit);
            } else {
                edges.add((Edge) unit);
            }
        }
        for (Edge e : edges) {
            if (!nodes.contains(graph.getEdgeSource(e)) || !nodes.contains(graph.getEdgeTarget(e))) {
                throw new IllegalArgumentException();
            }
        }
        for (Edge e : edges) {
            contract(graph, e);
        }
    }

    private static void contract(Graph graph, Edge e) {
        Node main = graph.getEdgeSource(e);
        Node aux = graph.getEdgeTarget(e);
        Set<Edge> auxEdges = new HashSet<>(graph.edgesOf(aux));
        auxEdges.remove(e);
        for (Edge a : auxEdges) {
            Node opposite = graph.getOppositeVertex(aux, a);
            Edge m = graph.getEdge(main, opposite);
            graph.removeEdge(a);
            if (m == null) {
                if (opposite == main) {
                    if (a.getWeight() >= 0) {
                        main.absorb(a);
                    }
                    continue;
                }
                graph.addEdge(main, opposite, a);
            } else {
                if (a.getWeight() >= 0 && m.getWeight() >= 0) {
                    m.absorb(a);
                } else {
                    if (m.getWeight() < a.getWeight()) {
                        graph.removeEdge(m);
                        graph.addEdge(main, opposite, a);
                    }
                }
            }
        }
        graph.removeVertex(aux);
        main.absorb(aux);
        main.absorb(e);
    }
}
