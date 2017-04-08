import java.io.ObjectOutputStream;
import java.io.ObjectOutput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

/*
 * Class to perform various operations on Byte arrays.
 */
public class ByteArrayManipulation {
    
	public synchronized static byte[] readBytes(InputStream in, byte[] byteArray, int length) throws IOException {
        int len = length;
        int index = 0;
        int lenOfDataRead = Math.min(len, in.available());
        while (len != 0) {
            byte[] dataRead = new byte[Math.min(len, in.available())];
            if (Math.min(len, in.available()) != 0) {
                in.read(dataRead);
                byteArray = ByteArrayManipulation.mergeByteArrays(byteArray, index, dataRead, lenOfDataRead);
                index = index + lenOfDataRead;
                len = len - lenOfDataRead;
            }
        }
        return byteArray;
    }
	
	public static int byteArrayToInt(byte[] b) {
    	return ByteBuffer.wrap(b).getInt();
    }

    public static byte[] intToByteArray(int value) {

    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			out.writeInt(value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        byte[] int_bytes = bos.toByteArray();
        try {
			bos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return int_bytes;
        }

    
    public static byte[] mergeByteArrays(byte[] a, byte[] b) throws IOException {
    	
    	ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	byte result[] = output.toByteArray( );
    	
    	return result;
     }

    public static byte[] mergeByteArrays(byte[] a, int aLength, byte[] b, int bLength) throws IOException {
        byte[] result = new byte[aLength + bLength];
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write(a);
    	output.write(b);

    	result = output.toByteArray( );
    	
    	return result;
    }

    public static byte[] mergeByteArray(byte b, byte[] a) throws IOException {
        byte[] result = new byte[a.length + 1];
        
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	result = output.toByteArray( );
    	
    	return result;
    }
    
    public static byte[] mergeByte(byte[] a, byte b) throws IOException {
        byte[] result = new byte[a.length + 1];
        
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	result = output.toByteArray( );
    	
    	return result;
    }
    
}
