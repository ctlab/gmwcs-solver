package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node

/**
 * Created by Nikolay Poperechnyi on 03/10/2017.
 */
class Preprocessor(val graph: Graph) {

    fun Graph.getAdjacent(e: Edge) = Pair(this.getEdgeSource(e), this.getEdgeTarget(e))


    fun preprocess(): Unit {
        for (edge in graph.edgeSet().toList()) {
            if (!graph.containsEdge(edge)) {
                continue;
            }
            val (from, to) = graph.getAdjacent(edge)
            val ew = edge.weight
            if (ew >= 0 && ew + from.weight >= 0 && ew + to.weight >= 0) {
                merge(edge, from, to)
            }
        }
        for (v in graph.vertexSet().toList()
                .filter { it.weight <= 0 && graph.degreeOf(it) == 2 }) {
            val edges = graph.edgesOf(v).toTypedArray()
            val nodes = edges.map { it -> graph.getOppositeVertex(v, it) }
            val (l, r) = Pair(nodes[0], nodes[1])
            graph.removeVertex(v);
            if (l != r) {
                edges[0].absorb(v);
                edges[0].absorb(edges[1])
                graph.addEdge(l, r, edges[0])
            }
        }
    }

    private fun merge(e: Edge, l: Node, r: Node): Unit {
        if (!listOf(l, r).containsAll(graph.getAdjacent(e).toList()))
            throw IllegalArgumentException()
        contract(e);
    }

    private fun contract(e: Edge): Unit {
        val (main, aux) = graph.getAdjacent(e)
        val auxEdges = graph.edgesOf(aux).toMutableSet()
        auxEdges.remove(e)
        for (edge in auxEdges) {
            val opposite = graph.getOppositeVertex(aux, edge)
            val m = graph.getEdge(main, opposite)
            graph.removeEdge(edge)
            if (m == null) {
                if (opposite == main) {
                    if (edge.weight >= 0) {
                        main.absorb(edge)
                    }
                    continue
                }
                graph.addEdge(main, opposite, edge)
            } else {
                if (edge.weight >= 0 && m.weight >= 0) {
                    m.absorb(edge)
                } else {
                    if (m.weight < edge.weight) {
                        graph.removeEdge(m);
                        graph.addEdge(main, opposite, edge)
                    }
                }
            }
        }
        graph.removeVertex(aux)
        main.absorb(aux)
        main.absorb(e)
    }
}