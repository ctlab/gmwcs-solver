package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph_kt.*

/**
 * Created by Nikolay Poperechnyi on 30/11/2017.
 */


fun greedyDegree(g: Graph): Node? {
    return g.nodeSet().minBy { g.degreeOf(it) }
}

fun makeClique(g: Graph, nodes: NodeList) {
    for (n1 in nodes) {
        for (n2 in nodes) {
            if (n1 != n2 && !g.neighborsOf(n1).contains(n2)) {
                val e = Edge(g.edgeSet().size, -1000.0)
                g.addEdge(e, n1, n2) //TODO: find out w for generated edges
            }
        }
    }
}

fun eliminate(g: Graph, v: Node) {
    val neighbors = g.neighborsOf(v)
    makeClique(g, neighbors)
    g.removeNode(v)
}

fun greedyElimination(g: Graph,
                      argmin: (Graph) -> Node? = ::greedyDegree,
                      acu: MutableNodeList ): NodeList {
    val min = argmin(g)
    return when (min) {
        null -> acu
        else -> {
            acu.add(min)
            eliminate(g, min)
            greedyElimination(g, argmin, acu)
        }
    }
}
