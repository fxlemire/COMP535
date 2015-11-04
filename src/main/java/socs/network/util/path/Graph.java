package socs.network.util.path;

import java.util.List;

public class Graph {
    private final List<Vertex> _vertexes;
    private final List<Edge> _edges;

    public Graph(List<Vertex> vertexes, List<Edge> edges) {
        _vertexes = vertexes;
        _edges = edges;
    }

    public List<Vertex> getVertexes() {
        return _vertexes;
    }

    public List<Edge> getEdges() {
        return _edges;
    }
}