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
    long auctionId = 0;
    DateFormat formatter = new SimpleDateFormat("dd/MM/yy-HH:mm:ss");

    public AuctionClient() throws RemoteException {
        try {
            System.out.format("Client starting.\n");
            Registry reg = LocateRegistry.getRegistry("localhost", Utils.AUCTION_HOUSE_SERVER_RMI_PORT);
            Object o = reg.lookup(Utils.AUCTION_HOUSE_REGISTRY_NAME);
            auctionHouseRemote = (IAuctionHouseRemote) o;

            this.id = auctionHouseRemote.registerClient(this);
            String result = auctionHouseRemote.registerClientForAuction(auctionId, this);

            System.out.format("Client with ID (-- %d --) registered.\n", this.id);
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

                if (buf.length() != 0) {
                    switch (input[0]) {
                        case "--l":
                            ;
                        case "--list-auction-items":
                            switch (input.length) {
                                case 2:
                                    System.out.println("Items currently in auction: ");
                                    try {
                                        long auctionId = Long.valueOf(input[1]);
                                        String auctionItems = auctionHouseRemote.getAuctionLiveItems(auctionId, this.id);
                                        System.out.println(auctionItems);
                                    } catch (NumberFormatException e) {
                                        System.out.printf("The ID must consist of only NUMBERS.\n");
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
                                case 3:
                                    try {
                                        Long itemId = Long.valueOf(input[1]);
                                        try {
                                            double bidValue = Double.parseDouble(input[2]);
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
                                case 4:
                                    try {
                                        String itemName = input[1];
                                        double value = Double.valueOf(input[2]);
                                        try {
                                            Date endDate = formatter.parse(input[3]);
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