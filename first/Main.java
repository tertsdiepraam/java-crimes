package first;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Main {
    static final int NUM_PROCESSES = 5;

    public static void main(String[] args) throws RemoteException {
        LocateRegistry.createRegistry(1888);

        System.err.println("Server ready");

        Draft[] drafts = new Draft[]{
            new Draft(0, 1, "A", 0, 900),
            new Draft(0, 2, "B", 300, 0),
            new Draft(2, 1, "C", 600, 0),
        };

        for (int i = 0; i < NUM_PROCESSES; i++) {
            try {
                new Thread(new Client(i, NUM_PROCESSES, drafts)).start();
            } catch (Exception e) {
            }
        }
    }
}
