import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Auction extends UnicastRemoteObject implements IAuctionRemote {
    // static finals
    private static final int AUCTION_ITEM_EXPIRE_TIME_MILISEC = 120000; // 120s expiration time
    private static final long TESTER_ID = -100l; // the ID for a tester
    private static final Level DEFAULT_LOG_LEVEL = Level.INFO;
    private static final long SERVER_ID = -1111;

    // static
    private static long auctionItemIds = 0;
    private static long clientIds = 0;
    // fields
    private Map<Long, IAuctionItem> liveActionItems;
    private Map<Long, IAuctionItem> finishedItems;
    private Map<Long, IAuctionClientRemote> activeClients;
    private String name;
    private final Lock liveAuctionsMapLock = new ReentrantLock();
    private List<Long> lastBulkAddition;

    // Logging
    private Logger LOGGER;
    private FileHandler fh;
    private ConsoleHandler cs;

    public Auction(String auctionName) throws IOException, NotBoundException {
        // initialize local variables
        liveActionItems = new ConcurrentHashMap<Long, IAuctionItem>();
        finishedItems = new ConcurrentHashMap<Long, IAuctionItem>();
        activeClients = new ConcurrentHashMap<Long, IAuctionClientRemote>();
        this.name = auctionName;

        this.LOGGER = Logger.getLogger(Auction.class.getName());
        LOGGER.setUseParentHandlers(false);
        this.fh = new FileHandler("log-Auction.txt", true);
        this.cs = new ConsoleHandler();
        LOGGER.addHandler(fh);
        LOGGER.addHandler(cs);
        LOGGER.setLevel(Level.INFO);

//        System.out.format("AUCTION: created.\n");
        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: created.\n");
    }


    /**
     * Can be used to see all active items in the Auction
     *
     * @param clientId The id of the client that requested to see active AuctionItems
     * @return String representation of all currently active AuctionItems
     * @throws RemoteException
     */
    @Override
    public String getAuctionLiveItems(long clientId) throws RemoteException {
//        System.out.format("AUCTION: Client (%d) requested auctionItems.\n", clientId);
        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client {0} requested auctionItems.\n", clientId);
        StringBuilder b = new StringBuilder();
        synchronized (liveAuctionsMapLock) {
            for (long key : liveActionItems.keySet()) {
                IAuctionItem item = liveActionItems.get(key);
                b.append("\t* " + key + " -> " + item.getItemName() + ", value:" + item.getValue() + ", endTime: " + Utils.formatter.format(item.getEndDate()) + "\n");
            }
        }
        return b.toString();
    }

    @Override
    public void setCommandLineLoggingLevel(long requesterId, Level level) throws RemoteException {
        if (requesterId == TESTER_ID) {
            LOGGER.log(Level.SEVERE, "Client (-- {0} --) changed the CommandLineLogginLevel to {1}", new Object[]{requesterId, level});
            cs.setLevel(level);
        }
    }

    /**
     * Can be used by server to remove all AuctionItems previously added by bulkCreateAndRegisterAuctionItems
     * Only the server can call this method
     * @param requesterId The requester ID
     * @throws AuctionException
     */
    public void rollBackLastBulkAdd(long requesterId) throws AuctionException {
        if( requesterId == SERVER_ID){
            synchronized (liveAuctionsMapLock){
                lastBulkAddition.forEach(liveActionItems::remove);
            }
        }
        throw new AuctionException("You have no access to this method!\n");
    }

    /**
     * Can be used to create multiple AuctionItems at once
     *
     * @param requesterId The requester id
     * @param items Items properties ( id, creatorId, itemName, lastBidderId, startValue, value, timeleft )
     * @throws AuctionException
     * @throws NumberFormatException
     */
    public void bulkCreateAndRegisterAuctionItems(long requesterId, List<String[]> items) throws AuctionException, NumberFormatException {
        // check for access
        if (requesterId == SERVER_ID) {
            Map<Long, IAuctionItem> itemsMap = new ConcurrentHashMap<>();
            lastBulkAddition = new ArrayList<>();
            for (String[] itemProperties : items) {
                long itemId = Long.valueOf(itemProperties[0]);
                long creatorId = Long.valueOf(itemProperties[1]);
                String itemName = itemProperties[2];
                long lastBidder = Long.valueOf(itemProperties[3]);
                double startValue = Double.valueOf(itemProperties[4]);
                double value = Double.valueOf(itemProperties[5]);
                long timeLeft = Long.valueOf(itemProperties[6]);

                // create and add new AuctionItem
                IAuctionItem i = AuctionItem.createAuctionItem(1000, this, itemId, creatorId, itemName, lastBidder, startValue, value, timeLeft);
                itemsMap.put(itemId, i);
                lastBulkAddition.add(itemId);

                if (itemId >= auctionItemIds) {
                    auctionItemIds = itemId + 1; // to make sure ids do not repeat
                }

                LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) created and registered a new AuctionItem ({1},{2},{3},{4}).\n", new Object[]{
                        "SERVER",
                        itemId,
                        itemName,
                        value,
                        Utils.formatter.format(new Date(new Date().getTime() + timeLeft))});
            }

            // add everthing in one go
            synchronized (liveAuctionsMapLock) {
                liveActionItems.putAll(itemsMap);
            }
        }
        throw new AuctionException("You have no access to this method!\n");
    }

    /**
     * Creates and Registers an AuctionItem to this .Auction
     *
     * @param creatorId   the creator id (must be registered)
     * @param itemName    the item name
     * @param value       the item initial value
     * @param closingDate the item end time
     * @return the ID of the new AuctionItem
     * @throws RemoteException
     */
    @Override
    public long createAndRegisterAuctionItem(long creatorId, String itemName, double value, Date closingDate) throws RemoteException, AuctionException {
        try {
            long newAuctionItemId = auctionItemIds++;
            IAuctionItem i = new AuctionItem(newAuctionItemId, creatorId, this, itemName, value, closingDate);
            synchronized (liveAuctionsMapLock) {
                liveActionItems.put(newAuctionItemId, i);
            }
//            System.out.format("AUCTION: Client (-- %d --) created and registered a new AuctionItem {%d,%s,%f,%s}.\n", creatorId, newAuctionItemId, itemName, value, Utils.formatter.format(closingDate));
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) created and registered a new AuctionItem ({1},{2},{3},{4}).\n", new Object[]{creatorId, newAuctionItemId, itemName, value, Utils.formatter.format(closingDate)});
            return newAuctionItemId;
        } catch (AuctionItem.AuctionItemNegativeStartValueException e) {
//            System.out.format("AUCTION: Client (%d) wanted to create invalid AuctionItem. Error:%s.\n", creatorId, e.getShortName());
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) wanted to create invalid AuctionItem. Error:{1}.\n", new Object[]{creatorId, e.getShortName()});
            throw new AuctionException(e.getMessage());
        } catch (AuctionItem.AuctionItemInvalidEndDateException e) {
//            System.out.format("AUCTION: Client (%d) wanted to create invalid AuctionItem. Error:%s.\n", creatorId, e.getShortName());
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) wanted to create invalid AuctionItem. Error:{1}.\n", new Object[]{creatorId, e.getShortName()});
            throw new AuctionException(e.getMessage());
        } catch (AuctionItem.AuctionItemInvalidItemNameException e) {
//            System.out.format("AUCTION: Client (%d) wanted to create invalid AuctionItem. Error:%s.\n", creatorId, e.getShortName());
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) wanted to create invalid AuctionItem. Error:{1}.\n", new Object[]{creatorId, e.getShortName()});
            throw new AuctionException(e.getMessage());
        }
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
        IAuctionItem item = null;
        synchronized (liveAuctionsMapLock) {
            item = liveActionItems.get(itemId); // get Item from currently live AuctionItems
        }
        if (item != null) {
            // if item exists
//            System.out.format("AUCTION: Client (-- %d --) is bidding '%f' for item(%d,%s) with current bid value:%f.\n", bidderId, bidValue, itemId, item.getItemName(), item.getValue());
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) is bidding '{1}' for item({2},{3}) with current bid value:{4}.\n", new Object[]{bidderId, bidValue, itemId, item.getItemName(), item.getValue()});
            return item.bidValue(bidderId, bidValue);
        } else {
            // if item DOES NOT exist
//            System.out.format("AUCTION: Client (-- %d --) is bidding '%f' for item(%d) that is not for sale.\n", bidderId, bidValue, itemId);
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) is bidding '{1}' for item({2}) that is not for sale.\n", new Object[]{bidderId, bidValue, itemId});
            throw new AuctionException(String.format("This item is no longer available for bidding or there is no Auction Item with id '%d'.", itemId));
        }
    }

    /**
     * Register as a participant in this Auction
     *
     * @param client The client that wants to get registered
     * @throws RemoteException
     */
    @Override
    public long registerClient(IAuctionClientRemote client) throws RemoteException {
        long newClientId = clientIds++;
        activeClients.put(newClientId, client);

//        System.out.format("AUCTION: Registering client: %s.\n", newClientId);
        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Registering client:{0}.\n", newClientId);
        return newClientId;
    }

    /**
     * Clients can use this method to unregister from this Auction. This way they will not receive notifications.
     *
     * @param clientId the client ID
     * @throws RemoteException
     */
    @Override
    public void unregisterClient(long clientId) throws RemoteException {
//        System.out.format("AUCTION: UNregistering client: %d.\n", clientId);
        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: UNregistering client: {0}.\n", clientId);
        activeClients.remove(clientId);
    }

    /**
     * Method to call when a AuctionItem is complete. It logs the data, removes the item from the list with live
     * Auction Items and notifies all clients with the result.
     *
     * @param finishedAuctionItem
     * @param bidders
     */
    public void itemCompleteCallback(long finishedItemId, IAuctionItem finishedAuctionItem, Set<Long> bidders) {
//        System.out.format("AUCTION: Auction notified that item {ID: %d,name: %s,finalValue: %f, lastBidder:  %d} has finished.\n",
//                finishedAuctionItem.getId(),
//                finishedAuctionItem.getItemName(),
//                finishedAuctionItem.getValue(),
//                finishedAuctionItem.getLastBidder());
        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Auction notified that item (ID: {0},name: {1},finalValue: {2}, lastBidder:  {3}) has finished.\n", new Object[]{
                finishedAuctionItem.getId(),
                finishedAuctionItem.getItemName(),
                finishedAuctionItem.getValue(),
                finishedAuctionItem.getLastBidder()});
        synchronized (liveAuctionsMapLock) {
            liveActionItems.remove(finishedItemId); // remove from on-going auctionItems
        }
        new FinishedAuctionItem(finishedItemId, finishedAuctionItem, finishedItems, LOGGER); // add to finished and schedule expiration

        for (long bidderId : bidders) {
            try {
                // notify client
                ((IAuctionClientRemote) activeClients.get(bidderId)).auctionItemEnd(
                        finishedAuctionItem.getId(),
                        finishedAuctionItem.isSold(),
                        finishedAuctionItem.getLastBidder(),
                        finishedAuctionItem.getItemName(),
                        finishedAuctionItem.getValue(),
                        finishedAuctionItem.getCreatorId()
                );
//                System.out.format("AUCTION: Client (-- %d --) notified of finished AuctionItem '%s'.\n", bidderId, finishedAuctionItem.getItemName());
                LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) notified of finished AuctionItem {1}.\n", new Object[]{bidderId, finishedAuctionItem.getItemName()});
            } catch (RemoteException e) {
                activeClients.remove(bidderId); // client no longer reachable.
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

    public long getServerId(String token) throws AuctionException {
        if (token.equals("SERVER_ID_TOKEN")) {
            return SERVER_ID;
        }
        throw new AuctionException("Incorrect token!\n");
    }

//    public Map<Long, IAuctionItem> getLiveActionItemsMap(long requesterId) throws AuctionException {
//        if (requesterId == SERVER_ID)
//            return liveActionItems;
//        throw new AuctionException("You are not allowed to access this method!\n");
//    }

    @Override
    public long getTesterId(String token) throws RemoteException, AuctionException {
        if (token.equals("TESTER_STRING_TOKEN"))
            return TESTER_ID;
        throw new AuctionException("Token incorrect!\n");
    }

    public List<Object[]> getLiveActionItemsForStorage(long requesterId) throws AuctionException {
        if (requesterId == SERVER_ID) {
            List<Object[]> result = new ArrayList<>();
            synchronized (liveAuctionsMapLock) {
                for (Long itemId : liveActionItems.keySet()) {
                    IAuctionItem item = liveActionItems.get(itemId);
                    long timeLeft = item.getEndDate().getTime() - (new Date()).getTime();
                    Object[] properties = new Object[]{
                            item.getId(),
                            item.getCreatorId(),
                            item.getItemName(),
                            item.getLastBidder(),
                            item.getStartValue(),
                            item.getValue(),
                            timeLeft
                    };
                    result.add(properties);
                }
            }
            return result;
        }
        throw new AuctionException("You have no access to this method.\n");
    }

    /**
     * A wrapper for finished Auction Items so that they unregister themselves after a specific amount of time
     */
    private static class FinishedAuctionItem {
        private long auctionItemId;
        private Map<Long, IAuctionItem> finishedItems;
        private Logger LOGGER;

        public FinishedAuctionItem(long finishedItemId, IAuctionItem item, Map<Long, IAuctionItem> finishedItems, Logger LOGGER) {
//            System.out.format("AUCTION: Finished item {%d,%s} set up to expire in %d seconds.\n", item.getId(), item.getItemName(), AUCTION_ITEM_EXPIRE_TIME_MILISEC / 1000);
            this.finishedItems = finishedItems;
            this.auctionItemId = finishedItemId;
            this.LOGGER = LOGGER;

            finishedItems.put(this.auctionItemId, item);
            Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    removeFromReady();
                }
            }, AUCTION_ITEM_EXPIRE_TIME_MILISEC);

            this.LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Finished item ({0},{1}) set up to expire in {2} seconds.\n", new Object[]{item.getId(), item.getItemName(), AUCTION_ITEM_EXPIRE_TIME_MILISEC / 1000});
        }

        private void removeFromReady() {
            finishedItems.remove(this.auctionItemId); // remove from ConcurrentHashMap is safe in multi-threaded apps
//            System.out.println("AUCTION: AuctionItem (" + this.auctionItemId + ") expired and can not be reclaimed anymore.");
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: AuctionItem (" + this.auctionItemId + ") expired and can not be reclaimed anymore.");
        }
    }
}
