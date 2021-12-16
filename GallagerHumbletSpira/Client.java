package GallagerHumbletSpira;

import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Objects;
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

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            Edge other = (Edge) o;
            if (this.weight != other.weight) {
                return false;
            }
            return (this.from == other.from && this.to == other.to) || (this.from == other.to && this.to == other.from);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, weight);
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

    record Fragment(Edge edge, int level) implements Serializable {
        Fragment increment() {
            return new Fragment(this.edge, this.level + 1);
        }
    };

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
    ArrayDeque<Message> messageQueue = new ArrayDeque<Message>();

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
        }
    }

    public void receive(Message m) throws RemoteException {
        messageQueue.addFirst(m);

        boolean anySucceeded = true;
        while (anySucceeded) {
            anySucceeded = false;
            ArrayDeque<Message> tmp = messageQueue;
            messageQueue = new ArrayDeque<>();
            for (Message msg : tmp) {
                anySucceeded = anySucceeded || process(msg);
            }
        }
    }

    public boolean process(Message m) throws RemoteException {
        logMsg(m);
        logMap();
        switch (m.type) {
            case Connect:
                if (state == State.Sleeping) {
                    wakeup();
                }
                if (m.fragment.level < fragment.level) {
                    edges.put(m.j, EdgeState.Included);
                    send(new Message(Type.Initiate, fragment, state, m.j, null));
                    if (state == State.Find) {
                        findCount++;
                        log("findCount=" + findCount);
                    }
                } else {
                    if (edges.get(m.j) == EdgeState.Unknown) {
                        messageQueue.add(m);
                        return false;
                    } else {
                        send(new Message(Type.Initiate, fragment.increment(), State.Find, m.j, null));
                    }
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
                            log("findCount=" + findCount);
                        }
                    }
                }
                if (m.S == State.Find)
                    test();
                break;
            case Test:
                if (state == State.Sleeping)
                    wakeup();
                if (m.fragment.level < fragment.level) {
                    log("1");
                    messageQueue.add(m);
                    return false;
                } else {
                    if (!m.fragment.edge.equals(fragment.edge))
                        send(new Message(Type.Accept, fragment, state, m.j, null));
                    else {
                        log("2");
                        if (edges.get(m.j) == EdgeState.Unknown) {
                            edges.put(m.j, EdgeState.Excluded);
                            logMap();
                        }
                        if (m.j.equals(testEdge)) {
                            log("reject me pls");
                            send(new Message(Type.Reject, fragment, state, m.j, null));
                        } else {
                            test();
                        }
                    }
                }
                break;
            case Reject:
                if (edges.get(m.j) == EdgeState.Unknown)
                    edges.put(m.j, EdgeState.Excluded);
                test();
                break;
            case Accept:
                testEdge = null;
                if (m.j.weight < bestWt) {
                    bestEdge = m.j;
                    bestWt = m.j.weight;
                }
                report();
                break;
            case Report:
                if (!m.j.equals(inBranch)) {
                    findCount--;
                    log("findCount=" + findCount);
                    if (m.w < bestWt) {
                        bestWt = m.w;
                        bestEdge = m.j;
                    }
                    report();
                    log("a");
                } else {
                    log("the state is " + state);
                    if (state == State.Find) {
                        messageQueue.add(m);
                        return false;
                    } else {
                        log(m.w.toString());
                        if (m.w > bestWt) {
                            changeRoot();
                        } else if (m.w == Integer.MAX_VALUE && bestWt == Integer.MAX_VALUE) {
                            halt();
                        }
                    }
                }
                break;
            case ChangeRoot:
                changeRoot();
                break;
        }
        return true;
    }

    void wakeup() throws RemoteException {
        log("WOKE UP!");

        Edge e = (Edge) edges.keySet().toArray()[0];
        for (Edge edge : edges.keySet()) {
            if (edge.weight < e.weight) {
                e = edge;
            }
        }

        edges.put(e, EdgeState.Included);
        fragment = new Fragment(e, 0);
        state = State.Found;
        findCount = 0;
        send(new Message(Type.Connect, fragment, state, e, null));
    }

    void send(Message m) throws RemoteException {
        RemoteClient c = findClient(m.j.other(id));
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
        if (found) {
            send(new Message(Type.Test, fragment, state, testEdge, null));
        } else {
            testEdge = null;
            report();
        }
    }

    void report() throws RemoteException {
        if (findCount == 0 && testEdge == null) {
            log("found!");
            state = State.Found;
            send(new Message(Type.Report, fragment, state, inBranch, bestWt));
        }
    }

    void changeRoot() throws RemoteException {
        if (edges.get(bestEdge) == EdgeState.Included)
            send(new Message(Type.ChangeRoot, fragment, state, bestEdge, null));
        else
            send(new Message(Type.Connect, fragment, state, bestEdge, null));
        edges.put(bestEdge, EdgeState.Included);
    }

    void halt() throws RemoteException {
        log("HALT");
        printTree();
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
        return other;
    }

    public void printTree() throws RemoteException {
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
        System.out.println("     " + id + ": " + s);
    }

    void logMsg(Message m) {
        System.out.println("-----------------------------------------");
        System.out.println(m.j.other(id)  + " -> " + id + ": " + m.type);
    }

    void logMap() {
        System.out.print("     " + id + ": ");
        System.out.print("{");
        boolean first = true;
        for (Entry<Edge, EdgeState> e : edges.entrySet()) {
            Edge edge = e.getKey();
            if (!first)
                System.out.print(", ");
            System.out.print(edge.other(id) + " => " + e.getValue());
            first = false;
        }
        System.out.println("}");
    }
}
