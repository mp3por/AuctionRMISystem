import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by vbk20 on 01/11/2015.
 */
public class AuctionHouseServer {

    public AuctionHouseServer() {
        try {
            IAuctionHouseRemote auctionHouseRemote = new AuctionHouse();
            System.out.format("Auction House created(%s). Now lets register it.\n", auctionHouseRemote);

            // register for users
            Registry auctionHouseRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_HOUSE_SERVER_RMI_PORT);
            auctionHouseRegistry.rebind(Utils.AUCTION_HOUSE_REGISTRY_NAME, auctionHouseRemote);

//            // register for auctions
//            Registry auctionRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_SERVER_RMI_PORT);
//            auctionRegistry.rebind(Utils.AUCTION_HOUSE_REGISTRY_NAME, auctionHouseRemote); // register for auctions
            System.out.printf("Auction House registered to the global AUCTION_HOUSE RMI registry.\n");
            System.out.println("---------------------------- Auction House STARTED -----------------------------\n");
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
