import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Auction extends UnicastRemoteObject implements IAuctionRemote {
    protected Auction() throws RemoteException {
        System.out.format("Creating server object\n");
    }

    @Override
    public String printTESTTEST(String str) {
        System.out.format("Caller send %s. Sending back 'test'.", str);
        return "test";
    }
}
