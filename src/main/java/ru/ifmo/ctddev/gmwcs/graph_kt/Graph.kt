package ru.ifmo.ctddev.gmwcs.graph_kt

/**
 * Created by Nikolay Poperechnyi on 02/11/2017.
 */

private typealias LinkSequence = Sequence<Link>

private typealias LinkList = MutableList<Link>

private typealias AdjacencyList = MutableMap<Edge, Node>

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
        if (u != v)
            addLink(v, Link(e, v, u))
        addLink(e, Link(e, u, v))
        nodeEdges[u]!![e] = v
        nodeEdges[v]!![e] = u
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

    fun getOpposite(u: Node, e: Edge): Node {
        assert(edgeLinks.containsKey(e))
        val link = edgeLinks[e]
        return when (u == link!!.u) {
            true -> link.v
            false -> link.u
        }
    }

    fun getAllEdges(u: Node, v: Node): EdgeList {
        return edgeSequence(u, v).toList()
    }

    fun getEdge(u: Node, v: Node): Edge? {
        return edgeSequence(u, v).firstOrNull()
    }

    fun getNodes(e: Edge): Pair<Node, Node> {
        assert(edgeLinks.containsKey(e))
        return Pair(edgeLinks[e]!!.u, edgeLinks[e]!!.v)
    }

    fun removeNode(v: Node) {
        assert(nodeLinks.containsKey(v))
        for ((e, v1, _) in nodeLinks[v]!!.toList()) {
            assert(v1 == v)
            removeEdge(e)
        }
        nodeLinks.remove(v)
    }

    fun removeEdge(e: Edge) {
        assert(edgeLinks.containsKey(e))
        val (_, u, v) = edgeLinks[e]!!
        nodeLinks[u]!!.removeAll { it.e == e }
        nodeLinks[v]!!.removeAll { it.e == e }
        edgeLinks.remove(e)
    }

    private fun linkSequence(n: Node): LinkSequence {
        assert(nodeLinks.containsKey(n))
        return nodeLinks[n]!!.asSequence()
    }

    private fun edgeSequence(u: Node, v: Node): EdgeSequence {
        return linkSequence(u).filter { it.v == v }.map { it.e }
    }

    fun dfs(u: Node) {
        return dfs(u, {}, mutableSetOf())
    }

    fun <T> dfs(u: Node, f: (NodeSet) -> T,
                visited: MutableNodeSet = mutableSetOf()) {
        visited.add(u)
        f(visited)
        neighborsOf(u).filterNot { visited.contains(it) }
                .forEach { dfs(it, f, visited) }
    }

    fun edgeSet(): EdgeSet {
        return edgeLinks.keys
    }

    fun nodeSet(): NodeSet {
        return nodeLinks.keys
    }

}