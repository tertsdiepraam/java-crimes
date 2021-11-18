package first;

import java.rmi.AlreadyBoundException;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class Client extends UnicastRemoteObject implements RemoteClient, Runnable {
    final int id;
    final int num_processes;
    final HashMap<Integer, VectorClock> buffer;
    final VectorClock clock;
    final Registry reg;

    public Client(int id, int num_processes) throws RemoteException, AlreadyBoundException, AccessException {
        super();
        this.id = id;
        this.num_processes = num_processes;
        this.buffer = new HashMap<>();
        this.clock = new VectorClock(num_processes);
        this.reg = LocateRegistry.getRegistry(1888);
        this.reg.bind(this.id + "", this);
    }

    @Override
    public void receive(Message msg) throws RemoteException {
        System.out.println("Got message: \"" + msg.message() + "\"");
    }

    public void run() {
        try {
            send((this.id + 1) % this.num_processes, "Hello!");
        } catch (Exception e) {
            System.err.println("Failed to send message from " + id);
        }
    }

    public RemoteClient find_client(int id) throws InterruptedException {
        final String other_id = id + "";
        RemoteClient other = null;
        while (other == null) {
            try {
                other = (RemoteClient) reg.lookup(other_id);
            } catch (Exception e) {
                Thread.sleep(100);
            }
        }
        return other;
    }

    public void send(int dest, String msg) throws RemoteException, InterruptedException {
        find_client(dest).receive(new Message(msg, this.buffer, this.clock));
        this.clock.tick(this.id);
    }
}