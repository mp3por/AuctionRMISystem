package version1;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by velin on 30/10/2015.
 */
public interface IAuctionClientRemote extends Remote{
    void auctionItemEnd(long itemId, boolean isSold,long winnerId,String itemName, double finalPrice, long creatorId) throws RemoteException;
    long getId() throws RemoteException;
}
