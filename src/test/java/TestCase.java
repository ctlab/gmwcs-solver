import ru.ifmo.ctddev.gmwcs.graph.Graph;

public class TestCase {
    private Graph graph;

    public TestCase(Graph graph) {
        this.graph = graph;
    }

    public Graph graph() {
        return graph;
    }

    public int n() {
        return graph.vertexSet().size();
    }

    public int m() {
        return graph.edgeSet().size();
    }
}
