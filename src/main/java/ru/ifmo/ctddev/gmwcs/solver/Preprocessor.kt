package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node

/**
 * Created by Nikolay Poperechnyi on 03/10/2017.
 */
class Preprocessor(val graph: Graph) {

    private fun Graph.getAdjacent(e: Edge) = Pair(this.getEdgeSource(e), this.getEdgeTarget(e))

    fun preprocess() {
        mergePositive()
        mergeNegative()
        negativeEdges()
        negativeVertices()
        cns()
    }

    private fun mergeNegative() {
        for (v in graph.vertexSet().toList()) {
            if (v.weight > 0 || graph.degreeOf(v) != 2) {
                continue
            }
            val edges = graph.edgesOf(v).toTypedArray()
            if (edges[0].weight > 0 || edges[1].weight > 0)
                continue
            val nodes = edges.map { graph.opposite(v, it) }
            val (l, r) = Pair(nodes[0], nodes[1])
            graph.removeVertex(v)
            if (l != r) {
                edges[0].absorb(v)
                edges[0].absorb(edges[1])
                graph.addEdge(l, r, edges[0])
            }
        }
    }

    private fun mergePositive() {
        for (edge in graph.edgeSet().toList()) {
            if (!graph.containsEdge(edge))
                continue
            val (from, to) = graph.getAdjacent(edge)
            val ew = edge.weight
            if (ew >= 0 && ew + from.weight >= 0 && ew + to.weight >= 0) {
                merge(edge, from, to)
            }
        }
    }

    private fun negativeVertices() {
        graph.vertexSet().toSet()
                .forEach {
                    vertexTest(it)
                }
    }

    private fun vertexTest(v: Node) {
        if (v.weight <= 0
                && graph.neighborListOf(v).size == 2
                && graph.edgesOf(v).all { it.weight <= 0 }) {
            val neighbors = graph.neighborListOf(v)
            val n1 = neighbors[0]
            val n2 = neighbors[1]
            if (Dijkstra(graph, n1).negativeVertex(n2, v))
                graph.removeVertex(v)
        }
    }

    private fun negativeEdges() {
        graph.vertexSet()
                .forEach { n ->
                    val neighs = graph.edgesOf(n)
                            .filter { it.weight <= 0 }
                            .map { graph.opposite(n, it) }.toSet()
                    if (!neighs.isEmpty())
                        Dijkstra(graph, n).negativeEdges(neighs)
                                .forEach { graph.removeEdge(it) }
                }
    }

    private fun cns() {
        val toRemove = mutableSetOf<Node>()
        val vertexSet = graph.vertexSet()
        for (v in vertexSet) {
            if (!toRemove.contains(v)) {
                toRemove.addAll(cnsTest(v))
            }
        }
        toRemove.forEach { graph.removeVertex(it) }
    }

    private fun cnsTest(v: Node): Set<Node> {
        val res = mutableSetOf<Node>()
        val (w, wSum) = goodNeighbors(v)
        for (u in w) {
            for (cand in graph.neighborListOf(u).filter { !w.contains(it) }) {
                if (res.contains(cand)) continue
                val bestSum = cand.weight + graph.edgesOf(cand)
                        .sumByDouble { Math.max(it.weight, 0.0) }
                if (bestSum >= 0) continue
                val candN = graph.neighborListOf(cand)
                if (w.containsAll(candN) && bestSum < wSum) {
                    res.add(cand)
                }
            }
        }
        return res
    }

    data class Neighbors(val w: MutableSet<Node>, val sum: Double)

    private fun goodNeighbors(v: Node): Neighbors {
        var wSum = minOf(v.weight, 0.0)
        val w = mutableSetOf(v)
        for (u in graph.neighborListOf(v)) {
            val edges = graph.getAllEdges(u, v)
            edges.sortBy { -it.weight }
            val posSum = u.weight + edges[0].weight + edges.drop(1)
                    .takeWhile { it.weight >= 0 }
                    .sumByDouble { it.weight }
            if (posSum >= 0) {
                wSum += edges.dropWhile { it.weight >= 0 }.sumByDouble { it.weight }
                w.add(u)
            }
        }
        return Neighbors(w, wSum)
    }

    private fun merge(e: Edge, l: Node, r: Node) {
        if (!listOf(l, r).containsAll(graph.getAdjacent(e).toList()))
            throw IllegalArgumentException()
        contract(e)
    }

    private fun contract(e: Edge) {
        val (main, aux) = graph.getAdjacent(e)
        val auxEdges = graph.edgesOf(aux).toMutableSet()
        auxEdges.remove(e)
        for (edge in auxEdges) {
            val opposite = graph.opposite(aux, edge)
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