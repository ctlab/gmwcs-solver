package ru.ifmo.ctddev.gmwcs.solver


import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node
import ru.ifmo.ctddev.gmwcs.graph.Elem
import java.util.*

/**
 * Created by Nikolay Poperechnyi on 04/10/2017.
 */
class Dijkstra(val graph: Graph) {

    private val n = graph.vertexSet().size

    private val visited = BooleanArray(n, { false })

    private var d = Array(
            n,
            { DoubleArray(n, { Double.MAX_VALUE }) }
    )


    private fun solve(from: Node): Unit {
        val s = from.num
        val queue = PriorityQueue<Node>(
                { n1, n2 -> (d[s][n1.num] - d[s][n2.num]).compareTo(0) }
        )
        d[s][s] = 0.0
        visited[s] = true
        queue.add(from)
        while (queue.isNotEmpty()) {
            val cur = queue.peek()
            visited[cur.num] = true
            for (adj in graph.neighborListOf(cur).filter { !visited[it.num] }) {
                val w = d[s][cur.num] - minOf(graph.getEdge(cur, adj).weight, 0.0)
                if (d[s][adj.num] > w) {
                    d[s][adj.num] = w
                    queue.add(adj)
                }
            }
        }
    }


    fun negativeEdges(s: Node): List<Edge> {
        if (d[s.num][s.num] == Double.MAX_VALUE)
            solve(s)
        return graph.edgesOf(s)
                .map { Pair(it, graph.getOppositeVertex(s, it)) }
                .filter { it.first.weight < 0 && d[s.num][it.second.num] <= it.first.weight }
                .unzip().first
    }


}