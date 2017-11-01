package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Elem
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node

/**
 * Created by Nikolay Poperechnyi on 03/10/2017.
 */

fun Graph.getAdjacent(e: Edge) = Pair(this.getEdgeSource(e), this.getEdgeTarget(e))

typealias EdgeSet = Set<Edge>
typealias MutableEdgeSet = MutableSet<Edge>
typealias NodeSet = Set<Node>
typealias MutableNodeSet = MutableSet<Node>

typealias Step<T> = (Graph, MutableSet<T>) -> Set<T>
typealias Reduction<T> = (Graph, Set<T>) -> Unit

private fun <T> powerset(left: Collection<T>, acc: Set<Set<T>> = setOf(emptySet())): Set<Set<T>> = when {
    left.isEmpty() -> acc
    else -> powerset(left.drop(1), acc + acc.map { it + left.first() })
}

class ReductionSequence<T : Elem>(private val step: Step<T>, private val reduction: Reduction<T>) {
    fun apply(graph: Graph) {
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

val nvk = ReductionSequence(
        { graph, toRemove -> negativeVertices(3, graph, toRemove) }
        , ::logAndRemoveNodes)

val allSteps: Reductions = listOf(mergeNeg, mergePos, negV, nvk, negE, cns)

// val allSteps: Reductions = listOf(mergeNeg)
//val allSteps: Reductions = emptyList()

fun mergeNegative(graph: Graph, toRemove: MutableNodeSet = mutableSetOf()): NodeSet {
    for (v in graph.vertexSet().toList()) {
        if (v.weight > 0 || graph.degreeOf(v) != 2) {
            continue
        }
        val edges = graph.edgesOf(v).toTypedArray()
        if (edges[0].weight > 0 || edges[1].weight > 0)
            continue
        val l = graph.opposite(v, edges[0])
        val r = graph.opposite(v, edges[1])
        toRemove.add(v) //TODO: 2 nodes 1 edge invariant broken here
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

private fun merge(graph: Graph, e: Edge, l: Node, r: Node) {
    if (!listOf(l, r).containsAll(graph.getAdjacent(e).toList()))
        throw IllegalArgumentException()
    contract(graph, e)
}

private fun contract(graph: Graph, e: Edge) {
    val (main, aux) = graph.getAdjacent(e)
    val auxEdges = graph.edgesOf(aux)
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
        } else if (edge.weight >= 0 && m.weight >= 0) {
            m.absorb(edge)
        } else if (m.weight <= edge.weight) {
            graph.removeEdge(m)
            graph.addEdge(main, opposite, edge)
        }
    }
    graph.removeVertex(aux)
    main.absorb(aux)
    main.absorb(e)
}

fun negativeVertices(graph: Graph, toRemove: MutableNodeSet = mutableSetOf()): NodeSet {
    return graph.vertexSet().filter { vertexTest(graph, it) }.toSet()
}

private fun vertexTest(graph: Graph, v: Node): Boolean {
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

fun cns(graph: Graph, toRemove: MutableNodeSet = mutableSetOf()): NodeSet {
    graph.vertexSet()
            .forEach {
                if (!toRemove.contains(it))
                    cnsTest(graph, it, toRemove)
            }
    return toRemove
}

private fun cnsTest(graph: Graph, v: Node, toRemove: MutableNodeSet) {
    val (w, wSum, wNeighbors) = constructW(graph, v, toRemove)
    for (u in w) {
        for (cand in graph.neighborListOf(u).filter { !w.contains(it) }) {
            if (toRemove.contains(cand)) continue
            val bestSum = cand.weight + graph.edgesOf(cand)
                    .sumByDouble { Math.max(it.weight, 0.0) }
            if (bestSum >= 0) continue
            val candN = graph.neighborListOf(cand).filter { !w.contains(it) }
            if (wNeighbors.containsAll(candN) && bestSum < wSum) {
                toRemove.add(cand)
            }
        }
    }
}

private data class ConnectedComponent(val w: MutableNodeSet,
                                      val sum: Double,
                                      val wNeighbors: MutableNodeSet)

private fun constructW(graph: Graph, v: Node, toRemove: MutableNodeSet): ConnectedComponent {
    var wSum = minOf(v.weight, 0.0)
    val w = mutableSetOf(v)
    for (u in graph.neighborListOf(v).filter { !toRemove.contains(it) }) {
        val edge = graph.getEdge(u, v)
        val weightSum = edge.weight + u.weight
        if (weightSum >= 0) {
            wSum += minOf(edge.weight, 0.0) + minOf(u.weight, 0.0)
            w.add(u)
        }
    }
    val wNeighbors = mutableSetOf<Node>()
    for (u in w) {
        val nbs = graph.neighborListOf(u)
        for (nb in nbs) {
            if (!w.contains(nb) && !toRemove.contains(nb)) {
                wNeighbors.add(nb)
                wSum += minOf(graph.getEdge(nb, u).weight, 0.0)
            }
        }
    }
    return ConnectedComponent(w, wSum, wNeighbors)
}

fun negativeEdges(graph: Graph, toRemove: MutableEdgeSet = mutableSetOf()): EdgeSet {
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

fun negativeVertices(k: Int, graph: Graph, toRemove: MutableNodeSet): NodeSet {
    if (k < 2) throw IllegalArgumentException("k must be >= 2")
    if (k == 2) {
        return negativeVertices(graph, toRemove)
    }
    graph.vertexSet().filter {
        it.weight <= 0 && 3 <= graph.degreeOf(it) && graph.degreeOf(it) <= k
    }.filterTo(toRemove) { nvkTest(graph, it) }
    return toRemove
}

fun nvkTest(graph: Graph, v: Node): Boolean {
    val vWeight = v.weight + graph.edgesOf(v).map { maxOf(0.0, it.weight) }.sum()
    if (vWeight >= 0) return false
    val delta = graph.neighborListOf(v).toSet()
    val subgraph = graph.subgraph(graph.vertexSet().minus(v))
    val ds = delta.map {
        Pair(it, Dijkstra(subgraph, it).negativeDistances(delta))
    }.toSet()
    val powerset = powerset(ds).map { it.toMap() }
            .map {
                it.mapValues { (_, v) ->
                    v.filterKeys { k -> it.containsKey(k) }
                }
            }
    return powerset.all { it.size < 2 || MST(it).solve() > vWeight }
}

private fun logEdges(graph: Graph, edges: Set<Edge>) {
    println("${edges.size} edges to remove")
}

private fun logNodes(graph: Graph, nodes: Set<Node>) {
    println("${nodes.size} nodes to remove")
}

private fun logAndRemoveEdges(graph: Graph, edges: Set<Edge>) {
    logEdges(graph, edges)
    edges.forEach { graph.removeEdge(it) }
}

private fun logAndRemoveNodes(graph: Graph, nodes: Set<Node>) {
    logNodes(graph, nodes)
    nodes.forEach { graph.removeVertex(it) }
}

fun preprocess(graph: Graph) {
    Preprocessor(graph).preprocess()
}

class Preprocessor(val graph: Graph,
                   private val reductions: Reductions = allSteps,
                   private val logLevel: Int = 0) {

    fun preprocess() {
        for (red in reductions) {
            red.apply(graph)
        }
    }

}