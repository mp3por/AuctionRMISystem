import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class AuctionClient extends UnicastRemoteObject implements IAuctionClientRemote {
    private long id;

    IAuctionHouseRemote auctionHouseRemote = null;
    DateFormat formatter = new SimpleDateFormat("dd/MM/yy-HH:mm:ss");

    public AuctionClient() throws RemoteException {
        try {
            System.out.format("Client starting.\n");
            Registry reg = LocateRegistry.getRegistry("localhost", Utils.AUCTION_HOUSE_SERVER_RMI_PORT);
            Object o = reg.lookup(Utils.AUCTION_HOUSE_REGISTRY_NAME);
            auctionHouseRemote = (IAuctionHouseRemote) o;

            this.id = auctionHouseRemote.registerClient(this);
//            auctionHouseRemote.registerClientForAuction(0, this);

            System.out.format("Client with ID (-- %d --) registered.\n", this.id);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        printHelp();
        Scanner stdin = new Scanner(System.in);
        String buf = null;
        Boolean eof = false;
        while (!eof) {
            try {
                buf = stdin.nextLine();
                String[] input = buf.split(" ");

                if (buf.length() != 0) {
                    switch (input[0]) {
                        case "--h":
                            ;
                        case "--help":
                            printHelp();
                            break;
                        case "--r":
                            ;
                        case "--register-for-auction":
                            switch (input.length){
                                case 2:
                                    try {
                                        long auctionId = Long.valueOf(input[1]);
                                        auctionHouseRemote.registerClientForAuction(auctionId, this);
                                    } catch (NumberFormatException e) {
                                        System.out.printf("The auctionID must consist of only NUMBERS.\n");
                                    } catch (RemoteException e) {
                                        System.out.println("Auction House is unavailable.");
                                    }

                                    break;
                                default:
                                    System.out.printf("Too many or too few arguments. Must be exactly 2 arguments.\n");
                            }
                            break;
                        case "--l":
                            ;
                        case "--list-auction-items":
                            switch (input.length) {
                                case 1:
                                    System.out.printf("Available Auctions in the Auction House:\n");
                                    auctionHouseRemote.getActiveAuctions();
                                    break;
                                case 2:
                                    try {
                                        long auctionId = Long.valueOf(input[1]);
                                        System.out.printf("Items currently in auction (-- %d -- ):\n",input[1]);
                                        String auctionItems = auctionHouseRemote.getAuctionLiveItems(auctionId, this.id);
                                        System.out.println(auctionItems);
                                    } catch (NumberFormatException e) {
                                        System.out.printf("The auctionID must consist of only NUMBERS.\n");
                                    } catch (RemoteException e) {
                                        System.out.println("Auction House is unavailable.");
                                    }
                                    break;
                                default:
                                    System.out.printf("Too many or too few arguments. Must be exactly 2 arguments.\n");
                            }
                            break;
                        case "--b":
                            ;
                        case "--bid":
                            switch (input.length) {
                                case 4:
                                    try {
                                        long auctionId = Long.valueOf(input[1]);
                                        try {
                                            long itemId = Long.valueOf(input[2]);
                                            try {
                                                double bidValue = Double.parseDouble(input[3]);
                                                String result = auctionHouseRemote.bidForItem(this.id, auctionId, itemId, bidValue);
                                                System.out.println(result);
                                            } catch (NumberFormatException e) {
                                                System.out.printf("The bidValue must be of type Double (E.x: 10.25).");
                                            } catch (RemoteException e) {
                                                System.out.println("Auction House is unavailable.");
                                            }
                                        } catch (NumberFormatException e) {
                                            System.out.printf("The itemID must consist of only NUMBERS.");
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.printf("The autionID must consist of only NUMBERS.");
                                    }
//                                    Long itemId = Long.valueOf(input[1]);
//                                    double bidValue = Double.parseDouble(input[2]);
//                                    String result = auctionHouseRemote.bidForItem(this.id, auctionId, itemId, bidValue);
//                                    System.out.println(result);
                                    break;
                                default:
                                    System.out.printf("Too many or too few arguments. Must be exactly 4 arguments\n");
                            }
                            break;
                        case "--c":
                            ;
                        case "--create":
                            switch (input.length) {
                                case 5:
                                    try {
                                        long auctionId = Long.valueOf(input[1]);
                                        try {
                                            String itemName = input[2];
                                            double value = Double.valueOf(input[3]);
                                            try {
                                                Date endDate = formatter.parse(input[4]);
                                                String result = auctionHouseRemote.createAndRegisterAuctionItem(this.id, auctionId, itemName, value, endDate);
                                                System.out.println("Create result: " + result);
                                            } catch (ParseException e) {
                                                System.out.println("Date must be in the format dd/MM/yyyy-hh:mm:ss.");
                                            } catch (RemoteException e) {
                                                System.out.println("Auction House is unavailable.");
                                            }
                                        } catch (NumberFormatException e) {
                                            System.out.printf("The bidValue must be of type Double (E.x: 10.25).");
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.printf("The autionID must consist of only NUMBERS.");
                                    }


//                                    String itemName = input[1];
//                                    double value = Double.valueOf(input[2]);
//                                    Date endDate = formatter.parse(input[3]);
//
////                                String result = auction.createAndRegisterAuctionItem(this.id, itemName, value, endDate);
//                                    String result = auctionHouseRemote.createAndRegisterAuctionItem(this.id, auctionId, itemName, value, endDate);
//
//                                    System.out.println("Create result: " + result);
                                    break;
                                default:
                                    System.out.printf("Too many or too few arguments. Must be exactly 5 arguments\n");
                            }
                            break;
                        default:
                            System.out.format("Unrecognizable command :%s\n", input[0]);
                    }
                    Thread.sleep(1000); // sleep for one second
                }

            } catch (InterruptedException e) {
                ; // ignored
            } catch (Exception e) {
                e.printStackTrace();
                eof = true;
            }
        }
        try {
            auctionHouseRemote.unregisterClient(this.id);
            auctionHouseRemote = null;
        } catch (RemoteException e) {
            auctionHouseRemote = null;
        }
    }

    private void printHelp() {
        System.out.printf("Hello,\nAvailable commands:\n" +
                "\t* --l: Prints all available Auctions with their name and Ids\n" +
                "\t* --l 'auctionId': Prints all available Auction Items (name + id) in the Auction with 'id'\n" +
                "\t* --c 'auctionId' 'itemName' 'startValue' 'endDate': Creates a new Auction Item with the specified details\n" +
                "\t* --b 'auctionId' 'itemId' 'bidValue':  Bids for that particular Auction Item in that particular Auction\n" +
                "\t* --r 'auctionId': Registers you as a client for this particular Auction so that you can bid and/or create Auction Items\n" +
                "\nRemember that you have to register (--r command) in an Auction before you can do bid for Items or create new Items in this Auction.\n");
    }

    public static void main(String[] args) {
        try {
            new AuctionClient();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void auctionItemEnd(long itemId, boolean isSold, long winnerId, String itemName, double finalPrice, long creatorId) throws RemoteException {
        if (isSold) {
            if (winnerId == this.id) {
                System.out.format("Concratulations! You won '%s,%d' with bid for: %f.\n", itemName, itemId, finalPrice);
            } else if (creatorId == this.id) {
                System.out.format("GREAT! Your item '%s,%d' was won by client (-- %d --) for %f.\n", itemName, itemId, winnerId, finalPrice);
            } else {
                System.out.format("Client '%d' won '%s,%d' with bid for: %f.\n", winnerId, itemName, itemId, finalPrice);
            }
        } else {
            if (creatorId == this.id) {
                System.out.format("Your item (%s,%d) was not sold because no one bid more than the starting price.\n", itemName, itemId);
            }
        }
    }

    @Override
    public long getId() throws RemoteException {
        return this.id;
    }
}
