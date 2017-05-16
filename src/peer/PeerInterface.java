package peer;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
	void backup(File file, int replication) throws RemoteException;

	void restore(File file) throws RemoteException;

	void delete(File file) throws RemoteException;

	void manageStorage(int space) throws RemoteException;

	String retrieveState() throws RemoteException;
}
