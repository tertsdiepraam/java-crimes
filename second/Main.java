package second;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Main {
    static final int NUM_PROCESSES = 5;

    public static void main(String[] args) throws RemoteException {
        LocateRegistry.createRegistry(1888);

        System.err.println("Server ready");

        Crime[] crimes = new Crime[] {
            new Crime(0, 0),
            new Crime(1, 100),
        };

        for (int i = 0; i < NUM_PROCESSES; i++) {
            try {
                new Thread(new Client(i, NUM_PROCESSES, crimes)).start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("FAILURE");
            }
        }
    }
}

