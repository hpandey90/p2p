
public class HandshakeMsg {

	String handshakeHeader;
	String zeroBits;
	int peerId;
	
	
	public HandshakeMsg (String header,String zeros, int id) {
		this.handshakeHeader = "P2PFILESHARINGPROJ";
		this.zeroBits = "0000000000";
		this.peerId = id;
	}
}
