package com.hospital.allocator.algo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Edmonds-Karp maximum flow (Ford-Fulkerson with BFS augmenting paths).
 * Self-implemented: no library flow code is used.
 *
 * Time: O(V * E^2) worst case. Space: O(V + E).
 * With unit-capacity middle edges (our roster network) each augmentation pushes
 * 1 unit, so the number of augmentations is bounded by the max-flow value F and
 * the practical cost is O(F * E).
 */
public final class EdmondsKarp {

    public static final class Edge {
        public final int from;
        public final int to;
        public final long capacity;
        public final boolean forward;
        final int rev;
        long residual;

        Edge(int from, int to, int rev, long capacity, boolean forward) {
            this.from = from;
            this.to = to;
            this.rev = rev;
            this.capacity = capacity;
            this.residual = capacity;
            this.forward = forward;
        }

        /** Flow currently routed through this (forward) edge. */
        public long flow() {
            return capacity - residual;
        }
    }

    private final int n;
    private final List<Edge>[] graph;
    private int augmentations;

    @SuppressWarnings("unchecked")
    public EdmondsKarp(int nodeCount) {
        if (nodeCount < 2) {
            throw new IllegalArgumentException("Flow network needs at least 2 nodes");
        }
        this.n = nodeCount;
        this.graph = new List[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            graph[i] = new ArrayList<>();
        }
    }

    public Edge addEdge(int u, int v, long capacity) {
        checkNode(u);
        checkNode(v);
        if (u == v) {
            throw new IllegalArgumentException("Self-loops are not allowed");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Negative capacity: " + capacity);
        }
        Edge fwd = new Edge(u, v, graph[v].size(), capacity, true);
        Edge back = new Edge(v, u, graph[u].size(), 0, false);
        graph[u].add(fwd);
        graph[v].add(back);
        return fwd;
    }

    /** Runs the algorithm and returns the value of the maximum flow from s to t. */
    public long maxFlow(int s, int t) {
        checkNode(s);
        checkNode(t);
        if (s == t) {
            throw new IllegalArgumentException("Source and sink must differ");
        }
        long total = 0;
        augmentations = 0;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        while (true) {
            Edge[] parent = new Edge[n];
            boolean[] seen = new boolean[n];
            seen[s] = true;
            queue.clear();
            queue.add(s);
            while (!queue.isEmpty() && !seen[t]) {
                int u = queue.poll();
                for (Edge e : graph[u]) {
                    if (e.residual > 0 && !seen[e.to]) {
                        seen[e.to] = true;
                        parent[e.to] = e;
                        queue.add(e.to);
                    }
                }
            }
            if (!seen[t]) {
                break;
            }
            long bottleneck = Long.MAX_VALUE;
            for (int v = t; v != s; v = parent[v].from) {
                bottleneck = Math.min(bottleneck, parent[v].residual);
            }
            for (int v = t; v != s; v = parent[v].from) {
                Edge e = parent[v];
                e.residual -= bottleneck;
                graph[e.to].get(e.rev).residual += bottleneck;
            }
            total += bottleneck;
            augmentations++;
        }
        return total;
    }

    /** Nodes reachable from s in the residual graph (source side of a minimum cut). Call after maxFlow. */
    public boolean[] sourceSide(int s) {
        checkNode(s);
        boolean[] seen = new boolean[n];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[s] = true;
        queue.add(s);
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (Edge e : graph[u]) {
                if (e.residual > 0 && !seen[e.to]) {
                    seen[e.to] = true;
                    queue.add(e.to);
                }
            }
        }
        return seen;
    }

    /** Saturated forward edges crossing the minimum cut. Their capacities sum to the max-flow value. */
    public List<Edge> minCutEdges(int s) {
        boolean[] side = sourceSide(s);
        List<Edge> cut = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            if (!side[u]) {
                continue;
            }
            for (Edge e : graph[u]) {
                if (e.forward && !side[e.to]) {
                    cut.add(e);
                }
            }
        }
        return cut;
    }

    public int augmentations() {
        return augmentations;
    }

    public int nodeCount() {
        return n;
    }

    private void checkNode(int v) {
        if (v < 0 || v >= n) {
            throw new IllegalArgumentException("Node out of range: " + v);
        }
    }
}
