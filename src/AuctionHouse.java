import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vbk20 on 01/11/2015.
 */
public class AuctionHouse extends UnicastRemoteObject implements IAuctionHouseRemote {
    private static long nextClientId = 0;
    private static long nextAuctionId = 0;

    private Map<Long, IAuctionRemote> activeAuctions;
    private Map<Long, IAuctionClientRemote> activeClients;

    public AuctionHouse() throws RemoteException {
        activeAuctions = new ConcurrentHashMap<Long, IAuctionRemote>();
        activeClients = new ConcurrentHashMap<Long, IAuctionClientRemote>();
        System.out.printf("AUCTION_HOUSE: Created.\n");
    }

    @Override
    public long registerAuction(IAuctionRemote auction) throws RemoteException {
        long auctionId = nextAuctionId++;
        System.out.printf("AUCTION_HOUSE: registering auction with ID: %d.\n", auctionId);
        activeAuctions.put(auctionId, auction);

        return auctionId;
    }

    @Override
    public void unregisterAuction(long auctionId) {
        System.out.printf("AUCTION_HOUSE: unregistering auction with ID: %d.\n", auctionId);
        activeAuctions.remove(auctionId);
    }

    @Override
    public long registerClient(IAuctionClientRemote client) throws RemoteException {
        long clientId = this.nextClientId++;
        activeClients.put(clientId, client);

        System.out.printf("AUCTION_HOUSE: Registering client with ID: %d.\n", clientId);
        return clientId;
    }

    @Override
    public void unregisterClient(long clientId) throws RemoteException {
        System.out.printf("AUCTION_HOUSE: Unregistering client with ID: %d.\n", clientId);
        activeClients.remove(clientId);
        for (long key : activeAuctions.keySet()) {
            try {
                activeAuctions.get(key).unregisterClient(clientId);
            } catch (RemoteException e) {
                activeAuctions.remove(key); // remove auction if unreachable
            }
        }
    }

    @Override
    public String getActiveAuctions() throws RemoteException {
        StringBuilder b = new StringBuilder();
        for (long key : activeAuctions.keySet()) {
            IAuctionRemote iAuctionRemote = activeAuctions.get(key);
            try {
                b.append("\t* " + iAuctionRemote.print() + "\n");
            } catch (RemoteException e) {
                // This Auction is not accessible any more so lets remove it.
                activeAuctions.remove(key);
            }
        }

        return b.toString();
    }

    @Override
    public String getAuctionLiveItems(long auctionId, long clientId) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                System.out.printf("AUCTION_HOUSE: Client (%d) wanted to see live items of Auction (%d).\n", clientId, auctionId);
                return auction.getAuctionLiveItems(clientId);
            } catch (RemoteException e) {
                System.out.printf("AUCTION_HOUSE: Client (%d) wanted to see live items of UNREACHABLE Auction (%d).\n", clientId, auctionId);
                throw new AuctionHouseException(String.format("Apologies! The Auction (%d) you are looking for is not longer available.\n", auctionId));
            }
        } else {
            throw new AuctionHouseException(String.format("Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }

    @Override
    public void registerClientForAuction(long auctionId, IAuctionClientRemote client) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                auction.registerClient(client);
            } catch (RemoteException e) {
                throw new AuctionHouseException(String.format("Apologies! The Auction (%d) you are looking for is not longer available.\n", auctionId));
            }
        } else {
            throw new AuctionHouseException(String.format("Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }

    @Override
    public boolean bidForItem(long bidderId, long auctionId, long itemId, double bidValue) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                return auction.bidForItem(bidderId, itemId, bidValue);
            } catch (RemoteException e) {
                throw new AuctionHouseException(String.format("Apologies! The Auction (%d) you are looking for is not longer available.\n", auctionId));
            } catch (AuctionException e) {
                throw new AuctionHouseException(e.getMessage());
            }
        } else {
            throw new AuctionHouseException(String.format("Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }

    @Override
    public long createAndRegisterAuctionItem(long creatorId, long auctionId, String itemName, double value, Date endDate) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                return auction.createAndRegisterAuctionItem(creatorId, itemName, value, endDate);
            } catch (RemoteException e) {
                throw new AuctionHouseException(String.format("Apologies! The Auction (%d) you are looking for is not longer available.\n", auctionId));
            } catch (AuctionException e) {
                throw new AuctionHouseException(e.getMessage());
            }
        } else {
            throw new AuctionHouseException(String.format("Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }
}
