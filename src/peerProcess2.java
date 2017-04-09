import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;
//testing
public class peerProcess2 {

	private static final Logger LOGGER = MyLogger.getMyLogger();

	// Synchronized list to maintain all the connected client peers of owner peer
	public static List<PeerThread2> listOfPeers = Collections.synchronizedList(new ArrayList<PeerThread2>());

	// List to maintain interested and unchoked peers
	List<PeerManager> listOfUnchokedPeers = null;

	// List to maintain interested and choked peers
	List<PeerManager> listOfchokedPeers = null;

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
		String portNum = string.split(" ")[2];

		// Create a peerProcess object to start client peers and owner server peer connection communication processes
		peerProcess2 peerProcessOb = new peerProcess2();

		// setup and start the client and server peer connections
		peerProcessOb.connectionSetup(peerId);
		
		// Create another peerProcess object to determinePreferredNeighbours,determineOptimisticallyUnchokedNeighbour & determineShutdownScheduler
		peerProcess peerProcessObj = new peerProcess();
		Map<String, String> comProp = CommonPeerConfig.retrieveCommonConfig();

		// Retrieve the property values from the common config file
		int m = Integer.parseInt(comProp.get("OptimisticUnchokingInterval"));
		int k = Integer.parseInt(comProp.get("NumberOfPreferredNeighbors"));
		int p = Integer.parseInt(comProp.get("UnchokingInterval"));

		// Determine preferred neighbour peers
		peerProcessObj.determineKPreferredNeighbours(k, p);

		// Determine optimistically unchoked neighbour peers
		peerProcessObj.findOptUnchokedNeighbour(m);

		// Determine the shut down process
		peerProcessObj.determineShutdownScheduler();
	}

	
	public void connectionSetup(int ownerPeerId) {

		// Obtain the peerInfo hashMap
		Map<Integer, String> peerInfo = CommonPeerConfig.retrievePeerInfo();

		for (Map.Entry<Integer, String> s : peerInfo.entrySet()) {

			// for every peer, obtain the peerId, host and listening port from the peerInfo map
			String line = peerInfo.get(s);
			String[] arr = line.split(" ");
			int peerId = Integer.parseInt(arr[0]);
			String host = arr[1];
			int portN = Integer.parseInt(arr[2]);

			// when the server owner peer found
			if ( peerId == ownerPeerId) {

				int peerIdGreaterCount = 0;

				// Count peers having id > owner peer id
				for (Map.Entry<Integer, String> e : peerInfo.entrySet()) {
					if (e.getKey() > ownerPeerId) {
						peerIdGreaterCount++;
					}
				}

				while (peerIdGreaterCount > 0) {

					Thread serverThread = new Thread(new Runnable() {
						public void run() {

							try {

								// Creating a server socket for the peer in which this program is running.
								ServerSocket serverSocket = new ServerSocket(portN);
								Socket acceptedSocket = serverSocket.accept();
								PeerThread2 r = new PeerThread2(acceptedSocket, false, -1);
								Thread listenThread = new Thread(r);						
								listenThread.start();
								listOfPeers.add(r);

							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});

					// Change the name of the serverThread for accepting client peer connections
					serverThread.setName("Connection Accepting Thread ");

					// start the serverThread
					serverThread.start();

					// Decrement the greaterPeerCount
					peerIdGreaterCount--;
				} 
				break;
			}

			else {
				if (peerId < ownerPeerId) { 

					Socket clientSocket;
					try {

						clientSocket = new Socket( host, portN);
						PeerThread2 r = new PeerThread2(clientSocket, true, peerId);
						Thread clientThread = new Thread(r);
						// Change the name of the client peer thread 
						clientThread.setName("Client thread for peer: "+ peerId);
						clientThread.start();
						listOfPeers.add(r);

					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}		
	}


	/**
	 * Determines k preferred neighbors every p seconds
	 */
	public void determineKPreferredNeighbours(final int k, final int p) {

		try {

			// Declare a runnable to determine k preferred neighbours every p seconds
			final Runnable kNeighborFinder = new Runnable() {

				public void run() {

					System.out.println("K preferred neighbours called");
					// calculate the downloading rate from each peer. set it initially to 0.

					// Obtain the list of interested peers for the owner peer and select the k preferred neighbours from them
					List<PeerManager> listOfInterestedPeers = PeerManager.interestedPeers;

					// Sort the list of peers in the interestedList using the peer comparator class based on download rates
					Collections.sort(listOfInterestedPeers, new PeerComparator<PeerManager>());

					// if interested list is non empty, select k peers which have the highest download rate
					if (listOfInterestedPeers != null) {

						System.out.println("Interested list size is " + listOfInterestedPeers.size());

						// Declare an iterator for the interestedList of peers
						Iterator<PeerManager> it = listOfInterestedPeers.iterator();

						// Instantiate unchoke and choke peers synchronized lists
						listOfUnchokedPeers = Collections.synchronizedList(new ArrayList<PeerManager>());
						listOfchokedPeers = Collections.synchronizedList(new ArrayList<PeerManager>());

						int count = k;

						StringBuffer unchokedNeighboursList = new StringBuffer(" ");

						// Iterator through the interestedList of peers for owner peer
						while (it.hasNext()) {

							PeerManager next = it.next();

							// If the interested peer has been initialized
							if (next.getIsPeerInitialized()) {

								// if <k preferred neighbbours have been determined
								if (count > 0) {

									System.out.println("peerProcess.run unchoked " + next.getPeerId());

									// Add the peer to the unchoke list of the owner peer
									listOfUnchokedPeers.add(next);

									// if the selected interested peer is choked previously
									if (next.isChoked()) {

										// unchoke it
										next.setChoked(false);

										// if the selected interested peer is not optimisticallyUnchokedPeer
										if (!next.optimisticallyUnchokedPeer) {

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
									unchokedNeighboursList.append(next.getPeerId() + ",");

								}

								// if k preferred neighbours have already been selected
								else {

									System.out.println("peerProcess.run choked " + next.getPeerId());
									// add the selected interested peer to the chokeList of owner peer
									listOfchokedPeers.add(next);

									// if the selected interested peer is not previously choked
									if (!next.isChoked()) {

										// set the choked value for it to be true
										next.setChoked(true);

										// if the selected interested peer is not optimisticallyUnchokedPeer
										if (!next.optimisticallyUnchokedPeer) {

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

						String neigh = unchokedNeighboursList.toString();

						if (!neigh.trim().isEmpty()) {

							log("Peer " + PeerManager.ownerId + " has the preferred neighbors " + neigh);
						}

					}
				}
			};


			// schedule the kNeighborDeterminer runnable to run after every p seconds using a ScheduledExectorService object
			final ScheduledFuture<?> kNeighborDeterminerHandle = scheduler.scheduleAtFixedRate(kNeighborFinder, p, p, SECONDS);

		} catch (Exception e) {

			System.out.println(e.getMessage());
		}

	}


	/**
	 * Determine optimistically unchocked neighbour every m seconds.
	 */
	PeerManager previousOptUnchokedPeer;

	public void findOptUnchokedNeighbour(final int m) {

		// Declare a runnable for determining optimisticallyUnchokedNeighbour
		final Runnable optUnchockedNeighbourFinder = new Runnable() {

			@Override
			public void run() {

				System.out.println("inside optimistically unchoked neighbour!!");

				// Obtain the size of chokeList of owner peer
				int chokeListSize = listOfchokedPeers.size();
				System.out.println("size = " + chokeListSize);

				if (chokeListSize != 0) {

					// Obtain a random index isolated to the current thread
					int randomIndex = ThreadLocalRandom.current().nextInt(0, chokeListSize);

					// Randomly select a peer from the choked peers list of owner peer optimistically
					PeerManager PeerManager = listOfchokedPeers.remove(randomIndex);

					System.out.println("selecting an optimistcally neighbor");
					System.out.println("randIndex = " + randomIndex);
					System.out.println("Peer selected is " + PeerManager.getPeerId());

					// if a peer is obtained by random selection and if it has not been selected previously
					if (PeerManager != null && PeerManager != previousOptUnchokedPeer) {

						System.out.println("selecting a new  optimistcally neighbor");
						// Set the value of optimistically unchoked peer as true for the peer
						PeerManager.optimisticallyUnchokedPeer = true;

						try {

							// send an unchoke message to the optimistically selected peer
							PeerManager.sendUnchokeMessage();
						} catch (IOException e1) {

							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						// if previousOptimisticallyUnchokedPeer
						if (previousOptUnchokedPeer != null) {

							// set the value of the previous optimistically unchoked peer to false
							previousOptUnchokedPeer.setOptUnchokedPeer(false);

							// if previous optimisticallyUnchokedPeer has been choked already
							if (previousOptUnchokedPeer.isChoked()) {

								System.out.println("Sending Choke msg from Optimistcally");

								try {

									// send choke message from it to the owner peer
									previousOptUnchokedPeer.sendChokeMessage();

								} catch (IOException e) {

									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
						}

						// reassign the previousOptimisticallyUnchokedPeer value to the current selected peer
						previousOptUnchokedPeer = PeerManager;

						log("PeerManager " + PeerManager.ownerId + " has the optimistically unchoked neighbor " + "PeerManager " + PeerManager.ownerId);
						System.out.println("Peer " + PeerManager.ownerId + " has the optimistically unchoked neighbor " + "Peer " + PeerManager.ownerId);
					}

				}

				// if owner peer chokeList is null
				else {

					// if previousOptimisticallyUnchokedPeer is not null
					if (previousOptUnchokedPeer != null) {

						// set the value of the previous optimistically Unchoked peer to false
						previousOptUnchokedPeer.setOptUnchokedPeer(false);

						// if previous optimistically Unchoked peer is already choked
						if (previousOptUnchokedPeer.isChoked()) {

							System.out.println("Sending Choke msg from Optimistcally");

							try {

								// send choked message from it to the owner peer
								previousOptUnchokedPeer.sendChokeMessage();
							} catch (IOException e) {

								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					// set the previous Optimistically unchoked peer to null
					previousOptUnchokedPeer = null;
				}

			}

		};

		// schedule the optimisticallyUnchockedNeighbourDeterminer to run after every m seconds
		// using a ScheduledExecutorService object
		scheduler.scheduleAtFixedRate(optUnchockedNeighbourFinder, m, m, SECONDS);
	}

	/**
	 * Determine when to shutdown...
	 */
	public void determineShutdownScheduler() {

		// Declare a runnable for determining the shut down process
		final Runnable determineShutDown = new Runnable() {

			@Override
			public void run() {

				// Obtain the bitField of owner peer
				byte[] ownerBitField = PeerManager.getOwnerBitField();
				System.out.println("Arrays.toString(myBitField) = " + Arrays.toString(ownerBitField));

				// if peersList size of owner peer equals the total number of peers in peerInfo config file
				if (listOfPeers.size() == CommonPeerConfig.retrievePeerInfo().size()) {

					// set the shut down flag to true
					boolean shutDownFlag = true;

					// for every peerThread in the peersList
					for (PeerThread2 p : listOfPeers) {

						// Obtain the bitField message of each peer
						byte[] peerBitFieldMsg = p.retrievePeerConnected().getbitFieldMessageOfPeer();

						// if the owner and client peer do not have the same bitField messages
						if (Arrays.equals(peerBitFieldMsg, ownerBitField) == false) {

							// dont shut down yet
							shutDownFlag = false;
							break;
						}
					}

					// if all the peers in the owner peersList have same bitField message as the owner
					if (shutDownFlag) {

						// for every peerThread in the peersList
						for (PeerThread2 p : listOfPeers) {

							// set the Peerthread toStop to true
							p.toStop = true;
							try {
								// before exiting check if peerSocket is already closed, if no close it
								if(p.peerSocket.isClosed());
								else
									p.peerSocket.close();
							} catch (IOException e) {
								System.out.println("Could not close socket:");
								e.printStackTrace();
							}

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
		scheduler.scheduleAtFixedRate(determineShutDown, 4, 4, SECONDS);

	}


	public void log(String msg) {
		Logger logger = LOGGER;
		if (logger == null) {
			logger = MyLogger.getMyLogger();
		}
		logger.info(msg);
	}

}
