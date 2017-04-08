import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Logger;

/*
 * Class for reading the Common and peerInfo configuration files.
 */
public class CommonPeerConfig {
	
	// Variable to store peerInfo configuration file name.
	private final String peerInfoFile = "PeerInfo.cfg";
		
	// Variable to store Common configuration file name.
	private final String commonConfigFile = "Common.cfg";
	
	// Map to maintain peerInfo configuration file details.
	private static HashMap<Integer, String> peerInfo = null;
	
	// Map to maintain Common configuration file details. 
	private static HashMap<String, String> commonConfig = null;
	
	// CommonPeerConfig class object.
	private static CommonPeerConfig cpc = null;
	
	public static HashMap<String, String> retrieveCommonConfig(){
		
		if(cpc != null) {}
		else cpc = new CommonPeerConfig();
		return commonConfig;
	}
	
	public static HashMap<Integer, String> retrievePeerInfo(){
		if(cpc != null) {}
		else cpc = new CommonPeerConfig();
		return peerInfo;
	}
	
	public CommonPeerConfig() {
		
		try {
			peerInfo = new HashMap<Integer, String>();
			commonConfig = new HashMap<String, String>();
			
		
			// Obtain the BufferedReader for the commonConfigFile.
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(commonConfigFile))));
		 
			// Read the commonConfigFile lines using the BufferedReader into the commonConfig map.
			String line1 = null;
			while ((line1 = br.readLine()) != null) {
				String[] split = line1.split(" ");
				commonConfig.put(split[0], split[1]);
			}
		 
			br.close();
			
			// Obtain the BufferedReader for the peerInfoFile.
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(peerInfoFile))));
			
			// Read the peerInfoFile lines using the BufferedReader into the peerInfo map.
			String line2 = null;
			while ((line2 = br.readLine()) != null) {
				String[] split = line2.split(" ");
				peerInfo.put(Integer.parseInt(split[0]), line2);
			}
		 
			br.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
		
	}
	
	
	
}



