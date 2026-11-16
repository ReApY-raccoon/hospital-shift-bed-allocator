package com.hospital.allocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hospital.allocator.algo.EdmondsKarp;
import java.util.List;
import org.junit.jupiter.api.Test;

class EdmondsKarpTest {

    /** Classic textbook network (CLRS 26.1): max flow is 23. */
    private static EdmondsKarp clrs() {
        EdmondsKarp g = new EdmondsKarp(6);
        g.addEdge(0, 1, 16);
        g.addEdge(0, 2, 13);
        g.addEdge(1, 2, 10);
        g.addEdge(2, 1, 4);
        g.addEdge(1, 3, 12);
        g.addEdge(3, 2, 9);
        g.addEdge(2, 4, 14);
        g.addEdge(4, 3, 7);
        g.addEdge(3, 5, 20);
        g.addEdge(4, 5, 4);
        return g;
    }

    @Test
    void textbookNetworkGives23() {
        assertEquals(23L, clrs().maxFlow(0, 5));
    }

    @Test
    void singleEdgeFlowEqualsCapacity() {
        EdmondsKarp g = new EdmondsKarp(2);
        g.addEdge(0, 1, 7);
        assertEquals(7L, g.maxFlow(0, 1));
    }

    @Test
    void bottleneckEdgeLimitsFlow() {
        EdmondsKarp g = new EdmondsKarp(4);
        g.addEdge(0, 1, 100);
        g.addEdge(1, 2, 3); // bottleneck
        g.addEdge(2, 3, 100);
        assertEquals(3L, g.maxFlow(0, 3));
    }

    @Test
    void disconnectedSinkGivesZero() {
        EdmondsKarp g = new EdmondsKarp(3);
        g.addEdge(0, 1, 5);
        assertEquals(0L, g.maxFlow(0, 2));
    }

    @Test
    void zeroCapacityEdgeCarriesNothing() { // boundary case
        EdmondsKarp g = new EdmondsKarp(2);
        g.addEdge(0, 1, 0);
        assertEquals(0L, g.maxFlow(0, 1));
    }

    @Test
    void minCutCapacityEqualsMaxFlow() {
        EdmondsKarp g = clrs();
        long flow = g.maxFlow(0, 5);
        long cut = 0;
        for (EdmondsKarp.Edge e : g.minCutEdges(0)) {
            cut += e.capacity;
        }
        assertEquals(flow, cut);
    }

    @Test
    void flowConservationHoldsAtInnerNodes() {
        EdmondsKarp g = new EdmondsKarp(4);
        List<EdmondsKarp.Edge> edges = List.of(g.addEdge(0, 1, 5), g.addEdge(0, 2, 4),
                g.addEdge(1, 3, 3), g.addEdge(2, 3, 6), g.addEdge(1, 2, 2));
        g.maxFlow(0, 3);
        long in1 = edges.get(0).flow();
        long out1 = edges.get(2).flow() + edges.get(4).flow();
        assertEquals(in1, out1);
    }

    @Test
    void invalidInputsAreRejected() {
        EdmondsKarp g = new EdmondsKarp(3);
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(0, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(0, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> g.addEdge(1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> g.maxFlow(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EdmondsKarp(1));
    }

    @Test
    void augmentationsAreCounted() {
        EdmondsKarp g = clrs();
        g.maxFlow(0, 5);
        assertTrue(g.augmentations() > 0);
    }
}
