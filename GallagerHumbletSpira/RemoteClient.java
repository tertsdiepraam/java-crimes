package GallagerHumbletSpira;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RemoteClient extends Remote {
    public void receive(Message m) throws RemoteException;
    public Integer getInBranch() throws RemoteException;
}