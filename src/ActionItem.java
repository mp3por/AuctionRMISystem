import java.util.Date;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class ActionItem implements IAuctionItem {
    String itemName;
    double value;
    Date endDate;

    public ActionItem(String itemName, double value, Date endDate) {
        this.itemName = itemName;
        this.value = value;
        this.endDate = endDate;
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
