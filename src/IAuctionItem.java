import java.io.Serializable;

/**
 * Created by vbk20 on 29/10/2015.
 */
public interface IAuctionItem extends Serializable {
    public Long getId();

    public void bidValue(Long bidderId, double bidValue);
}
