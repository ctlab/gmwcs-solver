import ru.ifmo.ctddev.gmwcs.graph.Graph;

class TestCase {
    private Graph graph;

    TestCase(Graph graph) {
        this.graph = graph;
    }

    Graph graph() {
        return graph;
    }

    int n() {
        return graph.vertexSet().size();
    }

    int m() {
        return graph.edgeSet().size();
    }
}
