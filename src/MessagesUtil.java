import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * This class represents the messsage's that are exchanged for the protocol to function
 */
public class MessagesUtil {

    public static byte[] getHandShakeMessage(int toPeerId) throws IOException {
    	return ByteArrayManipulation.mergeByteArrays(ByteArrayManipulation.mergeByteArrays(
		        MessageTypes.HANDSHAKE_BYTE_ARR, MessageTypes.ZERO_BITS), ByteArrayManipulation.intToByteArray(toPeerId));
    }

    public static byte[] getOriginalMessage(String payload, MessageTypes.OriginalMessageTypes msgType) throws IOException {

        int l = payload.getBytes().length;
        byte[] msgL = ByteArrayManipulation.intToByteArray(l + 1); // plus one for message type
        return ByteArrayManipulation.mergeByteArrays(msgL,
		        ByteArrayManipulation.mergeByteArray(msgType.messageValue, payload.getBytes()));
    }

    public static byte[] getOriginalMessage(MessageTypes.OriginalMessageTypes msgType) throws IOException {
        
    	byte[] msgL = ByteArrayManipulation.intToByteArray(1); // plus one for message type
        return ByteArrayManipulation.mergeByte(msgL, msgType.messageValue);
    }

    public static byte[] getOriginalMessage(byte[] payload, MessageTypes.OriginalMessageTypes msgType) throws IOException {
        
    	byte[] msgL = ByteArrayManipulation.intToByteArray(payload.length + 1); // plus one for message type
        return ByteArrayManipulation.mergeByteArrays(ByteArrayManipulation.mergeByte(msgL, msgType.messageValue), payload);
    }


    public static byte[] readOriginalMessage(InputStream in, MessageTypes.OriginalMessageTypes bitfield) {
        
    	byte[] lengthByte = new byte[4];
        int read = -1;
        byte[] data = null;
        
        try {
            
        	read = in.read(lengthByte);
            
        	if (read != 4) {
                System.out.println("Message length is not proper!!!");
            }
            
            int dataLength = ByteArrayManipulation.byteArrayToInt(lengthByte);
            
            //read msg type
            byte[] msgType = new byte[1];
            
            in.read(msgType);
            
            if (msgType[0] == bitfield.messageValue) {
                
            	int actualDataLength = dataLength - 1;
                data = new byte[actualDataLength];
                data = ByteArrayManipulation.readBytes(in, data, actualDataLength);
                
            } 
            
            else {
                System.out.println("Wrong message type sent");
            }

        } catch (IOException e) {
           
        	System.out.println("Could not read length of actual message");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return data;
    }

    public static MessageTypes.OriginalMessageTypes getMsgType(byte[] msgStat) {

    	String s = Arrays.toString(msgStat);
        
    	for (MessageTypes.OriginalMessageTypes actMsgType : MessageTypes.OriginalMessageTypes.values()) {
        
    		if (actMsgType.messageValue == msgStat[4]) {
                return actMsgType;
            }
        }
        return null;
    }
}
