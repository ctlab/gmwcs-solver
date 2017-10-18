package ru.ifmo.ctddev.gmwcs.solver


import ru.ifmo.ctddev.gmwcs.graph.Edge
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node
import java.util.*

/**
 * Created by Nikolay Poperechnyi on 04/10/2017.
 */
class Dijkstra(private val graph: Graph, private val from: Node) {

    private val s = from.num

    private val n = graph.vertexSet().maxBy { it.num }!!.num + 1

    private val visited = BooleanArray(n, { false })

    private var d = Array(
            n,
            { DoubleArray(n, { Double.MAX_VALUE }) }
    )

    private fun solve(neighbors: Set<Node>) {
        if (d[s][s] != Double.MAX_VALUE) return

        val queue = PriorityQueue<Node>(
                { n1, n2 -> (d[s][n1.num] - d[s][n2.num]).compareTo(0) }
        )
        d[s][s] = 0.0
        queue.add(from)
        while (queue.isNotEmpty()) {
            val cur = queue.poll()
            visited[cur.num] = true
            // Stop searching if shortest paths are found
            if (neighbors.contains(cur) && neighbors.all { visited[it.num] })
                break
            for (adj in graph.neighborListOf(cur).filter { !visited[it.num] }) {
                // 0 for positive, -weight for negative
                val w = d[s][cur.num] - minOf(graph.getEdge(cur, adj).weight, 0.0) - minOf(cur.weight, 0.0)
                if (d[s][adj.num] > w) {
                    d[s][adj.num] = w
                    queue.add(adj)
                }
            }
        }
    }

    fun negativeEdges(neighbors: Set<Node>): List<Edge> {
        solve(neighbors)
        return graph.edgesOf(from).filter {
            val end = graph.opposite(from ,it).num
            it.weight <= 0 && d[s][end] < -it.weight
        }
    }

    fun negativeVertex(dest: Node, candidate: Node): Boolean {
        solve(setOf(dest))
        // test is passed if candidate for removal is not in the solution
        val candPathW = d[s][candidate.num] + graph.getEdge(candidate, dest).weight
        return d[s][dest.num] != candPathW
    }
}