import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Auction extends UnicastRemoteObject implements IAuctionRemote {

    private static final int AUCTION_ITEM_EXPIRE_TIME = 10000; // 10s expiration time

    private Map<Long, IAuctionItem> liveActionItems;
    private Map<Long, IAuctionItem> finishedItems;

    public Auction() throws RemoteException {
        liveActionItems = new ConcurrentHashMap<Long,IAuctionItem>();
        finishedItems = new ConcurrentHashMap<Long, IAuctionItem>();
        System.out.format("Creating server object\n");
    }

    @Override
    public String printTESTTEST(String str) {
        System.out.format("Caller send %s. Sending back '%s'.", str, str);
        return str;
    }

    @Override
    public Map<Long, IAuctionItem> getAuctionItems() throws RemoteException {
        return liveActionItems;
    }

    @Override
    public long createAndRegisterAuctionItem(long creatorId, String itemName, double value, Date closingDate) throws RemoteException {
        long now = System.currentTimeMillis();
        long closingTime = closingDate.getTime();
        System.out.format("closingTime: " + closingTime + ", now: " + now + ", clTime>now: " + (closingTime > now) + ", clTime - now: " + (closingTime - now));
        long aliveTime = closingTime - now;
        AuctionItem i = new AuctionItem(creatorId, this, itemName, value, closingDate, aliveTime);
        registerAuctionItem(i);
        return i.getId();
    }

    @Override
    public void registerAuctionItem(IAuctionItem item) {
        liveActionItems.put(item.getId(), item);
    }

    @Override
    public void bidForItem(long bidderId,long itemId, double bidValue) throws RemoteException {
        IAuctionItem item = liveActionItems.get(itemId);
        if (item != null){
            System.out.format("Client(%d) is bidding '%f' for item(%d,%s) with current bid value:%f",bidderId,bidValue,itemId,item.getItemName(),item.getValue());
            item.bidValue(bidderId,bidValue);
        } else {
            System.out.format("Clinet(%d) is bidding '%f' for item(%d) that is not for sale",bidderId,bidValue,itemId);
        }

    }

//    public void notifyItemComplete(IAuctionItem finishedAuctionItem) {
//        System.out.format("Auction notified that item {%d,%s} has finished.",finishedAuctionItem.getId(),finishedAuctionItem.getItemName());
//        liveActionItems.remove(finishedAuctionItem.getId()); // remove from on-going auctionItems (thread-safe)
//        new FinishedAuctionItem(finishedAuctionItem, finishedItems); // add to finished and schedule expiration
//    }
//
//    private static class FinishedAuctionItem {
//        private long auctionItemId;
//        private Map<Long, IAuctionItem> finishedItems;
//        private Timer timer;
//
//        public FinishedAuctionItem(IAuctionItem item, Map<Long, IAuctionItem> finishedItems) {
//            System.out.format("Finished item {%d,%s} set up to expire in %d seconds",item.getId(),item.getItemName(),AUCTION_ITEM_EXPIRE_TIME/1000);
//
//            this.finishedItems = finishedItems;
//            this.auctionItemId = item.getId();
//
//            finishedItems.put(auctionItemId, item);
//            timer = new Timer(true);
//            timer.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    removeFromReady();
//                }
//            }, 0, AUCTION_ITEM_EXPIRE_TIME);
//        }
//
//        private void removeFromReady() {
//            finishedItems.remove(auctionItemId); // remove from ConcurrentHashMap is safe in multi-threaded apps
//            System.out.println("AuctionItem (" + auctionItemId + ") expired and can not be reclaimed anymore.");
//        }
//    }
}
