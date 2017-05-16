package message;

public enum MessageType {
	PUTCHUNK("PUTCHUNK"), STORED("STORED"), GETCHUNK("GETCHUNK"), CHUNK("CHUNK"), DELETE("DELETE"), REMOVED("REMOVED");

	private final String message;

	private MessageType(String operation) {
		this.message = operation;
	}

	public String toString() {
		return message;
	}
}
