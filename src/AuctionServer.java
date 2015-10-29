import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionServer {

    public AuctionServer (){
        try {
            IAuctionRemote auction = new Auction();
            Naming.rebind(Utils.ACTION_REGISTRY_NAME, auction);
        } catch (Exception e) {
            System.out.format("export exception - %s\n", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new AuctionServer();
    }
}
