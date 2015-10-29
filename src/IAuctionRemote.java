import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by vbk20 on 29/10/2015.
 */
public interface IAuctionRemote extends Remote {
    public String printTESTTEST(String str) throws RemoteException;
}
