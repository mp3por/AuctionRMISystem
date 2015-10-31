import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Utils {
    public static final String ACTION_REGISTRY_NAME = "rmi://localhost/AuctionServer";

    public static final long DEFAULT_BIDDER_ID = 0;
    public static final long DEFAULT_LAST_BIDDER_ID = -1;

    public static final DateFormat formatter = new SimpleDateFormat("dd/MM/yy-HH:mm:ss");
}
