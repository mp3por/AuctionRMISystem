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

    private static final int AUCTION_ITEM_EXPIRE_TIME = 10000; // 10s expiration time
    private static final DateFormat formatter = new SimpleDateFormat("dd/MM/yy-HH:mm:ss");
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
        System.out.format("Client (%d) requested auctionItems.\n", id);
        StringBuilder b = new StringBuilder();
        for (long key: liveActionItems.keySet()){
            IAuctionItem item = liveActionItems.get(key);
            b.append("\t* " + key + " -> " + item.getItemName() + ", value:" + item.getValue() + ", endTime: " + formatter.format(item.getEndDate())+"\n");
        }
        return b.toString();
    }

    @Override
    public String createAndRegisterAuctionItem(long creatorId, String itemName, double value, Date closingDate) throws RemoteException {
//        System.out.format("Client (%d)",creatorId);
//        System.out.format(" created and registered a new Auction Item {%s,", itemName);
//        System.out.format(" ,%f,", value);
//        System.out.format(" , %s).\n", formatter.format(closingDate));

        Date now = new Date();
        if (closingDate.after(now)) {
            long aliveTime = closingDate.getTime() - now.getTime();
            AuctionItem i = new AuctionItem(creatorId, this, itemName, value, closingDate, aliveTime);
            registerAuctionItem(i);

            System.out.format("Client (%d) created and registered a new AuctionItem {%s,%f,%s}.\n", creatorId, itemName, value, formatter.format(closingDate));
            return "AuctionItem id:" + i.getId();
        }

        System.out.format("Client (%d) wanted to create invalid AuctionItem.", creatorId);
        return "AuctionItem not created. Your endDate was before the current time.";
    }

    public void registerAuctionItem(IAuctionItem item) {
        liveActionItems.put(item.getId(), item);
    }

    @Override
    public String bidForItem(long bidderId, long itemId, double bidValue) throws RemoteException {
        IAuctionItem item = liveActionItems.get(itemId);
        String result = "Undefined. Please contact administrator.";
        if (item != null) {
            System.out.format("Client(%d) is bidding '%f' for item(%d,%s) with current bid value:%f.\n", bidderId, bidValue, itemId, item.getItemName(), item.getValue());
            result = item.bidValue(bidderId, bidValue) ? "You are the highest bidder now." : "Current bid value is more than you offer.";
        } else {
            System.out.format("Clinet(%d) is bidding '%f' for item(%d) that is not for sale.\n", bidderId, bidValue, itemId);
            result = "This item is no longer available for bidding.";
        }
        return result;
    }

    @Override
    public void registerClient(IAuctionClientRemote client) throws RemoteException {
        System.out.format("Registering client: %d.\n", client.getId());
        activeClients.put(client.getId(), client);
    }

    @Override
    public void unregisterClient(long clientId) throws RemoteException {
        System.out.format("UNregistering client: %d.\n", clientId);
        activeClients.remove(clientId);
    }

    public void itemCompleteCallback(IAuctionItem finishedAuctionItem) {
        System.out.format("Auction notified that item {%d,%s} has finished.\n", finishedAuctionItem.getId(), finishedAuctionItem.getItemName());
        liveActionItems.remove(finishedAuctionItem.getId()); // remove from on-going auctionItems (thread-safe)
        new FinishedAuctionItem(finishedAuctionItem, finishedItems); // add to finished and schedule expiration

        boolean winnerNotified = false;
        for (long key: activeClients.keySet()){
            try {
                ((IAuctionClientRemote) activeClients.get(key)).auctionItemEnd(
                        finishedAuctionItem.isSold(),
                        finishedAuctionItem.getLastBidder(),
                        finishedAuctionItem.getItemName(),
                        finishedAuctionItem.getValue(),
                        finishedAuctionItem.getCreatorId()
                );
            } catch (RemoteException e) {
                activeClients.remove(key); // client no longer reachable.
            }
        }
    }

    private static class FinishedAuctionItem {
        private long auctionItemId;
        private Map<Long, IAuctionItem> finishedItems;
        private Timer timer;

        public FinishedAuctionItem(IAuctionItem item, Map<Long, IAuctionItem> finishedItems) {
            System.out.format("Finished item {%d,%s} set up to expire in %d seconds.\n", item.getId(), item.getItemName(), AUCTION_ITEM_EXPIRE_TIME / 1000);

            this.finishedItems = finishedItems;
            this.auctionItemId = item.getId();

            finishedItems.put(auctionItemId, item);
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    removeFromReady();
                }
            }, AUCTION_ITEM_EXPIRE_TIME);
        }

        private void removeFromReady() {
            finishedItems.remove(auctionItemId); // remove from ConcurrentHashMap is safe in multi-threaded apps
            System.out.println("AuctionItem (" + auctionItemId + ") expired and can not be reclaimed anymore.");
        }
    }
}
