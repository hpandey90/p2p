import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Class for handling thread managing functionality for peers
 */
public class PeerThread extends Thread {
   
    // Declare the logger
    private static final Logger LOGGER = MyLogger.getMyLogger();
    
    // Declare the peerSocket
    private Socket peerSocket = null;
    
    // Declare peer Manager object for peer and other related variables
    private PeerManager peerConnected = null;
    private boolean toStop = false;
    private boolean isClient = false;
    
    // Declare thread for handling the initialization of peer to peer communication 
    private  Thread initialSetupThread = null;
    
    // get and set methods for PeerThread class variables
    public synchronized PeerManager retrievePeerConnected() {
        return peerConnected;
    }
    
    public synchronized PeerManager getPeerConnected(Socket s) {
        return new PeerManager(s);
    }
   
    public boolean getToStop() {
        return toStop;
    }

    public void setToStop(boolean b) {
        if(b == true){
            try {
                // before exiting check if peer peerSocket is already closed, if no close it
                if(peerSocket.isClosed());
                else
                    peerSocket.close();
            } catch (IOException e) {
                System.out.println("Could not close socket:");
                e.printStackTrace();
            }
        }
        
        // set the toStop flag to passed value
        toStop = b;
    }

    // PeerThread constructor
    public PeerThread(Socket psocket, boolean pisClient, int pid) throws IOException {
        
        // Initialize object variables with passed values
        this.peerSocket = psocket;
        this.isClient = pisClient;
        
        // Obtain a peerManager object peerConnected for the received peerId(client or server peer) 
        // by passing its peerSocket and obtain its corresponding input & output buffered streams.
        peerConnected = getPeerConnected(peerSocket);
        
        // if a client peer
        if (isClient) {
            
            // set the peerId(not myId) of peerManager object to the client's peerId and set the clientValue 
            peerConnected.setPeerId(pid);
            peerConnected.setClientValue(true);
            
            // Send handshake message from the owner peer (retrieved from the commonConfig file written in PeerProcess)
            // to the client peer. 
            peerConnected.sndHandshakeMessage();
            
            // Receive the handshake message from the peer to whom a handshake was previously sent
            // (using peer's socket inputbufferedstream).
            peerConnected.rcvHandshakeMessage();
        
        } 
        // if a server owner peer
        else {
            
            // if server owner peerThread, receive the handshake message from a client peer 
            int peerId = peerConnected.rcvHandshakeMessage();
            
            // set the peerId of peerManager object to the client's peerId from whom the handshake was received
            peerConnected.setPeerId(peerId);
            
            // send handshake message to client peer
            peerConnected.sndHandshakeMessage();

        }
        
        // Change the name of peerThread to the peerManager object's peerId(set above based on isClient).
        this.setName("Peer : " + peerConnected.getPeerId());
        
        // Create a thread for the peerManager object for bitfield and 
        // interested/not interested message communication as part of initial setup.
        initialSetupThread = new Thread() {
            
            public void run() {
                System.out.println("Peer connected is initialized " + peerConnected.getIsPeerInitialized());
                
                // send bitfield message from one peer to other
                peerConnected.sndBitFieldMessageToPeer();
                
                // read the bitfield message of peer from its input peerSocket and save in a byte array 
                peerConnected.readBitFieldMessageOfPeer();

                // check if owner has missing bit field present in client's bit field message, if yes -> interested
                if (peerConnected.isInterested()) {
                    

                	System.out.println("Sending interested message to  " + peerConnected.getPeerId());
                    
                	try {
						// send interested message to client peer
                		peerConnected.sendInterestedMessage();
					} 
                	catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                    System.out.println("Sending interested message to  " + peerConnected.getPeerId());
                    
                    try {
                        // send interested message to client peer
                        peerConnected.sendInterestedMessage();
                    } 
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                } 
                
                // else, owner peer not interested in client peer
                else {
                    System.out.println("Sending not interested msg to " + peerConnected.getPeerId());
                    
                    try {
                        // send not interested message to peer
                        peerConnected.sendNotInterestedMessage();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                // do logging 
                if (isClient == true) {
                    LOGGER.info("Peer " + CommonPeerConfig.retrieveCommonConfig().get("peerId")
                            + " initiated a connection to " + peerConnected.getPeerId());
                } else {
                    LOGGER.info(CommonPeerConfig.retrieveCommonConfig().get("peerId")
                            + " is connected from " + peerConnected.getPeerId());
                }
                
                // after exchanging the bitfield and interested messages, set the peerInitialized 
                peerConnected.setisPeerInitialized(true);
            }

        };
        
        System.out.println("Is initialized before thread start " + peerConnected.getIsPeerInitialized());
        // Start the initialized peer thread to initialize the peer, peer thread will be initialized after this thread runs
        initialSetupThread.start();


    }

    @Override
    public void run() {
        // Handle peer communication following the initialization 

        // Obtain isInitialized of peerConnected for checking if peer is initialized.
        peerConnected.getIsPeerInitialized();
        System.out.println("peerConnected.isInitialized() = " + peerConnected.getIsPeerInitialized());
        
        try {
            
            // Retrive the input stream of peer's socket
            InputStream inputStream = new BufferedInputStream(peerSocket.getInputStream());
            
            // each peer thread runs till not asked to toStop
            while (!getToStop()) {
                
                
                byte[] messageBytesOfPeer = new byte[5];
                messageBytesOfPeer = ByteArrayManipulation.readBytes(inputStream, messageBytesOfPeer, 5);
                MessageTypes.OriginalMessageTypes msgType = MessagesUtil.getMsgType(messageBytesOfPeer);
                
                switch (msgType) {
                    
                    case BITFIELD:
                        // already handled in initialization while accepting connections 
                        break;
                    
                    case HAVE:
                        System.out.println("Have message has been received from peer: " + peerConnected.getPeerId());
                        
                        // read the 4 byte piece index field of peer's have message from its peerSocket's input stream
                        // into readIndexOfPieceFieldBytes array of 4 bytes.
                        byte[] readIndexOfPieceFieldBytes = new byte[4];
                        readIndexOfPieceFieldBytes = ByteArrayManipulation.readBytes(inputStream, readIndexOfPieceFieldBytes, 4);
                        
                        // obtain the index of the piece by wrapping the byte array into byte buffer and obtain its integer value
                        int indexOfPiece = ByteArrayManipulation.byteArrayToInt(readIndexOfPieceFieldBytes);
                        
                        // Obtain the bitField message byte array of owner peer
                        byte[] ownerBitFieldMessage = PeerManager.getOwnerBitField();
                        
                        // Obtain the byte of the owner at the indexOfPiece in ownerBitFieldMessage 
                        byte ownerByte = ownerBitFieldMessage[indexOfPiece / 8];

                        // check if the owner peer has the piece or not
                        // by doing BITWISE AND between owner byte and 1 left shifted to the indexOfPiece value
                        // ex: indexOfPiece = 3 then 1 << (7 - 3) = 1 << 4 = 00001000
                        // bitwise AND will result in zero if owner byte does not have that bit set
                        if ((ownerByte & (1 << (7 - (indexOfPiece % 8)))) == 0) {
                            // if the owner peer does not have the piece, send interested message
                            peerConnected.sendInterestedMessage();
                        }
                        
                        // update the indexOfPiece bit of bit field message of client peer  
                        peerConnected.updateBitFieldMessageOfPeer(indexOfPiece);
                        
                        LOGGER.info("Peer " + PeerManager.ownerId + " received the have message from " + peerConnected.getPeerId());
                        break;
                    
                    case CHOKE:
                        System.out.println("Choke message has been received from peer: " + peerConnected.getPeerId());
                        
                        // Obtain the index of requested piece
                        int indexOfRequestedPiece = peerConnected.getindexOfRequestedPiece();
                        
                        // Obtain the owner bitField message
                        byte[] ownerBitfieldMessage = PeerManager.getOwnerBitField();
                        
                        // Obtain the owner's byte at the requested piece index
                        byte ownerByteAtRequestedPieceIndex = ownerBitfieldMessage[indexOfRequestedPiece / 8];

                        // check if the owner has the requested piece at indexOfRequestedPiece/8
                        if ((ownerByteAtRequestedPieceIndex & (1 << (7 - (indexOfRequestedPiece % 8)))) == 0) {
                            // Owner has not received the requested piece yet
                            // reset the indexOfRequestedPiece%8 bit at indexOfRequestedPiece/8 of requestedBitField
                            PeerManager.resetIndexOfPieceRequested(indexOfRequestedPiece / 8, indexOfRequestedPiece % 8);
                        }
                        
                        // put the peer in the chokedPeers map of owner peer
                        PeerManager.chokedPeers.put(peerConnected.getPeerId(), peerConnected);
                        
                        LOGGER.info("Peer " + PeerManager.ownerId + " is choked by "+ peerConnected.getPeerId());
                        break;
                    
                    case INTERESTED:
                        System.out.println("Interested message has been received from peer:" + peerConnected.getPeerId());
                        
                        // flag to check if the peer is an interested peer
                        boolean isInterestedPeer = false;
                        
                        // check if the peer exists in the interestedPeers list of owner peer
                        for(PeerManager p : PeerManager.interestedPeers){
                        
                            if(p.getPeerId() == peerConnected.getPeerId()){
                                isInterestedPeer = true;
                            }
                        
                        }
                        
                        // if not already an interested peer
                        if(!isInterestedPeer){
                            // add the peer to the interested peers list of owner peer
                            PeerManager.interestedPeers.add(peerConnected);
                        }
                        
                        LOGGER.info("Peer " + PeerManager.ownerId + " received the interested message from " + peerConnected.getPeerId());
                        break;
                    
                    case NOT_INTERESTED:
                        System.out.println("Not interested message has been received from peer:"  + peerConnected.getPeerId());
                        
                        //remove the peer from interestedPeers list of owner peer 
                        PeerManager.interestedPeers.remove(peerConnected);
                        
                        // set the choke value of peer to true
                        peerConnected.setChoked(true);
                        
                        // add the peer to notinterestedPeers list of owner peer
                        PeerManager.notinterestedPeers.put(peerConnected.getPeerId(), peerConnected);
                        
                        LOGGER.info("Peer " + PeerManager.ownerId + " received the not interested message from " + peerConnected.getPeerId());
                        break;
                    
                    case PIECE:
                        System.out.println("Piece message has been received from peer:"  + peerConnected.getPeerId());
                        
                        // byte array to hold piece message bytes of peer
                        byte[] sizeByteArray = new byte[4];
                        
                        for (int i = 0; i < 4; i++) {
                            sizeByteArray[i] = messageBytesOfPeer[i];
                        }
                        
                        // obtain size of message bytes of peer
                        int sizeOfMessage = ByteArrayManipulation.byteArrayToInt(sizeByteArray);
                        
                        // read the piece 
                        byte[] pieceIndexBytes = new byte[4];
                        pieceIndexBytes = ByteArrayManipulation.readBytes(inputStream, pieceIndexBytes, 4);
                        
                        int sizeOfPieceMsg = sizeOfMessage - 1;
                        int sizeOfPiecePayLoad = sizeOfPieceMsg - 4;
                        
                        byte[] piece = new byte[sizeOfPiecePayLoad];
                        
                        piece = ByteArrayManipulation.readBytes(inputStream, piece, sizeOfPiecePayLoad);
                        
                        Long downTime = System.nanoTime() - PeerManager.peerRequestTime.get(peerConnected.getPeerId());
                        
                        PeerManager.peerDownloadTime.put(peerConnected.getPeerId(), downTime);
                        
                        peerConnected.setPeerDownloadRate(downTime);
                        
                        System.out.println("The downloading time set is " + downTime);
                        
                        int pieceI = ByteArrayManipulation.byteArrayToInt(pieceIndexBytes);
                        System.out.println("The piece index value is " + pieceI +"from " + peerConnected.getPeerId());
                        
                        int stdPieceSize = Integer.parseInt(CommonPeerConfig.retrieveCommonConfig().get("PieceSize"));
                        
                        for (int i = 0; i < sizeOfPiecePayLoad; i++) {
                            PeerManager.sharedDataArr[pieceI * stdPieceSize + i] = piece[i];
                        }
                        
                        LOGGER.info("Peer " + PeerManager.ownerId + " has downloaded the piece " + pieceI + " from " + peerConnected.getPeerId());
                        
                        // send have message to rest of the peers
                        int index = pieceI / 8;
                        int pos = pieceI % 8;
                        PeerManager.setOwnerBitFieldIndex(index, pos);
                        
                        for (PeerThread peerThread : PeerProcess.peersList) {
                            
                            System.out.println("Reached Inside Piece Have check ");
                            peerThread.retrievePeerConnected().sendHaveMessage(pieceI);
                          
                        }
                        
                        int nxtidx = peerConnected.getNextBitFieldIndexToRequest();
                        System.out.println("next index requested is " + nxtidx +"from " + peerConnected.getPeerId());
                        
                        if (nxtidx != -1
                                && PeerManager.unchokedPeers.containsKey(peerConnected.getPeerId())) {
                            
                            System.out.println("Yes its being requested" + nxtidx);
                            peerConnected.sendRequestMessage(nxtidx);
                        
                        }
                        
                        System.out.println("piece peer Bit field msg = " + Arrays.toString(peerConnected.getbitFieldMessageOfPeer()));
                        System.out.println("piece my bit field msg = " + Arrays.toString(PeerManager.getOwnerBitField()));
                        
                        if(nxtidx == -1){
                            System.out.println("bit fields are equal" + peerConnected.getPeerId() );
                            peerConnected.sendNotInterestedMessage();
                        }
                        
                        if(nxtidx == -1 && !(Arrays.equals(peerConnected.getOwnerBitField(), peerConnected.getbitFieldMessageOfPeer())))
                        {
                            System.out.println("bit fields are not  equal" + peerConnected.getPeerId() );
                            peerConnected.sendInterestedMessage();
                        }

                        if(nxtidx == -1 && Arrays.equals(PeerManager.getOwnerBitField(), PeerManager.getFileBitfield())){
                            
                            System.out.println("Creating the file");
                            File file = new File(CommonPeerConfig.retrieveCommonConfig().get("FileName"));
                            
                            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                                fileOutputStream.write(PeerManager.sharedDataArr);
                            } catch (IOException e) {
                                System.out.println("shutdown called !! -- file create");
                                e.printStackTrace();
                            }
                        }

                        break;
                    
                    case REQUEST:
                        System.out.println("Request message has been received from peer:" +  peerConnected.getPeerId());
                        
                        // read the 4 byte piece index field from request message of client peer
                        byte[] ind = new byte[4];
                        inputStream.read(ind);
                        
                        // Obtain the index of piece requested
                        int pieceIndex = ByteArrayManipulation.byteArrayToInt(ind);
                        
                        // if client peer is not currently choked
                        if (!peerConnected.isChoked()) {
                            System.out.println("sending piece message from request the piece index requested is " + pieceIndex);
                            // send piece message starting from pieceIndex from owner peer when request message received 
                            peerConnected.sendPieceMessage(pieceIndex);
                        }
                        break;
                    
                    case UNCHOKE:
                        System.out.println("Unchoke message has been received from peer:" +   peerConnected.getPeerId());
                        
                        // add peer to the unchokedPeers hashmap of owner peer
                        PeerManager.unchokedPeers.put(peerConnected.getPeerId(), peerConnected);
                        
                        
                        LOGGER.info("Peer " + PeerManager.ownerId + " has been unchoked by " + peerConnected.getPeerId());
                        
                        // request a piece owner peer doesn't have and did not request from other peers, 
                        // select next piece to request index randomly
                        int nextIndexToRequest = peerConnected.getNextBitFieldIndexToRequest();
                     
                        // if nextIndexToRequest != -1, owner and client peer bit fields are unequal
                        // owner peer has not yet requested for what it doesn't have
                        if (nextIndexToRequest != -1) {
                            // send request message from owner peer to client peer 
                            peerConnected.sendRequestMessage(nextIndexToRequest);
                        }

                        System.out.println("piece peer Bit field msg in unchoke  = " + Arrays.toString(peerConnected.getbitFieldMessageOfPeer()));
                        System.out.println("piece owner bit field msg in unchoke = " + Arrays.toString(PeerManager.getOwnerBitField()));
                        
                        // if nextIndexToRequest == -1, owner and client peer bit fields are equal
                        if(nextIndexToRequest == -1){
                            // send not interested message from owner peer to client peer
                            peerConnected.sendNotInterestedMessage();
                        }

                        // if owner and client peer bit field messages are not equal
                        if( nextIndexToRequest == -1 && !(Arrays.equals(peerConnected.getOwnerBitField(), peerConnected.getbitFieldMessageOfPeer())))
                        {
                            System.out.println("bit fields are not  equal in unchoke " + peerConnected.getPeerId() );
                            System.out.println("Sending interested");
                            // send interested message from owner peer to client peer
                            peerConnected.sendInterestedMessage();
                        }

                        break;
                    
                    default:
                        System.out.println("something was received");
                }
            }

        } catch (IOException e) {
            if(!getToStop()) {
                e.printStackTrace();
            }
        }finally {
            System.out.println("Exit from peer: " + peerConnected.getPeerId());
        }

    }
}