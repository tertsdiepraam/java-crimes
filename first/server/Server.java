package first.server;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import first.client.Client;

public class Server {
    static int NUM_PROCESSES = 5;

    public static void main(String[] args){
        try {
            Registry lr = LocateRegistry.createRegistry(1888);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
            return;
        }

        for (int i=0; i<NUM_PROCESSES; i++) {
            Client c;
            try {
                c = new Client(i);
                System.out.println("erg;xjkt");
            } catch (Exception e) {
                System.err.println("AAAHAAHAHAHAH");
                continue;
            }
            new Thread(c).start();
        }
    }
}
