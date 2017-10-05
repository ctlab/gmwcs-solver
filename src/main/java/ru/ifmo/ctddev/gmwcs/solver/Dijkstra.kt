package ru.ifmo.ctddev.gmwcs.solver


import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node
import ru.ifmo.ctddev.gmwcs.graph.Elem
import java.util.*

/**
 * Created by Nikolay Poperechnyi on 04/10/2017.
 */
class Dijkstra(val graph: Graph, val from: Node) {
    private val s = from.num;

    private val n = graph.vertexSet().size

    private val visited = BooleanArray(n, { false })

    private var d = Array(
            n,
            { DoubleArray(n, { Double.MAX_VALUE }) }
    )

    private fun solve(): Unit {
        val queue = PriorityQueue<Node>(
                { n1, n2 -> (d[s][n1.num] - d[s][n2.num]).compareTo(0) }
        )
        d[s][s] = 0.0
        queue.add(from)
        while (queue.isNotEmpty()) {
            val cur = queue.peek()
            visited[cur.num] = true
            for (adj in graph.neighborListOf(cur).filter { !visited[it.num] }) {
                // 0 for positive, -weight for negative
                val w = d[s][cur.num] - minOf(graph.getEdge(cur, adj).weight, 0.0)
                                      - minOf(cur.weight, 0.0)
                if (d[s][adj.num] > w) {
                    d[s][adj.num] = w
                    queue.add(adj)
                }
            }
        }
    }

    fun negativeEdges(): List<Edge> {
        if (d[s][s] == Double.MAX_VALUE)
            solve()
        return graph.edgesOf(from).filter { it.weight <= 0
                    && d[s][graph.getOppositeVertex(from, it).num] < -it.weight
        }
    }


}