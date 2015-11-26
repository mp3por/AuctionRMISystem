Hello,


In order to run the program you need to do the following steps:

1. bash-4.1$ make

2. Start the AuctionServer:
   bash-4.1$ java -cp out velin.server.AuctionServer MyAuctionserver

3. Start the AuctionClient with IP of host:
   bash-4.1$ java -cp out velin.client.AuctionClient 130.209.245.225

4. And there you go they are connected.

Commands available to the Client:
--c 'itemNAme' 'startValue' 'endDate'
--l
--b 'itemId' 'bidValue'

Commands available to the Server:
--slaf 'fileName'
--llaf 'fileName'