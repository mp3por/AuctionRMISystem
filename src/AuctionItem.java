import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionItem implements IAuctionItem {
    // static consts
    private static final DateFormat formatter = Utils.formatter;
    private static final int ITEM_NAME_MIN_LENGTH = 3;
    private static final long ITEM_MIN_ALIVE_TIME_SEC = 10;
    private static final double ITEM_MIN_START_VALUE = 10;

    // properties
    private long id;
    private long creatorId;
    private long lastBidder;
    private String itemName;
    private double startValue;
    private double value;
    private Date endDate;
    private List<Long> bidders;

    // connections
    private AuctionItem auctionItem;
    private Auction auction;

    // alive variables
    private Timer aliveTimer;
    private boolean isAlive;
    private long timeAlive;

//    public static AuctionItem createAuctionItem(long creatorId, Auction auction, String itemName, double startValue, Date endDate) {
//        AuctionItem item = null;
//        System.out.println("endDate.after(now)?:" + endDate.after(new Date()));
//        Date now = new Date();
//        if (endDate.after(now)) {
//            long timeAlive = endDate.getTime() - now.getTime();
//            item = new AuctionItem(creatorId, auction, itemName, startValue, endDate,timeAlive);
//        }
//        return item;
//    }

    public AuctionItem(long id, long creatorId, Auction auction, String itemName, double startValue, Date endDate) throws AuctionItemNegativeStartValueException, AuctionItemInvalidEndDateException, AuctionItemInvalidItemNameException {

        // Error checking
        Date now = new Date();
        long aliveTime = endDate.getTime() - now.getTime();
        if (endDate.before(now) || aliveTime < ITEM_NAME_MIN_LENGTH) {
            String m = String.format("Invalid endDate ( %s )! The endDate must be at least %dsec after the current time - %s.\n", formatter.format(endDate), ITEM_MIN_ALIVE_TIME_SEC, formatter.format(now));
            throw new AuctionItemInvalidEndDateException(m, endDate, formatter);
        }
        if (startValue <= ITEM_MIN_START_VALUE) {
            System.out.println("AUCTION_ITEM: startValue(" + startValue + ") < ITEM_MIN_START_VALUE (10)");
            String m = String.format("Invalid startValue ( %f )! The initial item value must be above %f.\n", startValue);
            throw new AuctionItemNegativeStartValueException(m, startValue);
        }
        if (itemName.length() == ITEM_NAME_MIN_LENGTH) {
            String m = String.format("Invalid itemName! The name of the Item must be more than %d characters long.\n", ITEM_NAME_MIN_LENGTH);
            throw new AuctionItemInvalidItemNameException(m, itemName);
        }

        // Fields assignment
        this.timeAlive = aliveTime;
        this.creatorId = creatorId;
        this.id = id;
        this.itemName = itemName;
        this.startValue = startValue;
        this.value = this.startValue;
        this.lastBidder = Utils.DEFAULT_LAST_BIDDER_ID;
        this.endDate = endDate;
        this.auction = auction;
        this.isAlive = true;
        this.bidders = new ArrayList<Long>();

        // set up the Alive Timer
        this.aliveTimer = new Timer(true);
        this.auctionItem = this;
        aliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopBids();
                auction.itemCompleteCallback(auctionItem, bidders);
            }
        }, this.timeAlive);
    }

    /**
     * Stops any bidding.
     */
    private void stopBids() {
        System.out.format("AUCTION_ITEM: Item {%d,%s} bidding closed.\n", this.id, this.itemName);
        isAlive = false;
        aliveTimer = null;
    }

    /**
     * A mmethod to bid for this value
     *
     * @param bidderId the bidder Id
     * @param bidValue the bid value
     * @return boolean if the bid was successfull.
     */
    @Override
    public synchronized boolean bidValue(Long bidderId, double bidValue) {
        bidders.add(bidderId);
        boolean result = false;
        if (isAlive && bidValue > 0 && bidValue > this.value) {
            System.out.format("AUCTION_ITEM: Bid of '%f' value for item {%d,%s} by bidder(%d) successful.", bidValue, this.id, this.itemName, bidderId);
            this.value = bidValue;
            this.lastBidder = bidderId;
            result = true;
        }
        return result;
    }

    /* --------- Getters ------------ */
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Date getEndDate() {
        return endDate;
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
    public boolean isSold() {
        return Utils.DEFAULT_LAST_BIDDER_ID != this.lastBidder;
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

    public class AuctionItemInvalidItemNameException extends Exception {
        private static final String shortName = "Name Exception:%s.\n";
        private final String clientStartValue;

        public AuctionItemInvalidItemNameException(String message, String clientStartValue) {
            super(message);
            this.clientStartValue = clientStartValue;
        }

        public String getShortName() {
            return String.format(shortName, clientStartValue);
        }
    }

    public class AuctionItemInvalidEndDateException extends Exception {
        private static final String shortName = "endDate Exception:%s.\n";
        private final java.util.Date clientStartValue;
        private final DateFormat formatter;

        public AuctionItemInvalidEndDateException(String message, java.util.Date clientStartValue, DateFormat formatter) {
            super(message);
            this.clientStartValue = clientStartValue;
            this.formatter = formatter;
        }

        public String getShortName() {
            return String.format(shortName, formatter.format(clientStartValue));
        }
    }

    public class AuctionItemNegativeStartValueException extends Exception {
        private static final String shortName = "StartValue Exception:%f.\n";
        private final double clientStartValue;

        public AuctionItemNegativeStartValueException(String message, double clientStartValue) {
            super(message);
            this.clientStartValue = clientStartValue;
        }

        public String getShortName() {
            return String.format(shortName, clientStartValue);
        }
    }
}
