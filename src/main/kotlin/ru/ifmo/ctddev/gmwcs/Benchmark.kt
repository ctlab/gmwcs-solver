package ru.ifmo.ctddev.gmwcs

import ru.ifmo.ctddev.gmwcs.graph.Elem
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.Node
import ru.ifmo.ctddev.gmwcs.graph.SimpleIO
import ru.ifmo.ctddev.gmwcs.solver.RLTSolver
import ru.ifmo.ctddev.gmwcs.solver.preprocessing.*
import ru.ifmo.ctddev.gmwcs.solver.preprocessing.Preprocessor
import java.io.File
import java.io.FileReader
import java.util.*

/**
 * Created by Nikolay Poperechnyi on 19/10/2017.
 */


typealias NamedReductions = Pair<String, List<ReductionSequence<out Elem>>>

data class NamedGraph(val graph: Graph, val name: String)


fun sampleGraph(g: Graph, samples: Int = 10, maxSize: Int = 15): List<NamedGraph> {
    assert(samples > 0)
    val res = List(samples, { NamedGraph(Graph(), "graph-$it") })
    val vertices = g.vertexSet().toMutableList()
    Collections.shuffle(vertices, Random(1337))
    vertices.take(samples).zip(res).forEach { (start, sample) ->
        val deque = ArrayDeque<Node>()
        val visited = HashSet<Node>()
        sample.graph.addVertex(start)
        visited.add(start)
        deque.add(start)
        var count = 1
        while (!deque.isEmpty()) {
            val cur = deque.pollFirst()
            val nbrs = g.neighborListOf(cur).filter {!visited.contains(it)}
            if (nbrs.size + count > maxSize)
                break
            count += nbrs.size
            nbrs.forEach {
                deque.add(it)
                sample.graph.addVertex(it)
                sample.graph.addEdge(it, cur, g.getEdge(it, cur))
            }
            visited.addAll(nbrs)
            deque.addAll(nbrs)
        }
    }
    return res
}


fun benchmark(graphs: Array<NamedGraph>, preprocessingList: List<NamedReductions>) {
    graphs.forEach {
        benchmarkGraph(it, preprocessingList)
    }
}

private fun benchmarkGraph(graph: NamedGraph, reductions: List<NamedReductions>) {
    println("Benchmarks for graph ${graph.name}")
    for (red in reductions) {
        benchmarkGraphPreprocessing(graph.graph, red)
    }
}

private fun benchmarkGraphPreprocessing(graph: Graph, preprocessing: NamedReductions) {
    val benchName = preprocessing.first
    println("--------$benchName--------")
    val sizeBefore = Pair(graph.vertexSet().size, graph.edgeSet().size)
    val timeStart = System.currentTimeMillis()
    Preprocessor(graph, preprocessing.second).preprocess()
    val timeDelta = (System.currentTimeMillis() - timeStart).toDouble() / 1000
    val sizeAfter = Pair(graph.vertexSet().size, graph.edgeSet().size)
    val delta = Pair(sizeBefore.first - sizeAfter.first
            , sizeBefore.second - sizeAfter.second)
    println("---------Results---------")
    println("Time: $timeDelta\nRemoved nodes ${delta.first}, edges: ${delta.second}") // Todo
}

val reductions = mapOf(
        "mergeNeg" to mergeNeg,
        "mergePos" to mergePos,
        "npv" to negV,
        "npe" to negE,
        "cns" to cns,
        "nvk" to nvk
)

fun main(args: Array<String>) {
    val nodeFile = File(args[0])
    val edgeFile = File(args[1])
    val rulesFile = File(args[2])
    val graphIO = SimpleIO(nodeFile, File(nodeFile.toString() + ".out"),
            edgeFile, File(edgeFile.toString() + ".out"))
    val graph = graphIO.read()
    // val tests = mutableListOf<NamedReductions>()
    val samples = sampleGraph(graph)
    for (sample in samples) {
        println("Solving sample ${sample.name}")
        RLTSolver().solve(sample.graph)
    }

    /* setThreads(4)
     FileReader(rulesFile).forEachLine {
         if (!it.contains('#')) {
             val p = Pair(it, it.split(" ").mapNotNull { reductions[it] })
             tests.add(p)
         }
     }
     benchmarkGraph(NamedGraph(graph, "$nodeFile\t$edgeFile"), tests)
     for (test in tests) {
         benchmarkGraphPreprocessing(graph.subgraph(graph.vertexSet()), test)
     } */
}