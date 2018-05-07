package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Elem
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node
import java.util.*

/**
 * Created by Nikolay Poperechnyi on 19.04.18.
 */


data class Region(val center: Set<Node>,
                  val elems: Set<Elem>,
                  val boundary: Set<Elem>)


class Dijkstra(val graph: Graph, val rootNodes: Set<Node>, val posEdges: Set<Edge>) {

    private val n = graph.vertexSet().maxBy { it.num }!!.num + 1

    private val d = DoubleArray(n, { Double.MAX_VALUE })

    private val s = HashMap<Elem, Region>()

    fun findRegions() {
        val queue = PriorityQueue<Node>(
                { n1, n2 -> (d[n1.num] - d[n2.num]).compareTo(0) }
        )
        for (node in rootNodes) {
            d[node.num] = maxOf(0.0, -node.weight)
            queue.add(node)
        }
        while (!queue.isEmpty()) {
            val cur = queue.poll()

        }
    }

}
/*
fun getRegions(graph: Graph, solution: List<Elem>): Set<Region>? {
    val centers = mutableSetOf<Node>()
    val posEdges = mutableSetOf<Edge>()
    for (elem in solution) {
        if (elem.weight > 0) {
            if (elem is Edge) {
                posEdges.add(elem)
            }
            if (elem is Node) {
                centers.add(elem)
            }
        }
    }
    for (e in posEdges) {
        val u = graph.getEdgeSource(e)
        val v = graph.getEdgeTarget(e)
        if (u.weight < 0) {
            centers.add(u)
        }
        if (v.weight < 0) {
            centers.add(v)
        }
    }
}

*/
