import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldEqual
import io.kotlintest.specs.StringSpec
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.SimpleIO
import ru.ifmo.ctddev.gmwcs.sampleGraph
import ru.ifmo.ctddev.gmwcs.solver.MSTSolver
import ru.ifmo.ctddev.gmwcs.solver.RLTSolver
import ru.ifmo.ctddev.gmwcs.solver.solve
import java.io.File


/**
 * Created by Nikolay Poperechnyi on 16.03.18.
 */

class TreeSolverSpec: StringSpec() {
    init {
        val eps = 1e-7
        for (test in 2..3) {
            val nodeFile = File("instance$test/nodes_out")
            val edgeFile = File("instance$test/edges_out")
            val graphIO = SimpleIO(nodeFile, File(nodeFile.toString() + ".out"),
                    edgeFile, File(edgeFile.toString() + ".out"))
            val graph = graphIO.read()

            "TreeSolver must properly solve GMWCS problem for trees" {
                sampleGraph(graph, 50, 300, false).forEach {
                    val g = it.graph
                    val edges = g.edgeSet()
                    val nodes = g.vertexSet()
                    nodes.size shouldBe edges.size + 1
                    val sol = solve(g, nodes.iterator().next(), null).bestD
                    val refSol = RLTSolver().solve(g).sumByDouble { it.weight }
                    (Math.abs(sol - refSol) < eps) shouldBe true
                }
            }
        }

    }

}

