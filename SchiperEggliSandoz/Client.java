package SchiperEggliSandoz;

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
    final Draft[] drafts;

    public Client(int id, int num_processes, Draft[] drafts) throws RemoteException, AlreadyBoundException, AccessException {
        super();
        this.id = id;
        this.num_processes = num_processes;
        this.drafts = drafts;
        buffer = new HashMap<>();
        clock = new VectorClock(num_processes);
        reg = LocateRegistry.getRegistry(1888);
        reg.bind(id + "", this);
        msg_buffer = new ArrayList<>();
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
            msg_buffer.add(m);
        }
    }

    private void deliver(Message m) {
        System.out.println("Got message: \"" + m.message() + "\"");
        msg_buffer.remove(m);
        for (Map.Entry<Integer, VectorClock> e : m.buffer().entrySet()) {
            int k = e.getKey();
            VectorClock v = e.getValue();
            if (buffer.containsKey(k)) {
                buffer.get(k).update(v);
            } else {
                buffer.put(k, v.clone());
            }
            clock.update(v);
        }
        clock.update(m.clock());
        clock.tick(id);
    }

    private void send(int dest, String msg, int delay) throws RemoteException, InterruptedException {
        clock.tick(id);
        new Thread(
            new Connection(find_client(dest), new Message(msg, (HashMap<Integer,VectorClock>) buffer.clone(), clock.clone(), id, dest), delay)
        ).start();
        buffer.put(dest, clock.clone());
    }

    private boolean expected(Message msg) {
        if (msg.buffer().containsKey(id)) {
            return msg.buffer().get(id).lessThanEq(clock);
        } else {
            return true;
        }
    }

    public void run() {
        Client self = this;
        for (Draft draft : drafts) {
            if (draft.from() != id)
                continue;
            
            new java.util.Timer().schedule( 
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            self.send_draft(draft);
                        } catch (Exception e) {
                        }
                    }
                },
                draft.time()
            );
        }
    }

    public void send_draft(Draft draft) throws RemoteException, InterruptedException {
        send(draft.to(), draft.text(), draft.delay());
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