package remoteSubprotocols;

import java.io.File;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import message.Message;
import message.MessageType;
import peer.Peer;
import util.*;
import fileManager.FileBackup;

public class RestoreRemote extends Thread
{
	
	private Message msg;
	private Peer peerInfo;	
	
	public RestoreRemote(Message msg, Peer peerInfo) 
	{
		this.msg = msg;
		this.peerInfo = peerInfo;
	}
	
	@Override
	public void run()
	{
		//procurar chunk
		String chunkPath = FileBackup.createChunkPath(this.peerInfo.getBackupFolder(),this.msg.getFileID(), this.msg.getChunkNo());
		File file = new File(chunkPath);
		byte[] buff;
		
		//se tiver criar msg
		if(!file.exists()){Util.programInfo("RESTORE: File does not exist: " + chunkPath);return;} //nao existe
		
		//ler o chunk
		buff = FileBackup.readChunk(file); if(buff == null){Util.programInfo("RESTORE: Failed to read from chunk: " + chunkPath);return;}
		
		//criar datagrama
		DatagramPacket packet = createPacket(buff);
		if(packet == null){Util.programInfo("RESTORE: Failed to create CHUNK packet: " + chunkPath);return;}
		
		//criar socket
		MulticastSocket mc = Util.createMulticastSocket(this.peerInfo.getMulticastChannelRestoreAddress(), this.peerInfo.getMulticastChannelRestorePort());
		if(mc == null){Util.programInfo("RESTORE: Failed to create multicast socket"); return;}
		
		//generate receive time
		int receiveTime = this.peerInfo.getRandom().nextInt(this.peerInfo.getRandomTime());
		
		//esperar por rececao no MBR
		if(this.receiveChunkFromRestore(mc, receiveTime)) return;
		
				
		//se nao existe nada entao envia
		if(!Util.sendPacket(this.peerInfo.getSocket(), packet)){Util.programInfo("RESTORE: Failed to send packet");}
	}
	
	private DatagramPacket createPacket(byte[] chunk)
	{
		String[] args = new String[6];
		
		//create vailid message with artibutes
		args[0] = MessageType.CHUNK.toString();
		args[1] = this.peerInfo.getProtocolVersion();
		args[2] = this.peerInfo.getServerID();
		args[3] = this.msg.getFileID();
		args[4] = Integer.toString(msg.getChunkNo());
		args[5] = Message.CRLFCRLF;
		
		Message msg = new Message(args, chunk);
		
		if(!msg.isValid())
		{
			Util.programInfo("Message is not valid");
			return null;
		}
		
		//setting the address and port for paramethers
		msg.getPacket().setAddress(this.peerInfo.getMulticastChannelRestoreAddress());
		msg.getPacket().setPort(this.peerInfo.getMulticastChannelRestorePort());
		
		return msg.getPacket();
	}
	
	private boolean receiveChunkFromRestore(MulticastSocket mc, int receiveTime)
	{
		
		//creating times
		long lastTime = System.currentTimeMillis() + receiveTime;
		long newReceiveTime; 
		
		//initiating loop
		while(System.currentTimeMillis() < lastTime)
		{
			newReceiveTime = lastTime - System.currentTimeMillis();
			DatagramPacket packet = Util.receiveFromMulticast(mc, (int) newReceiveTime, this.peerInfo.getControlPackage());
			
			Message newMessage = new Message(packet);
			
			if(newMessage.isValid() 
					&& newMessage.getMessageType().equals(MessageType.CHUNK)
					&& newMessage.getFileID().equals(this.msg.getFileID())
					&& newMessage.getChunkNo() == this.msg.getChunkNo())
			{
				return true;
			}	
		}		
		return false;
	}
	
}
