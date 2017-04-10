import java.io.IOException;
import java.util.Arrays;
import java.io.*;

/*
 * Class to perform various operations on Byte arrays.
 */
public class ByteArrayManipulation {
    
	public synchronized static byte[] readBytes(InputStream input, byte[] byteArr, int length) throws IOException {
        /*int len = length;
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
        return byteArray;*/
        int index = 0;
        for (;length != 0;) {
            int inputAvail = input.available();
            //int readData = Math.min(length, inputAvail);
            byte[] data = new byte[Math.min(length, inputAvail)];
            if (Math.min(length, inputAvail) != 0) {
                input.read(data);
                byteArr = ByteArrayManipulation.mergeByteArrays(byteArr, index, data, Math.min(length, inputAvail));
                index = index + Math.min(length, inputAvail);
                length = length - Math.min(length, inputAvail);
            }
        }
        return byteArr;
    }
	
	public static int byteArrayToInt(byte[] b) {
    	//return ByteBuffer.wrap(b).getInt();
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
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
        /*byte[] mergedByte = new byte[a.length + b.length];
    	for(int i = 0;i<a.length; i++){
    		mergedByte[i] = a[i];
    	}
    	for(int i = a.length;i< b.length; i++){
    		mergedByte[i] = b[i];
    	}
    	return mergedByte;*/
     }

    public static byte[] mergeByteArrays(byte[] a, int aLength, byte[] b, int bLength) throws IOException {
        byte[] mergedByte = new byte[aLength + bLength];
    	for(int i = 0;i<aLength; i++){
    		mergedByte[i] = a[i];
    	}
    	for(int i = aLength;i<bLength; i++){
    		mergedByte[i] = b[i];
    	}
    	/*result = new byte[aLength + bLength];
		 System.arraycopy(a, 0, result, 0, aLength);
		 System.arraycopy(b, 0, result, aLength, bLength);
		 System.out.println("result22======>"+Arrays.toString(result));*/
		 return mergedByte;
    }

    public static byte[] mergeByteArray(byte b, byte[] a) throws IOException {
        /*byte[] result = new byte[a.length + 1];
        
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	result = output.toByteArray( );
    	
    	return result;*/
    	/*
    	byte[] result = new byte[a.length + 1];
        System.arraycopy(a, 0, result, 0, a.length);
        result[a.length] = b;
        return result;*/
    	byte[] mergedByte = new byte[a.length + 1];
    	for(int i = 0;i<=a.length; i++){
    		mergedByte[i] = a[i];
    	}
    	mergedByte[a.length] = b;
    	return mergedByte;
    }
    
    public static byte[] mergeByte(byte[] a, byte b) throws IOException {
        /*byte[] result = new byte[a.length + 1];
        
        ByteArrayOutputStream output = new ByteArrayOutputStream( );
    	output.write( a );
    	output.write( b );

    	result = output.toByteArray( );
    	
    	return result;*/
    	 /*byte[] result = new byte[a.length + 1];
         System.arraycopy(a, 0, result, 0, a.length);
         result[a.length] = b;
         return result;*/
    	byte[] mergedByte = new byte[a.length + 1];
    	for(int i = 0;i<a.length; i++){
    		mergedByte[i] = a[i];
    	}
    	mergedByte[a.length] = b;
    	return mergedByte;
    }
    
}