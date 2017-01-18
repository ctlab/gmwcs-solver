package ru.ifmo.ctddev.gmwcs.graph;

import java.io.*;
import java.text.ParseException;
import java.util.*;

public class SimpleIO implements GraphIO {
    private File nodeIn;
    private File nodeOut;
    private File edgeIn;
    private File compFile;
    private List<String> nodeList;
    private Map<String, Node> nodeMap;

    public SimpleIO(File nodeIn, File nodeOut, File edgeIn, File compFile) {
        this.nodeIn = nodeIn;
        this.edgeIn = edgeIn;
        this.nodeOut = nodeOut;
        this.compFile = compFile;
        nodeMap = new LinkedHashMap<>();
        nodeList = new ArrayList<>();
    }

    @Override
    public Graph read() throws FileNotFoundException, ParseException {
        Graph graph;
        try (Scanner nodes = new Scanner(new BufferedReader(new FileReader(nodeIn)));
             Scanner edges = new Scanner(new BufferedReader(new FileReader(edgeIn)))) {
            graph = new Graph();
            parseNodes(nodes, graph);
            parseEdges(edges, graph);
        }
        if(compFile != null){
            try(Scanner comp = new Scanner(new BufferedReader(new FileReader(compFile)))){
                parseRequirements(comp);
            }
        }
        return graph;
    }

    private void parseRequirements(Scanner comp) throws ParseException {
        int lnum = 0;
        while(comp.hasNext()){
            String vertex = comp.nextLine();
            if(!nodeMap.containsKey(vertex)){
                throw new ParseException("Compulsory file contains non-vertex " + vertex, lnum);
            }
            nodeMap.get(vertex).setRequired(true);
        }
    }

    private void parseNodes(Scanner nodes, Graph graph) throws ParseException {
        int lnum = 0;
        while (nodes.hasNextLine()) {
            lnum++;
            String line = nodes.nextLine();
            if (line.startsWith("#")) {
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(line);
            if (!tokenizer.hasMoreTokens()) {
                continue;
            }
            String node = tokenizer.nextToken();
            nodeList.add(node);
            if (!tokenizer.hasMoreTokens()) {
                throw new ParseException("Expected weight of node in line", lnum);
            }
            String weightStr = tokenizer.nextToken();
            try {
                double weight = Double.parseDouble(weightStr);
                Node vertex = new Node(lnum, weight);
                nodeMap.put(node, vertex);
                graph.addVertex(vertex);

            } catch (NumberFormatException e) {
                throw new ParseException("Expected floating point value of node weight in line", lnum);
            }
        }
    }

    private void parseEdges(Scanner edges, Graph graph) throws ParseException {
        int lnum = 0;
        while (edges.hasNextLine()) {
            lnum++;
            String line = edges.nextLine();
            if (line.startsWith("#")) {
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(line);
            if (!tokenizer.hasMoreTokens()) {
                continue;
            }
            String first = tokenizer.nextToken();
            if (!tokenizer.hasMoreTokens()) {
                throw new ParseException("Expected name of second node in edge list in line", lnum);
            }
            String second = tokenizer.nextToken();
            try {
                if (!nodeMap.containsKey(first) || !nodeMap.containsKey(second)) {
                    throw new ParseException("There's no such vertex in edge list in line", lnum);
                }
                Edge edge = new Edge(lnum, 0.0);
                Node from = nodeMap.get(first);
                Node to = nodeMap.get(second);
                graph.addEdge(from, to, edge);
            } catch (NumberFormatException e) {
                throw new ParseException("Expected floating point value of edge in line", lnum);
            }
        }
    }

    @Override
    public void write(List<Unit> units) throws IOException {
        Set<Unit> unitSet = new LinkedHashSet<>();
        if (units == null) {
            units = new ArrayList<>();
        }
        unitSet.addAll(units);
        writeNodes(unitSet);
    }

    private void writeNodes(Set<Unit> units) throws IOException {
        double sum = 0.0;
        try (Writer writer = new BufferedWriter(new FileWriter(nodeOut))) {
            for (String name : nodeList) {
                Node node = nodeMap.get(name);
                if (units.contains(node)) {
                    sum += node.getWeight();
                }
                if(units.contains(node)) {
                    writer.write(name + "\n");
                }
            }
            writer.write("#score\t" + sum);
        }
    }
}
