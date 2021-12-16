package GallagerHumbletSpira;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import GallagerHumbletSpira.Client.Edge;

public class Main {
    static final int NUM_PROCESSES = 5;

    public static void main(String[] args) throws RemoteException, AlreadyBoundException {
        
        boolean isServer = false;
        if (args.length == 2 && args[1].equals("server")) {
            LocateRegistry.createRegistry(1888);
            isServer = true;
            System.out.println("Server ready");
        }

        int start = (isServer ? 0 : NUM_PROCESSES);
        int end = NUM_PROCESSES + start;

        System.out.println("Making processes " + start + " to " + end);

        Edge[] edges = new Edge[] {
            
        };

        for (int i = start; i < end; i++) {
            new Thread(new Client(i, edges, 0)).start();
        }
    }
}
