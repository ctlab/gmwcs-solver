import org.jgrapht.UndirectedGraph;
import ru.ifmo.ctddev.gmwcs.graph.Edge;
import ru.ifmo.ctddev.gmwcs.graph.Node;

public class TestCase {
    private UndirectedGraph<Node, Edge> graph;

    public TestCase(UndirectedGraph<Node, Edge> graph) {
        this.graph = graph;
    }

    public UndirectedGraph<Node, Edge> graph() {
        return graph;
    }

    public int n() {
        return graph.vertexSet().size();
    }

    public int m() {
        return graph.edgeSet().size();
    }
}
