import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.Level;

/**
 * Created by vbk20 on 08/11/2015.
 */
public class PerformanceTester {

    private final IAuctionRemote auction;
    private long id;

    public PerformanceTester() throws Exception {
        // connect to an auction to test
        Registry auctionServerRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_SERVER_RMI_PORT);
        Object o = auctionServerRegistry.lookup(Utils.AUCTION_REGISTRY_NAME);
        auction = (IAuctionRemote) o;

        this.id = auction.getTesterId("TESTER_STRING_TOKEN"); // one call to prime system.
//        auction.setCommandLineLoggingLevel(this.id, Level.WARNING); // to disable all logs

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++)
            auction.getAuctionLiveItems(this.id);
        long elapsedTime = System.currentTimeMillis() - startTime;

        System.out.format("10000 calls in %d ms - %d.%03d ms/call\n",
                elapsedTime, elapsedTime / 10000, (elapsedTime % 10000) / 10);

//        auction.setCommandLineLoggingLevel(this.id, Level.INFO); // to reanable all logs

    }

    public static void main(String[] args) throws Exception {
        new PerformanceTester();
    }
}
