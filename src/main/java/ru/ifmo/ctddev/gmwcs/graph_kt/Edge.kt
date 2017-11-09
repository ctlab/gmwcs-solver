package ru.ifmo.ctddev.gmwcs.graph_kt

/**
 * Created by Nikolay Poperechnyi on 02/11/2017.
 */

class Edge(num: Int, w: Double) : Elem(num, w) {

    override fun toString(): String {
        return "E($num, $w)"
    }

}

typealias EdgeSequence = Sequence<Edge>

typealias EdgeList = List<Edge>

typealias MutableEdgeList = MutableList<Edge>

typealias EdgeSet = Set<Edge>

typealias MutableEdgeSet = MutableSet<Edge>
