package localSubprotocols;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import message.Message;
import message.MessageType;
import peer.Peer;
import util.Util;
import fileManager.FileEncrypt;
import fileManager.FileInfo;
import fileManager.FilePath;

public class BackupLocal extends Thread
{
	private File file;
	private int replication;
	private Peer peerInfo;
	private String fileID;
	
	public BackupLocal(File file, int replication, Peer peerInfo){
		this.file = file;
		this.replication = replication;
		this.peerInfo = peerInfo;
		this.fileID = FileEncrypt.encrypt(this.file, this.peerInfo);	
	}
	
	@Override
	public void run() 
	{
		//Check if all paramethers are initialized
		if(!this.checkParameters()){
			Util.programInfo("Invalid Parameters For PUTCHUNK File: " + this.file.getName()); 
			return;
		}
			
		//Start putting the chunk
		if(!this.enterLoopProcess())
		{
			Util.programInfo("Failed To PUTCHUNK File: " + this.file.getName());
		}
		else{
			Util.programInfo("PUTCHUNK Complete For File: " + this.file.getName());
		}	
	}
	
	private boolean checkParameters() 
	{
		if(this.file == null || this.replication < 1 || this.peerInfo == null || this.fileID == null)
			return false;
		else return true;
	}

	private boolean enterLoopProcess()
	{
		Util.programInfo("PUTCHUNK: Trying To Backup " + this.file.getName());
		//Declaring local variables
		BufferedInputStream bis = Util.openBufferInputStream(this.file);
		byte[] chunk;
		int lastRead = 0;
		int chunkNo = 0;	
		//Opening buffered input stream
		if(bis == null)
		{
			Util.programInfo("File Not Found: " + this.file.getName());
			Util.closeBufferInputStream(bis); return false;
		}
		//Constant reading while not EOF	
		while(true)
		{
			chunk = Util.readFromBuffer(bis, this.peerInfo.getChunkSize());
			//If invalid chunk, return false
			if(chunk == null)
			{
				Util.programInfo("Failed To Retrieve Info From: " + this.file.getName());
				Util.closeBufferInputStream(bis); 
				return false;
			}
			
			if(chunk.length <= 0) break;
			//Trying to backup chunk and receive stored
			if(!this.putchunkProcess(chunk,  chunkNo)) 
			{
				Util.closeBufferInputStream(bis); 
				return false;
			}
			
			Util.programInfo("PUTCHUNK: Success, Chunk Number " + chunkNo + 
					", For File: " + this.file.getName());		
			//Update variables
			lastRead = chunk.length;
			chunkNo++;
		}
		
		//If last chunk has same chunk bytes size, create another chunk with 0 length
		if(lastRead == this.peerInfo.getChunkSize())
		{
			chunk = new byte[0];
			//Trying to backup chunk and receive stored
			if(!this.putchunkProcess(chunk, chunkNo)) 
			{
				Util.closeBufferInputStream(bis); 
				return false;
			}
		}
		//In case reaches here, eveything worked fine
		Util.closeBufferInputStream(bis);
		return true;
	}
	
	private boolean putchunkProcess(byte[] chunk, int chunkNo)
	{
		//Setting local variables
		int receiveTime = this.peerInfo.getConfirmationTime();
		String storedChunkPath = FileInfo.createInfoPath(this.peerInfo.getLocalInfoFolder(), this.fileID, chunkNo);
		String pathFileInfo = FilePath.createFilePathPath(this.peerInfo.getLocalInfoFolder(), this.fileID, chunkNo);
		DatagramPacket packet = this.createPacket(chunk, chunkNo);

		if(packet == null)
		{
			Util.programInfo("Failed to Create Packet: " + this.file.getName());
			return false;
		}

		//Inializing loop with MaxRetries
		for(int i = 1; i <= this.peerInfo.getMaxTries(); i++)
		{	
			//Create info file for chunk
			File storedChunkInfo = FileInfo.createStored(storedChunkPath, replication);
			boolean create = FilePath.createPathFile(pathFileInfo, this.file.getPath());
			if(storedChunkInfo == null || !create)
			{
				Util.programInfo("Failed To Create File Info Controller: " + storedChunkPath);
				return false;
			}	
			
			//for info purpose
			Util.programInfo("PUTCHUNK: Attempt, Chunk Number " + chunkNo + ", For File: " + this.file.getName() + ". Attempt Number " + i);
					
			//this.peerInfo.getSocket().send(msg.getPacket());
			if(this.sendAndReceive(packet, receiveTime, storedChunkInfo, chunkNo))
				return true;
			
			Util.programInfo("PUTCHUNK: Failed,  Chunk Number " + chunkNo + ", For File: " + this.file.getName() + ". Attempt Number " + i);
			//Prepare next time
			receiveTime = 2 * receiveTime;
		}
		
		//deleting because failed
		File f = new File(storedChunkPath);
		f.delete();
		
		File f2 = new File(pathFileInfo);
		f2.delete();
		
		return false;
	}
	
	private boolean sendAndReceive(DatagramPacket packet, int receiveTime, File monitorFile, int chunkNo)
	{	
		int replies = 0;
		long lastTime;
		MulticastSocket mc;
		
		//Creating multicast socket and joining	
		mc = Util.createMulticastSocket(this.peerInfo.getMulticastChannelControlAddress(), this.peerInfo.getMulticastChannelControlPort());
		if(mc == null)
		{
			Util.programInfo("Failed To Join Multicast Group Control: " + this.file.getName());
			return false;
		}
		
		//Sending
		if(!Util.sendPacket(this.peerInfo.getSocket(), packet))
		{
			Util.programInfo("Failed To Send File From Backup, Socket Send Failed: " + this.file.getName());
			return false;
		}
		
		//The royal loop
		lastTime = System.currentTimeMillis() + receiveTime;
		long newReceiveTime;
		while(System.currentTimeMillis() < lastTime)
		{
			//Attempt to receive
			newReceiveTime = lastTime - System.currentTimeMillis();
			DatagramPacket packetReceive = Util.receiveFromMulticast(mc, (int) newReceiveTime, this.peerInfo.getControlPackage());
				
			//Decrypt package
			Message msg = new Message(packetReceive);
			//If msg is valid, if type stored, if fileID the same as this...add line to file
			if(msg.isValid() && msg.getMessageType().equals(MessageType.STORED) && msg.getFileID().equals(fileID) && msg.getChunkNo() == chunkNo)
			{
				FileInfo.addStoredLine(monitorFile, msg.getSenderID());
				replies++;
			}
		}
		
		//close socket
		mc.close();
		
		if(replies < this.replication)
			return false;
		else return true;	
	}
	
	
	private DatagramPacket createPacket(byte[] chunk, int chunkNo)
	{
		String[] args = new String[7];
		
		//create vailid message with artibutes
		args[0] = MessageType.PUTCHUNK.toString();
		args[1] = this.peerInfo.getProtocolVersion();
		args[2] = this.peerInfo.getServerID();
		args[3] = this.fileID;
		args[4] = Integer.toString(chunkNo);
		args[5] = Integer.toString(this.replication);
		args[6] = Message.CRLFCRLF;
		
		Message msg = new Message(args, chunk);
		
		if(!msg.isValid())
		{
			Util.programInfo("Message is not valid");
			return null;
		}
		
		//setting the address and port for paramethers
		msg.getPacket().setAddress(this.peerInfo.getMulticastChannelBackupAddress());
		msg.getPacket().setPort(this.peerInfo.getMulticastChannelBackupPort());
		
		return msg.getPacket();
	}
}
