
public class MessageTypes {
	
	public static final byte[] HANDSHAKE_BYTE_ARR = "P2PFILESHARINGPROJ".getBytes();
	public static final byte[] ZERO_BITS = {0,0,0,0,0,0,0,0,0,0};
	
	public enum OriginalMessageTypes{
		CHOKE((byte)0), 
		UNCHOKE((byte)1), 
		INTERESTED((byte)2), 
		NOT_INTERESTED((byte)3), 
		HAVE((byte)4), 
		BITFIELD((byte)5), 
		REQUEST((byte)6), 
		PIECE((byte)7);
		
		byte messageValue = -1;
		
		private OriginalMessageTypes(byte b){
			this.messageValue = b;
		}
	}
}
