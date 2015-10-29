import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Created by vbk20 on 29/10/2015.
 */
public class Auction extends UnicastRemoteObject implements IAuctionRemote {

    List<IAuctionItem> auctionItems;
    Map<Long,IAuctionItem> auctionItemsMap;

    public Auction() throws RemoteException {
        super();
        auctionItems = new ArrayList<IAuctionItem>();
        auctionItemsMap = new HashMap<Long,IAuctionItem>();
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
    public long createAndRegisterAuctionItem(long creatorId,String itemName, double value, Date closingDate) throws RemoteException {
        ActionItem i = new ActionItem(creatorId,itemName,value,closingDate);
        registerAuctionItem(i);
        return i.getId();
    }

    @Override
    public void registerAuctionItem(IAuctionItem item) {
        auctionItemsMap.put(item.getId(),item);
        auctionItems.add(item);
    }

    @Override
    public void bidForItem(long itemId, double bidValue) throws RemoteException {

    }


}
