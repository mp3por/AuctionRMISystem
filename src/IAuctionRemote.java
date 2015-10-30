import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by vbk20 on 29/10/2015.
 */
public interface IAuctionRemote extends Remote {
    public String printTESTTEST(String str) throws RemoteException;

    public Map<Long, IAuctionItem> getAuctionItems() throws RemoteException;

    public long createAndRegisterAuctionItem(long creatorId, String itemName, double value, Date closingDate) throws RemoteException;

    public void registerAuctionItem(IAuctionItem item) throws RemoteException;

    public void bidForItem(long bidderId, long itemId, double bidValue) throws RemoteException;
}
