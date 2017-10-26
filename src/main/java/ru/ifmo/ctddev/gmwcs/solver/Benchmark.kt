package ru.ifmo.ctddev.gmwcs.solver

import ru.ifmo.ctddev.gmwcs.graph.Elem
import ru.ifmo.ctddev.gmwcs.graph.Graph
import ru.ifmo.ctddev.gmwcs.graph.SimpleIO
import java.io.File
import java.io.FileReader
import java.time.Clock

/**
 * Created by Nikolay Poperechnyi on 19/10/2017.
 */

data class NamedGraph(val graph: Graph, val name: String)

fun benchmark(graphs: Array<NamedGraph>, preprocessingList: Array<Reductions>) {
    graphs.forEach {
        benchmarkGraph(it, preprocessingList)
    }
}

private fun benchmarkGraph(graph: NamedGraph, reductions: Array<Reductions>) {
    println("Benchmarks for graph ${graph.name}")
    for (red in reductions) {
        benchmarkGraphPreprocessing(graph.graph, red)
    }
}

private fun benchmarkGraphPreprocessing(graph: Graph, preprocessing: Reductions) {
    val benchName = preprocessing.joinToString("\t")
    println("--------Reductions $benchName--------")
    val sizeBefore = Pair(graph.vertexSet().size, graph.edgeSet().size)
    val timeStart = System.currentTimeMillis()
    Preprocessor(graph, preprocessing, 2).preprocess()
    val timeDelta = (System.currentTimeMillis() - timeStart).toDouble() / 1000
    val sizeAfter = Pair(graph.vertexSet().size, graph.edgeSet().size)
    val delta = Pair(sizeAfter.first - sizeBefore.first
            , sizeAfter.second - sizeBefore.second)
    println("----------------Results ----------------")
    println("Time: $timeDelta\nRemoved nodes, edges: $delta") // Todo
}

val reductions = mapOf(
        "mergeNeg" to mergeNeg,
        "mergePos" to mergePos,
        "npv" to negV,
        "npe" to negE,
        "cns" to cns
)

fun main(args: Array<String>) {
    val nodeFile = File(args[0])
    val edgeFile = File(args[1])
    val rulesFile = File(args[2])
    val graphIO = SimpleIO(nodeFile, File(nodeFile.toString() + ".out"),
            edgeFile, File(edgeFile.toString() + ".out"))
    val namedGraph = NamedGraph(graphIO.read(), "Graph 1")
    val tests = mutableListOf<Reductions>()
    FileReader(rulesFile).forEachLine {
        tests.add(it.split(" ").mapNotNull { reductions[it] })
    }
    benchmarkGraphPreprocessing(namedGraph.graph, tests[0])
}