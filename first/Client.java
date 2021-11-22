package first;

import java.rmi.AlreadyBoundException;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Client extends UnicastRemoteObject implements RemoteClient, Runnable {
    final int id;
    final int num_processes;
    final HashMap<Integer, VectorClock> buffer;
    final ArrayList<Message> msg_buffer;
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
        this.msg_buffer = new ArrayList<>();
    }

    @Override
    public void receive(Message m) throws RemoteException {
        if (expected(m)) {
            deliver(m);
            boolean delivered = true;
            while (delivered) {
                delivered = false;
                for (Message msg : this.msg_buffer) {
                    if (expected(msg)) {
                        deliver(msg);
                        delivered = true;
                    }
                }
            }
            ;
        } else {
            this.msg_buffer.add(m);
        }
    }

    private void deliver(Message m) {
        System.out.println("Got message: \"" + m.message() + "\"");
        this.msg_buffer.remove(m);
        for (Map.Entry<Integer, VectorClock> e : m.buffer().entrySet()) {
            int k = e.getKey();
            VectorClock v = e.getValue();
            if (this.buffer.containsKey(k)) {
                this.buffer.get(k).update(v);
            } else {
                this.buffer.put(k, v.clone());
            }
            this.clock.update(v);
        }
        this.clock.tick(this.id);
    }

    private void send(int dest, String msg) throws RemoteException, InterruptedException {
        this.clock.tick(this.id);
        new Thread(
            new Connection(find_client(dest), new Message(msg, (HashMap<Integer,VectorClock>) this.buffer.clone(), this.clock.clone(), this.id, dest))
        ).start();
        this.buffer.put(dest, this.clock.clone());
    }

    private boolean expected(Message msg) {
        if (msg.buffer().containsKey(this.id)) {
            return msg.buffer().get(this.id).lessThanEq(this.clock);
        } else {
            return true;
        }
    }

    public void run() {
        try {
            String msg = this.id + " -> " + ((this.id + 1) % this.num_processes);
            send((this.id + 1) % this.num_processes, msg + " (1)" + " " + this.clock.printValues());
            Thread.sleep(500);
            send((this.id + 1) % this.num_processes, msg + " (2)");
        } catch (Exception e) {
            System.err.println("Failed to send message from " + id);
        }
    }

    private RemoteClient find_client(int id) throws InterruptedException {
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
}