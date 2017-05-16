package localSubprotocols;

import java.io.File;
import java.net.DatagramPacket;

import message.Message;
import message.MessageType;
import peer.Peer;
import util.*;
import fileManager.*;

public class DeleteLocal extends Thread 
{
	private File file;
	private Peer peerInfo;
	
	public DeleteLocal(File file, Peer peerInfo)
	{
		this.file = file;
		this.peerInfo = peerInfo;
	}
	
	@Override
	public void run()
	{
		Util.programInfo("DELETE: Deleting Remote Back Up File: " + this.file.getName());
		//Create fileID
		String fileID = FileEncrypt.encrypt(this.file, this.peerInfo);
		//Delete Info Files
		FileInfo.deleteLocalFiles(this.peerInfo.getLocalInfoFolder(), fileID);
		//Delete file path files
		FilePath.deletePathFiles(this.peerInfo.getLocalInfoFolder(), fileID);
		//Create packet
		DatagramPacket packet = this.createPacket(fileID);
		//Send
		Util.sendPacket(this.peerInfo.getSocket(), packet);
		Util.programInfo("DELETE: Back Up File " + this.file.getName() + " Delete Completed.");
	}
	
	private DatagramPacket createPacket(String fileID)
	{
		String[] args = new String[5];
		//Create valid message with attributes
		args[0] = MessageType.DELETE.toString();
		args[1] = this.peerInfo.getProtocolVersion();
		args[2] = this.peerInfo.getServerID();
		args[3] = fileID;
		args[4] = Message.CRLFCRLF;
		
		Message msg = new Message(args, null);
		
		if(!msg.isValid())
		{
			Util.programInfo("Message Is Not Valid");
			return null;
		}
		//setting the address and port for paramethers
		msg.getPacket().setAddress(this.peerInfo.getMulticastChannelControlAddress());
		msg.getPacket().setPort(this.peerInfo.getMulticastChannelControlPort());
		
		return msg.getPacket();
	}
}
