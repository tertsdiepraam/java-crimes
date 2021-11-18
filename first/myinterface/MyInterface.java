package first.myinterface;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MyInterface extends Remote {
    public void hello(String name) throws RemoteException;
}