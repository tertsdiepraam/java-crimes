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
            new Edge[] {
                new Edge(0, 1, 0),
            },
            new Edge[] {
                new Edge(0, 1, 0),
                new Edge(1, 2, 1),
                new Edge(0, 2, 2),
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

        Edge[] edges = testCases[1];

        for (int i = start; i < end; i++) {
            new Thread(new Client(i, edges, i == 0 ? 0 : null)).start();
        }
    }
}
