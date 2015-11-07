import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionServer {
    static final long ONE_MINUTE_IN_MILLIS=60000;//millisecs

    public AuctionServer(String auctionName) {
        try {
            System.out.println("Creating Auction.");
            IAuctionRemote auction = new Auction(auctionName);
            System.out.format("Auction created (%s)\n. Now lets register it.\n", auction);
            Registry auctionServerRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_SERVER_RMI_PORT);
            auctionServerRegistry.rebind(Utils.AUCTION_REGISTRY_NAME, auction);
            System.out.println("Auction registered to the global AUCTION_SERVER RMI registry.\n");

            System.out.println("Adding initial bids.\n");
            Date now = new Date();
            Date defaultEndDate = new Date(now.getTime() + 10*ONE_MINUTE_IN_MILLIS);
            auction.createAndRegisterAuctionItem(Utils.DEFAULT_CREATOR_ID,"Guitar",100,defaultEndDate);
            auction.createAndRegisterAuctionItem(Utils.DEFAULT_CREATOR_ID,"Piano",100,defaultEndDate);
            auction.createAndRegisterAuctionItem(Utils.DEFAULT_CREATOR_ID,"Bottle",100,defaultEndDate);

            System.out.println("---------------------------- AUCTION STARTED -----------------------------\n");
//        } catch (AlreadyBoundException e) {
//            System.out.printf("There exist an version1.Auction with this name. Please try a different name.\n");
//            e.printStackTrace();
        } catch (Exception e) {
            System.out.format("export exception - %s\n", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new AuctionServer(args[0]);
    }
}