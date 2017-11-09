import io.kotlintest.Spec
import io.kotlintest.forAll
import io.kotlintest.matchers.shouldBe
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import ru.ifmo.ctddev.gmwcs.graph_kt.Edge
import ru.ifmo.ctddev.gmwcs.graph_kt.Graph
import ru.ifmo.ctddev.gmwcs.graph_kt.Node
import java.util.*
import kotlin.streams.toList

fun abs(x: Int) = Math.abs(x)

/**
 * Created by Nikolay Poperechnyi on 09/11/2017.
 */

class GraphSpec : StringSpec() {
    init {
        val N = 5000; val E = 50000L
        val rand = Random()
        val g = Graph()
        val nodes = (1..N).map { Node(it, it + 0.0) }
        val edges = rand.ints(E).map {abs(it)}
                .distinct().mapToObj { Edge(it, it + 0.0) }.toList()
        nodes.forEach { g.addNode(it) }

        "Graph should properly insert nodes" {
            forAll(nodes) { g.nodeSet().contains(it) shouldBe true }
        }

        edges.forEach { g.addEdge(it, nodes[abs(rand.nextInt()).rem(N)], nodes[abs(rand.nextInt()).rem(N)]) }

        "Graph should properly insert edges" {
            forAll(edges) { g.edgeSet().contains(it) shouldBe true }
        }

        "Graph should properly remove elements" {
            val removedEdges = edges.take((E/10).toInt())
            val remainingEdges = edges.drop((E/10).toInt())
            removedEdges.forEach { g.removeEdge(it) }
            forAll(removedEdges) { g.edgeSet().contains(it) shouldBe false }
            forAll(remainingEdges) { g.edgeSet().contains(it) shouldBe true }
            val removedNodes = nodes.take(N/10)
            val edgesWithNodes = removedNodes.flatMap { g.edgesOf(it) }
            val remEdges = g.edgeSet().minus(edgesWithNodes)
            removedNodes.forEach { g.removeNode(it) }
            forAll(removedNodes) { g.nodeSet().contains(it) shouldBe false }
            forAll(edgesWithNodes) { g.edgeSet().contains(it) shouldBe false }
            forAll(remEdges) { g.edgeSet().contains(it) shouldBe true }
        }
    }
}