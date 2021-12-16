package GallagerHumbletSpira;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RemoteClient extends Remote {
    void receive(Message m) throws Exception;
}