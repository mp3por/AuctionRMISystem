package velin.server;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by vbk20 on 29/10/2015.
 */
public interface IAuctionItem extends Serializable {
    public Long getId();

    public boolean bidValue(Long bidderId, double bidValue);

    Date getEndDate();

    String getItemName();

    double getValue();

    long getLastBidder();

    double getStartValue();

    long getCreatorId();

    boolean isSold();
}
