import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

public class peerProcess {

	// Declare the logger
	private static final Logger LOGGER = MyLogger.loggerInstance();

	// Synchronized list to maintain all the connected peers of owner peer
	public static List<PeerThread> listOfPeers = Collections.synchronizedList(new ArrayList<PeerThread>());

	// List to maintain interested and unchoked peers
	List<PeerManager> listOfUnchokedPeers = null;

	// List to maintain interested and choked peers
	List<PeerManager> listOfchokedPeers = null;

	public static void main(String[] args) {

		// Obtain peerId from command line and set it as owner and put it in the commonConfig hashmap
		// consisting of property, value pairs used later in the peerManager
		Scanner scan = new Scanner(System.in);
		int peerId = Integer.valueOf(args[0]);

		// set the ownerId of PeerManager
		PeerManager.ownerId = peerId;

		//CommonPeerConfig.retrieveCommonConfig().put("peerId", String.valueOf(peerId));
		scan.close();

		// Initialize custom logger
		try {
			MyLogger.setup();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Problems with creating the log files");
		}

		// Create a peerProcess object to start client peers and owner server peer connection communication processes
		peerProcess peerProcessOb = new peerProcess();

		// setup and start the client and server peer connections
		peerProcessOb.peerConnectionSetup(peerId);

		// schedule the tasks for determining k preferred neighbours and optimistically unchoked neighbour peers
		PeerSchedules st = new PeerSchedules(new peerProcess());
	}

	/*
	 * Start client peer threads and server peer threads to accept each client connection in a seperate thread
	 */
	public void peerConnectionSetup(int ownerPeerId) {

		// Obtain the peerInfo hashMap
		Map<Integer, String> peerInfo = CommonPeerConfig.retrievePeerInfo();

		for (Map.Entry<Integer, String> s : peerInfo.entrySet()) {

			// for every peer, obtain the peerId, host and listening port from the peerInfo map
			String line = peerInfo.get(s.getKey());
			String[] arr = line.split(" ");
			int peerId = Integer.parseInt(arr[0]);
			String host = arr[1];
			int portN = Integer.parseInt(arr[2]);

			// when the server owner peer found
			if ( peerId == ownerPeerId) {

				Thread serverThread = new Thread(new Runnable() {
					@Override
					public void run() {

						try {

							int peerIdGreaterCount = 0;

							// Count peers having id > owner peer id
							for (Map.Entry<Integer, String> e : peerInfo.entrySet()) {
								if (e.getKey() > ownerPeerId) {
									peerIdGreaterCount++;
								}
							}

							while (peerIdGreaterCount > 0) {

								// Creating a server socket for the peer in which this program is running.
								ServerSocket serverSocket = new ServerSocket(portN);
								Socket acceptedSocket = serverSocket.accept();
								PeerThread r = new PeerThread(acceptedSocket, false, -1);
								Thread listenThread = new Thread(r);						
								listenThread.start();
								listOfPeers.add(r);

								// Decrement the greaterPeerCount
								peerIdGreaterCount--;
							}

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
			}

			else {
				if (peerId < ownerPeerId) { 

					Socket clientSocket;
					try {

						clientSocket = new Socket( host, portN);
						PeerThread r = new PeerThread(clientSocket, true, peerId);
						Thread clientThread = new Thread(r);
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

	public void logging(String logMessage) {
		Logger logger = LOGGER;
		if (logger != null){}
		else
			logger = MyLogger.loggerInstance();
		logger.info(logMessage);
	}

}