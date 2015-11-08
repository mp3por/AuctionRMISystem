import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionServer {
    static final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs
    private Auction auction; // for direct access
    private long id;

    public AuctionServer(String auctionName) {
        try {
            System.out.println("Creating Auction.");
            auction = new Auction(auctionName);
            this.id = auction.getServerId("SERVER_ID_TOKEN");

            IAuctionRemote auctionRemote = auction; // for Remote Registering
            System.out.format("Auction created (%s)\n. Now lets register it.\n", auction);
            Registry auctionServerRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_SERVER_RMI_PORT);
            auctionServerRegistry.rebind(Utils.AUCTION_REGISTRY_NAME, auctionRemote);
            System.out.println("Auction registered to the global AUCTION_SERVER RMI registry.\n");

//            System.out.println("Adding initial bids.\n");
//            Date now = new Date();
//            Date defaultEndDate = new Date(now.getTime() + 10 * ONE_MINUTE_IN_MILLIS);
//            auction.createAndRegisterAuctionItem(Utils.DEFAULT_CREATOR_ID, "Guitar", 100, defaultEndDate);
//            auction.createAndRegisterAuctionItem(Utils.DEFAULT_CREATOR_ID, "Piano", 100, defaultEndDate);
//            auction.createAndRegisterAuctionItem(Utils.DEFAULT_CREATOR_ID, "Bottle", 100, defaultEndDate);

            System.out.println("---------------------------- AUCTION STARTED -----------------------------\n");
        } catch (Exception e) {
            System.out.format("export exception - %s\n", e.getMessage());
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
                if (input.length != 0) {
                    switch (input[0]) {
                        case "--slaf":
                            ;
                        case "--save-live-auctions-to-file":
                            switch (input.length) {
                                case 2:
                                    String fileName = input[1];
                                    File f = new File(fileName);
                                    if(!f.exists()){
                                        f.createNewFile();
                                    }
                                    try (FileWriter fr = new FileWriter(f); BufferedWriter bfr = new BufferedWriter(fr)) {
                                        List<Object[]> liveActionItemsForStorage = auction.getLiveActionItemsForStorage(this.id);
                                        System.out.println("AuctionLiveItems: "+ liveActionItemsForStorage.size());
                                        StringBuilder b = new StringBuilder();
                                        for (Object[] itemProperties : liveActionItemsForStorage) {
                                            for (Object property : itemProperties) {
                                                b.append(property + " ");
                                            }
                                            b.append("\n");
                                        }
                                        bfr.write(b.toString());
                                        System.out.format("Live AuctionItems exported to file '%s'.\n", fileName);
                                    } catch (FileNotFoundException e) {
                                        System.out.format("Unable to find file '%s'\n", fileName);
                                    } catch (IOException e) {
                                        System.out.format("Unable to write to file '%s'\n", fileName);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                default:
                                    System.out.printf("Please supply a destination filename.\n");
                            }
                            break;
                        case "--llaf":
                            ;
                        case "--load-love-auctions-from-file":
                            switch (input.length) {
                                case 2:
                                    String fileName = input[1];
                                    File f = new File(fileName);
                                    try(FileReader fr = new FileReader(f); BufferedReader bfr = new BufferedReader(fr)){
                                        String line;
                                        List<String[]> result = new ArrayList<>();
                                        while((line = bfr.readLine()) != null) {
                                            String[] inputs = line.split(" ");
                                            result.add(inputs);
                                        }
                                        auction.bulkCreateAndRegisterAuctionItems(-1111, result);
                                    } catch (NumberFormatException e){
                                        e.printStackTrace();
                                        auction.rollBackLastBulkAdd(this.id);
                                    }
                                    break;
                                default:
                                    System.out.printf("Please supply a source filename.\n");
                            }
                            break;
                        default:
                            System.out.format("Unrecognizable command :%s\n", input[0]);
                    }
                }
            } catch (Exception e) {
                eof = true;
            }
        }
    }

    public static void main(String[] args) {
        AuctionServer as = new AuctionServer(args[0]);
    }
}
