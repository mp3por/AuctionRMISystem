import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Auction extends UnicastRemoteObject implements IAuctionRemote {
    // static finals
    private static final int AUCTION_ITEM_EXPIRE_TIME_MILISEC = 120000; // 120s expiration time
    private static final DateFormat formatter = Utils.formatter;
    // static
    private static long auctionItemIds = 0;
    private static long clientIds = 0;
    // fields
    private Map<Long, IAuctionItem> liveActionItems;
    private Map<Long, IAuctionItem> finishedItems;
    private Map<Long, IAuctionClientRemote> activeClients;
    private String name;

    public Auction(String auctionName) throws RemoteException, NotBoundException {
        // initialize local variables
        liveActionItems = new ConcurrentHashMap<Long, IAuctionItem>();
        finishedItems = new ConcurrentHashMap<Long, IAuctionItem>();
        activeClients = new ConcurrentHashMap<Long, IAuctionClientRemote>();
        this.name = auctionName;
        System.out.format("AUCTION: created.\n");
    }

    /**
     * Can be used to see all active items in the version1.Auction
     * @param clientId The id of the client that requested to see active AuctionItems
     * @return String representation of all currently active AuctionItems
     * @throws RemoteException
     */
    @Override
    public String getAuctionLiveItems(long clientId) throws RemoteException {
        System.out.format("AUCTION: Client (%d) requested auctionItems.\n", clientId);
        StringBuilder b = new StringBuilder();
        for (long key : liveActionItems.keySet()) {
            IAuctionItem item = liveActionItems.get(key);
            b.append("\t* " + key + " -> " + item.getItemName() + ", value:" + item.getValue() + ", endTime: " + formatter.format(item.getEndDate()) + "\n");
        }
        return b.toString();
    }

    /**
     * Creates and Registers an version1.Auction Item to this version1.Auction
     *
     * @param creatorId   the creator id (must be registered)
     * @param itemName    the item name
     * @param value       the item initial value
     * @param closingDate the item end time
     * @return the ID of the new version1.AuctionItem
     * @throws RemoteException
     */
    @Override
    public long createAndRegisterAuctionItem(long creatorId, String itemName, double value, Date closingDate) throws RemoteException, AuctionException {
        try {
            long newAuctionItemId = auctionItemIds++;
            AuctionItem i = new AuctionItem(newAuctionItemId, creatorId, this, itemName, value, closingDate);
            registerAuctionItem(newAuctionItemId, i);

            System.out.format("AUCTION: Client (-- %d --) created and registered a new version1.AuctionItem {%d,%s,%f,%s}.\n", creatorId, newAuctionItemId, itemName, value, formatter.format(closingDate));
            return newAuctionItemId;
        } catch (AuctionItem.AuctionItemNegativeStartValueException e) {
            System.out.format("AUCTION: Client (%d) wanted to create invalid version1.AuctionItem. Error:%s.\n", creatorId, e.getShortName());
            throw new AuctionException(e.getMessage());
        } catch (AuctionItem.AuctionItemInvalidEndDateException e) {
            System.out.format("AUCTION: Client (%d) wanted to create invalid version1.AuctionItem. Error:%s.\n", creatorId, e.getShortName());
            throw new AuctionException(e.getMessage());
        } catch (AuctionItem.AuctionItemInvalidItemNameException e) {
            System.out.format("AUCTION: Client (%d) wanted to create invalid version1.AuctionItem. Error:%s.\n", creatorId, e.getShortName());
            throw new AuctionException(e.getMessage());
        }
    }

    /**
     * Registers an version1.Auction Item to this version1.Auction
     *
     * @param item The item that you want to register
     */
    public void registerAuctionItem(long auctionItemId, IAuctionItem item) {
        liveActionItems.put(auctionItemId, item);
    }

    /**
     * A method for bidding for an item in this auction
     *
     * @param bidderId the id of the bidder
     * @param itemId   the id of the item
     * @param bidValue the bid value
     * @return boolean if the bid was successful or not
     * @throws RemoteException
     */
    @Override
    public boolean bidForItem(long bidderId, long itemId, double bidValue) throws RemoteException, AuctionException {

        // check if the API is being abused
        if (!activeClients.containsKey(bidderId)) {
            throw new AuctionException("You have not registered as a client to this auction. Please register first!");
        }

        // Get Item and bid
        IAuctionItem item = liveActionItems.get(itemId); // get Item from currently live AuctionItems
        if (item != null) {
            // if item exists
            System.out.format("AUCTION: Client (-- %d --) is bidding '%f' for item(%d,%s) with current bid value:%f.\n", bidderId, bidValue, itemId, item.getItemName(), item.getValue());
            return item.bidValue(bidderId, bidValue);
        } else {
            // if item DOES NOT exist
            System.out.format("AUCTION: Client(%d) is bidding '%f' for item(%d) that is not for sale.\n", bidderId, bidValue, itemId);
            throw new AuctionException(String.format("This item is no longer available for bidding or there is no version1.Auction Item with id '%d'.", itemId));
        }
    }

    /**
     * Register as a participant in this version1.Auction
     *
     * @param client The client that wants to get registered
     * @throws RemoteException
     */
    @Override
    public long registerClient(IAuctionClientRemote client) throws RemoteException {
        long newClientId = clientIds++;
        activeClients.put(newClientId, client);

        System.out.format("AUCTION: Registering client: %s.\n", newClientId);
        return newClientId;
    }

    /**
     * Clients can use this method to unregister from this version1.Auction. This way they will not receive notifications.
     *
     * @param clientId the client ID
     * @throws RemoteException
     */
    @Override
    public void unregisterClient(long clientId) throws RemoteException {
        System.out.format("AUCTION: UNregistering client: %d.\n", clientId);
        activeClients.remove(clientId);
    }

    /**
     * Method to call when a version1.AuctionItem is complete. It logs the data, removes the item from the list with live
     * version1.Auction Items and notifies all clients with the result.
     *
     * @param finishedAuctionItem
     */
    public void itemCompleteCallback(IAuctionItem finishedAuctionItem, List<Long> bidders) {
        System.out.format("AUCTION: version1.Auction notified that item {ID: %d,name: %s,finalValue: %f, lastBidder:  %d} has finished.\n",
                finishedAuctionItem.getId(),
                finishedAuctionItem.getItemName(),
                finishedAuctionItem.getValue(),
                finishedAuctionItem.getLastBidder());
        liveActionItems.remove(finishedAuctionItem.getId()); // remove from on-going auctionItems (thread-safe)
        new FinishedAuctionItem(finishedAuctionItem, finishedItems); // add to finished and schedule expiration

        for (long key : bidders) {
            try {
                // notify client
                ((IAuctionClientRemote) activeClients.get(key)).auctionItemEnd(
                        finishedAuctionItem.getId(),
                        finishedAuctionItem.isSold(),
                        finishedAuctionItem.getLastBidder(),
                        finishedAuctionItem.getItemName(),
                        finishedAuctionItem.getValue(),
                        finishedAuctionItem.getCreatorId()
                );
                System.out.format("AUCTION: Client (-- %d --) notified of finished version1.AuctionItem '%s'.\n", key, finishedAuctionItem.getItemName());
            } catch (RemoteException e) {
                activeClients.remove(key); // client no longer reachable.
            } catch (NullPointerException e) {
                // client unregistered -> do nothing
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String print() throws RemoteException {
        return this.name;
    }

    /**
     * A wrapper for finished version1.Auction Items so that they unregister themselves after a specific amount of time
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
            System.out.println("AUCTION: version1.AuctionItem (" + auctionItemId + ") expired and can not be reclaimed anymore.");
        }
    }
}
