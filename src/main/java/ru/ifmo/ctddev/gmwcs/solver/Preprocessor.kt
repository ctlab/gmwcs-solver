package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Elem
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node

/**
 * Created by Nikolay Poperechnyi on 03/10/2017.
 */
fun Graph.getAdjacent(e: Edge) = Pair(this.getEdgeSource(e), this.getEdgeTarget(e))

typealias Step<T> = (Graph, MutableSet<T>) -> Set<T>
typealias Reduction<T> = (Graph, Set<T>) -> Unit

class ReductionSequence<T : Elem>(val step: Step<T>, val reduction: Reduction<T>) {
    fun apply(graph: Graph) {
        val res = step(graph, mutableSetOf())
        reduction(graph, res)
    }
}

class ReductionStep<T : Elem>(val graph: Graph, private val step: Step<T>, private val reduction: Reduction<T>) {
    fun apply() {
        val res = step(graph, mutableSetOf())
        reduction(graph, res)
    }
}

typealias Reductions = List<ReductionSequence<out Elem>>

val mergeNeg = ReductionSequence(::mergeNegative, ::logNodes)

val mergePos = ReductionSequence(::mergePositive, { _, _ -> })

val negV = ReductionSequence(::negativeVertices, ::logAndRemoveNodes)

val negE = ReductionSequence(::negativeEdges, ::logAndRemoveEdges)

val cns = ReductionSequence(::cns, ::logAndRemoveNodes)

val allSteps: Reductions = listOf(mergeNeg, mergePos, negV, negE, cns)

fun mergeNegative(graph: Graph, toRemove: MutableSet<Node> = mutableSetOf()): Set<Node> {
    for (v in graph.vertexSet().toList()) {
        if (v.weight > 0 || graph.degreeOf(v) != 2) {
            continue
        }
        val edges = graph.edgesOf(v).toTypedArray()
        if (edges[0].weight > 0 || edges[1].weight > 0)
            continue
        val nodes = edges.map { graph.opposite(v, it) }
        val (l, r) = Pair(nodes[0], nodes[1])
        toRemove.add(v)
        graph.removeVertex(v)
        if (l != r) {
            edges[0].absorb(v)
            edges[0].absorb(edges[1])
            graph.addEdge(l, r, edges[0])
        }
    }
    return toRemove
}

fun mergePositive(graph: Graph, toRemove: MutableSet<Node> = mutableSetOf()): Set<Node> {
    for (edge in graph.edgeSet().toList()) {
        if (!graph.containsEdge(edge))
            continue
        val (from, to) = graph.getAdjacent(edge)
        val ew = edge.weight
        if (ew >= 0 && ew + from.weight >= 0 && ew + to.weight >= 0) {
            merge(graph, edge, from, to)
        }
    }
    return toRemove
}

fun merge(graph: Graph, e: Edge, l: Node, r: Node) {
    if (!listOf(l, r).containsAll(graph.getAdjacent(e).toList()))
        throw IllegalArgumentException()
    contract(graph, e)
}

fun contract(graph: Graph, e: Edge) {
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
                    graph.removeEdge(m)
                    graph.addEdge(main, opposite, edge)
                }
            }
        }
    }
    graph.removeVertex(aux)
    main.absorb(aux)
    main.absorb(e)
}

fun negativeVertices(graph: Graph, toRemove: MutableSet<Node> = mutableSetOf()): Set<Node> {
    return graph.vertexSet().filter { vertexTest(graph, it) }.toSet()
}

fun vertexTest(graph: Graph, v: Node): Boolean {
    return if (v.weight <= 0
            && graph.neighborListOf(v).size == 2
            && graph.edgesOf(v).all { it.weight <= 0 }) {
        val neighbors = graph.neighborListOf(v)
        val n1 = neighbors[0]
        val n2 = neighbors[1]
        Dijkstra(graph, n1).negativeVertex(n2, v)
    } else {
        false
    }
}

private fun cnsTest(graph: Graph, v: Node): Set<Node> {
    val res = mutableSetOf<Node>()
    val (w, wSum) = goodNeighbors(graph, v)
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

fun goodNeighbors(graph: Graph, v: Node): Neighbors {
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

fun negativeEdges(graph: Graph, toRemove: MutableSet<Edge> = mutableSetOf()): Set<Edge> {
    graph.vertexSet()
            .forEach { n ->
                val neighs = graph.edgesOf(n)
                        .filter { it.weight <= 0 }
                        .map { graph.opposite(n, it) }.toSet()
                if (!neighs.isEmpty())
                    Dijkstra(graph, n).negativeEdges(neighs)
                            .forEach { toRemove.add(it) }
            }
    return toRemove
}

fun cns(graph: Graph, toRemove: MutableSet<Node> = mutableSetOf()): Set<Node> {
    graph.vertexSet()
            .forEach {
                if (!toRemove.contains(it))
                    toRemove.addAll(cnsTest(graph, it))
            }
    return toRemove
}

private fun logEdges(graph: Graph, edges: Set<Edge>) {
    val sz = edges.size
    println("$sz edges to remove")
}

private fun logNodes(graph: Graph, nodes: Set<Node>) {
    val sz = nodes.size
    println("$sz nodes to remove")
}

private fun logAndRemoveEdges(graph: Graph, edges: Set<Edge>) {
    logEdges(graph, edges)
    edges.forEach { graph.removeEdge(it) }
}

private fun logAndRemoveNodes(graph: Graph, nodes: Set<Node>) {
    logNodes(graph, nodes)
    nodes.forEach { graph.removeVertex(it) }
}

class Preprocessor(val graph: Graph,
                   private val reductions: Reductions = allSteps,
                   private val logLevel: Int = 1) {

    fun preprocess() {
        for (red in reductions) {
            red.apply(graph)
        }
    }
}