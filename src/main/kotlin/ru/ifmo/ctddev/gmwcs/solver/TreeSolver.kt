package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Elem
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node

/**
 * Created by Nikolay Poperechnyi on 18/01/2018.
 */



data class D(val best: Set<Node>,
             val withRoot: Set<Node>,
             val bestD: Double,
             val withRootD: Double)

fun solve(g: Graph, root: Node, parent: Node?): D {
    val children = if (parent == null) g.neighborListOf(root)
    else g.neighborListOf(root).minus(parent)
    val withRoot = mutableSetOf(root)
    val solutions = mutableSetOf<D>()
    var withRootD = root.weight
    if (children.isEmpty()) {
        return if (root.weight < 0) D(withRoot, emptySet(), 0.0, root.weight)
        else D(withRoot, withRoot, root.weight, root.weight)
    }
    for (e in g.edgesOf(root)) {
        val opp = g.opposite(root, e)
        if (parent != null && opp == parent) continue
        val sub = solve(g, opp, root)
        if (sub.bestD > 0) {
            solutions.add(sub)
        }
        if (sub.withRootD + e.weight > 0) {
            withRoot.addAll(sub.withRoot)
            withRootD += sub.withRootD + e.weight
        }
    }
    val bestSub = solutions.maxBy { it.bestD }!!
    val bestSol = if (bestSub.bestD > withRootD) bestSub.best
    else withRoot
    return D(bestSol, withRoot, maxOf(bestSub.bestD, withRootD), withRootD)
}


fun main(args: Array<String>) {
        val g = Graph()
        val nodes = arrayOf(Node(1, 1.0), Node(2, -1.0), Node(3, 3.0))
        val edges = arrayOf(Edge(1, 1.0), Edge(2, -1.0))
        nodes.forEach { g.addVertex(it) }
        edges.forEach { g.addEdge(nodes[it.num - 1], nodes[it.num], it) }
        print(solve(g, nodes[1], null).withRootD)
}
