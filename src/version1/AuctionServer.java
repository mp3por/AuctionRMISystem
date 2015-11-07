package version1;

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
            Registry auctionHouseServerRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_HOUSE_SERVER_RMI_PORT);
            IAuctionHouseRemote auctionHouseRemote = (IAuctionHouseRemote) auctionHouseServerRegistry.lookup(Utils.AUCTION_HOUSE_REGISTRY_NAME);
            IAuctionRemote auction = new Auction(auctionName, auctionHouseRemote);

//            System.out.format("version1.Auction created(%s). Now lets register it.\n", auction);
//            String auctionRegistryName = String.format(version1.Utils.AUCTION_REGISTRY_NAME, auctionName);
//            Registry auctionServerRegistry = LocateRegistry.getRegistry("localhost", version1.Utils.AUCTION_SERVER_RMI_PORT);
//            auctionServerRegistry.rebind(auctionRegistryName, auction);
//            System.out.println("version1.Auction registered to the global AUCTION_SERVER RMI registry.\n");

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
