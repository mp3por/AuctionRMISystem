package velin.server;

import velin.common.IAuctionClientRemote;
import velin.common.IAuctionRemote;
import velin.common.Utils;

import java.io.IOException;
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

    /**
     * Basic velin.server.Auction Constructor
     *
     * @param auctionName The velin.server.Auction Name
     * @throws IOException If there is a problem opening 'log-velin.server.Auction.txt' file used for logging
     */
    public Auction(String auctionName) throws IOException {
        // initialize local variables
        liveActionItems = new ConcurrentHashMap<Long, IAuctionItem>();
        finishedItems = new ConcurrentHashMap<Long, IAuctionItem>();
        activeClients = new ConcurrentHashMap<Long, IAuctionClientRemote>();
        this.name = auctionName;

        // setUp logger
        this.LOGGER = Logger.getLogger(Auction.class.getName());
        LOGGER.setUseParentHandlers(false);
        this.fh = new FileHandler("log-Auction.txt", true);
        this.cs = new ConsoleHandler();
        LOGGER.addHandler(fh);
        LOGGER.addHandler(cs);
        LOGGER.setLevel(Level.INFO);

        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: created.\n");
    }


    /**
     * Can be used to see all active items in the velin.server.Auction
     *
     * @param clientId The id of the client that requested to see active AuctionItems
     * @return String representation of all currently active AuctionItems
     * @throws RemoteException
     */
    @Override
    public String getAuctionLiveItems(long clientId) throws RemoteException {
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

    /**
     * To be used by Automatic Testers to disable console logging for testing purposes
     *
     * @param requesterId The requester ID
     * @param level       The new Logging Level ( must be higher than LEVEL.INFO )
     * @throws RemoteException
     */
    public void setCommandLineLoggingLevel(long requesterId, Level level) throws RemoteException {
        if (requesterId == TESTER_ID) {
            LOGGER.log(Level.SEVERE, "Client (-- {0} --) changed the CommandLineLogginLevel to {1}", new Object[]{requesterId, level});
            cs.setLevel(level);
        }
    }

    /**
     * Can be used by server to remove all AuctionItems previously added by bulkCreateAndRegisterAuctionItems
     * Only the server can call this method
     *
     * @throws AuctionException
     */
    private void rollBackLastBulkAdd() {
        synchronized (liveAuctionsMapLock) {
            lastBulkAddition.forEach(liveActionItems::remove);
        }
    }

    /**
     * Can be used to create multiple AuctionItems at once
     *
     * @param requesterId The requester id
     * @param items       Items properties ( id, creatorId, itemName, lastBidderId, startValue, value, timeleft )
     * @throws AuctionException
     * @throws NumberFormatException
     */
    public void bulkCreateAndRegisterAuctionItems(long requesterId, List<String[]> items) throws AuctionException, NumberFormatException {
        // check for access
        if (requesterId == SERVER_ID) {
            Map<Long, IAuctionItem> itemsMap = new ConcurrentHashMap<>();
            lastBulkAddition = new ArrayList<>();
            try {
                for (String[] itemProperties : items) {
                    long itemId = Long.valueOf(itemProperties[0]);
                    long creatorId = Long.valueOf(itemProperties[1]);
                    String itemName = itemProperties[2];
                    long lastBidder = Long.valueOf(itemProperties[3]);
                    double startValue = Double.valueOf(itemProperties[4]);
                    double value = Double.valueOf(itemProperties[5]);
                    long timeLeft = Long.valueOf(itemProperties[6]);

                    // create and add new velin.server.AuctionItem
                    IAuctionItem i = AuctionItem.createAuctionItem(1000, this, itemId, creatorId, itemName, lastBidder, startValue, value, timeLeft);
                    itemsMap.put(itemId, i);
                    lastBulkAddition.add(itemId);

                    if (itemId >= auctionItemIds) {
                        auctionItemIds = itemId + 1; // to make sure ids do not repeat
                    }

                    LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) created and registered a new velin.server.AuctionItem ({1},{2},{3},{4}).\n", new Object[]{
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
            } catch (Exception e) {
                rollBackLastBulkAdd();
                throw new AuctionException(e.getMessage());
            }
        }
        throw new AuctionException("You have no access to this method!\n");
    }

    /**
     * Creates and Registers an velin.server.AuctionItem to this .velin.server.Auction
     *
     * @param creatorId   the creator id (must be registered)
     * @param itemName    the item name
     * @param value       the item initial value
     * @param closingDate the item end time
     * @return the ID of the new velin.server.AuctionItem
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
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) created and registered a new velin.server.AuctionItem ({1},{2},{3},{4}).\n", new Object[]{creatorId, newAuctionItemId, itemName, value, Utils.formatter.format(closingDate)});
            return newAuctionItemId;
        } catch (AuctionItem.AuctionItemNegativeStartValueException e) {
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) wanted to create invalid velin.server.AuctionItem. Error:{1}.\n", new Object[]{creatorId, e.getShortName()});
            throw new AuctionException(e.getMessage());
        } catch (AuctionItem.AuctionItemInvalidEndDateException e) {
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) wanted to create invalid velin.server.AuctionItem. Error:{1}.\n", new Object[]{creatorId, e.getShortName()});
            throw new AuctionException(e.getMessage());
        } catch (AuctionItem.AuctionItemInvalidItemNameException e) {
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) wanted to create invalid velin.server.AuctionItem. Error:{1}.\n", new Object[]{creatorId, e.getShortName()});
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
        if (bidderId != TESTER_ID && !activeClients.containsKey(bidderId)) {
            throw new AuctionException("You have not registered as a client to this auction. Please register first!");
        }

        IAuctionItem item = null;
        synchronized (liveAuctionsMapLock) {
            item = liveActionItems.get(itemId);
        }
        if (item != null) {
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) is bidding '{1}' for item({2},{3}) with current bid value:{4}.\n", new Object[]{bidderId, bidValue, itemId, item.getItemName(), item.getValue()});
            return item.bidValue(bidderId, bidValue);
        } else {
            LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) is bidding '{1}' for item({2}) that is not for sale.\n", new Object[]{bidderId, bidValue, itemId});
            throw new AuctionException(String.format("This item is no longer available for bidding or there is no velin.server.Auction Item with id '%d'.", itemId));
        }
    }

    /**
     * Register as a participant in this velin.server.Auction
     *
     * @param client The client that wants to get registered
     * @throws RemoteException
     */
    @Override
    public long registerClient(IAuctionClientRemote client) throws RemoteException {
        long newClientId = clientIds++;
        activeClients.put(newClientId, client);

        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Registering client:{0}.\n", newClientId);
        return newClientId;
    }

    /**
     * Clients can use this method to unregister from this velin.server.Auction. This way they will not receive notifications.
     *
     * @param clientId the client ID
     * @throws RemoteException
     */
    @Override
    public void unregisterClient(long clientId) throws RemoteException {
        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: UNregistering client: {0}.\n", clientId);
        activeClients.remove(clientId);
    }

    /**
     * Method to call when a velin.server.AuctionItem is complete. It logs the data, removes the item from the list with live
     * velin.server.Auction Items and notifies all clients with the result.
     *
     * @param finishedAuctionItem
     * @param bidders
     */
    public void itemCompleteCallback(long finishedItemId, IAuctionItem finishedAuctionItem, Set<Long> bidders) {
        LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: velin.server.Auction notified that item (ID: {0},name: {1},finalValue: {2}, lastBidder:  {3}) has finished.\n", new Object[]{
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
                LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: Client (-- {0} --) notified of finished velin.server.AuctionItem {1}.\n", new Object[]{bidderId, finishedAuctionItem.getItemName()});
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

    public long getServerId(String token) throws AuctionException {
        if (token.equals("SERVER_ID_TOKEN")) {
            return SERVER_ID;
        }
        throw new AuctionException("Incorrect token!\n");
    }

    @Override
    public long getTesterId(String token) throws RemoteException, AuctionException {
        if (token.equals("TESTER_STRING_TOKEN"))
            return TESTER_ID;
        throw new AuctionException("Token incorrect!\n");
    }

    /**
     * A method to be used by the velin.server.AuctionServer to get all active AuctionsItems
     *
     * @param requesterId the ID of the requester
     * @return A list of currently active AuctionItems
     * @throws AuctionException thrown if the requesterId is not a server
     */
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
     * A wrapper for finished velin.server.Auction Items so that they unregister themselves after a specific amount of time
     */
    private static class FinishedAuctionItem {
        private long auctionItemId;
        private Map<Long, IAuctionItem> finishedItems;
        private Logger LOGGER;

        public FinishedAuctionItem(long finishedItemId, IAuctionItem item, Map<Long, IAuctionItem> finishedItems, Logger LOGGER) {
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
            finishedItems.remove(this.auctionItemId);
            this.LOGGER.log(DEFAULT_LOG_LEVEL, "AUCTION: velin.server.AuctionItem (" + this.auctionItemId + ") expired and can not be reclaimed anymore.");
        }
    }
}