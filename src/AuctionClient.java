import javax.rmi.CORBA.Util;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.DoubleSummaryStatistics;
import java.util.Scanner;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionClient {
    private static long ids = 0;
    private long id;

    IAuctionRemote auction = null;
    DateFormat formatter = new SimpleDateFormat("MM/dd/yy-hh:mm:ss");

    public AuctionClient() {
        this.id = ids++;
        try {
            System.out.format("Client starting\n");
            Object o = Naming.lookup(Utils.ACTION_REGISTRY_NAME);
            auction = (IAuctionRemote) o;
        } catch (Exception e) {
            System.out.format("Error obtaining (--" + Utils.ACTION_REGISTRY_NAME + "--) from registry\n");
            e.printStackTrace();
            System.exit(1);
        }

        Scanner stdin = new Scanner(System.in);
        String buf = null;
        Boolean eof = false;
        while (!eof) {
            try {
                buf = stdin.nextLine();
                String[] input = buf.split(" ");
                System.out.println(Arrays.toString(input));

                switch (input.length) {
                    case 1: // one argument commands
                        switch (input[0]) {
                            case "--l":// handle 'l' to do same as 'list'
                                ;
                            case "--list-auction-items": // list all auction items
                                System.out.println("Items currently in auction: " + auction.getAuctionItems().toString());
                                break;
                            default:
                                System.out.format("Unrecognizable command :%s\n", input[0]);
                        }
                        break;
                    case 2: // two arguments commands
                        switch (input[0]) {
                            case "--b":// handle 'b' to do same as 'bid'
                                ;
                            case "--bid":
                                break;
                            default:
                                System.out.format("Unrecognizable command :%s\n", input[0]);
                        }
                        break;
                    case 4: // four arguments commands
                        switch (input[0]) {
                            case "--c":// handle 'c' to do the same as 'create'
                                ;
                            case "--create": // create auction items
                                String itemName = input[1];
                                double value = Double.valueOf(input[2]);
                                Date endDate = formatter.parse(input[3]);

                                auction.createAndRegisterAuctionItem(this.id, itemName, value, endDate);
                                break;
                            default:
                                System.out.format("Unrecognizable command :%s\n", input[0]);
                        }
                        break;
                    default:
                        System.out.format("Too many arguments in the command :%s\n", buf);
                }

                Thread.sleep(1000); // sleep for one second
            } catch (InterruptedException e) {
                ; // ignored
            } catch (Exception e) {
                eof = true;
            }
        }
    }

    public static void main(String[] args) {
        new AuctionClient();
    }
}
