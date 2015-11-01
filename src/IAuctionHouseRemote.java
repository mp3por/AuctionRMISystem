import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * Created by vbk20 on 01/11/2015.
 */
public interface IAuctionHouseRemote extends Remote {

    // register Auctions
    long registerAuction(IAuctionRemote auction)throws RemoteException;;
    void unregisterAuction(UUID auctionId) throws RemoteException;;

    // register Clients
    long registerClient(IAuctionClientRemote client) throws RemoteException;
    void unregisterClient(long clientId) throws RemoteException;

    String getActiveAuctions() throws RemoteException;

    String getAuctionLiveItems(long auctionId, long clientId) throws RemoteException;

    void registerClientForAuction(long auctionId, IAuctionClientRemote client) throws RemoteException;
}
