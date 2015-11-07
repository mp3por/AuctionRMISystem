package version1;

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

    /**
     * Registers an version1.Auction to this version1.AuctionHouse which starts to manage the version1.Auction
     * @param auction The version1.Auction that is trying to get registered
     * @return The auction ID assigned by the version1.AuctionHouse
     * @throws RemoteException
     */
    @Override
    public long registerAuction(IAuctionRemote auction) throws RemoteException {
        long auctionId = nextAuctionId++;
        System.out.printf("AUCTION_HOUSE: registering auction with ID: %d.\n", auctionId);
        activeAuctions.put(auctionId, auction);

        return auctionId;
    }

    /**
     * Unregisters the version1.Auction from the version1.AuctionHouse
     * @param auctionId The id of the version1.Auction
     */
    @Override
    public void unregisterAuction(long auctionId) {
        System.out.printf("AUCTION_HOUSE: unregistering auction with ID: %d.\n", auctionId);
        activeAuctions.remove(auctionId);
    }

    /**
     * Registers a client to this version1.AuctionHouse
     * @param client The client that wants to get registered in this version1.AuctionHouse
     * @return The client ID assigned by the version1.AuctionHouse
     * @throws RemoteException
     */
    @Override
    public long registerClient(IAuctionClientRemote client) throws RemoteException {
        long clientId = this.nextClientId++;
        activeClients.put(clientId, client);

        System.out.printf("AUCTION_HOUSE: Registering client with ID: %d.\n", clientId);
        return clientId;
    }

    /**
     * Unregisteres a client from this version1.AuctionHouse
     * @param clientId The id of the Client
     * @throws RemoteException
     */
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

    /**
     *  Can be used by the Client to view currently available Auctions
     * @return String representation of all currently available Auctions (not sorted)
     * @throws RemoteException
     */
    @Override
    public String getActiveAuctions() throws RemoteException {
        StringBuilder b = new StringBuilder();
        for (long key : activeAuctions.keySet()) {
            IAuctionRemote iAuctionRemote = activeAuctions.get(key);
            try {
                b.append("\t* " + iAuctionRemote.print() + "\n");
            } catch (RemoteException e) {
                // This version1.Auction is not accessible any more so lets remove it.
                activeAuctions.remove(key);
            }
        }

        return b.toString();
    }

    /**
     * Can be used by the Client to view active AuctionItems in particular Auctions
     * @param auctionId The particular version1.Auction ID
     * @param clientId The Client's ID
     * @return String representation of all currently active AuctionItems in the specified version1.Auction
     * @throws RemoteException
     * @throws AuctionHouseException
     */
    @Override
    public String getAuctionLiveItems(long auctionId, long clientId) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                System.out.printf("AUCTION_HOUSE: Client (%d) wanted to see live items of version1.Auction (%d).\n", clientId, auctionId);
                return auction.getAuctionLiveItems(clientId);
            } catch (RemoteException e) {
                System.out.printf("AUCTION_HOUSE: Client (%d) wanted to see live items of UNREACHABLE version1.Auction (%d).\n", clientId, auctionId);
                throw new AuctionHouseException(String.format("Apologies! The version1.Auction (%d) you are looking for is not longer available.\n", auctionId));
            }
        } else {
            throw new AuctionHouseException(String.format("version1.Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }

    /**
     * Can be used by the Client to register to an version1.Auction allowing him to bid for active AuctionItems or create new
     * AuctionItems in the specified version1.Auction
     * @param auctionId The ID of the version1.Auction
     * @param client The Client
     * @throws RemoteException
     * @throws AuctionHouseException
     */
    @Override
    public void registerClientForAuction(long auctionId, IAuctionClientRemote client) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                auction.registerClient(client);
            } catch (RemoteException e) {
                throw new AuctionHouseException(String.format("Apologies! The version1.Auction (%d) you are looking for is not longer available.\n", auctionId));
            }
        } else {
            throw new AuctionHouseException(String.format("version1.Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }

    /**
     * Can be used by the Client to bid for an version1.AuctionItem in a particular version1.Auction
     * @param bidderId The id of the bidder
     * @param auctionId The id of the version1.Auction where the version1.AuctionItem is
     * @param itemId The id of an version1.AuctionItem in the specified version1.Auction
     * @param bidValue The value of the bid
     * @return
     * @throws RemoteException
     * @throws AuctionHouseException
     */
    @Override
    public boolean bidForItem(long bidderId, long auctionId, long itemId, double bidValue) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                return auction.bidForItem(bidderId, itemId, bidValue);
            } catch (RemoteException e) {
                throw new AuctionHouseException(String.format("Apologies! The version1.Auction (%d) you are looking for is not longer available.\n", auctionId));
            } catch (AuctionException e) {
                throw new AuctionHouseException(e.getMessage());
            }
        } else {
            throw new AuctionHouseException(String.format("version1.Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }

    /**
     * Can be used by the Client to create new AuctionItems in a particular version1.Auction
     * @param creatorId The creater ID
     * @param auctionId The particular version1.Auction ID
     * @param itemName The name of the new Item
     * @param value The starting value of the new version1.AuctionItem
     * @param endDate The end of the version1.AuctionItem after which no more bids are allowed (format dd/MM/yyyy-hh:mm:ss)
     * @return The ID of the new version1.AuctionItem
     * @throws RemoteException
     * @throws AuctionHouseException
     */
    @Override
    public long createAndRegisterAuctionItem(long creatorId, long auctionId, String itemName, double value, Date endDate) throws RemoteException, AuctionHouseException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        if (auction != null) {
            try {
                return auction.createAndRegisterAuctionItem(creatorId, itemName, value, endDate);
            } catch (RemoteException e) {
                throw new AuctionHouseException(String.format("Apologies! The version1.Auction (%d) you are looking for is not longer available.\n", auctionId));
            } catch (AuctionException e) {
                throw new AuctionHouseException(e.getMessage());
            }
        } else {
            throw new AuctionHouseException(String.format("version1.Auction with id (-- %d --) does not exist.\n", auctionId));
        }
    }
}
