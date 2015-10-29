import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionServer {
    public static void main(String[] args) {
        try {
            IAuctionRemote auction = new Auction();
            LocateRegistry.createRegistry(1099);
            Registry reg = LocateRegistry.getRegistry("localhost",1099);
            reg.rebind("AUCTION",auction);
        } catch (Exception e) {
            System.out.format("export exception - %s\n", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
