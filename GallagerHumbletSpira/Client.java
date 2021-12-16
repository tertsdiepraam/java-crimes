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
    };

    record Fragment(int from, int to, int level) {
    };

    final int id;
    Registry reg;
    final Integer wakeupTime;

    Fragment fragment;
    State state = State.Sleeping;
    int findCount = 0;
    Integer bestEdge = null;
    Integer testEdge = null;
    Integer inBranch = null;
    int bestWt = Integer.MAX_VALUE;
    final ArrayList<Edge> edges = new ArrayList<Edge>();
    final ArrayDeque<Message> messageQueue = new ArrayDeque<Message>();

    public Client(int id, Edge[] edges, Integer wakeupTime) throws RemoteException, AlreadyBoundException {
        super();
        this.id = id;
        this.wakeupTime = wakeupTime;
        try {
            reg = LocateRegistry.getRegistry(1888);
        } catch (ConnectException e) {
            reg = LocateRegistry.getRegistry("10.0.2.2", 1888);
        }
        reg.bind(id + "", this);

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

    public void receive(Message m) {
        switch (m.type) {
            case Initiate:
                break;
            case Connect:
                break;
            case Accept:
                break;
            case Reject:
                break;
            case Report:
                break;
            case Test:
                break;
            case ChangeRoot:
                break;
        }
    }

    void wakeup() throws InterruptedException, RemoteException {
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
        fragment = new Fragment(id, e.to, 0);
        state = State.Found;
        findCount = 0;
        send(new Message(Type.Connect, fragment, state, e));
    }

    void send(Message m) throws InterruptedException, RemoteException {
        RemoteClient c = findClient(m.j.to);
        c.receive(m);
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
