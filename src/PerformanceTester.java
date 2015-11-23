import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

/**
 * Created by vbk20 on 08/11/2015.
 */
public class PerformanceTester {

    private final IAuctionRemote auction;
    private int N = 1000;
    private long id, startTime, elapsedTime;

    public PerformanceTester(String host, int test) throws Exception {

        // connect to an auction to test
//        Registry auctionServerRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_SERVER_RMI_PORT);
//        Object o = auctionServerRegistry.lookup("rmi://" + Utils.AUCTION_SERVER_HOST + "/" + Utils.AUCTION_SERVER_NAME);

        Object o = Naming.lookup("rmi://" + Utils.AUCTION_SERVER_HOST + "/" + Utils.AUCTION_SERVER_NAME);
        auction = (IAuctionRemote) o;

        this.id = auction.getTesterId("TESTER_STRING_TOKEN"); // one call to prime system and get the TESTER_ID
        auction.setCommandLineLoggingLevel(this.id, Level.WARNING); // to disable all logs
        System.out.println("Testing Auction: " + auction.getName());

        switch (test) {
            case 1:
                getStatsGetAuctionLiveItems();
                break;
            case 2:
                getStatBidItem();
                break;
            case 3:
                getStatCreateAuctionItem();
                break;
            case 4:
                getStataConcurrrentCreateAndBid();
                break;
        }

        auction.setCommandLineLoggingLevel(this.id, Level.INFO); // to reanable all logs

    }

    private void getStataConcurrrentCreateAndBid() throws InterruptedException {
        startTime = System.currentTimeMillis();
        testConcurrentCreateAndBid();
        elapsedTime = System.currentTimeMillis() - startTime;
        System.out.format(N + " concurrentCalls calls in %d ms - %d.%03d ms/call\n\n", elapsedTime, elapsedTime / N, (elapsedTime % N) / 10);

    }

    private void getStatCreateAuctionItem() {
        startTime = System.currentTimeMillis();
        testCreateAuctionItem();
        elapsedTime = System.currentTimeMillis() - startTime;
        System.out.format((N) + " createAuctionItem calls in %d ms - %d.%03d ms/call\n", elapsedTime, elapsedTime / N, (elapsedTime % N) / 10);

    }

    private void getStatBidItem() {
        startTime = System.currentTimeMillis();
        testBid();
        elapsedTime = System.currentTimeMillis() - startTime;
        System.out.format(N + " bid calls in %d ms - %d.%03d ms/call\n", elapsedTime, elapsedTime / N, (elapsedTime % N) / 10);

    }


    private void getStatsGetAuctionLiveItems() {
        startTime = System.currentTimeMillis();
        testGetAuctionLiveItems();
        elapsedTime = System.currentTimeMillis() - startTime;
        System.out.format(N + " getAuctionLiveItems in %d ms - %d.%03d ms/call\n", elapsedTime, elapsedTime / N, (elapsedTime % N) / 10);

    }

    private void testConcurrentCreateAndBid() throws InterruptedException {
        Thread client1 = getTestCreateThread();
        Thread client2 = getTestBidThread();
        client1.start();
        client2.start();
        client1.join();
        client2.join();
    }

    public Thread getTestBidThread() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                testBid();
            }
        });
        return t;
    }

    public Thread getTestCreateThread() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                testCreateAuctionItem();
            }
        });
        return t;
    }

    private void testCreateAuctionItem() {
        Date d = new Date(new Date().getTime() + 60000);
        try {
            for (int i = 0; i < N; i++) {
                auction.createAndRegisterAuctionItem(this.id, "TEST_ITEM", 100, d);
            }
        } catch (RemoteException e) {
            System.out.println("Auction unreachable. Stop testing!");
        } catch (AuctionException e) {
            e.printStackTrace();
        }
    }

    private void testBid() {
        Date d = new Date(new Date().getTime() + 60000);
        try {
            long auctionItemId = auction.createAndRegisterAuctionItem(this.id, "TESTING_ITEM", 100, d);
            for (int i = 0; i < N; i++) {
                auction.bidForItem(this.id, auctionItemId, i * 100);
            }
        } catch (RemoteException e) {
            System.out.println("Auction unreachable. Stop testing!");
        } catch (AuctionException e) {
            e.printStackTrace();
        }
    }


    private void testGetAuctionLiveItems() {
        for (int i = 0; i < N; i++) {
            try {
                auction.getAuctionLiveItems(this.id);
            } catch (RemoteException e) {
                System.out.println("Auction unreachable. Stop testing!");
                break;
            }
        }
    }


    public static void main(String[] args) throws Exception {
        new PerformanceTester(args[0], Integer.valueOf(args[1]));
    }
}
