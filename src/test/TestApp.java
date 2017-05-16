package test;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import peer.PeerInterface;

public class TestApp {

	private static final String BACKUP = "backup";
	private static final String DELETE = "delete";
	private static final String RESTORE = "restore";
	private static final String RECLAIM = "reclaim";
	private static final String STATE = "state";
	private static final String HOST_ADDR = "localhost";
	private static final int BACKUP_ARGS = 4;
	private static final int RESTORE_ARGS = 3;
	private static final int DELETE_ARGS = 3;
	private static final int RECLAIM_ARGS = 3;
	private static final int STATE_ARGS = 2;

	private static String operation, remoteNameObject;
	private static int replication, space;
	private static PeerInterface peer;// peer_stub
	private static File file; // filepath
	private static Registry registry;

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");// for Mac
		if (!argumentsValidation(args))
			return;
		/**
		 * BACKUP, RESTORE, DELETE, RECLAIM, STATE java TestApp <peer_ap>(peer's
		 * access point) <sub_protocol>(operation) <opnd_1> <opnd_2> java
		 * TestApp 1923 BACKUP test1.pdf 3 java TestApp 1923 RESTORE test1.pdf
		 * java TestApp 1923 STATE
		 * 
		 */
		try {
			registry = LocateRegistry.getRegistry(HOST_ADDR);
			/**
			 * The client looks up the remote object by its name
			 * "remoteNameObject" in the server's registry. peer => remote
			 * reference The peer is responsible for carrying out the method
			 * invocation on the remote object
			 */
			peer = (PeerInterface) registry.lookup(remoteNameObject);

			if (operation.equalsIgnoreCase(BACKUP))
				peer.backup(file, replication);
			else if (operation.equalsIgnoreCase(RESTORE))
				peer.restore(file);
			else if (operation.equals(DELETE))
				peer.delete(file);
			else if (operation.equals(RECLAIM))
				peer.manageStorage(space);
			else {
				String str = peer.retrieveState();
				System.out.println(str);
			}

		} catch (Exception e) {
			System.err.println("Peer exception: " + e.toString());
		}

	}

	private static boolean argumentsValidation(String[] args) {
		if (args.length < 2) {
			StringBuilder sb = new StringBuilder();
			sb.append("Usage:\n" + "<peer_access_point> BACKUP <filepath> <replication_degree>\n"
					+ "<peer_access_point> RESTORE <filepath>\n" + "<peer_access_point> DELETE <filepath>\n"
					+ "<peer_access_point> RECLAIM <space>\n" + "<peer_access_point> STATE");
			System.out.println(sb.toString());
			return false;
		}
		remoteNameObject = args[0];
		operation = args[1];

		if (operation.equals(BACKUP)) {
			if (args.length != BACKUP_ARGS) {
				System.out.println("Usage:\n<peer_access_point> BACKUP <filepath> <replication_degree>");
				return false;
			}
			if (!validPath(args[2]))
				return false;
			if (!replicationInteger(args[3]))
				return false;
		} else if (operation.equals(RESTORE)) {
			if (args.length != RESTORE_ARGS) {
				System.out.println("Usage:\n<peer_access_point> RESTORE <filepath>");
				return false;
			}
			if (!validPath(args[2]))
				return false;
		} else if (operation.equals(DELETE)) {
			if (args.length != DELETE_ARGS) {
				System.out.println("Usage:\n<peer_access_point> DELETE <filepath>");
				return false;
			}
			if (!validPath(args[2]))
				return false;
		} else if (operation.equals(RECLAIM)) {
			if (args.length != RECLAIM_ARGS) {
				System.out.println("Usage:\n<peer_access_point> RECLAIM <space>");
				return false;
			}
			if (!spaceInteger(args[2]))
				return false;
		} else if (operation.equals(STATE)) {
			if (args.length != STATE_ARGS) {
				System.out.println("Usage:\n<peer_access_point> STATE");
				return false;
			}
		}
		return true;
	}

	private static boolean spaceInteger(String value) {
		try {
			space = Integer.parseInt(value);
		} catch (Exception e) {
			System.err.println("Space Is Not An Integer.");
			return false;
		}
		return true;
	}

	private static boolean replicationInteger(String value) {
		try {
			replication = Integer.parseInt(value);
		} catch (Exception e) {
			System.err.println("Replication Is Not An Integer.");
			return false;
		}
		if (replication < 1 || replication > 9) {
			System.err.println("Replication must be between 1 and 9");
			return false;
		}

		return true;
	}

	private static boolean validPath(String name) {
		file = new File(name);
		if (!file.exists() || file.isDirectory()) {
			System.err.println("File Does Not Exist");
			return false;
		}
		return true;
	}
}
