package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public abstract class Util 
{
	public final static String FILE_HEADER = "Replication: ";
	public final static String FILE_APPEND = "Stored In: ";
	public final static String INFO_EXTENSION = ".info";
	public final static String PATH_EXTENSION = ".path";
	public final static String CHUNK_EXTENSION = ".ck";
	public final static String SEPARATOR = "-";
	
	public static void programInfo(String msg){
		System.out.println(msg);
	}
	
	public static void programInfo(String msg, int num){
		if(num == 1)
			System.err.println(msg);
		else
			programInfo(msg);
	}

	public static String[] retrieveAddPort(String channel){		
		String pattern[] = channel.split(":");
		return pattern;
	}
	
	private static String bytesToHex(byte[] hashValue) {
	    Formatter form = new Formatter();
	    for (int i = 0; i < hashValue.length; i++)
	       form.format("%02x", hashValue[i]);
	    String sha = form.toString();
	    form.close();
	    return sha;
	}
	
	public static String calculateSha256(String fileToEncrypt) throws IOException, NoSuchAlgorithmException {
	    MessageDigest md = MessageDigest.getInstance("SHA-256");
	    md.update(fileToEncrypt.getBytes("UTF-8"));
	    byte[] hashValue = md.digest();
	    StringBuilder sb = new StringBuilder(bytesToHex(hashValue));
	    
	    return sb.toString();
	}

	public static BufferedOutputStream openBufferOutputStream(String path)
	{
		File file = new File(path);
		
		if(!file.exists())
		{
			file.getParentFile().mkdirs();
			
			try {
				file.createNewFile();
			} 
			catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		BufferedOutputStream bos;
		
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file));
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
			bos = null;
		}
		
		return bos;
	}
	
	public synchronized static boolean writeToBuffer(BufferedOutputStream bos, byte[] buff){
		try {
			bos.write(buff);
			return true;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean closeBufferOutputStram(BufferedOutputStream bos)
	{
		try {
			bos.close();
			return true;
		} 
		catch (IOException e) {
			e.printStackTrace();
			return false;	
		}
	}

	public static BufferedInputStream openBufferInputStream(File file){
		BufferedInputStream bis;

		try {
			bis = new BufferedInputStream(new FileInputStream(file));
		} 
		catch (FileNotFoundException e) {
			bis = null;
		}
		
		return bis;
	}

	public static boolean closeBufferInputStream(BufferedInputStream bis)
	{
		try {
			bis.close();
			return true;
		} 
		catch (IOException e) {
			return false;
		}
	}

	public synchronized static byte[] readFromBuffer(BufferedInputStream bis, int size)
	{
		byte[] buff = new byte[size];
		
		try {
			int tmpRead = bis.read(buff);
			if(tmpRead > 0)
			{
				byte[] chunk = new byte[tmpRead];
				System.arraycopy(buff, 0, chunk, 0, tmpRead);
				return chunk;
			}
			else{
				byte[] chunk = new byte[0];
				return chunk;
			}	
		} 
		catch (IOException e) {
			return null;
		}
	}
	
	public static boolean sendPacket(DatagramSocket socket, DatagramPacket packet)
	{
		try{
			socket.send(packet);
			return true;
		} 
		catch (IOException e) {
			return false;
		}
	}

	public static MulticastSocket createMulticastSocket(InetAddress addr, int port)
	{
		MulticastSocket mc;
		
		try {
			mc = new MulticastSocket(port);
			mc.joinGroup(addr);
			return mc;
		} 
		catch (IOException e){
			return null;
		}
	}
	
	public static DatagramPacket receiveFromMulticast(MulticastSocket mc, int receiveTime, int bufflength){
		byte[] buff = new byte[bufflength];
		DatagramPacket packetReceive = new DatagramPacket(buff, buff.length);
		
		try{
			mc.setSoTimeout(receiveTime);
			mc.receive(packetReceive);
			return packetReceive;
		} 
		catch (IOException e) {
			return null;
		}
	}
	
	public static boolean isInteger(String str)
	{
		try{
			Integer.parseInt(str);
			return true;
		}
		catch(NumberFormatException e){
			return false;
		}
	}
}
