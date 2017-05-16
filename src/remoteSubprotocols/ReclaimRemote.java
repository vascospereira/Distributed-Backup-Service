package remoteSubprotocols;

import java.io.BufferedInputStream;
import java.io.File;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import message.Message;
import message.MessageType;
import peer.Peer;
import util.*;
import fileManager.FileBackup;
import fileManager.FileInfo;
import fileManager.FileStateInfo;

public class ReclaimRemote extends Thread {

	private Message msg;
	private Peer peerInfo;

	public ReclaimRemote(Message msg, Peer peerInfo) {
		this.msg = msg;
		this.peerInfo = peerInfo;
	}

	@Override
	public void run() {
		// create the paths and the files
		String path = FileInfo.createInfoPath(this.peerInfo.getBackupFolder(), this.msg.getFileID(),
				this.msg.getChunkNo());
		String backupPath = FileBackup.createChunkPath(this.peerInfo.getBackupFolder(), this.msg.getFileID(),
				this.msg.getChunkNo());
		File file = new File(path);
		File backupFile = new File(backupPath);
		FileStateInfo info;

		// Verificar se o ficheiro existe
		if (this.amIOwner()) {
			// if i am owner just return, backup will do everything for me
			return;
		}

		if (!file.exists() || !backupFile.exists()) {
			Util.programInfo(
					"RECLAIM: Cancelling for nonexisting: " + this.msg.getChunkNo() + ", " + this.msg.getFileID());
			this.syncTheInfoFileIfNotHave(file);
			return;
		}

		// Atualize the info file
		if ((info = FileInfo.deleteInfoAboutPeer(file, this.msg.getFileID(), this.msg.getChunkNo(),
				this.msg.getSenderID())) == null) {
			Util.programInfo("RECLAIM: No info about the chunk" + this.msg.getSenderID() + " in chunk: "
					+ this.msg.getChunkNo() + ", " + this.msg.getFileID());
			return;
		}

		// verify if stored is bigger or equal
		if (info.getReplication() <= info.getPeers()) {
			// no need to continue cause it still has ammount of peers for the
			// replication
			return;
		}

		// start time
		int time = this.peerInfo.getRandom().nextInt(this.peerInfo.getRandomTime());
		long lastTime = System.currentTimeMillis() + time;
		int newTime;
		// esperar esse tempo
		MulticastSocket socket = Util.createMulticastSocket(this.peerInfo.getMulticastChannelBackupAddress(),
				this.peerInfo.getMulticastChannelBackupPort());

		while (System.currentTimeMillis() < lastTime) {
			newTime = (int) (lastTime - System.currentTimeMillis());
			DatagramPacket packet = Util.receiveFromMulticast(socket, newTime, this.peerInfo.getChunkPackage());

			Message msg = new Message(packet);

			if (msg.isValid() && !msg.getSenderID().equals(this.peerInfo.getServerID())
					&& msg.getMessageType() == MessageType.PUTCHUNK && msg.getFileID().equals(this.msg.getFileID())
					&& msg.getChunkNo() == this.msg.getChunkNo())

			{
				// Putchunk was already sent, but needs to add the sender as
				// well to the info file
				this.syncTheInfoFile(file, msg.getSenderID());
				return;
			}
		}

		// putchunk

		BufferedInputStream bis = Util.openBufferInputStream(backupFile);
		byte[] chunk = Util.readFromBuffer(bis, (int) backupFile.length());
		Util.closeBufferInputStream(bis);

		if (!this.putchunkProcess(chunk, info.getReplication())) {
			Util.programInfo("PUTCHUNK: Failed to putchunk in reclaim");
		}
	}

	private boolean putchunkProcess(byte[] chunk, int replication) {
		// setting local variables
		int receiveTime = this.peerInfo.getConfirmationTime();
		String storedChunkPath = FileInfo.createInfoPath(this.peerInfo.getBackupFolder(), this.msg.getFileID(),
				this.msg.getChunkNo());
		DatagramPacket packet = this.createPacket(chunk, replication);

		if (packet == null) {
			Util.programInfo("Failed to Create Packet: " + this.msg.getFileID() + "-" + this.msg.getChunkNo());
			return false;
		}

		// inializing loop with MAXRETRIES
		for (int i = 1; i <= this.peerInfo.getMaxTries(); i++) {
			// create info file for chunk
			File storedChunkInfo = FileInfo.createStored(storedChunkPath, replication);
			if (storedChunkInfo == null) {
				Util.programInfo("Failed to create File info controller: " + storedChunkPath);
				return false;
			}

			FileInfo.addStoredLine(storedChunkInfo, this.peerInfo.getServerID());

			// for info purpose
			Util.programInfo("PUTCHUNK: Attempt, chunk number " + this.msg.getChunkNo() + ", for file: "
					+ this.msg.getFileID() + ". Attempt number " + i);

			// this.peerInfo.getSocket().send(msg.getPacket());
			if (this.sendAndReceive(packet, receiveTime, storedChunkInfo, replication))
				return true;

			// for info purpose
			Util.programInfo("PUTCHUNK: Failed,  chunk number " + this.msg.getChunkNo() + ", for file: "
					+ this.msg.getFileID() + ". Attempt number " + i);

			// prepare next time
			receiveTime = 2 * receiveTime;
		}

		// deleting because failed
		File f = new File(storedChunkPath);
		f.delete();

		return false;

	}

	private boolean sendAndReceive(DatagramPacket packet, int receiveTime, File monitorFile, int replication) {
		// setting variables
		int replies = 0;
		long lastTime;
		MulticastSocket mc;

		// creating multicast socket and joining
		mc = Util.createMulticastSocket(this.peerInfo.getMulticastChannelControlAddress(),
				this.peerInfo.getMulticastChannelControlPort());
		if (mc == null) {
			Util.programInfo("Failed to join Multicast Group Control: " + this.msg.getFileID());
			return false;
		}

		// Sending
		if (!Util.sendPacket(this.peerInfo.getSocket(), packet)) {
			Util.programInfo("Failed to send file from Backup, socket send failed: " + this.msg.getFileID());
			return false;
		}

		// the royal looping
		lastTime = System.currentTimeMillis() + receiveTime;
		long newReceiveTime;
		while (System.currentTimeMillis() < lastTime) {
			// Attempt to receive
			newReceiveTime = lastTime - System.currentTimeMillis();
			DatagramPacket packetReceive = Util.receiveFromMulticast(mc, (int) newReceiveTime,
					this.peerInfo.getControlPackage());

			// Decrypt package
			Message msg = new Message(packetReceive);

			// if msg is valid, if type stored, if fileID the same as this...add
			// line to file
			if (msg.isValid() && msg.getMessageType().equals(MessageType.STORED)
					&& msg.getFileID().equals(this.msg.getFileID()) && msg.getChunkNo() == this.msg.getChunkNo()) {
				FileInfo.addStoredLine(monitorFile, msg.getSenderID());
				replies++;
			}

		}

		// close socket
		mc.close();

		// replies + 1 because i have one as well
		if ((replies + 1) < replication)
			return false;
		else
			return true;
	}

	private DatagramPacket createPacket(byte[] chunk, int replication) {
		String[] args = new String[7];

		// create vailid message with artibutes
		args[0] = MessageType.PUTCHUNK.toString();
		args[1] = this.peerInfo.getProtocolVersion();
		args[2] = this.peerInfo.getServerID();
		args[3] = this.msg.getFileID();
		args[4] = Integer.toString(this.msg.getChunkNo());
		args[5] = Integer.toString(replication);
		args[6] = Message.CRLFCRLF;

		Message msg = new Message(args, chunk);

		if (!msg.isValid()) {
			Util.programInfo("Message is not valid");
			return null;
		}

		// setting the address and port for paramethers
		msg.getPacket().setAddress(this.peerInfo.getMulticastChannelBackupAddress());
		msg.getPacket().setPort(this.peerInfo.getMulticastChannelBackupPort());

		return msg.getPacket();
	}

	// Risky function, supose if i receive a putchunk in 1 sec it is not from
	// the owner, but from a peer with backup of it.
	private boolean syncTheInfoFileIfNotHave(File file) {
		MulticastSocket socket = Util.createMulticastSocket(this.peerInfo.getMulticastChannelBackupAddress(),
				this.peerInfo.getMulticastChannelBackupPort());

		int time = this.peerInfo.getRandom().nextInt(this.peerInfo.getConfirmationTime());
		long lastTime = System.currentTimeMillis() + time;
		int newTime;

		while (System.currentTimeMillis() < lastTime) {
			newTime = (int) (lastTime - System.currentTimeMillis());
			DatagramPacket packet = Util.receiveFromMulticast(socket, newTime, this.peerInfo.getChunkPackage());

			Message msg = new Message(packet);

			if (msg.isValid() && !msg.getSenderID().equals(this.peerInfo.getServerID())
					&& msg.getMessageType() == MessageType.PUTCHUNK && msg.getFileID().equals(this.msg.getFileID())
					&& msg.getChunkNo() == this.msg.getChunkNo())

			{
				// Putchunk was already sent, but needs to add the sender as
				// well to the info file
				if (!this.syncTheInfoFile(file, msg.getSenderID())) {
					Util.programInfo("RECLAIM: Putchunk did not succeed, dit not sync");
				}
				return true;
			}
		}

		return false;
	}

	private boolean amIOwner() {
		String path = FileInfo.createInfoPath(this.peerInfo.getLocalInfoFolder(), this.msg.getFileID(),
				this.msg.getChunkNo());

		File file = new File(path);

		return file.exists();

	}

	private boolean syncTheInfoFile(File file, String senderID) {
		try {
			Thread.sleep(this.peerInfo.getConfirmationTime());
		} catch (InterruptedException e) {
		}

		if (file.exists()) {
			FileInfo.addStoredLine(file, senderID);
			return true;
		} else
			return false;
	}

}
