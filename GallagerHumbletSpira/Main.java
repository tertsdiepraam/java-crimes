package GallagerHumbletSpira;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import GallagerHumbletSpira.Client.Edge;

public class Main {
    static final int NUM_PROCESSES = 5;

    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        
        boolean isServer = false;

        Edge[][] testCases = new Edge[][] {
            // Single edge
            new Edge[] {
                new Edge(0, 1, 0),
            },
            // Triangle
            new Edge[] {
                new Edge(0, 1, 0),
                new Edge(1, 2, 1),
                new Edge(0, 2, 2),
            },
            // Opposite low edges in square
            new Edge[] {
                new Edge(0, 1, 0),
                new Edge(2, 3, 1),
                new Edge(0, 2, 2),
                new Edge(1, 3, 3),
            },
            // Something
            new Edge[] {
                new Edge(0, 1, 0),
                new Edge(1, 2, 1),
                new Edge(0, 2, 2),
                new Edge(2, 3, 3),
                new Edge(3, 4, 4),
            },
            // Cycles with increasing weights
            // and a node in the middle
            new Edge[] {
                new Edge(0, 1, 1),
                new Edge(1, 2, 2),
                new Edge(2, 3, 3),
                new Edge(3, 0, 4),
                new Edge(3, 4, 5),
                new Edge(2, 4, 6),
                new Edge(1, 4, 7),
                new Edge(0, 4, 8),
            },
            // Same as before but truly distributed
            new Edge[] {
                new Edge(0, 1, 1),
                new Edge(1, 2, 2),
                new Edge(2, 5, 3),
                new Edge(5, 0, 4),
                new Edge(5, 6, 5),
                new Edge(2, 6, 6),
                new Edge(1, 6, 7),
                new Edge(0, 6, 8),
            }
        };

        if (args.length == 2 && args[1].equals("server")) {
            LocateRegistry.createRegistry(1888);
            isServer = true;
            System.out.println("Server ready");
        }

        int start = (isServer ? 0 : NUM_PROCESSES);
        int end = NUM_PROCESSES + start;

        System.out.println("Making processes " + start + " to " + end);

        Edge[] edges = testCases[5];

        for (int i = start; i < end; i++) {
            new Thread(new Client(i, edges, i == 0 ? 0 : null)).start();
        }
    }
}
