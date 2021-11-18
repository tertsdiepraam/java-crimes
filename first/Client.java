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
            };
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
                this.buffer.put(k, v);
            }
        }
    }

    private void send(int dest, String msg) throws RemoteException, InterruptedException {
        find_client(dest).receive(new Message(msg, this.buffer, this.clock));
        this.buffer.put(dest, this.clock);
    }

    private boolean expected(Message msg){
        return msg.clock().lessThanEq(this.clock.ticked(this.id));
    }


    public void run() {
        try {
            send((this.id + 1) % this.num_processes, "Hello!");
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