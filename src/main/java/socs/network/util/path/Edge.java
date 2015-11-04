package socs.network.util.path;

public class Edge  {
    private final String _id;
    private final Vertex _source;
    private final Vertex _destination;
    private final int _weight;

    public Edge(String id, Vertex source, Vertex destination, int weight) {
        _id = id;
        _source = source;
        _destination = destination;
        _weight = weight;
    }

    public String getId() {
        return _id;
    }

    public Vertex getSource() {
        return _source;
    }

    public Vertex getDestination() {
        return _destination;
    }

    public int getWeight() {
        return _weight;
    }

    @Override
    public String toString() {
        return _source + " -> " + _destination;
    }
}