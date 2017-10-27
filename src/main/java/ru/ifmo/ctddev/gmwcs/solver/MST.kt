package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Node

/**
 * Created by Nikolay Poperechnyi on 27/10/2017.
 */
typealias NodeArray = Array<Node>
typealias Dist = Map<Node, Double>

class MST(val nodes: Map<Node, Dist>) {
    private var res: Double? = null

    fun solve(): Double {
        if (res != null) {
            return res!!
        }
        res = 0.0
        val entries = nodes.entries
        val (start, neighbors) = entries.first()
        assert(nodes.size > 1, { "MST for non-tree" })
        assert(nodes.size == neighbors.size, { "MST for non-clique" })
        val tree = mutableSetOf(start)
        while (tree.size != nodes.size) {
            var best: Node? = null
            var bestW = 0.0
            for (t in tree) {
                val near = nodes[t]!!
                for ((node, _) in near) {
                    if (tree.contains(node)) {
                        continue
                    }
                    if (best == null || near[node]!! < bestW) {
                        best = node
                        bestW = near[node]!!
                    }
                }
            }
            tree.add(best!!)
            res = res!!.plus(bestW)
        }
        return res!!
    }

}