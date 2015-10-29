import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Auction extends UnicastRemoteObject implements IAuctionRemote {

    List<IAuctionItem> auctionItems;

    public Auction() throws RemoteException {
        super();
        auctionItems = new ArrayList<IAuctionItem>();
        System.out.format("Creating server object\n");
    }

    @Override
    public String printTESTTEST(String str) {
        System.out.format("Caller send %s. Sending back '%s'.", str,str);
        return str;
    }

    @Override
    public List<IAuctionItem> getAuctionItems() throws RemoteException {
        return auctionItems;
    }

    @Override
    public IAuctionItem createAuctionItem(String itemName, double value, Date closingDate) throws RemoteException {
        return new ActionItem(itemName,value, closingDate);
    }

    @Override
    public void createAndRegisterAuctionItem(String itemName, double value, Date closingDate) throws RemoteException {
        ActionItem i = new ActionItem(itemName,value,closingDate);
        registerAuctionItem(i);
    }

    @Override
    public void registerAuctionItem(IAuctionItem item) {
        auctionItems.add(item);
    }


}
