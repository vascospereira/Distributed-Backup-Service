package remoteSubprotocols;

import java.io.File;
import java.net.DatagramPacket;
import message.Message;
import message.MessageType;
import peer.Peer;
import supportThreads.BackupRemoteSupport;
import fileManager.FileBackup;
import fileManager.FileInfo;
import util.Util;

public class BackupRemote extends Thread
{
	private Message msg;
	private Peer peerInfo;	
	
	public BackupRemote(Message msg, Peer peerInfo) 
	{
		this.msg = msg;
		this.peerInfo = peerInfo;
	}
	
	@Override
	public void run() 
	{
		//Creating paths
		String infoPath = FileInfo.createInfoPath(this.peerInfo.getBackupFolder(), this.msg.getFileID(), this.msg.getChunkNo());
		String chunkPath = FileBackup.createChunkPath(this.peerInfo.getBackupFolder(), this.msg.getFileID(), this.msg.getChunkNo());
	
		//If i am the owner of the chunk, i do not store it, from reclaim	 
		if(this.ownerOfChunk())
		{
			Util.programInfo("PUTCHUNK: Receiving Chunk Which I am The Owner, Not Storing, Just Reseting Info");
			
			String infoPathLocal = FileInfo.createInfoPath(this.peerInfo.getLocalInfoFolder(), this.msg.getFileID(), this.msg.getChunkNo());
			File infoFile = FileInfo.createStored(infoPathLocal, msg.getReplicationDeg());
			
			if(infoFile == null){
				Util.programInfo("Failed To Create Info File For Remote Backup: " + infoPath);
				return;
			}
			
			FileInfo.addStoredLine(infoFile, this.msg.getSenderID());
			
			Thread support = new BackupRemoteSupport(this.peerInfo, this.msg, infoFile);
			support.start();
			return;
		}
		
		if(!this.canBackup())
		{
			Util.programInfo("Cannot Backup Remote Chunk, Dedicated Space Limit Size Reached.");
			return;
		}
		
		//creating monitoring file
		File info = FileInfo.createStored(infoPath, msg.getReplicationDeg());
		if(info == null)
		{
			Util.programInfo("Failed to Create Info file for Remote Backup: " + infoPath);
			return;
		}
		
		//Save info in me first, Always
		FileInfo.addStoredLine(info, this.peerInfo.getServerID());
		
		//Creating chunk file if don't have
		if(!this.alreadyHaveChunk())
		{
			File chunk = FileBackup.createChunk(chunkPath, msg.getBody());
			if(chunk == null)
			{
				Util.programInfo("Failed To Chunk File For Remote Backup: " + chunkPath);
				return;
			}
		}
		else{
			Util.programInfo("PUTCHUNK: Already Have Chunk, Resetting Info");
		}

		//Generating the random time that it will wait to send
		int sendTime = this.peerInfo.getRandom().nextInt(this.peerInfo.getRandomTime());
		
		//Initializing support thread
		Thread support = new BackupRemoteSupport(this.peerInfo, this.msg, info);
		support.start();
		
		//Sleeping
		if(!this.sleeping(sendTime))
		{
			Util.programInfo("Failed To Sleep: " + this.msg.getFileID());
			return;
		}
		
		DatagramPacket packet = this.createPacket();
		Util.sendPacket(this.peerInfo.getSocket(), packet);	
	}
	
	
	
	
	private boolean sleeping(long time)
	{
		try {
			Thread.sleep(time);
			return true;
		} 
		catch (InterruptedException e) {
			return false;
		}
	}
	//STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	private DatagramPacket createPacket()
	{
		String[] args = new String[6];
		
		args[0] = MessageType.STORED.toString();
		args[1] = this.peerInfo.getProtocolVersion();
		args[2] = this.peerInfo.getServerID();
		args[3] = this.msg.getFileID();
		args[4] = Integer.toString(this.msg.getChunkNo());
		args[5] = Message.CRLFCRLF;
		
		Message msg = new Message(args, null);
		
		if(!msg.isValid())
			return null;
		else{
			msg.getPacket().setAddress(this.peerInfo.getMulticastChannelControlAddress());
			msg.getPacket().setPort(this.peerInfo.getMulticastChannelControlPort());
			return msg.getPacket();
		}
		
	}
	
	private boolean canBackup()
	{
		long total = FileBackup.getBackupFolderSize(this.peerInfo.getBackupFolder()) + FileInfo.getLocalFolderSize(this.peerInfo.getBackupFolder());
		
		if(this.peerInfo.getDedicatedSpace() - total >= this.msg.getBody().length)
			return true;
		else return false;
	}
	
	private boolean ownerOfChunk()
	{
		String infoPath = FileInfo.createInfoPath(this.peerInfo.getLocalInfoFolder(), this.msg.getFileID(), this.msg.getChunkNo());
		File file = new File(infoPath);
		
		return file.exists();	
	}
	
	private boolean alreadyHaveChunk()
	{
		String chunkPath = FileBackup.createChunkPath(this.peerInfo.getBackupFolder(), this.msg.getFileID(), this.msg.getChunkNo());
		File file = new File(chunkPath);
		
		return file.exists();
	}
}
 