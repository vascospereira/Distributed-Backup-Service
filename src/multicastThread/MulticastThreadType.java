package multicastThread;

public enum MulticastThreadType {
	CONTROL("CONTROL"), BACKUP("BACKUP"), RESTORE("RESTORE");

	private final String message;

	private MulticastThreadType(String operation) {
		this.message = operation;
	}

	public String toString() {
		return message;
	}
}
