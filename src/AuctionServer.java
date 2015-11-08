import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionServer {
    static final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs
    private Auction auction; // for direct access
    private long id;
    private Logger logger;
    private static final Level DEFAULT_LOG_LEVEL = Level.INFO;

    public AuctionServer(String[] args) {
        logger = Logger.getLogger(this.getClass().getName());
        String auctionName = args[0];
        try {
            logger.log(DEFAULT_LOG_LEVEL, "Creating Auction (-- {0} --)", auctionName);
            auction = new Auction(auctionName);
            this.id = auction.getServerId("SERVER_ID_TOKEN");

            IAuctionRemote auctionRemote = auction; // for Remote Registering
            logger.log(DEFAULT_LOG_LEVEL, "Auction created \n. Now lets register it.\n");

            Registry auctionServerRegistry = LocateRegistry.getRegistry("localhost", Utils.AUCTION_SERVER_RMI_PORT);
            auctionServerRegistry.rebind(Utils.AUCTION_REGISTRY_NAME, auctionRemote);
            logger.log(DEFAULT_LOG_LEVEL, "Auction registered to the global AUCTION_SERVER RMI registry.\n");

            try {
                String fileName = args[1];
                readAuctionItemsFromFile(fileName);
            } catch (IndexOutOfBoundsException e) {
                logger.log(DEFAULT_LOG_LEVEL, "No file supplied to pre-load AuctionItems!\n");
            }

            logger.log(DEFAULT_LOG_LEVEL, "---------------------------- AUCTION STARTED -----------------------------\n");
        } catch (Exception e) {
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
                                    if (!f.exists()) {
                                        f.createNewFile();
                                    }
                                    try (FileWriter fr = new FileWriter(f); BufferedWriter bfr = new BufferedWriter(fr)) {
                                        List<Object[]> liveActionItemsForStorage = auction.getLiveActionItemsForStorage(this.id);
                                        StringBuilder b = new StringBuilder();
                                        for (Object[] itemProperties : liveActionItemsForStorage) {
                                            for (Object property : itemProperties) {
                                                b.append(property + " ");
                                            }
                                            b.append("\n");
                                        }
                                        bfr.write(b.toString());
                                        logger.log(DEFAULT_LOG_LEVEL, "{0} Live AuctionItems exported to file '{1}'.\n", new Object[]{liveActionItemsForStorage.size(), fileName});
                                    } catch (FileNotFoundException e) {
                                        logger.log(DEFAULT_LOG_LEVEL, "Unable to find file '{0}'\n", fileName);
                                    } catch (IOException e) {
                                        logger.log(DEFAULT_LOG_LEVEL, "Unable to write to file '{0}'\n", fileName);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                default:
                                    logger.log(DEFAULT_LOG_LEVEL, "Please supply a destination filename.\n");
                            }
                            break;
                        case "--llaf":
                            ;
                        case "--load-love-auctions-from-file":
                            switch (input.length) {
                                case 2:
                                    readAuctionItemsFromFile(input[1]);
                                    break;
                                default:
                                    logger.log(DEFAULT_LOG_LEVEL, "Please supply a destination filename.\n");
                            }
                            break;
                        default:
                            logger.log(DEFAULT_LOG_LEVEL, "Unrecognizable command :{0}\n", input[0]);
                    }
                }
            } catch (Exception e) {
                eof = true;
            }
        }
    }

    private void readAuctionItemsFromFile(String fileName) throws AuctionException {
        File f = new File(fileName);
        try (FileReader fr = new FileReader(f); BufferedReader bfr = new BufferedReader(fr)) {
            String line;
            List<String[]> result = new ArrayList<>();
            while ((line = bfr.readLine()) != null) {
                String[] inputs = line.split(" ");
                result.add(inputs);
            }
            auction.bulkCreateAndRegisterAuctionItems(-1111, result);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            auction.rollBackLastBulkAdd(this.id);
        } catch (FileNotFoundException e) {
            logger.log(DEFAULT_LOG_LEVEL, "Unable to find file '{0}'\n", f.getName());
        } catch (IOException e) {
            logger.log(DEFAULT_LOG_LEVEL, "Unable to write to file '{0}'\n", f.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new AuctionServer(args);

    }
}
