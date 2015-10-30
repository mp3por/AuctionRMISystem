import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionItem implements IAuctionItem {

    private static long ids = 0;

    private long id;
    private long creatorId;
    private long lastBidder;
    private String itemName;
    private double startValue;
    private double value;
    private Date endDate;

    private AuctionItem auctionItem;
    private Auction auction;

    private Timer aliveTimer;
    private boolean isAlive;
    private long timeAlive;

    public AuctionItem(long creatorId, Auction auction, String itemName, double startValue, Date endDate, long timeAlive) {
        this.creatorId = creatorId;
        this.id = ids++;
        this.itemName = itemName;
        this.startValue = startValue;
        this.value = this.startValue;
        this.lastBidder = Utils.DEFAULT_LAST_BIDDER_ID;
        this.endDate = endDate;

        this.auction = auction;

        this.timeAlive = timeAlive;
        this.isAlive = true;
        this.aliveTimer = new Timer(true);
        aliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopBids();
//                auction.notifyItemComplete(auctionItem);
            }
        }, 0, timeAlive);
        this.auctionItem = this;
    }

    private void stopBids() {
        System.out.format("Item {%d,%s} bidding closed.", this.id, this.itemName);
        isAlive = false;
    }

    @Override
    public void bidValue(Long bidderId, double bidValue) {
        if (isAlive && bidValue > 0 && bidValue > this.value) {
            System.out.format("Bid of '%f' value for item {%d,%s} by bidder(%d) successful.",bidValue,this.id,this.itemName,bidderId);
            this.value = bidValue;
            this.lastBidder = bidderId;
        }
    }

    /* --------- Getters ------------ */
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getItemName() {
        return itemName;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public long getLastBidder() {
        return lastBidder;
    }

    @Override
    public double getStartValue() {
        return startValue;
    }

    @Override
    public long getCreatorId() {
        return creatorId;
    }

    @Override
    public String toString() {
        return "AuctionItem{" +
                "id=" + id +
                ", creatorId=" + creatorId +
                ", lastBidder=" + lastBidder +
                ", itemName='" + itemName + '\'' +
                ", startValue=" + startValue +
                ", value=" + value +
                ", endDate=" + endDate +
                ", aliveTimer=" + aliveTimer +
                ", isAlive=" + isAlive +
                ", timeAlive=" + timeAlive +
                '}';
    }
}
