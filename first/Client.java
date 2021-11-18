package first;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Client extends UnicastRemoteObject implements MyInterface, Runnable {
    int id;
    int num_processes;
    Registry reg;
    HashMap<Integer, VectorClock> buffer;
    VectorClock clock;

    public Client(int id, int num_processes) throws RemoteException {
        super();
        this.id = id;
        this.num_processes = num_processes;
        this.buffer = new HashMap<>();
        this.clock = new VectorClock(num_processes);
    }

    @Override
    public void receive(Message msg) throws RemoteException {
        System.out.println("got le message");
    }

    public void run() {
        try {
            this.reg = LocateRegistry.getRegistry(1888);
            this.reg.bind(this.id + "", this);
        } catch (Exception e) {
            System.err.println("Failed to create client " + id);
            return;
        }

        send((this.id + 1) % this.num_processes, "Hello!");
    }

    public MyInterface find_client(int id) {
        String other_id = id + "";
        MyInterface other = null;
        while (other == null) {
            try {
                other = (MyInterface) reg.lookup(other_id);
                System.out.println("heyo");
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                    System.out.println("sleep");
                } catch (Exception e2) {}
            }
        }
        return other;
    }

    public void send(int dest, String msg) {
        try {
            find_client(dest).receive(new Message(msg, this.buffer, this.clock));
            System.out.println("Heyo2");
        } catch (Exception e) {}
        this.clock.tick(this.id);
    }
}