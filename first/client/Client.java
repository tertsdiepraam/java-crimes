package first.client;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import first.myinterface.MyInterface;

public class Client extends UnicastRemoteObject implements MyInterface, Runnable {
    int id;

    public Client(int id) throws RemoteException {
        super();
        this.id = id;
    }

    @Override
    public void hello(String name) throws RemoteException {
        System.out.println(name + " -> " + this.id);
    }

    public void run() {
        Registry lr;
        try {
            lr = LocateRegistry.getRegistry(1888);
            lr.bind(this.id + "", this);
        } catch (Exception e) {
            System.err.println("Failed to create client " + id);
            return;
        }

        String other_id = ((this.id + 1) % 5) + "";
        MyInterface other = null;
        while (other == null) {
            try {
                other = (MyInterface) lr.lookup(other_id);
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                    System.out.println("sleep");
                } catch (Exception e2) {}
            }
        }

        try { other.hello(this.id + ""); } catch (Exception e) {}
    }
}