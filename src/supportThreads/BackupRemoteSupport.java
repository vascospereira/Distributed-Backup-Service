package supportThreads;

import java.io.File;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import message.Message;
import message.MessageType;
import peer.Peer;
import fileManager.*;
import util.Util;

public class BackupRemoteSupport extends Thread {

	Peer peerInfo;
	Message msg;
	File infoFile;

	public BackupRemoteSupport(Peer peerInfo, Message msg, File infoFile) {
		this.peerInfo = peerInfo;
		this.msg = msg;
		this.infoFile = infoFile;
	}

	@Override
	public void run() {
		// Create socket
		MulticastSocket socket = Util.createMulticastSocket(this.peerInfo.getMulticastChannelControlAddress(),
				this.peerInfo.getMulticastChannelControlPort());
		if (socket == null) {
			Util.programInfo("Failed To Create Backup Remote Support Socket: " + this.msg.getFileID());
			return;
		}
		// Cicle
		long lastTime = System.currentTimeMillis() + this.peerInfo.getConfirmationTime();
		long receiveTime;

		while (System.currentTimeMillis() < lastTime) {
			receiveTime = lastTime - System.currentTimeMillis();
			DatagramPacket packet = Util.receiveFromMulticast(socket, (int) receiveTime,
					this.peerInfo.getControlPackage());
			Message newMsg = new Message(packet);

			// Receives remote and self stored
			if (newMsg.isValid() && newMsg.getMessageType().equals(MessageType.STORED)
					&& newMsg.getFileID().equals(msg.getFileID()) && !newMsg.getSenderID().equals(msg.getSenderID())
					&& !newMsg.getSenderID().equals(this.peerInfo.getServerID())) {
				FileInfo.addStoredLine(this.infoFile, newMsg.getSenderID());
			}
		}
		socket.close();
	}
}
