import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * Created by vbk20 on 01/11/2015.
 */
public interface IAuctionHouseRemote extends Remote {
    long registerAuction(IAuctionRemote auction) throws RemoteException;

    void unregisterAuction(long auctionId) throws RemoteException;

    long registerClient(IAuctionClientRemote client) throws RemoteException;

    void unregisterClient(long clientId) throws RemoteException;

    String getActiveAuctions() throws RemoteException;

    String getAuctionLiveItems(long auctionId, long clientId) throws RemoteException;

    String registerClientForAuction(long auctionId, IAuctionClientRemote client) throws RemoteException;

    String bidForItem(long bidderId, long auctionId, long itemId, double bidValue) throws RemoteException;

    long createAndRegisterAuctionItem(long creatorId, long auctionId, String itemName, double value, Date endDate) throws RemoteException, AuctionHouseException;
}
