package fileManager;

public class FileStateInfo {
	private int replication;
	private int peers;
	private String fileID;
	private String chunkNo;
	private long backupSizeFile;
	private String path;

	public FileStateInfo(int rep, int peers) {
		this.replication = rep;
		this.peers = peers;
		this.fileID = null;
		this.chunkNo = null;
	}

	public FileStateInfo(int rep, int peers, String fileID, String chunkNo, long backupSize, String path) {
		this.replication = rep;
		this.peers = peers;
		this.fileID = fileID;
		this.chunkNo = chunkNo;
		this.backupSizeFile = backupSize;
		this.path = path;
	}

	public int getReplication() {
		return replication;
	}

	public int getPeers() {
		return peers;
	}

	@Override
	public String toString() {
		return "Replication: " + this.replication + ", Peers: " + this.peers;
	}

	public String getFileID() {
		return fileID;
	}

	public void setFileID(String fileID) {
		this.fileID = fileID;
	}

	public String getChunkNo() {
		return chunkNo;
	}

	public void setChunkNo(String chunkNo) {
		this.chunkNo = chunkNo;
	}

	public long getBackupSizeFile() {
		return backupSizeFile;
	}

	public String getPath() {
		return path;
	}
}
