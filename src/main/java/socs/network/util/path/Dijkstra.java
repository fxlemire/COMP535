package socs.network.util.path;

import java.util.*;

public class Dijkstra {
    private final List<Vertex> _nodes;
    private final List<Edge> _edges;
    private Set<Vertex> _settledNodes;
    private Set<Vertex> _unSettledNodes;
    private Map<Vertex, Vertex> _predecessors;
    private Map<Vertex, Integer> _distance;

    public Dijkstra(Graph graph) {
        // create a copy of the array so that we can operate on this array
        _nodes = new ArrayList<>(graph.getVertexes());
        _edges = new ArrayList<>(graph.getEdges());
    }

    public void execute(Vertex source) {
        _settledNodes = new HashSet<>();
        _unSettledNodes = new HashSet<>();
        _distance = new HashMap<>();
        _predecessors = new HashMap<>();
        _distance.put(source, 0);
        _unSettledNodes.add(source);

        while (_unSettledNodes.size() > 0) {
            Vertex node = getMinimum(_unSettledNodes);
            _settledNodes.add(node);
            _unSettledNodes.remove(node);
            findMinimalDistances(node);
        }
    }

    private void findMinimalDistances(Vertex node) {
        List<Vertex> adjacentNodes = getNeighbors(node);

        adjacentNodes.forEach(target -> {
            int formerDistance = getShortestDistance(target);
            int newDistance = getShortestDistance(node) + getDistance(node, target);
            if (formerDistance > newDistance) {
                _distance.put(target, getShortestDistance(node) + getDistance(node, target));
                _predecessors.put(target, node);
                _unSettledNodes.add(target);
            }
        });
    }

    private int getDistance(Vertex node, Vertex target) {
        for (Edge edge : _edges) {
            if (edge.getSource().equals(node) && edge.getDestination().equals(target)) {
                return edge.getWeight();
            }
        }

        throw new RuntimeException("Should not happen");
    }

    private List<Vertex> getNeighbors(Vertex node) {
        List<Vertex> neighbors = new ArrayList<>();

        _edges.forEach(e -> {
            if (e.getSource().equals(node) && !isSettled(e.getDestination())) {
                neighbors.add(e.getDestination());
            }
        });

        return neighbors;
    }

    private Vertex getMinimum(Set<Vertex> vertexes) {
        Vertex minimum = null;

        for (Vertex vertex : vertexes) {
            if (minimum == null) {
                minimum = vertex;
            } else {
                if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
                    minimum = vertex;
                }
            }
        }

        return minimum;
    }

    private boolean isSettled(Vertex vertex) {
        return _settledNodes.contains(vertex);
    }

    private int getShortestDistance(Vertex destination) {
        Integer d = _distance.get(destination);

        if (d == null) {
            return Integer.MAX_VALUE;
        }

        return d;
    }

    public LinkedList<Vertex> getPath(Vertex target) {
        LinkedList<Vertex> path = new LinkedList<>();
        Vertex step = target;

        if (_predecessors.get(step) == null) {
            return null;
        }

        path.add(step);

        while (_predecessors.get(step) != null) {
            step = _predecessors.get(step);
            path.add(step);
        }

        Collections.reverse(path);

        return path;
    }
}