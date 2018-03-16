package ru.ifmo.ctddev.gmwcs

import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node
import java.util.*

/**
 * Created by Nikolay Poperechnyi on 16.03.18.
 */


fun sampleGraph(g: Graph,
                samples: Int = 10,
                maxSize: Int = 15,
                allowCycles: Boolean = false,
                seed: Long = 1337): List<NamedGraph> {
    assert(samples > 0)
    val res = List(samples, { NamedGraph(Graph(), "graph-$it") })
    val vertices = g.vertexSet().toMutableList()
    Collections.shuffle(vertices, Random(seed))
    vertices.take(samples).zip(res).forEach { (start, sample) ->
        val deque = ArrayDeque<Node>()
        val visited = HashSet<Node>()
        sample.graph.addVertex(start)
        visited.add(start)
        deque.add(start)
        var count = 1
        while (!deque.isEmpty()) {
            val cur = deque.pollFirst()
            val nbrs = g.neighborListOf(cur).filter { allowCycles || !visited.contains(it) }
            if (nbrs.size + count > maxSize)
                break
            count += nbrs.size
            nbrs.forEach {
                if (!visited.contains(it)) {
                    deque.add(it)
                    visited.add(it)
                    sample.graph.addVertex(it)
                }
                sample.graph.addEdge(it, cur, g.getEdge(it, cur))
            }
        }
    }
    return res
}