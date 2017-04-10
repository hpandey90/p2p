import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

public class PeerSchedules {

	// Declare variable for previousOptUnchokedPeer
	PeerManager previousOptUnchokedPeer;

	// Declare the timers for the three scheduled tasks
	Timer timer1;
	Timer timer2;
	Timer timer3;

	// Declare class variables for peerProcess object 
	// and one each for the time intervals and number of preferred neighbours
	peerProcess pp;
	int p;
	int m;
	int k;

	//  class constructor
	public PeerSchedules (peerProcess pp) {

		this.pp = pp;
		
		Map<String, String> comProp = CommonPeerConfig.retrieveCommonConfig();
		
		// Retrieve the common peer configuration values for scheduling tasks from common config file
		int m = Integer.parseInt(comProp.get("OptimisticUnchokingInterval"));
		int k = Integer.parseInt(comProp.get("NumberOfPreferredNeighbors"));
		int p = Integer.parseInt(comProp.get("UnchokingInterval"));

		// schedule the determining k preferred neighbours task every p seconds
		timer1 = new Timer();
		timer1.scheduleAtFixedRate(new SelectKPreferredNeighbours(this.pp, k), 0, p*1000);

		// schedule the determining optimistically unchoked neighbour task every m seconds
		timer2 = new Timer();
		timer2.scheduleAtFixedRate(new SelectOptUnchokedNeighbour(this.pp), 0, m*1000);

		// schedule the determining shut down process of schedulers task every 4 seconds
		timer3 = new Timer();
		timer3.scheduleAtFixedRate(new DetermineShutDownProcess(this.pp), 0, 4*1000);
	}

	/**
	 * Determines k preferred neighbors every p seconds
	 */
	class SelectKPreferredNeighbours extends TimerTask {

		peerProcess pp;
		int k;
		public SelectKPreferredNeighbours (peerProcess pp, int k) {
			this.pp = pp;
			this.k=k;
		}    	

		@Override
		public void run() {

			try {

				System.out.println("K preferred neighbours called");
				// calculate the downloading rate from each peer. set it initially to 0.

				// Obtain the list of interested peers for the owner peer and select the k preferred neighbours from them
				List<PeerManager> listOfInterestedPeers = PeerManager.interestedPeers;

				// Sort the list of peers in the interestedList using the peer comparator class based on download rates
				
				Collections.sort(listOfInterestedPeers, new Comparator<PeerManager>(){
					public int compare(PeerManager pm1, PeerManager pm2) {
						return (new Long(pm1.getPeerDownloadRate())).compareTo(new Long(pm2.getPeerDownloadRate()));
					}
				});

				// if interested list is non empty, select k peers which have the highest download rate
				if (listOfInterestedPeers != null) {

					System.out.println("Interested list size is " + listOfInterestedPeers.size());

					// Declare an iterator for the interestedList of peers
					Iterator<PeerManager> it = listOfInterestedPeers.iterator();

					// Instantiate unchoke and choke peers synchronized lists
					pp.listOfUnchokedPeers = Collections.synchronizedList(new ArrayList<PeerManager>());
					pp.listOfchokedPeers = Collections.synchronizedList(new ArrayList<PeerManager>());

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
								pp.listOfUnchokedPeers.add(next);

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
								pp.listOfchokedPeers.add(next);

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

						pp.log("Peer " + PeerManager.ownerId + " has the preferred neighbors " + neigh);
					}

				};

			} catch (Exception e) {

				System.out.println(e.getMessage());
			}

		}
	}

	/**
	 * Determine optimistically unchocked neighbour every m seconds.
	 */
	class SelectOptUnchokedNeighbour extends TimerTask {

		peerProcess pp;

		public SelectOptUnchokedNeighbour(peerProcess pp) {
			this.pp = pp;
		}    	

		@Override
		public void run(){

			System.out.println("inside optimistically unchoked neighbour!!");
			int chokeListSize = 0;
			
			// Obtain the size of chokeList of owner peer
			if (pp.listOfchokedPeers != null) {
				chokeListSize = pp.listOfchokedPeers.size();
				System.out.println("size = " + chokeListSize);
			}

			if (chokeListSize != 0) {

				// Obtain a random index isolated to the current thread
				int randomIndex = ThreadLocalRandom.current().nextInt(0, chokeListSize);

				// Randomly select a peer from the choked peers list of owner peer optimistically
				PeerManager PeerManager = pp.listOfchokedPeers.remove(randomIndex);

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
						previousOptUnchokedPeer.optimisticallyUnchokedPeer = false;

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

					pp.log("PeerManager " + PeerManager.ownerId + " has the optimistically unchoked neighbor " + "PeerManager " + PeerManager.ownerId);
					System.out.println("Peer " + PeerManager.ownerId + " has the optimistically unchoked neighbor " + "Peer " + PeerManager.ownerId);
				}

			}

			// if owner peer chokeList is null
			else {

				// if previousOptimisticallyUnchokedPeer is not null
				if (previousOptUnchokedPeer != null) {

					// set the value of the previous optimistically Unchoked peer to false
					previousOptUnchokedPeer.optimisticallyUnchokedPeer = false;

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
			};
		}
	}


	/**
	 * Determine when to shutdown...
	 */
	class DetermineShutDownProcess extends TimerTask {

		peerProcess pp;

		public DetermineShutDownProcess(peerProcess pp) {
			this.pp = pp;
		}    	

		@Override
		public void run() {

			// Obtain the bitField of owner peer
			byte[] ownerBitField = PeerManager.getOwnerBitField();
			System.out.println("Arrays.toString(myBitField) = " + Arrays.toString(ownerBitField));

			// if peersList size of owner peer equals the total number of peers in peerInfo config file
			if (peerProcess.listOfPeers.size() == CommonPeerConfig.retrievePeerInfo().size()) {

				// set the shut down flag to true
				boolean shutDownFlag = true;

				// for every peerThread in the peersList
				for (PeerThread pt : peerProcess.listOfPeers) {

					// Obtain the bitField message of each peer
					byte[] peerBitFieldMsg = pt.retrievePeerConnected().getbitFieldMessageOfPeer();

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
					for (PeerThread pt : peerProcess.listOfPeers) {

						// set the Peerthread toStop to true
						pt.toStop = true;
						try {
							// before exiting check if peerSocket is already closed, if no close it
							if(pt.peerSocket.isClosed());
							else
								pt.peerSocket.close();
						} catch (IOException e) {
							System.out.println("Could not close socket:");
							e.printStackTrace();
						}

					}

					//lets write it to a file
					System.out.println("Shut down the scheduled timer tasks..");

					// Terminate the timers, discarding any currently scheduled tasks.
					// Removes all cancelled tasks from this timer's task queue.
					timer1.cancel();
					timer1.purge();

					timer2.cancel();
					timer2.purge();

					timer3.cancel();
					timer3.purge();

				}
			}

		}
	};


}


