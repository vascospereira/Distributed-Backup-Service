package localSubprotocols;

import java.io.BufferedOutputStream;
import java.io.File;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import message.Message;
import message.MessageType;
import peer.Peer;
import util.*;
import fileManager.FileBackup;
import fileManager.FileEncrypt;

public class RestoreLocal extends Thread {
	private File file;
	private Peer peerInfo;

	public RestoreLocal(File file, Peer peerInfo) {
		this.file = file;
		this.peerInfo = peerInfo;
	}

	@Override
	public void run() {
		// create file
		String fileID = FileEncrypt.encrypt(this.file, this.peerInfo);
		String restoredFilePath = FileBackup.createRestorePath(this.peerInfo.getRestoreFolder(), this.file.getName());
		if (fileID == null || restoredFilePath == null) {
			Util.programInfo("Faile to retrieve fileId or path: " + this.file.getName());
			return;
		}

		// opening file for write
		BufferedOutputStream bos = Util.openBufferOutputStream(restoredFilePath);
		if (bos == null) {
			Util.programInfo("Failed to create BOS for restore: " + this.file.getName());
			return;
		}

		// File for delete if fail
		File restored = new File(restoredFilePath);

		// creating mc
		MulticastSocket mc = Util.createMulticastSocket(this.peerInfo.getMulticastChannelRestoreAddress(),
				this.peerInfo.getMulticastChannelRestorePort());
		if (mc == null) {
			Util.programInfo("Failed to create MC socket for Restore: " + this.file.getName());
			restored.delete();
			return;
		}

		// loop for restoring
		int chunkNo = 0;
		while (true) {
			// create getchunk
			DatagramPacket packet = this.createPacket(fileID, chunkNo);
			if (packet == null) {
				Util.programInfo("Failed to create GETCHUNK: " + this.file.getName());
				restored.delete();
				return;
			}

			// send packet
			if (!Util.sendPacket(this.peerInfo.getSocket(), packet)) {
				Util.programInfo("Faile to send GETCHUNK: " + this.file.getName());
				restored.delete();
				return;
			}

			// receive packet CHUNK
			DatagramPacket packet2 = Util.receiveFromMulticast(mc, this.peerInfo.getConfirmationTime(),
					this.peerInfo.getChunkPackage());
			if (packet2 == null) {
				Util.programInfo(
						"Failed to Restore file, no other Peers seem to have it backed up: " + this.file.getName());
				restored.delete();
				return;
			}

			Message msg = new Message(packet2);

			if (msg.isValid() && msg.getMessageType().equals(MessageType.CHUNK) && msg.getFileID().equals(fileID)
					&& msg.getChunkNo() == chunkNo) {

				Util.writeToBuffer(bos, msg.getBody());

				Util.programInfo("Received Chunk: " + chunkNo + ", " + msg.getBody().length);
				if (msg.getBody().length < this.peerInfo.getChunkSize())
					break;
				chunkNo++;

			}

		}

		// close file
		Util.closeBufferOutputStram(bos);
		Util.programInfo("RESTORE: Finished backup: " + restoredFilePath);

	}

	private DatagramPacket createPacket(String fileID, int chunkNo) {

		String[] args = new String[6];

		// create vailid message with artibutes
		args[0] = MessageType.GETCHUNK.toString();
		args[1] = this.peerInfo.getProtocolVersion();
		args[2] = this.peerInfo.getServerID();
		args[3] = fileID;
		args[4] = Integer.toString(chunkNo);
		args[5] = Message.CRLFCRLF;

		Message msg = new Message(args, null);

		if (!msg.isValid()) {
			Util.programInfo("Message is not valid");
			return null;
		}

		// setting the address and port for paramethers
		msg.getPacket().setAddress(this.peerInfo.getMulticastChannelControlAddress());
		msg.getPacket().setPort(this.peerInfo.getMulticastChannelControlPort());

		return msg.getPacket();
	}
}
