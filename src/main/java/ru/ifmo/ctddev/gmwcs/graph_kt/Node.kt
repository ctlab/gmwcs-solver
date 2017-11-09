package ru.ifmo.ctddev.gmwcs.graph_kt

/**
 * Created by Nikolay Poperechnyi on 02/11/2017.
 */

class Node(num: Int, w: Double) : Elem(num, w) {

    override fun toString(): String {
        return "N($num, $w)"
    }

}

typealias NodeSequence = Sequence<Node>

typealias NodeList = List<Node>

typealias MutableNodeList = MutableList<Node>

typealias NodeSet = Set<Node>

typealias MutableNodeSet = MutableSet<Node>