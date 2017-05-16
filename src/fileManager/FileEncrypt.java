package fileManager;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import util.Util;

public abstract class FileEncrypt 
{
	public static final String FIELD_SEPARATOR = "-";
	public static String encrypt(File file, peer.Peer peerInfo)
	{
		String str = file.getName() + FIELD_SEPARATOR + peerInfo.getServerID() + FIELD_SEPARATOR + file.lastModified();
		
		try {
			return Util.calculateSha256(str);
		} 
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} 
		catch (IOException e){
			e.printStackTrace();
			return null;
		}
	}
}
