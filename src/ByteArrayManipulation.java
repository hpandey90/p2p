import java.io.IOException;
import java.io.InputStream;

/*
 * Class to perform various operations on Byte arrays.
 */
public class ByteArrayManipulation {
    
	public synchronized static byte[] readBytes(InputStream in, byte[] byteArray, int length) throws IOException {
        /*int len = length;
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
        return byteArray;*/
        int len = length;
        int idx = 0;
        while (len != 0) {
        	//System.out.println("entered readBytes while loop:"+len);
            int dataAvailableLength = in.available();
            //System.out.println("checking if data available"+ dataAvailableLength);
            int read = Math.min(len, dataAvailableLength);
            byte[] dataRead = new byte[read];
            if (read != 0) {
                in.read(dataRead);
                //System.out.println("inside the if(read!=0)" + len);
                byteArray = ByteArrayManipulation.mergeByteArrays(byteArray, idx, dataRead, read);
                idx += read;
                len -= read;
            }
        }
        return byteArray;
    }
	
	public static int byteArrayToInt(byte[] b) {
  
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value += (b[i] & 0x000000FF) << ((4 - 1 - i) * 8);
        }
        return value;
    }

	
    public static byte[] intToByteArray(int integer) {

        byte[] result = new byte[4];

        result[0] = (byte) ((integer & 0xFF000000) >> 24);
        result[1] = (byte) ((integer & 0x00FF0000) >> 16);
        result[2] = (byte) ((integer & 0x0000FF00) >> 8);
        result[3] = (byte) (integer & 0x000000FF);

        return result;
    	/*ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
        return int_bytes;*/
        }

    
    public static byte[] mergeByteArrays(byte[] a, byte[] b) throws IOException {
    	
    	/*ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	byte result[] = output.toByteArray( );
    	
    	return result;*/
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
     }

    public static byte[] mergeByteArrays(byte[] a, int aLength, byte[] b, int bLength) throws IOException {
        /*byte[] result = new byte[aLength + bLength];
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write(a);
    	output.write(b);

    	result = output.toByteArray( );
    	
    	return result;*/
    	 byte[] result = new byte[aLength + bLength];
         System.arraycopy(a, 0, result, 0, aLength);
         System.arraycopy(b, 0, result, aLength, bLength);
         return result;
    }

    public static byte[] mergeByteArray(byte b, byte[] a) throws IOException {
        /*byte[] result = new byte[a.length + 1];
        
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	result = output.toByteArray( );
    	
    	return result;*/
    	byte[] result = new byte[a.length + 1];
        System.arraycopy(a, 0, result, 0, a.length);
        result[a.length] = b;
        return result;
    }
    
    public static byte[] mergeByte(byte[] a, byte b) throws IOException {
        /*byte[] result = new byte[a.length + 1];
        
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	result = output.toByteArray( );
    	
    	return result;*/
    	 byte[] result = new byte[a.length + 1];
         System.arraycopy(a, 0, result, 0, a.length);
         result[a.length] = b;
         return result;
    }
    
}