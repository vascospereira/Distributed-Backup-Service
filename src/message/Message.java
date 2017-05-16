package message;

import java.net.DatagramPacket;
import message.MessageType;

public class Message {

	public static final String CRLFCRLF = "\r\n\r\n";
	public static final int SHA_BYTES = 64;
	private DatagramPacket packet;
	private MessageType messageType;
	private String version;
	private String senderID;
	private String fileID;
	private int chunkNo;
	private int replicationDeg;
	private byte[] body;
	private boolean valid;

	/**
	 * 
	 * @param packet
	 */
	public Message(DatagramPacket packet) {

		this.initialize();

		if (packet != null) {
			this.packet = packet;
			String args[] = this.splitPacket();
			this.valid = this.initializeAtributes(args);

			if (this.isValid())
				this.setBody(); // directly retrieve from packet
		}
	}

	/**
	 * 
	 * @param args
	 */
	public Message(String args[], byte[] buff) {
		this.initialize();

		if (args.length > 0)
			this.valid = this.initializeAtributes(args);

		if (this.isValid()) {
			this.packet = this.createPacketByType(buff);
		}

	}

	/**
	 * 
	 */
	private void initialize() {
		this.packet = null;
		this.messageType = null;
		this.version = null;
		this.senderID = null;
		this.fileID = null;
		this.chunkNo = -1;
		this.replicationDeg = -1;
		this.body = null;
		this.valid = false;
	}

	/**
	 * Initialize attributes according with the type
	 * 
	 * @param args
	 * @return
	 */
	private boolean initializeAtributes(String args[]) {
		if (args[0].equalsIgnoreCase(MessageType.PUTCHUNK.toString()))
			return this.putchunkInitialize(args);
		else if (args[0].equalsIgnoreCase(MessageType.STORED.toString()))
			return this.storeInitialize(args);
		else if (args[0].equalsIgnoreCase(MessageType.GETCHUNK.toString()))
			return this.getchunkInitialize(args);
		else if (args[0].equalsIgnoreCase(MessageType.CHUNK.toString()))
			return this.chunkInitialize(args);
		else if (args[0].equalsIgnoreCase(MessageType.DELETE.toString()))
			return this.deleteInitialize(args);
		else if (args[0].equalsIgnoreCase(MessageType.REMOVED.toString()))
			return this.removedInitialize(args);
		else
			return false;
	}

	/**
	 * Create packet by type
	 * 
	 * @return packet
	 */
	private DatagramPacket createPacketByType(byte[] buff) {
		String msg = this.messageType.toString();

		switch (this.messageType) {
		case PUTCHUNK:
			msg += " " + this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " "
					+ this.replicationDeg + " " + CRLFCRLF;
			break;
		case STORED:
			msg += " " + this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " " + CRLFCRLF;
			break;
		case GETCHUNK:
			msg += " " + this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " " + CRLFCRLF;
			break;
		case CHUNK:
			msg += " " + this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " " + CRLFCRLF;
			break;
		case DELETE:
			msg += " " + this.version + " " + this.senderID + " " + this.fileID + " " + CRLFCRLF;
			break;
		case REMOVED:
			msg += " " + this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " " + CRLFCRLF;
			break;
		default:
			break;
		}

		byte[] data = new byte[msg.getBytes().length];
		System.arraycopy(msg.getBytes(), 0, data, 0, data.length);

		if (buff != null) {
			byte[] tmp = new byte[data.length + buff.length];
			System.arraycopy(data, 0, tmp, 0, data.length);
			System.arraycopy(buff, 0, tmp, data.length, buff.length);
			data = tmp;
		}

		this.body = buff;
		DatagramPacket packet = new DatagramPacket(data, data.length);
		return packet;
	}

	public DatagramPacket getPacket() {
		return packet;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public String getVersion() {
		return version;
	}

	public String getSenderID() {
		return senderID;
	}

	public String getFileID() {
		return fileID;
	}

	public int getChunkNo() {
		return chunkNo;
	}

	public int getReplicationDeg() {
		return replicationDeg;
	}

	public byte[] getBody() {
		return body;
	}

	public boolean isValid() {
		return valid;
	}

	/**
	 * 
	 * @return
	 */
	private String[] splitPacket() {
		String packetData = new String(this.packet.getData(), 0, this.packet.getLength());
		String[] arguments = packetData.split("[ ]+");
		return arguments;
	}

	/**
	 * Individual initializers
	 * 
	 * @param args
	 * @return
	 */
	// PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg>
	// <CRLF><CRLF><Body>
	private boolean putchunkInitialize(String[] args) {
		this.messageType = MessageType.PUTCHUNK;

		return (args.length >= 7 && checkVersion(args[1]) && checkSenderID(args[2]) && checkFileID(args[3])
				&& checkChunkNo(args[4]) && checkReplicationDeg(args[5]) && checkCRLF(args[6]));
	}

	// STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	private boolean storeInitialize(String[] args) {
		this.messageType = MessageType.STORED;

		return (args.length >= 6 && checkVersion(args[1]) && checkSenderID(args[2]) && checkFileID(args[3])
				&& checkChunkNo(args[4]) && checkCRLF(args[5]));
	}

	// GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	private boolean getchunkInitialize(String[] args) {
		this.messageType = MessageType.GETCHUNK;

		return (args.length >= 6 && checkVersion(args[1]) && checkSenderID(args[2]) && checkFileID(args[3])
				&& checkChunkNo(args[4]) && checkCRLF(args[5]));
	}

	// CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
	private boolean chunkInitialize(String[] args) {
		this.messageType = MessageType.CHUNK;
		// verificar se o body come�a com '\r\n\r\n', setar body e criar chunk
		return (args.length >= 6 && checkVersion(args[1]) && checkSenderID(args[2]) && checkFileID(args[3])
				&& checkChunkNo(args[4]) && checkCRLF(args[5]));
	}

	// DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
	private boolean deleteInitialize(String[] args) {
		this.messageType = MessageType.DELETE;

		return (args.length >= 5 && checkVersion(args[1]) && checkSenderID(args[2]) && checkFileID(args[3])
				&& checkCRLF(args[4]));
	}

	// REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
	private boolean removedInitialize(String[] args) {
		this.messageType = MessageType.REMOVED;

		return (args.length >= 6 && checkVersion(args[1]) && checkSenderID(args[2]) && checkFileID(args[3])
				&& checkChunkNo(args[4]) && checkCRLF(args[5]));
	}

	// verificar se a versao esta no formato x.m e se tanto x e m sao inteiros,
	// setar como float?
	private boolean checkVersion(String arg) {
		String[] arguments = arg.split("\\.");

		if (arguments.length != 2)
			return false;

		try {
			Integer.parseInt(arguments[0]);
		} catch (NumberFormatException e) {
			return false; // false
		}
		try {
			Integer.parseInt(arguments[1]);
		} catch (NumberFormatException e) {
			return false; // false
		}

		this.version = arg;
		return true;
	}

	private boolean checkSenderID(String arg) {
		if (!(arg.length() > 0))
			return false;

		this.senderID = arg;
		return true;
	}

	private boolean checkFileID(String arg) {
		if (arg.length() != SHA_BYTES)
			return false;

		this.fileID = arg;
		return true;
	}

	// verificar se chunkNo � um inteiro maior ou igual que 0
	private boolean checkChunkNo(String arg) {
		if (arg.length() > 6) {
			return false;
		}

		int tmp;
		try {
			tmp = Integer.parseInt(arg);
		} catch (NumberFormatException e) {
			return false; // false
		}

		if (!(tmp >= 0))
			return false;

		this.chunkNo = tmp;
		return true;
	}

	// verificar se replication deg � inteiro e maior que 0
	private boolean checkReplicationDeg(String arg) {
		int tmp;
		try {
			tmp = Integer.parseInt(arg);
		} catch (NumberFormatException e) {
			return false; // false
		}

		if (tmp < 0 || tmp > 9)
			return false;

		this.replicationDeg = tmp;
		return true;
	}

	// Check if body starts with CRLFCRLF, set body and chunk
	private void setBody() {
		if (this.messageType.equals(MessageType.CHUNK) || this.messageType.equals(MessageType.PUTCHUNK)) {
			int l = this.packet.getLength();
			byte[] buff = this.packet.getData();

			String str = new String(buff, 0, buff.length);

			int i = str.indexOf(Message.CRLFCRLF);

			i += 4;

			int length = l - i;
			this.body = new byte[length];

			System.arraycopy(buff, i, this.body, 0, length);

		}
	}

	private boolean checkCRLF(String arg) {
		if (arg.length() < 4)
			return false;
		String tmp = arg.substring(0, 4);
		if (!tmp.equals(CRLFCRLF))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String str = "";
		str += this.messageType + "\n";
		str += this.version + "\n";
		str += this.senderID + "\n";
		str += this.fileID + "\n";
		str += this.chunkNo + "\n";
		str += this.replicationDeg + "\n";

		return str;
	}
}
