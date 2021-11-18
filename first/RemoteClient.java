package first;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteClient extends Remote {
    public void receive(Message name) throws RemoteException;
}