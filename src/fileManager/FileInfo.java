package fileManager;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import util.Util;
import message.Message;

public abstract class FileInfo 
{
	public synchronized static File createStored(String name, int replicationDeg){
		File file = new File(name);
		//Creating directories if do not exist
		file.mkdirs();
		//Delete file if exists
		file.delete();
		
		if(!file.exists() && file.isDirectory())
			Util.programInfo("Failed To Create Stored File: " + name);
		
		String concat = Util.FILE_HEADER + replicationDeg + "\n";
		
		try {
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bf = new BufferedWriter(fw);	
			bf.write(concat); 
			bf.close();
		} 
		catch (IOException e) {
			Util.programInfo("Failed To Concat To File: " + file.getName());
			return null;
		}
		
		return file;
	}
	
	
	public synchronized static boolean addStoredLine(File file, String serverID)
	{
		String concat = Util.FILE_APPEND + serverID + "\n";
		
		try {
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bf = new BufferedWriter(fw);	
			bf.write(concat); 
			bf.close();
		} 
		catch (IOException e) {
			Util.programInfo("Failed To Concat To File: " + file.getName());
			return false;
		}
		
		return true;
	}
	
	public synchronized static void deleteLocalFiles(String folder, String fileID) {
		File localInfoFolder = new File(folder);
		File[] files = localInfoFolder.listFiles();
		
		for(File child : files)
		{
			String fileName = child.getName();
			String[] args1 = fileName.split(Util.SEPARATOR);
			String[] args2 = fileName.split("\\.");
			String ext = "." + args2[1];
 			
			if(args1[0].equals(fileID) && ext.equals(Util.INFO_EXTENSION)){
				while(child.exists())
					child.delete();
			}
		}	
	}
	
	public synchronized static void deleteLocalFile(String folder, String fileID, String chunkNo)
	{
		String infoFile = createInfoPath(folder, fileID, chunkNo);
		File file = new File(infoFile);
		file.delete();
	}
	
	public synchronized static long getLocalFolderSize(String folder)
	{
		long folderLength = 0;
		File file = new File(folder);
		File[] files = file.listFiles();
		
		for(File child : files) {
			String[] args = child.getName().split("\\.");
			
			if(args.length == 2){
				String ext = "." + args[1];
				
				if(ext.equals(Util.INFO_EXTENSION))	
					folderLength += child.length();
			}
		}
		
		return folderLength;
	}
	
	public synchronized static FileStateInfo deleteInfoAboutPeer(File file, String fileId, int chunkNo, String peerId)
	{
		//Creating header and tmp file
		String info = Util.FILE_APPEND + peerId; 
		File tmp = new File(file.getParent() + File.separator + fileId + "-" + chunkNo + "-tmp" + Util.INFO_EXTENSION);
		BufferedReader bir;
		BufferedWriter bow;
		
		int replication = 0;
		int storedlines = 0;
		
		try {
			//Opening files
			bir = new BufferedReader(new FileReader(file));
			bow = new BufferedWriter(new FileWriter(tmp));
			String temp = "";
			//Read line to check header
			temp = bir.readLine();
			
			if(temp == null) {
				bir.close();
				bow.close();
				tmp.delete();
				return null;
			}
			
			String[] tempSplit = temp.split(Util.FILE_HEADER);
			if(tempSplit.length != 2 || !tempSplit[0].equals("") || !Util.isInteger(tempSplit[1])) {
				bir.close();bow.close();tmp.delete();
				return null;
			}
			
			bow.write(temp + "\n");	
			replication = Integer.parseInt(tempSplit[1]);
			//Check line by line
			while((temp = bir.readLine()) != null) {
				tempSplit = temp.split(Util.FILE_APPEND);
				if(tempSplit.length == 2 && tempSplit[0].equals(""))
				{
					if(!temp.equals(info))
					{
						bow.write(temp + "\n");
						storedlines++;
					}
				}
			}
			//Close rename and delete
			bir.close();
			bow.close();
			file.delete();
			tmp.renameTo(file);
		
			FileStateInfo f = new FileStateInfo(replication, storedlines);
			return f;
		} 
		catch (IOException e) {
			tmp.delete();
			return null;
		}
	}
	
	public static FileStateInfo getInfo(File file)
	{
		//Testing first args 
		String[] args = file.getName().split(Util.SEPARATOR);
		if(args.length != 2 || args[0].length() != Message.SHA_BYTES) return null;
		//Testing second args
		String[] args2 = args[1].split("\\.");
		if(args2.length != 2)return null;
		
		String tmpExtension = "." + args2[1];
		if(!Util.isInteger(args2[0]) || !tmpExtension.equals(Util.INFO_EXTENSION)) return null;
		//Reading
		BufferedReader bir;
		
		String name = args[0];
		String chunkNo = args2[0];
		int replication;
		int storedlines = 0;
		
		try {
			//Opening files
			bir = new BufferedReader(new FileReader(file));
			String temp = "";
			//Read line to check header
			temp = bir.readLine();
			
			if(temp == null) {
				bir.close();
				return null;
			}
			
			String[] tempSplit = temp.split(Util.FILE_HEADER);
			if(tempSplit.length != 2 || !tempSplit[0].equals("") || !Util.isInteger(tempSplit[1])) {
				bir.close();
				return null;
			}
			
			replication = Integer.parseInt(tempSplit[1]);
			//Check line by line
			while((temp = bir.readLine()) != null) {
				tempSplit = temp.split(Util.FILE_APPEND);
				if(tempSplit.length == 2 && tempSplit[0].equals(""))
					storedlines++;
			}	
			//Close rename and delete
			bir.close();
		} 
		catch (IOException e){
			e.printStackTrace();
			return null;
		}

		String backupPath = FileBackup.createChunkPath(file.getParent(), name, chunkNo);
		File backup = new File(backupPath);
		long size;
		
		if(backup.exists())
			size = backup.length();
		else
			size = -1;
		
		//Checking if has path for file
		String pathPath = FilePath.createFilePathPath(file.getParent(), name, Integer.parseInt(chunkNo));
		File path = new File(pathPath);
		String pathname;
		
		if(path.exists())
			pathname = FilePath.readPath(path);
		else
			pathname = null;

		FileStateInfo f = new FileStateInfo(replication, storedlines,name, chunkNo, size, pathname);
		return f;	
	}
	
	public static String createInfoPath(String folder, String fileID, int chunkNo){
		return folder + File.separatorChar + fileID + Util.SEPARATOR + chunkNo + Util.INFO_EXTENSION;
	}
	
	public static String createInfoPath(String folder, String fileID, String chunkNo){
		return folder + File.separatorChar + fileID + Util.SEPARATOR + chunkNo + Util.INFO_EXTENSION;
	}
	
}
