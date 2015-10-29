import java.util.Date;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class ActionItem implements IAuctionItem {

    private static long ids = 1;

    private long id;
    private long creatorId;
    private long lastBidder;
    private String itemName;
    private double startValue;
    private double value;
    private Date endDate;

    public ActionItem(long creatorId, String itemName, double startValue, Date endDate) {
        this.creatorId = creatorId;
        this.id = ids++;
        this.itemName = itemName;
        this.startValue = startValue;
        this.value = this.startValue;
        this.lastBidder = -1;
        this.endDate = endDate;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void bidValue(Long bidderId,double bidValue) {
        if (bidValue>0 && bidValue>this.value){
            this.value = bidValue;
            this.lastBidder = bidderId;
        }
    }

    public String getItemName() {
        return itemName;
    }

    public double getValue() {
        return value;
    }

    public Date getEndDate() {
        return endDate;
    }

    public long getLastBidder() {
        return lastBidder;
    }

    @Override
    public String toString() {
        return "ActionItem{" +
                "itemName='" + itemName + '\'' +
                ", value=" + value +
                ", endDate=" + endDate +
                '}';
    }
}
