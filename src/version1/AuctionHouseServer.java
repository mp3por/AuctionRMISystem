package version1;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by vbk20 on 01/11/2015.
 */
public class AuctionHouseServer {

    public AuctionHouseServer() {
        try {
            IAuctionHouseRemote auctionHouseRemote = new AuctionHouse();
            System.out.format("version1.Auction House created(%s)\n. Now lets register it.\n", auctionHouseRemote);

            // register for users
            Registry auctionHouseRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_HOUSE_SERVER_RMI_PORT);
            System.out.println("registry found");
            auctionHouseRegistry.rebind(Utils.AUCTION_HOUSE_REGISTRY_NAME, auctionHouseRemote);
//            // register for auctions
//            Registry auctionRegistry = LocateRegistry.getRegistry("localhost", version1.Utils.AUCTION_SERVER_RMI_PORT);
//            auctionRegistry.rebind(version1.Utils.AUCTION_HOUSE_REGISTRY_NAME, auctionHouseRemote); // register for auctions
            System.out.printf("version1.Auction House registered to the global AUCTION_HOUSE RMI registry.\n");
            System.out.println("---------------------------- version1.Auction House STARTED -----------------------------\n");
        } catch (Exception e) {
            System.out.format("export exception - %s\n", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new AuctionHouseServer();
    }
}
