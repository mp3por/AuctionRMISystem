package velin.server;

import velin.common.Utils;

import java.text.DateFormat;
import java.util.*;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionItem implements IAuctionItem {
    // static consts
    private static final int ITEM_NAME_MIN_LENGTH = 3;
    private static final long ITEM_MIN_ALIVE_TIME_SEC = 10;
    private static final double ITEM_MIN_START_VALUE = 10;
    private final static int AUCTION_TOKEN = 1000;

    // properties
    private long id;
    private long creatorId;
    private long lastBidder;
    private String itemName;
    private double startValue;
    private double value;
    private Date endDate;
    private Set<Long> bidders;

    // connections
    private AuctionItem auctionItem;
    private Auction auction;

    // alive variables
    private Timer aliveTimer;
    private boolean isAlive;
    private long timeAlive;

    /**
     * Constructor to be used by the client to create AuctionItems
     *
     * @param id
     * @param creatorId
     * @param auction
     * @param itemName
     * @param startValue
     * @param endDate
     * @throws AuctionItemNegativeStartValueException
     * @throws AuctionItemInvalidEndDateException
     * @throws AuctionItemInvalidItemNameException
     */
    public AuctionItem(long id, long creatorId, Auction auction, String itemName, double startValue, Date endDate) throws AuctionItemNegativeStartValueException, AuctionItemInvalidEndDateException, AuctionItemInvalidItemNameException {

        // Error checking
        Date now = new Date();
        long aliveTime = endDate.getTime() - now.getTime();
        if (endDate.before(now) || aliveTime < ITEM_NAME_MIN_LENGTH) {
            String m = String.format("Invalid endDate ( %s )! The endDate must be at least %dsec after the current time - %s.\n", Utils.formatter.format(endDate), ITEM_MIN_ALIVE_TIME_SEC, Utils.formatter.format(now));
            throw new AuctionItemInvalidEndDateException(m, endDate, Utils.formatter);
        }
        if (startValue <= ITEM_MIN_START_VALUE) {
            String m = String.format("Invalid startValue ( %f )! The initial item value must be above %f.\n", startValue, ITEM_MIN_START_VALUE);
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
        this.bidders = new HashSet<>();
        bidders.add(creatorId);


        // set up the Alive Timer
        setUpTheAliveTimer();
    }

    /**
     * Sets up the timer to notify after a certain time.
     */
    private synchronized void setUpTheAliveTimer() {
        this.aliveTimer = new Timer(true);
        this.auctionItem = this;
        aliveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopBids();
                auction.itemCompleteCallback(auctionItem.id, auctionItem, bidders);
            }
        }, this.timeAlive);
    }

    /**
     * Method to be used by the velin.server.Auction to create AuctionItems
     *
     * @param token
     * @param auction
     * @param itemId
     * @param creatorId
     * @param itemName
     * @param lastBidder
     * @param startValue
     * @param value
     * @param timeLeft
     * @return
     */
    public synchronized static AuctionItem createAuctionItem(int token, Auction auction, long itemId, long creatorId, String itemName, long lastBidder, double startValue, double value, long timeLeft) {
        if (token == AUCTION_TOKEN) {
            return new AuctionItem(auction, itemId, creatorId, itemName, lastBidder, startValue, value, timeLeft);
        }
        return null;
    }

    /**
     * Constructor to use from createAuctionItem method
     *
     * @param auction The velin.server.Auction
     * @param itemId The ItemId
     * @param creatorId The createrID
     * @param itemName The Name
     * @param lastBidder The lastBidderID
     * @param startValue The startValue
     * @param value The currentValue
     * @param timeLeft The timeAlive
     */
    private AuctionItem(Auction auction, long itemId, long creatorId, String itemName, long lastBidder, double startValue, double value, long timeLeft) {
        Date endDate = new Date((new Date()).getTime() + timeLeft);
        this.timeAlive = timeLeft;
        this.creatorId = creatorId;
        this.id = itemId;
        this.itemName = itemName;
        this.lastBidder = lastBidder;
        this.startValue = startValue;
        this.value = value;
        this.endDate = endDate;
        this.auction = auction;
        this.isAlive = true;
        this.bidders = new HashSet<>();
        bidders.add(creatorId);

        setUpTheAliveTimer();
    }

    /**
     * Stops any bidding.
     */
    private void stopBids() {
        isAlive = false;
        aliveTimer = null;
    }

    /**
     * A method to bid for this value.
     *
     * @param bidderId the bidder Id
     * @param   the bid value
     * @return boolean if the bid was successfull.
     */
    @Override
    public synchronized boolean bidValue(Long bidderId, double bidValue) {
        bidders.add(bidderId);
        boolean result = false;
        if (isAlive && bidValue > 0 && bidValue > this.value) {
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
    public synchronized long getLastBidder() {
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
        return "velin.server.AuctionItem{" +
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
        private final Date clientStartValue;
        private final DateFormat formatter;

        public AuctionItemInvalidEndDateException(String message, Date clientStartValue, DateFormat formatter) {
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
