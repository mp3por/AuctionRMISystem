package velin.client;

import velin.common.IAuctionClientRemote;
import velin.common.Utils;
import velin.server.AuctionException;
import velin.common.IAuctionRemote;

import java.rmi.Naming;
import java.rmi.RemoteException;
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

    IAuctionRemote auction;
    DateFormat formatter = new SimpleDateFormat("dd/MM/yy-HH:mm:ss");
    Map<Long, List<Long>> itemsBid;

    public AuctionClient(String host) throws RemoteException {
        try {
            System.out.format("Client starting.\n");
//            Registry reg = LocateRegistry.getRegistry("localhost", velin.common.Utils.AUCTION_SERVER_RMI_PORT);
            Object o = Naming.lookup("rmi://" + host + "/" + Utils.AUCTION_SERVER_NAME);
            auction = (IAuctionRemote) o;

            itemsBid = new HashMap<Long, List<Long>>();

            this.id = auction.registerClient(this);


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
                        case "--l":
                            ;
                        case "--list-auction-items":
                            switch (input.length) {
                                case 1:
                                    System.out.printf("Available Auctions in the version1.velin.server.Auction House:\n");
                                    String result = auction.getAuctionLiveItems(this.id);
                                    System.out.println(result.length() == 0 ? "\t NO ACTIVE AUCTIONS" : result);
                                    break;
                                default:
                                    System.out.printf("Too many or too few arguments. Must be exactly 0 arguments.\n");
                            }
                            break;
                        case "--b":
                            ;
                        case "--bid":
                            switch (input.length) {
                                case 3:
                                    try {
                                        long itemId = Long.valueOf(input[1]);
                                        try {
                                            double bidValue = Double.parseDouble(input[2]);
                                            boolean result = auction.bidForItem(this.id, itemId, bidValue);
                                            System.out.println(result ? "You are the highest bidder!" : "Your bid was less than the current value of the item!");
                                        } catch (NumberFormatException e) {
                                            System.out.printf("The bidValue must be of type Double (E.x: 10.25, 25).\n");
                                        } catch (AuctionException e) {
                                            System.out.println(e.getMessage());
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.printf("The itemID must consist of only NUMBERS.");
                                    }
                                    break;
                                default:
                                    System.out.printf("Too many or too few arguments. Must be exactly 2 arguments\n");
                            }
                            break;
                        case "--c":
                            ;
                        case "--create":
                            switch (input.length) {
                                case 1:
                                    System.out.println("--c <itemName> <startValue> <dd/MM/yyyy-hh:mm:ss endDate>");
                                    break;
                                case 4:
                                    try {
                                        String itemName = input[1];
                                        double value = Double.valueOf(input[2]);
                                        try {
                                            Date endDate = formatter.parse(input[3]);
                                            long result = auction.createAndRegisterAuctionItem(this.id, itemName, value, endDate);
                                            System.out.println("Create item with ID: " + result);
                                        } catch (ParseException e) {
                                            System.out.println("Date must be in the format dd/MM/yyyy-hh:mm:ss.");
                                        } catch (AuctionException e) {
                                            System.out.println(e.getMessage());
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.printf("The bidValue must be of type Double (E.x: 10.25).\n");
                                    }
                                    break;
                                default:
                                    System.out.printf("Too many or too few arguments.\n");
                            }
                            break;
                        default:
                            System.out.format("Unrecognizable command :%s\n", input[0]);
                    }
                    Thread.sleep(1000); // sleep for one second
                }

            } catch (InterruptedException e) {
                ; // ignored
            } catch (RemoteException e) {
                System.out.println("version1.velin.server.Auction House is unavailable.");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                eof = true;
            }
        }
        try {
            auction.unregisterClient(this.id);
            auction = null;
        } catch (RemoteException e) {
            auction = null;
        }
    }

    private void printHelp() {
        System.out.printf("Hello,\nAvailable commands:\n" +
                "\t* --l: Prints all available Auctions with their name and Ids\n" +
                "\t* --c 'itemName' 'startValue' 'endDate': Creates a new version1.velin.server.Auction Item with the specified details\n" +
                "\t* --b 'itemId' 'bidValue':  Bids for that particular version1.velin.server.Auction Item in that particular version1.velin.server.Auction\n");
    }

    public static void main(String[] args) {
        try {
            new AuctionClient(args[0]);
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
}
