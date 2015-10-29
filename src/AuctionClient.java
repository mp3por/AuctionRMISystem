import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionClient {

    public static void main(String[] args) {
        IAuctionRemote auction = null;

        try {
            System.out.format("Client starting\n");
            Registry reg = LocateRegistry.getRegistry("localhost", 1099);
            Object o = reg.lookup("AUCTION");
            auction = (IAuctionRemote) o;
        } catch (Exception e) {
            System.out.format("Error obtaining (--" + Utils.ACTION_REGISTRY_NAME + "--) from registry\n");
            e.printStackTrace();
            System.exit(1);
        }

        Scanner stdin = new Scanner(System.in);
        String buf = null;
        Boolean eof = false;
        while (! eof) {
            try {
                buf = stdin.nextLine();

                System.out.format(auction.printTESTTEST(buf));


                Thread.sleep(1000); // sleep for one second
            } catch (InterruptedException e) {
                ; // ignored
            } catch (Exception e) {
                eof = true;
            }
        }

    }
}
