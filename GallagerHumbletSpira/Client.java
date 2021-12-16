package GallagerHumbletSpira;

import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.ArrayList;

import GallagerHumbletSpira.Message.Type;

public class Client extends UnicastRemoteObject implements RemoteClient, Runnable {
    enum State {
        Sleeping, Find, Found
    }

    enum EdgeState {
        Unknown, Included, Excluded
    }

    public class Edge {
        private int from;
        private int to;
        int weight;
        EdgeState state;

        public Edge(int from, int to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.state = EdgeState.Unknown;
        }

        public boolean equals(Edge other) {
            if (this.weight != other.weight) {
                return false;
            }
            return (this.from == other.from && this.to == other.to) || (this.from == other.to && this.to == other.from);
        }

        public int other(int self) throws Exception {
            if (this.from == self) {
                return this.to;
            } else if (this.to == self) {
                return this.from;
            }
            throw new Exception("YOU IDIOT");
        }

        public boolean includes(int id) {
            return this.from == id || this.to == id;
        }
    }

    record Fragment(Edge edge, int from, int to, int level) {
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
    final ArrayList<Edge> edges = new ArrayList<Edge>();
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
                this.edges.add(e);
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
        }
    }

    public void receive(Message m) throws Exception {
        switch (m.type) {
            case Connect:
                if (state == State.Sleeping)
                    wakeup();
                if (m.fragment.level < fragment.level) {
                    int cur = -1;
                    for (int i = 0; i < edges.size(); i++) {
                        if (edges.get(i).equals(m.j)) {
                            cur = i;
                        }
                    }
                    m.j.state = EdgeState.Included;
                    edges.set(cur, m.j);
                    send(new Message(Type.Initiate, fragment, state, m.j));
                    if (state == State.Find)
                        findCount++;
                }
                else {
                    if (m.j.state == EdgeState.Unknown)
                        messageQueue.add(m);
                    else send(new Message(Type.Initiate, incrementFragment(fragment, m.j), State.Find, m.j));
                }
                break;
            case Initiate:
                fragment = m.fragment;
                state = m.S;
                inBranch = m.j;
                bestEdge = null;
                bestWt = Integer.MAX_VALUE;
                for(Edge edge : edges) {
                    if (!edge.equals(m.j) && edge.state == EdgeState.Included) {
                        send(new Message(Type.Initiate, m.fragment, m.S, edge));
                        if (m.S == State.Find) {
                            findCount++;
                        }
                    }
                }
                if (m.S == State.Find) test();
                break;
            case Test:
                break;
            case Accept:
                break;
            case Reject:
                break;
            case Report:
                break;
            case ChangeRoot:
                break;
        }
    }

    void wakeup() throws Exception {
        System.out.println("Client " + id + " woke up!");

        int j = 0;
        int minWeight = Integer.MAX_VALUE;
        for (int i = 0; i < edges.size(); i++) {
            if (edges.get(i).weight < minWeight) {
                j = i;
                minWeight = edges.get(i).weight;
            }
        }

        Edge e = edges.get(j);
        e.state = EdgeState.Included;
        fragment = new Fragment(e, id, e.to, 0);
        state = State.Found;
        findCount = 0;
        send(new Message(Type.Connect, fragment, state, e));
    }

    void send(Message m) throws Exception {
        RemoteClient c = findClient(m.j.to);
        c.receive(m);
    }

    void test() throws Exception {
        boolean found = false;
        int weight = Integer.MAX_VALUE;
        for (int i = 0; i < edges.size(); i++) {
            Edge cur = edges.get(i);
            if (cur.state == EdgeState.Unknown && cur.weight < weight) {
                testEdge = cur;
                weight = cur.weight;
                found = true;
            }
        }
        if (found) send(new Message(Type.Test, fragment, state, testEdge));
        else report();
    }

    void report() {
        // do some magic
    }

    private RemoteClient findClient(int id) throws InterruptedException {
        final String otherId = id + "";
        RemoteClient other = null;
        while (other == null) {
            try {
                other = (RemoteClient) reg.lookup(otherId);
            } catch (Exception e) {
                Thread.sleep(100);
            }
        }
        return other;
    }
}
