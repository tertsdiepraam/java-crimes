package first;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MyInterface extends Remote {
    public void receive(Message name) throws RemoteException;
}