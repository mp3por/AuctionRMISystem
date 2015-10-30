import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by vbk20 on 29/10/2015.
 */
public interface IAuctionRemote extends Remote {
    String getAuctionLiveItems(long id) throws RemoteException;

    String createAndRegisterAuctionItem(long creatorId, String itemName, double value, Date closingDate) throws RemoteException;

    String bidForItem(long bidderId, long itemId, double bidValue) throws RemoteException;

    void registerClient(IAuctionClientRemote client) throws RemoteException;

    void unregisterClient(long clientId) throws RemoteException;
}
