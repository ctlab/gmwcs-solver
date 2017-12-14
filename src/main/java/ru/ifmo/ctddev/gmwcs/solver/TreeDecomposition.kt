package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph_kt.*

/**
 * Created by Nikolay Poperechnyi on 30/11/2017.
 */

var edgeNumber = 0

fun greedyDegree(g: Graph): Node? {
    return g.nodeSet().minBy { g.degreeOf(it) }
}

fun makeClique(g: Graph, nodes: NodeList) {
    for (n1 in nodes) {
        for (n2 in nodes) {
            if (n1 != n2 && !g.neighborsOf(n1).contains(n2)) {
                val e = Edge(edgeNumber, -1000.0)
                edgeNumber++
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
                      ordering: MutableNodeList = mutableListOf()): NodeList {
    if (edgeNumber == 0) edgeNumber = g.edgeSet().size
    val min = argmin(g)
    return when (min) {
        null -> ordering
        else -> {
            ordering.add(min)
            eliminate(g, min)
            greedyElimination(g, argmin, ordering)
        }
    }
}

data class TreeDecomposition(val bags: MutableSet<Set<Node>>, val t: Graph)

fun bag(g: Graph, node: Node): Set<Node> {
    return g.neighborsOf(node).plus(node).toSet()
}


fun treeDecomposition(g: Graph, ordering: NodeList): TreeDecomposition {
    val v = ordering[0]
    return if (ordering.size == 1) {
        TreeDecomposition(mutableSetOf(setOf(v)), g)
    } else {
        val bag = bag(g, v)
        val u = g.neighborsOf(v).minBy { ordering.indexOf(it) }!!
        val e = g.getEdge(v, u)!!
        eliminate(g, v)
        val decomp = treeDecomposition(g, ordering.drop(1))
        decomp.t.addNode(v)
        decomp.bags.add(bag)
        decomp.t.addEdge(e, v, u)
        decomp
    }
}
