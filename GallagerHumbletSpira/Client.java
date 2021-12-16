package GallagerHumbletSpira;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import GallagerHumbletSpira.Message.Type;

public class Client extends UnicastRemoteObject implements RemoteClient, Runnable {
    enum State {
        Sleeping, Find, Found
    }

    enum EdgeState {
        Unknown, Included, Excluded
    }

    public static class Edge implements Serializable {
        private int from;
        private int to;
        int weight;

        public Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        public boolean equals(Edge other) {
            if (this.weight != other.weight) {
                return false;
            }
            return (this.from == other.from && this.to == other.to) || (this.from == other.to && this.to == other.from);
        }

        public Integer other(int self) {
            if (this.from == self) {
                return this.to;
            } else if (this.to == self) {
                return this.from;
            }
            return null;
        }

        public boolean includes(int id) {
            return this.from == id || this.to == id;
        }

    }

    record Fragment(Edge edge, int from, int to, int level) implements Serializable {
    };

    public Fragment incrementFragment(Fragment old, Edge cur) {
        return new Fragment(cur, old.from, old.to, old.level + 1);
    }

    final int id;
    Registry reg;
    final Integer wakeupTime;

    Fragment fragment;
    State state = State.Sleeping;
    int findCount = 0;
    Edge bestEdge = null;
    Edge testEdge = null;
    Edge inBranch = null;
    int bestWt = Integer.MAX_VALUE;
    final HashMap<Edge, EdgeState> edges = new HashMap<>();
    final ArrayDeque<Message> messageQueue = new ArrayDeque<Message>();

    public Client(int id, Edge[] edges, Integer wakeupTime) throws RemoteException, AlreadyBoundException {
        super();
        this.id = id;
        this.wakeupTime = wakeupTime;
        try {
            reg = LocateRegistry.getRegistry(1888);
            reg.bind(id + "", this);
        } catch (Exception e) {
            reg = LocateRegistry.getRegistry("10.0.2.2", 1888);
            reg.bind(id + "", this);
        }

        for (Edge e : edges) {
            if (e.includes(id)) {
                this.edges.put(e, EdgeState.Unknown);
            }
        }
    }

    public void run() {
        if (wakeupTime != null) {
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            try {
                                if (state == State.Sleeping)
                                    wakeup();
                            } catch (Exception e) {
                            }
                        }
                    },
                    wakeupTime);
        } else
            log("sleepy");
    }

    public void receive(Message m) throws RemoteException {
        log("Got a message");
        switch (m.type) {
            case Connect:
                log("Got a connect message");
                if (state == State.Sleeping)
                    wakeup();
                if (m.fragment.level < fragment.level) {
                    edges.put(m.j, EdgeState.Included);
                    send(new Message(Type.Initiate, fragment, state, m.j, null));
                    if (state == State.Find)
                        findCount++;
                } else {
                    if (edges.get(m.j) == EdgeState.Unknown)
                        messageQueue.add(m);
                    else
                        send(new Message(Type.Initiate, incrementFragment(fragment, m.j), State.Find, m.j, null));
                }
                break;
            case Initiate:
                fragment = m.fragment;
                state = m.S;
                inBranch = m.j;
                bestEdge = null;
                bestWt = Integer.MAX_VALUE;
                for (Entry<Edge, EdgeState> entry : edges.entrySet()) {
                    if (!entry.getKey().equals(m.j) && entry.getValue() == EdgeState.Included) {
                        send(new Message(Type.Initiate, m.fragment, m.S, entry.getKey(), null));
                        if (m.S == State.Find) {
                            findCount++;
                        }
                    }
                }
                if (m.S == State.Find)
                    test();
                break;
            case Test:
                break;
            case Accept:
                break;
            case Reject:
                break;
            case Report:
                if (m.j != inBranch) {
                    findCount--;
                    if (m.w < bestWt) {
                        bestWt = m.w;
                        bestEdge = m.j;
                    }
                    report();
                } else {

                }
                break;
            case ChangeRoot:
                break;
        }
    }

    void wakeup() throws RemoteException {
        log("woke up!");

        Edge e = (Edge) edges.keySet().toArray()[0];
        for (Edge edge : edges.keySet()) {
            if (edge.weight < e.weight) {
                e = edge;
            }
        }

        edges.put(e, EdgeState.Included);
        fragment = new Fragment(e, id, e.to, 0);
        state = State.Found;
        findCount = 0;
        log(e.toString());
        send(new Message(Type.Connect, fragment, state, e, null));
        log("Done");
    }

    void send(Message m) throws RemoteException {
        RemoteClient c = findClient(m.j.other(id));
        log("djdjddjd");
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            c.receive(m);
                        } catch (Exception e) {
                        }
                    }
                },
                100);
    }

    void test() throws RemoteException {
        boolean found = false;
        int weight = Integer.MAX_VALUE;
        for (Entry<Edge, EdgeState> entry : edges.entrySet()) {
            Edge cur = entry.getKey();
            if (entry.getValue() == EdgeState.Unknown && cur.weight < weight) {
                testEdge = cur;
                weight = cur.weight;
                found = true;
            }
        }
        if (found)
            send(new Message(Type.Test, fragment, state, testEdge, null));
        else {
            testEdge = null;
            report();
        }
    }

    void report() {
        // do some magic
    }

    private RemoteClient findClient(int id) {
        final String otherId = id + "";
        RemoteClient other = null;
        while (other == null) {
            try {
                other = (RemoteClient) reg.lookup(otherId);
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e2) {
                }
            }
        }
        log(otherId + " tada");
        return other;
    }

    public void printTree() throws RemoteException {
        log("PRINTING");
        try {
            for (int i = 0; i < 10; i++) {
                if (i == id)
                    continue;
                log(i + "," + findClient(i).getInBranch());
            }
        } catch (Exception e) {
        }
    }

    public Integer getInBranch() throws RemoteException {
        if (inBranch == null)
            return null;
        else
            try {
                return inBranch.other(id);
            } catch (Exception e) {
                return null;
            }
    }

    void log(String s) {
        System.out.println(id + ": " + s);
    }
}
