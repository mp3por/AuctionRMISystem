import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.UUID;
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
    public long registerAuction(IAuctionRemote auction) {
        long auctionId = nextAuctionId++;
        System.out.printf("AUCTION_HOUSE: registering auction with ID: %d.\n", auctionId);
        activeAuctions.put(auctionId, auction);

        return auctionId;
    }

    @Override
    public void unregisterAuction(UUID auctionId) {
        System.out.printf("AUCTION_HOUSE: unregistering auction with ID: %s.\n", auctionId.toString());
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
    }

    @Override
    public String getActiveAuctions() {
        StringBuilder b = new StringBuilder();
        for (long key : activeAuctions.keySet()) {
            IAuctionRemote iAuctionRemote = activeAuctions.get(key);
            try {
                b.append("\t* " + iAuctionRemote.getName() + "\n");
            } catch (RemoteException e) {
                // This Auction is not accessible any more so lets remove it.
                activeAuctions.remove(key);
            }
        }

        return b.toString();
    }

    @Override
    public String getAuctionLiveItems(long auctionId, long clientId) {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        String result = "";
        try {
            result = auction.getAuctionLiveItems(clientId);
            System.out.printf("AUCTION_HOUSE: Client (%d) wanted to see live items of Auction (%d).\n", clientId, auctionId);
        } catch (RemoteException e) {
            System.out.printf("AUCTION_HOUSE: Client (%d) wanted to see live items of UNREACHABLE Auction (%d).\n", clientId, auctionId);
            result = String.format("Apologies! The Auction (%d) you are looking for is not longer available.\n", auctionId);
        }

        return result;
    }

    @Override
    public void registerClientForAuction(long auctionId, IAuctionClientRemote client) throws RemoteException {
        IAuctionRemote auction = activeAuctions.get(auctionId);
        auction.registerClient(client);
    }
}
