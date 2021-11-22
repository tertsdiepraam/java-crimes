package second;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteClient extends Remote {
    public void receive_request(int from, int n) throws RemoteException;
    public void receive_token(int from, Token token) throws RemoteException;
}
