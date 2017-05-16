package remoteSubprotocols;

import message.Message;
import peer.Peer;
import util.*;
import fileManager.FileBackup;

public class DeleteRemote extends Thread {
	private Message msg;
	private Peer peerInfo;

	public DeleteRemote(Message msg, Peer peerInfo) {
		this.msg = msg;
		this.peerInfo = peerInfo;
	}

	@Override
	public void run() {
		// Delete local info files (.info) and Delete chunks (.ck's)
		FileBackup.deleteBackupFiles(this.peerInfo.getBackupFolder(), msg.getFileID());
		Util.programInfo("DELETE: Delete Finished");
	}
}
