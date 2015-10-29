import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

/**
 * Created by vbk20 on 29/10/2015.
 */
public interface IAuctionRemote extends Remote {
    public String printTESTTEST(String str) throws RemoteException;

    public List<IAuctionItem> getAuctionItems() throws RemoteException;
    public IAuctionItem createAuctionItem(String itemName, double value, Date closingDate) throws RemoteException;
    public void createAndRegisterAuctionItem(String itemName, double value, Date closingDate) throws RemoteException;

    public void registerAuctionItem(IAuctionItem item) throws RemoteException;

}
