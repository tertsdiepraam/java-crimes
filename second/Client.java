package second;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

public class Client extends UnicastRemoteObject implements RemoteClient, Runnable {
    int id;
    int num_processes;
    Crime[] crimes;
    State[] S;
    int[] N;
    Token token;

    final Registry reg;

    public Client(int id, int num_processes, Crime[] crimes) throws RemoteException, AlreadyBoundException {
        super();
        this.id = id;
        this.num_processes = num_processes;
        this.crimes = crimes;

        reg = LocateRegistry.getRegistry(1888);
        reg.bind(id + "", this);

        S = new State[num_processes];
        Arrays.fill(S, State.Other);
        N = new int[num_processes];

        token = new Token(N.clone(), S.clone());

        if (id == 0) {
            S[0] = State.Holding;
        } else {
            for (int i = 0; i < id; i++) {
                S[i] = State.Requesting;
            }
        }
    }

    public void request() throws InterruptedException {
        if (S[id] == State.Holding) {
            receive_token(id, token);
            return;
        }
        
        S[id] = State.Requesting;
        N[id]++;
        for (int j = 0; j < num_processes; j++) {
            if (id == j)
                continue;

            if (S[j] == State.Requesting) {
                send_request(j, N[id]);
            }
        }
    }

    public void send_request(int to, int n) throws InterruptedException {
        send_request(to, n, 0);
    }

    public void send_request(int to, int n, int delay) throws InterruptedException {
        System.out.println("Sending request!");
        int from = this.id;
        RemoteClient other = find_client(to);
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    other.receive_request(from, n);
                } catch (Exception e) {
                }
            }
        }, delay);
    }

    public void receive_request(int from, int n) {
        System.out.println("Received request!");
        N[from] = n;
        System.out.println(S[id] + "");
        switch (S[id]) {
        case Executing:
        case Other:
            S[from] = State.Requesting;
            break;
        case Requesting:
            if (S[from] != State.Requesting) {
                S[from] = State.Requesting;
                try {
                    send_request(from, N[id]);
                } catch (Exception e) {
                }
            }
            break;
        case Holding:
            System.out.println("Holding!");
            S[from] = State.Requesting;
            S[id] = State.Other;
            token.S()[from] = State.Requesting;
            token.N()[from] = n;
            try {
                send_token(from);
            } catch (Exception e) {
            }
            break;
        }
    }

    public void send_token(int to) throws InterruptedException {
        send_token(to, 0);
    }

    public void send_token(int to, int delay) throws InterruptedException {
        int from = this.id;
        RemoteClient other = find_client(to);
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    other.receive_token(from, token);
                } catch (Exception e) {
                }
            }
        }, delay);
    }

    public void receive_token(int from, Token token) {
        S[id] = State.Executing;
        crimes();
        S[id] = State.Other;
        token.S()[id] = State.Other;
        for (int j = 0; j < num_processes; j++) {
            if (N[j] > token.N()[j]) {
                token.N()[j] = N[j];
                token.S()[j] = S[j];
            } else {
                N[j] = token.N()[j];
                S[j] = token.S()[j];
            }
        }
        boolean all_other = true;
        for (State s : S) {
            if (s != State.Other) {
                all_other = false;
                break;
            }
        }
        if (all_other) {
            S[id] = State.Holding;
        } else {
            for (int j = 0; j < num_processes; j++) {
                if (S[j] == State.Requesting) {
                    try {
                        send_token(j);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    public void crimes() {
        System.out.println(id + " is starting crimes");
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        System.out.println(id + " has stopped doing crimes");
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

    public void run() {
        Client self = this;
        for (Crime c : crimes) {
            if (c.client() == id) {
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            self.request();
                        } catch (Exception e) {
                        }
                    }
                }, c.time());
            }
        }
    }
}
