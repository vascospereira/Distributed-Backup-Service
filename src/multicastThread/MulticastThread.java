package multicastThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;

import remoteSubprotocols.*;
import message.*;
import peer.Peer;
import util.Util;


public class MulticastThread extends Thread
{
	private DatagramSocket socket;
	private MulticastSocket mcSocket;
	private MulticastThreadType type;
	private Peer peerInfo;
	
	public MulticastThread(MulticastThreadType type,  DatagramSocket socket, MulticastSocket mcSocket, Peer peerInfo)
	{
		this.type = type;
		this.peerInfo = peerInfo;
		this.socket = socket;
		this.mcSocket = mcSocket;
	}
	
	@Override
	public void run() 
	{
		while(true){
			byte[] buff = new byte[this.peerInfo.getChunkPackage()];
			DatagramPacket packet = new DatagramPacket(buff, buff.length);
			
			try {
				mcSocket.receive(packet);
			}catch (IOException e){}
			
			if(this.socket.isClosed() || this.mcSocket.isClosed())
				break;
			
			this.handle(packet);
		}
	}	

	
	private void handle(DatagramPacket packet)
	{
		switch(this.type){	
			case CONTROL: this.handleControl(packet); 
				break;	
			case BACKUP: this.handleBackup(packet); 
				break;	
			case RESTORE: this.handleRestore(packet); 
				break;	
			default: 
				break;		
		}
	}
	
	private void handleControl(DatagramPacket packet)
	{
		Message msg = new Message(packet);
		
		if(msg.isValid() && !msg.getSenderID().equals(this.peerInfo.getServerID()))
		{
			switch(msg.getMessageType())
			{
				case GETCHUNK: this.initializeRemoteRestore(msg); 
					break;
				case DELETE: this.initializeRemoteDelete(msg); 
					break;
				case REMOVED: this.intializeRemoteReclaim(msg); 
					break;
				default: 
					break;
			}
		}
	}
	
	private void handleBackup(DatagramPacket packet)
	{
		Message msg = new Message(packet);

		if(msg.isValid() && !msg.getSenderID().equals(this.peerInfo.getServerID()))
		{	
			switch(msg.getMessageType())
			{
				case PUTCHUNK: this.initializeRemoteBackup(msg); 
					break;
				default: 
					break;
			}	
		}
	}
	
	private void handleRestore(DatagramPacket packet){}
	
	private void initializeRemoteBackup(Message msg)
	{
		Util.programInfo("BACKUP: Receiving Remote Backup, Body Size: " + msg.getPacket().getLength());
		//initialting Backup Remote
		Thread backupRemote = new BackupRemote(msg, this.peerInfo);
		backupRemote.start();	
	}
	
	private void initializeRemoteRestore(Message msg)
	{
		Util.programInfo("RESTORE: Receiving Remote Restore, Size: " + msg.getPacket().getLength());
		
		Thread restoreRemote = new RestoreRemote(msg, this.peerInfo);
		restoreRemote.start();
	}
	
	private void initializeRemoteDelete(Message msg)
	{
		Util.programInfo("DELETE: Receiving Delete");
		
		Thread deleteRemote = new DeleteRemote(msg, this.peerInfo);
		deleteRemote.start();
	}
	
	private void intializeRemoteReclaim(Message msg)
	{
		Util.programInfo("REMOVED: Receiving Reclaim");
		
		Thread remoteReclaim = new ReclaimRemote(msg, this.peerInfo);
		remoteReclaim.start();
	}
}
