import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Auction extends UnicastRemoteObject implements IAuctionRemote {
    // static finals
    private static final int AUCTION_ITEM_EXPIRE_TIME_MILISEC = 120000; // 10s expiration time
    private static final DateFormat formatter = Utils.formatter;
    // static
    private static long clientIds = 0;
    // fields
    private Map<Long, IAuctionItem> liveActionItems;
    private Map<Long, IAuctionItem> finishedItems;
    private Map<Long, IAuctionClientRemote> activeClients;

    public Auction() throws RemoteException {
        liveActionItems = new ConcurrentHashMap<Long, IAuctionItem>();
        finishedItems = new ConcurrentHashMap<Long, IAuctionItem>();
        activeClients = new ConcurrentHashMap<Long, IAuctionClientRemote>();
        System.out.format("Creating server object\n");
    }

    @Override
    public String getAuctionLiveItems(long id) throws RemoteException {
        System.out.format("AUCTION: Client (%d) requested auctionItems.\n", id);
        StringBuilder b = new StringBuilder();
        for (long key : liveActionItems.keySet()) {
            IAuctionItem item = liveActionItems.get(key);
            b.append("\t* " + key + " -> " + item.getItemName() + ", value:" + item.getValue() + ", endTime: " + formatter.format(item.getEndDate()) + "\n");
        }
        return b.toString();
    }

    /**
     * Creates and Registers an Auction Item to this Auction
     * @param creatorId the creator id (must be registered)
     * @param itemName  the item name
     * @param value the item initial value
     * @param closingDate the item end time
     * @return String representing the result. If the creation was successful it contains the created Auction Item ID for client's reference
     * @throws RemoteException
     */
    @Override
    public String createAndRegisterAuctionItem(long creatorId, String itemName, double value, Date closingDate) throws RemoteException {
        String result = "";
        AuctionItem i = null;
        try {
            i = new AuctionItem(creatorId, this, itemName, value, closingDate);
            registerAuctionItem(i);

            System.out.format("AUCTION: Client (-- %d --) created and registered a new AuctionItem {%d,%s,%f,%s}.\n", creatorId, i.getId(), itemName, value, formatter.format(closingDate));
            result = "Created new AuctionItem with id:" + i.getId();
        } catch (AuctionItem.AuctionItemNegativeStartValueException e) {
            result = e.getMessage();
            System.out.format("AUCTION: Client (%d) wanted to create invalid AuctionItem. Error:%s.\n", creatorId, e.getShortName());
        } catch (AuctionItem.AuctionItemInvalidEndDateException e) {
            result = e.getMessage();
            System.out.format("AUCTION: Client (%d) wanted to create invalid AuctionItem. Error:%s.\n", creatorId, e.getShortName());
        } catch (AuctionItem.AuctionItemInvalidItemNameException e) {
            result = e.getMessage();
            System.out.format("AUCTION: Client (%d) wanted to create invalid AuctionItem. Error:%s.\n", creatorId, e.getShortName());
        }

        return result;
    }

    /**
     * Registers an Auction Item to this Auction
     * @param item
     */
    public void registerAuctionItem(IAuctionItem item) {
        liveActionItems.put(item.getId(), item);
    }

    /**
     * A method for clients to use when they want to bid for an item in this auction
     * @param bidderId the id of the bidder
     * @param itemId the id of the item
     * @param bidValue the bid value
     * @return result holds the String representation of what happened so that the client knows
     * @throws RemoteException
     */
    @Override
    public String bidForItem(long bidderId, long itemId, double bidValue) throws RemoteException {

        // check if the API is being abused
        if (!activeClients.containsKey(bidderId)){
            return "You have not registered as a client to this auction. Please register first!";
        }

        // Get Item and bid
        IAuctionItem item = liveActionItems.get(itemId); // get Item from currently live AuctionItems
        String result = "Undefined. Please contact administrator.";
        if (item != null) {
            // if item exists
            System.out.format("AUCTION: Client (-- %d --) is bidding '%f' for item(%d,%s) with current bid value:%f.\n", bidderId, bidValue, itemId, item.getItemName(), item.getValue());
            result = item.bidValue(bidderId, bidValue) ? "You are the highest bidder now." : "Current bid value is more than you offer.";
        } else {
            // if item DOES NOT exist
            System.out.format("AUCTION: Client(%d) is bidding '%f' for item(%d) that is not for sale.\n", bidderId, bidValue, itemId);
            result = String.format("This item is no longer available for bidding or there is no Auction Item with id '%d'.",itemId);
        }

        return result;
    }

    /**
     * Clients can use this method to register as a participant in this Auction
     * @param client
     * @return
     * @throws RemoteException
     */
    @Override
    public long registerClient(IAuctionClientRemote client) throws RemoteException {
        Long clientId = this.clientIds++;
        System.out.format("AUCTION: Registering client: %d.\n", clientId);
        activeClients.put(clientId, client);
        return clientId;
    }

    /**
     * Clients can use this method to unregister from this Auction. This way they will not receive notifications.
     * @param clientId the client ID
     * @throws RemoteException
     */
    @Override
    public void unregisterClient(long clientId) throws RemoteException {
        System.out.format("AUCTION: UNregistering client: %d.\n", clientId);
        activeClients.remove(clientId);
    }

    /**
     * Method to call when a AuctionItem is complete. It logs the data, removes the item from the list with live
     * Auction Items and notifies all clients with the result.
     * @param finishedAuctionItem
     */
    public void itemCompleteCallback(IAuctionItem finishedAuctionItem) {
        System.out.format("AUCTION: Auction notified that item {ID: %d,name: %s,finalValue: %f, lastBidder:  %d} has finished.\n",
                finishedAuctionItem.getId(),
                finishedAuctionItem.getItemName(),
                finishedAuctionItem.getValue(),
                finishedAuctionItem.getLastBidder());
        liveActionItems.remove(finishedAuctionItem.getId()); // remove from on-going auctionItems (thread-safe)
        new FinishedAuctionItem(finishedAuctionItem, finishedItems); // add to finished and schedule expiration

        boolean winnerNotified = false;
        for (long key : activeClients.keySet()) {
            try {
                ((IAuctionClientRemote) activeClients.get(key)).auctionItemEnd(
                        finishedAuctionItem.getId(),
                        finishedAuctionItem.isSold(),
                        finishedAuctionItem.getLastBidder(),
                        finishedAuctionItem.getItemName(),
                        finishedAuctionItem.getValue(),
                        finishedAuctionItem.getCreatorId()
                );
                System.out.format("AUCTION: Client (-- %d --) notified of finished AuctionItem '%s'.\n", key, finishedAuctionItem.getItemName());
            } catch (RemoteException e) {
                activeClients.remove(key); // client no longer reachable.
            }
        }
    }

    /**
     * A wrapper for finished Auction Items so that they unregister themselves after a specific amount of time
     */
    private static class FinishedAuctionItem {
        private long auctionItemId;
        private Map<Long, IAuctionItem> finishedItems;
        private Timer timer;

        public FinishedAuctionItem(IAuctionItem item, Map<Long, IAuctionItem> finishedItems) {
            System.out.format("AUCTION: Finished item {%d,%s} set up to expire in %d seconds.\n", item.getId(), item.getItemName(), AUCTION_ITEM_EXPIRE_TIME_MILISEC / 1000);

            this.finishedItems = finishedItems;
            this.auctionItemId = item.getId();

            finishedItems.put(auctionItemId, item);
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    removeFromReady();
                }
            }, AUCTION_ITEM_EXPIRE_TIME_MILISEC);
        }

        private void removeFromReady() {
            finishedItems.remove(auctionItemId); // remove from ConcurrentHashMap is safe in multi-threaded apps
            System.out.println("AUCTION: AuctionItem (" + auctionItemId + ") expired and can not be reclaimed anymore.");
        }
    }
}
