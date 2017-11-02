package ru.ifmo.ctddev.gmwcs.graph_kt

/**
 * Created by Nikolay Poperechnyi on 02/11/2017.
 */

private typealias LinkList = MutableList<Link>

private typealias AdjacencyList = MutableMap<Node, Edge>

private typealias AdjacencyMatrix = MutableMap<Node, AdjacencyList>

private typealias EdgeLinkMap = MutableMap<Edge, Link>

private typealias NodeLinkListMap = MutableMap<Node, LinkList>

data class Link(val e: Edge, val u: Node, val v: Node)

class Graph {

    private val nodeEdges: AdjacencyMatrix = mutableMapOf()
    private val edgeLinks: EdgeLinkMap = mutableMapOf()
    private val nodeLinks: NodeLinkListMap = mutableMapOf()

    fun addNode(v: Node) {
        assert(!nodeLinks.containsKey(v))
        nodeLinks[v] = mutableListOf()
        nodeEdges[v] = mutableMapOf()
    }

    fun addEdge(e: Edge, u: Node, v: Node) {
        assert(!edgeLinks.containsKey(e))
        assert(nodeEdges.containsKey(u))
        assert(nodeEdges.containsKey(v))
        addLink(u, Link(e, u, v))
        addLink(v, Link(e, v, u))
        addLink(e, Link(e, u, v))
        nodeEdges[u]!![v] = e
        nodeEdges[v]!![u] = e
    }

    private fun addLink(e: Edge, l: Link) {
        edgeLinks[e] = l
    }

    private fun addLink(u: Node, l: Link) {
        nodeLinks.getOrPut(u, { mutableListOf() }).add(l)
    }

    fun edgesOf(v: Node): EdgeList {
        return nodeLinks[v].orEmpty().map { it.e }
    }

    fun neighborsOf(v: Node): NodeList {
        return nodeLinks[v].orEmpty().map { it.v }
    }

    fun getNode(u: Node, e: Edge): Node {
        assert(edgeLinks.containsKey(e))
        val link = edgeLinks[e]
        return when (u == link!!.u) {
            true -> link.v
            false -> link.u
        }
    }

    fun getEdge(u: Node, v: Node): Edge? {
        assert(nodeEdges.containsKey(u))
        return nodeEdges[u]!![v]
    }

    fun removeNode(v: Node) {
        assert(nodeLinks.containsKey(v))
        for ((e, v1, u) in nodeLinks[v]!!) {
            assert(v1 == v)
            nodeLinks[u]!!.removeAll {it.v == v}
            edgeLinks.remove(e)
        }
        nodeLinks.remove(v)
    }

    fun removeEdge(e: Edge) {
        assert(edgeLinks.containsKey(e))
        val (_, u, v) = edgeLinks[e]!!
        nodeLinks[u]!!.removeAll {it.u == u}
        nodeLinks[v]!!.removeAll {it.u == v}
        edgeLinks.remove(e)
    }

}