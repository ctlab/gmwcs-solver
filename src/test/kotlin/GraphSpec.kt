import io.kotlintest.forAll
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import ru.ifmo.ctddev.gmwcs.graph_kt.Edge
import ru.ifmo.ctddev.gmwcs.graph_kt.Graph
import ru.ifmo.ctddev.gmwcs.graph_kt.Node
import ru.ifmo.ctddev.gmwcs.solver.greedyElimination
import ru.ifmo.ctddev.gmwcs.solver.treeDecomposition
import java.util.*
import kotlin.streams.*

fun abs(x: Int) = Math.abs(x)

/**
 * Created by Nikolay Poperechnyi on 09/11/2017.
 */

/*
class GraphSpec : StringSpec() {
    init {
        val N = 50
        val E = 600L
        val rand = Random()
        val g = Graph()
        val nodes = (1..N).map { Node(it, it + 0.0) }
        val edges = rand.ints(E).map { abs(it) }
                .distinct().asSequence().zip(1.rangeTo(E.toInt()).asSequence())
                .map { Edge(it.second, it.first + 0.0) }.toList()
        nodes.forEach { g.addNode(it) }

        "Graph should properly insert nodes" {
            forAll(nodes) { g.nodeSet().contains(it) shouldBe true }
        }

        edges.forEach {
            g.addEdge(it, nodes[abs(rand.nextInt()).rem(N)],
                    nodes[abs(rand.nextInt()).rem(N)])
        }

        "Graph should properly insert edges" {
            forAll(edges) { g.edgeSet().contains(it) shouldBe true }
        }

        "Graph should properly remove elements" {
            val removedEdges = edges.take((E / 10).toInt())
            val remainingEdges = edges.drop((E / 10).toInt())
            removedEdges.forEach { g.removeEdge(it) }
            forAll(removedEdges) { g.edgeSet().contains(it) shouldBe false }
            forAll(remainingEdges) { g.edgeSet().contains(it) shouldBe true }
            val removedNodes = nodes.take(N / 10)
            val edgesWithNodes = removedNodes.flatMap { g.edgesOf(it) }
            val remEdges = g.edgeSet().minus(edgesWithNodes)
            removedNodes.forEach { g.removeNode(it) }
            forAll(removedNodes) { g.nodeSet().contains(it) shouldBe false }
            forAll(edgesWithNodes) { g.edgeSet().contains(it) shouldBe false }
            forAll(remEdges) { g.edgeSet().contains(it) shouldBe true }
        }
        val dfsG = Graph()
        val dfsNodes = (1..N).map { Node(it, it + 0.0) }
        val dfsEdges = (1..N).map { Edge(it, it + 0.0) }
        val compSize = 10
        dfsNodes.forEach { dfsG.addNode(it) }

        (0 until N / compSize).map { it * compSize }
                .forEach { s ->
                    dfsEdges.drop(s).take(compSize).
                            forEach {
                                dfsG.addEdge(it
                                        , dfsNodes[s + it.num.rem(compSize)]
                                        , dfsNodes[s + (it.num + 1).rem(compSize)])
                            }
                }
        "DFS should return reachable nodes" {
            for (comp in 0..N / compSize)
                dfsG.dfs(dfsNodes[comp]).size shouldBe compSize
        }
        "neighborsOf(u) contains v -> neighborsOf(v) contains u" {
            for (n in nodes) {
                val near = g.neighborsOf(n)
                for (neigh in near) {
                    g.neighborsOf(neigh).contains(n) shouldBe true
                }
            }
        }
        "opposite(u) = v -> opposite(v) = u" {
            for (e in g.edgeSet()) {
                val (u, v) = g.getNodes(e)
                g.getOpposite(u, e) shouldBe v
                g.getOpposite(v, e) shouldBe u
            }
        }
        "expected subgraph is returned" {
            val g2 = g.subgraph(g.nodeSet())
            g2.edgeSet().containsAll(g.edgeSet()) shouldBe true
            val g3 = g.subgraph(g.nodeSet().filter { it.num % 2 == 1 })
            g3.nodeSet().all { it.num % 2 == 1 } shouldBe true
        }
        "connected components" {
            val isolated = Graph()
            val n = Math.min(N, 100)
            nodes.take(n).forEach { isolated.addNode(it) }
            isolated.connectedComponents().size shouldBe n
        } //TODO: better test for connected components
        "tree decomposition" {
            g.removeLoops()
            val g2 = g.subgraph(g.nodeSet())
            val g3 = g.subgraph(g.nodeSet())
            val dec = treeDecomposition(g2, greedyElimination(g3))
            dec.t.edgeSet().size shouldBe (dec.t.nodeSet().size - 1)
        }
    }
}
        */