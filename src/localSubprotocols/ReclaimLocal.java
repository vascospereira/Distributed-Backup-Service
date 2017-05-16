package localSubprotocols;

import java.io.File;
import java.net.DatagramPacket;

import peer.Peer;
import util.*;
import fileManager.FileBackup;
import fileManager.FileEncrypt;
import fileManager.FileInfo;
import message.*;

public class ReclaimLocal extends Thread {
	private Peer peerInfo;
	private int space;

	public ReclaimLocal(int space, Peer peerInfo) {
		this.peerInfo = peerInfo;
		this.space = space * 1000; // receives in KB, converts ro BYTES
	}

	@Override
	public void run() {
		Util.programInfo("RECLAIM: Trying to Reclaim");

		// set new space
		this.peerInfo.reclaimDedicatedSpace(this.space);

		// check if need to send removeds
		while (needsToRemove()) {
			Util.programInfo("RECLAIM: Backup size exceeded, trying to REMOVE, random chunk...");

			String[] arguments = this.getRandomFile();

			if (arguments != null && arguments.length == 2) {

				FileBackup.deleteBackupFile(this.peerInfo.getBackupFolder(), arguments[0], arguments[1]);
				FileInfo.deleteLocalFile(this.peerInfo.getBackupFolder(), arguments[0], arguments[1]);

				DatagramPacket packet = this.createPacket(arguments[0], arguments[1]);

				Util.sendPacket(this.peerInfo.getSocket(), packet);
			}

			try {
				Thread.sleep(this.peerInfo.getConfirmationTime());
			} catch (InterruptedException e) {
			}

		}

		Util.programInfo("RECLAIM: Reclaim Completed");
	}

	private boolean needsToRemove() {
		long total = FileBackup.getBackupFolderSize(this.peerInfo.getBackupFolder())
				+ FileInfo.getLocalFolderSize(this.peerInfo.getBackupFolder());

		if (this.peerInfo.getDedicatedSpace() - total < 0)
			return true;
		else
			return false;
	}

	private String[] getRandomFile() {
		String[] arguments = null;

		while (arguments == null) {
			File folder = new File(this.peerInfo.getBackupFolder());
			File[] files = folder.listFiles();

			if (files.length == 0)
				return null;

			int index = this.peerInfo.getRandom().nextInt(files.length);

			File file = files[index];

			if (!file.isDirectory()) {
				// check if refers to chunk
				String name = file.getName();
				String args[] = name.split(FileEncrypt.FIELD_SEPARATOR + "|\\.");

				if (args.length == 3) {
					String ext = "." + args[2];
					if (args[0].length() == Message.SHA_BYTES && Util.isInteger(args[1])
							&& (ext.equals(Util.CHUNK_EXTENSION) || ext.equals(Util.INFO_EXTENSION))) {
						arguments = new String[2];
						arguments[0] = args[0];
						arguments[1] = args[1];
					}
				}
			}
		}

		return arguments;
	}

	private DatagramPacket createPacket(String fileID, String chunkNo) {

		String[] args = new String[6];

		// create vailid message with artibutes
		args[0] = MessageType.REMOVED.toString();
		args[1] = this.peerInfo.getProtocolVersion();
		args[2] = this.peerInfo.getServerID();
		args[3] = fileID;
		args[4] = chunkNo;
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
