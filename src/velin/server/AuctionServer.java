package velin.server;

import velin.common.IAuctionRemote;
import velin.common.Utils;

import java.io.*;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
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

    private void printHelp() {
        System.out.printf("Hello,\nAvailable commands:\n" +
                "\t* --slaf 'fileName': Saves all active AuctionItems to that file\n" +
                "\t* --llaf 'fileName': Loads AuctionItems from that file\n" +
                "\n\nNote: file loading format:\n" +
                "\t1. One item per line\n" +
                "\t2. format: 'itemId' '-1' 'itemName' '-1' 'startPrice' 'currentPrice' 'timeLeft'\n");
    }

    public AuctionServer(String[] args) {
        logger = Logger.getLogger(this.getClass().getName());
        String auctionName = args[0];
        try {
            logger.log(DEFAULT_LOG_LEVEL, "Creating velin.server.Auction (-- {0} --)", auctionName);
            LocateRegistry.createRegistry(Utils.AUCTION_SERVER_RMI_PORT);

            auction = new Auction(auctionName);

            // needed for extracting/loading files with AuctionItems
            this.id = auction.getServerId("SERVER_ID_TOKEN");

            IAuctionRemote auctionRemote = auction; // for Remote Registering
            logger.log(DEFAULT_LOG_LEVEL, "velin.server.Auction created \n. Now lets register it.\n");

//            Registry auctionServerRegistry = LocateRegistry.getRegistry("localhost", velin.common.Utils.AUCTION_SERVER_RMI_PORT)
//            auctionServerRegistry.rebind(velin.common.Utils.AUCTION_REGISTRY_NAME, auctionRemote);
            Naming.rebind("rmi://" + Utils.AUCTION_SERVER_HOST + "/" + Utils.AUCTION_SERVER_NAME, auctionRemote);
            logger.log(DEFAULT_LOG_LEVEL, "velin.server.Auction registered to the global AUCTION_SERVER RMI registry.\n");

            try {
                String fileName = args[1];
                readAuctionItemsFromFile(fileName);
            } catch (IndexOutOfBoundsException e) {
                logger.log(DEFAULT_LOG_LEVEL, "No file supplied to pre-load AuctionItems!\n");
            }

            logger.log(DEFAULT_LOG_LEVEL, "---------------------------- AUCTION HELP -----------------------------\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        printHelp();
        logger.log(DEFAULT_LOG_LEVEL, "---------------------------- AUCTION STARTED -----------------------------\n");

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
                        case "--save-live-auctionsItems-to-file":
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
                        case "--load-live-auctionItems-from-file":
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
            auction.bulkCreateAndRegisterAuctionItems(this.id, result);
        } catch (NumberFormatException e) {
            e.printStackTrace();
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
