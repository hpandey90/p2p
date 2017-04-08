import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;
//testing
public class peerProcess {
    
	private static final Logger LOGGER = MyLogger.getMyLogger();

    // Synchronized list to maintain all the connected client peers of owner peer
	public static List<PeerThread> peersList = Collections.synchronizedList(new ArrayList<PeerThread>());

    // List to maintain interested and unchoked peers
    List<PeerManager> unchokeList = null; 
    
    // List to maintain interested and choked peers
    List<PeerManager> chokeList = null; 
    
    // Obtain a ScheduledExecutorSevice object from a scheduledThreadPool for determining preferredNeighbours & optimisticallyUnchokedNeighbour
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    
    public static void main(String[] args) {
       
    	// Obtain peerId from command line and set it as owner and put it in the commonConfig hashmap 
    	// consisting of property, value pairs used later in the peerManager
    	Scanner scan = new Scanner(System.in);
        int peerId = Integer.valueOf(args[0]);
        
        // set the ownerId of PeerManager
        PeerManager.ownerId = peerId;
        
        CommonPeerConfig.retrieveCommonConfig().put("peerId", String.valueOf(peerId));
        scan.close();
        
        // Obtain the hostname, listening port and has file or not values for the 
        // owner peerId(acting as server) from the peerInfo hashmap
        String string = CommonPeerConfig.retrievePeerInfo().get(peerId);

        // Initialize custom logger
        try {
            MyLogger.setup();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Problems with creating the log files");
        }
        
        // Obtain the listening port for the owner peerId
        String portNo = string.split(" ")[2];
        
        // Create a peerProcess object to start client peers and owner server peer connection communication processes 
        peerProcess peerProcessOb = new peerProcess();
        
        // Connect the owner peer to all the available client peers in the peerInfo file with id < owner peerId
        peerProcessOb.clientConnect(peerId);
        
        // Create a serverSocket for owner peer and start accepting connection requests from client peers 
        // in a seperate thread each on its port number. 
        peerProcessOb.acceptConnection(peerId, Integer.valueOf(portNo));
        
        // Create another peerProcess object to determinePreferredNeighbours,determineOptimisticallyUnchokedNeighbour & determineShutdownScheduler   
        peerProcess peerProcessObj = new peerProcess();
        Map<String, String> comProp = CommonPeerConfig.retrieveCommonConfig();
        
        // Retrieve the property values from the common config file
        int m = Integer.parseInt(comProp.get("OptimisticUnchokingInterval"));
        int k = Integer.parseInt(comProp.get("NumberOfPreferredNeighbors"));
        int p = Integer.parseInt(comProp.get("UnchokingInterval"));
        
        // Determine preferred neighbour peers
        peerProcessObj.determinePreferredNeighbours(k, p);
        
        // Determine optimistically unchoked neighbour peers
        peerProcessObj.determineOptimisticallyUnchokedNeighbour(m);
        
        // Determine the shut down process
        peerProcessObj.determineShutdownScheduler();
    }

    
    /**
     * Connects to all available clients. PeerId is self myPeerId as to not to
     * connect to self or anyone with greater peer id.
     */
    public void clientConnect(int myPeerId) {
        
    	// Obtain the peerInfo hashMap
    	Map<Integer, String> peerProp = CommonPeerConfig.retrievePeerInfo();
    	
    	// Iterate by selecting all the peers with peerId < owner peerId
        for (Integer s : peerProp.keySet()) {
            
        	if (s < myPeerId) {
                
        		// for every selected peer, obtain the client peerId, host and listening port from the peerProp map
        		String line = peerProp.get(s);
                String[] split = line.split(" ");
                String peerId = split[0];
                String host = split[1];
                String port = split[2];
                
                try {
                    
                	// Create a socket for every selected client peer using its host name and the listening port
                	Socket socket = new Socket(host, Integer.parseInt(port));
                	
                	// Create and start new peerThread process for each client peer by passing its socket, peerId
                	// client peer thread initialization which has bitfield, interested/notinterested message exchange happens in the constructor
                    PeerThread peerThread = new PeerThread(socket, true, Integer.parseInt(peerId));
                    
                    // Start each selected client peer thread after it has been initialized above
                    peerThread.start();
                    
                    // Add the peerThread to the synchronized peersList of the owner peer 
                    peersList.add(peerThread);
                
                } catch (NumberFormatException | IOException e) {
                    
                	// TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    }
    
    
    /**
     * Accepts connection for every peer in a separate thread..
     *
     * @param portNumber
     */
    int greaterPeerCount = 0;

    public void acceptConnection(int myPeerId, final int portNumber) {
        
    	// TODO : Determine to shut down this thread.
    	// Retreive the peerInfo hashmap into peerProp map
    	Map<Integer, String> peerProp = CommonPeerConfig.retrievePeerInfo();
        
    	// Count peers having id > owner peer id
    	for (Integer s : peerProp.keySet()) {
            
    		if (s > myPeerId) {
                greaterPeerCount++;
            }
        }
    	
    	// Create a thread for accepting client peer connections
        Thread connectionAcceptThread = new Thread() {
            
        	public void run() {
                
        		// Obtain a serverSocket for the owner peer using its portNumber
        		try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
                    
        			while (greaterPeerCount > 0) {
                        
        				// Start accepting the connection requests from the client peers using the serverSocket created 
        				Socket acceptedSocket = serverSocket.accept();
                        
        				// if owner's serverSocket accepts a client connection
                        if (acceptedSocket != null) {
                            
                        	// create a peerThread each for handling each clien peer request
                        	PeerThread peerThread = new PeerThread(acceptedSocket, false, -1);
                            
                        	// Start each peerThread created to accept each client peer connection request
                        	peerThread.start();
                        	
                        	// Add the peerThread created to the peersList
                            peersList.add(peerThread);
                            
                            // Decrement the greaterPeerCount
                            greaterPeerCount--;
                        }
                    }
                } catch (Exception e) {
                  
                	e.printStackTrace();
                    System.out.println("Error in creating serverSocket: " + e.getMessage());
                }
            }
        };
        
        // Change the name of the thread for accepting client peer connections
        connectionAcceptThread.setName("Connection Accepting Thread ");
        
        // Start the thread for accepting client peer connections
        connectionAcceptThread.start();
    }

    
    /**
     * Determines k preferred neighbors every p seconds
     */
    public void determinePreferredNeighbours(final int k, final int p) {
        
    	try {
            
    		// Declare a runnable to determine k preferred neighbours every p seconds 
    		final Runnable kNeighborDeterminer = new Runnable() {
                
    			public void run() {
                    
    				System.out.println("K preferred neighbours called");
                    // calculate the downloading rate from each peer. set it initially to 0.
                    
    				// Obtain the list of interested peers for the owner peer and select the k preferred neighbours from them
    				List<PeerManager> interestedList = PeerManager.interestedPeers;
                    
    				// Sort the list of peers in the interestedList using the peer comparator class based on download rates
    				Collections.sort(interestedList, new PeerComparator<PeerManager>());
                    
    				// if interested list is non empty, select k peers which have the highest download rate
                    if (interestedList != null) {
                        
                    	System.out.println("Interested list size is " + interestedList.size());
                        
                    	// Declare an iterator for the interestedList of peers
                    	Iterator<PeerManager> iterator = interestedList.iterator();
                        
                    	// Instantiate unchoke and choke peers synchronized lists 
                    	unchokeList = Collections.synchronizedList(new ArrayList<PeerManager>());
                        chokeList = Collections.synchronizedList(new ArrayList<PeerManager>());
                        
                    	int count = k;

                        StringBuffer listOfUnchokedNeighbours = new StringBuffer(" ");
                        
                        // Iterator through the interestedList of peers for owner peer
                        while (iterator.hasNext()) {
                        
                        	PeerManager next = iterator.next();
                            
                        	// If the interested peer has been initialized
                        	if (next.getIsPeerInitialized()) {
                            
                        		// if <k preferred neighbbours have been determined
                        		if (count > 0) {
                                
                        			System.out.println("peerProcess.run unchoked " + next.getPeerId());
                                    
                        			// Add the peer to the unchoke list of the owner peer
                        			unchokeList.add(next);
                                    
                        			// if the selected interested peer is choked previously
                                    if (next.isChoked()) {
                                    
                                    	// unchoke it
                                    	next.setChoked(false);
                                        
                                    	// if the selected interested peer is not optimisticallyUnchokedPeer
                                    	if (!next.isOptimisticallyUnchokedPeer()) {
                                        
                                    		System.out.println("Sending  unchoking msg " + next.getPeerId());
                                            
                                    		try {
                                    			// send unchoke message to it
                                    			next.sendUnchokeMessage();
											
                                    		} catch (IOException e) {
												
                                    			// TODO Auto-generated catch block
												e.printStackTrace();
											} // now expect recieve message
                                        }
                                    }
                                    
                                    // Add the selected interested peerId to the listOfUnchokedNeighbours 
                                    listOfUnchokedNeighbours.append(next.getPeerId() + ",");
                                
                        		} 
                        		
                        		// if k preferred neighbours have already been selected
                        		else {
                                    
                                	System.out.println("peerProcess.run choked " + next.getPeerId());
                                    // add the selected interested peer to the chokeList of owner peer
                                	chokeList.add(next);
                                    
                                	// if the selected interested peer is not previously choked
                                    if (!next.isChoked()) {
                                    
                                    	// set the choked value for it to be true
                                    	next.setChoked(true);
                                        
                                    	// if the selected interested peer is not optimisticallyUnchokedPeer
                                    	if (!next.isOptimisticallyUnchokedPeer()) {
                                        
                                    		System.out.println("Sending  choke msg " + next.getPeerId());
                                            
                                    		try {
											
                                    			// send choke message to it
                                    			next.sendChokeMessage();
											} catch (IOException e) {
												
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
                                        }
                                    }
                                }
                            }
                            
                        	// decrement the count for selected preferred neighbours
                        	count--;
                        }

                        String neigh = listOfUnchokedNeighbours.toString();
                        
                        if (!neigh.trim().isEmpty()) {
                        
                        	log("Peer " + PeerManager.ownerId + " has the preferred neighbors " + neigh);
                        }

                    }
                }
            };
            
            
            // schedule the kNeighborDeterminer runnable to run after every p seconds using a ScheduledExectorService object
            final ScheduledFuture<?> kNeighborDeterminerHandle = scheduler.scheduleAtFixedRate(kNeighborDeterminer, p, p, SECONDS);
        
    	} catch (Exception e) {
        
            System.out.println(e.getMessage());
        }


    }


    /**
     * Determine optimistically unchocked neighbour every m seconds.
     */
    PeerManager previousOptimisticallyUnchokedPeer;

    public void determineOptimisticallyUnchokedNeighbour(final int m) {
        
    	// Declare a runnable for determining optimisticallyUnchokedNeighbour
    	final Runnable optimisticallyUnchockedNeighbourDeterminer = new Runnable() {

            @Override
            public void run() {
                
            	System.out.println("inside optimistically unchoked neighbour!!");
            	
            	// Obtain the size of chokeList of owner peer
                int size = chokeList.size();
                System.out.println("size = " + size);
                
                if (size != 0) {
                    
                	// Obtain a random index isolated to the current thread
                    int randIndex = ThreadLocalRandom.current().nextInt(0, size);
                    
                    // Randomly select a peer from the choked peers list of owner peer optimistically
                    PeerManager PeerManager = chokeList.remove(randIndex);
                    
                    System.out.println("selecting an optimistcally neighbor");
                    System.out.println("randIndex = " + randIndex);
                    System.out.println("Peer selected is " + PeerManager.getPeerId());
                    
                    // if a peer is obtained by random selection and if it has not been selected previously
                    if (PeerManager != null && PeerManager != previousOptimisticallyUnchokedPeer) {
                    
                    	System.out.println("selecting a new  optimistcally neighbor");
                        // Set the value of optimistically unchoked peer as true for the peer
                    	PeerManager.setOptimisticallyUnchokedPeer(true);
                        
                        try {
							
                        	// send an unchoke message to the optimistically selected peer
                        	PeerManager.sendUnchokeMessage();
						} catch (IOException e1) {
							
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
                        
                        // if previousOptimisticallyUnchokedPeer 
                        if (previousOptimisticallyUnchokedPeer != null) {
                            
                        	// set the value of the previous optimistically unchoked peer to false
                        	previousOptimisticallyUnchokedPeer.setOptimisticallyUnchokedPeer(false);
                            
                        	// if previous optimisticallyUnchokedPeer has been choked already 
                        	if (previousOptimisticallyUnchokedPeer.isChoked()) {
                            
                        		System.out.println("Sending Choke msg from Optimistcally");
                                
                        		try {
                        			
                        			// send choke message from it to the owner peer
                        			previousOptimisticallyUnchokedPeer.sendChokeMessage();
								
                        		} catch (IOException e) {
									
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
                            }
                        }
                        
                        // reassign the previousOptimisticallyUnchokedPeer value to the current selected peer
                        previousOptimisticallyUnchokedPeer = PeerManager;
                        
                        log("PeerManager " + PeerManager.ownerId + " has the optimistically unchoked neighbor " + "PeerManager " + PeerManager.ownerId);
                        System.out.println("Peer " + PeerManager.ownerId + " has the optimistically unchoked neighbor " + "Peer " + PeerManager.ownerId);
                    }
                
                } 
                
                // if owner peer chokeList is null
                else {
                    
                	// if previousOptimisticallyUnchokedPeer is not null
                	if (previousOptimisticallyUnchokedPeer != null) {
                      
                		// set the value of the previous optimistically Unchoked peer to false
                		previousOptimisticallyUnchokedPeer.setOptimisticallyUnchokedPeer(false);
                        
                		// if previous optimistically Unchoked peer is already choked 
                		if (previousOptimisticallyUnchokedPeer.isChoked()) {
                        
                			System.out.println("Sending Choke msg from Optimistcally");
                            
                			try {
							
                				// send choked message from it to the owner peer
                				previousOptimisticallyUnchokedPeer.sendChokeMessage();
							} catch (IOException e) {
								
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
                        }
                    }
                    
                	// set the previous Optimistically unchoked peer to null
                	previousOptimisticallyUnchokedPeer = null;
                }

            }

        };
        
        // schedule the optimisticallyUnchockedNeighbourDeterminer to run after every m seconds 
        // using a ScheduledExecutorService object
        scheduler.scheduleAtFixedRate(optimisticallyUnchockedNeighbourDeterminer, m, m, SECONDS);
    }
    
    /**
     * Determine when to shutdown...
     */
    public void determineShutdownScheduler() {
        
    	// Declare a runnable for determining the shut down process
    	final Runnable shutDownDeterminer = new Runnable() {
            @Override
            public void run() {
                
            	// Obtain the bitField of owner peer
            	byte[] myBitField = PeerManager.getOwnerBitField();
                System.out.println("Arrays.toString(myBitField) = " + Arrays.toString(myBitField));
                
                // if peersList size of owner peer equals the total number of peers in peerInfo config file
                if (peersList.size() == CommonPeerConfig.retrievePeerInfo().size()) {
                
                	// set the shut down flag to true
                	boolean shutDown = true;
                	
                    // for every peerThread in the peersList
                	for (PeerThread p : peersList) {
                    
                		// Obtain the bitField message of each peer
                		byte[] pBitFieldMsg = p.getPeer().getbitFieldMessageOfPeer();
                        
                		// if the owner and client peer do not have the same bitField messages
                		if (Arrays.equals(pBitFieldMsg, myBitField) == false) {
                        
                			// dont shut down yet
                            shutDown = false;
                            break;
                        }
                    }
                	
                	// if all the peers in the owner peersList have same bitField message as the owner
                    if (shutDown) {
                        
                    	// for every peerThread in the peersList
                    	for (PeerThread p : peersList) {
                        
                    		// set the stop flag for peer to be true
                    		p.setStop(true);
                    		
                    		// interrupt the peerThread to stop running 
                            p.interrupt();
                        }

                        //lets write it to a file
                    	System.out.println("Scheduler shutdown called !!");
                        
                    	// shut down the executor service to prevent it from accepting new tasks
                    	// allow previously submitted tasks to execute before terminating
                    	scheduler.shutdown();
                        
                    	// if the scheduler has not been successfully shut down 
                        if (!scheduler.isShutdown()) {
                        
                        	System.out.println("Unsuccessful shut down.");
                        }
                        try {
                            
                        	// Blocks until all tasks have completed execution after a shutdown request, 
                        	// or the timeout occurs, or the current thread is interrupted, whichever happens first.
                        	scheduler.awaitTermination(5, SECONDS);
                        } catch (InterruptedException e) {
                            
                        	e.printStackTrace();
                        }
                    }
                }

            }
        };
        
        // schedule the shutDownDeterminer to run every 4 seconds
        scheduler.scheduleAtFixedRate(shutDownDeterminer, 4, 4, SECONDS);

    }

    
    public void log(String msg) {
        Logger logger = LOGGER;
        if (logger == null) {
            logger = MyLogger.getMyLogger();
        }
        logger.info(msg);
    }

}