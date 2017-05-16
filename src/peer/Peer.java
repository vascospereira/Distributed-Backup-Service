package peer;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;
import java.util.Scanner;
import localSubprotocols.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import multicastThread.*;
import util.Util;
import fileManager.FileBackup;
import fileManager.FileInfo;
import fileManager.FileStateInfo;

public class Peer implements PeerInterface{

	private final String shutDown = "close";			
	private String backupFolder;
	private String localInfoFolder;
	private String restoreFolder;
	private long dedicatedSpace = 10000 * 1000; 	//10MB
	private int chunkSize = 64 * 1000; 			//In Bytes
	private int confirmationTime = 1000;			//Initial Confirmation Time
	private int sendTime = 400;					//Sending Random Time For Send Answer
	private int maxTries = 5;
	private int randomTime = 400;
	private int controlPackage = 1000;
	private int chunkPackage = chunkSize + controlPackage;
	private Random random;
	
	//server ID and arguments
	private String protocolVersion;
	private String serverID;
	private String accessPoint;
	private InetAddress multicastChannelControlAddress;
	private int multicastChannelControlPort;
	private InetAddress multicastChannelBackupAddress;
	private int multicastChannelBackupPort;
	private InetAddress multicastChannelRestoreAddress;
	private int multicastChannelRestorePort;
	
	//sockets
	private DatagramSocket socket;
	private MulticastSocket socketMCC;
	private MulticastSocket socketMCB;
	private MulticastSocket socketMCR;
	
	//threads
	private MulticastThread threadMCC;
	private MulticastThread threadMCB;
	private MulticastThread threadMCR;
	
	//RMI
	private PeerInterface stubObject;
	private Registry registry;
	
	//Peer info
	private Peer peerInfo;
	
	public static void main(String[] args) 
	{
		System.setProperty("java.net.preferIPv4Stack", "true");
		if(args.length != 6)
		{
			Util.programInfo("ERROR: Missing arguments. \nUsage: \nApplication <protocolVersion> <serverID> "
					+ "<accessPoint> <MC>:<MCPort> <MCB>:<MCBPort> <MCR>:<MCRPort>");		
			return;
		}

		new Peer(args[0], args[1], args[2], args[3], args[4], args[5]);
	}

	public Peer() {}
	/**
	 * Creates a Peer And Saves All Its Necessary Info
	 */
	private Peer(String protocolVersion, String serverID, String accessPoint, String multicastChannelControl, String multicastChannelBackup, String multicastChannelRestore)
	{
		//initializing attributes and verifying arguments
		if(!this.parseArguments(protocolVersion, serverID, accessPoint, multicastChannelControl, multicastChannelBackup, multicastChannelRestore)){
			Util.programInfo("Failed To Initialize Application. Check Above For Errors..."); 
			return;
		}
		else{
			Util.programInfo("Parsing Complete, Trying To Initialize...");
		}
		//RMI Bind
		if(!this.rmiConnect()){
			Util.programInfo("Failed To Initialize RMI Connection. Check Above Errors...");
			return;
		}
		
		//Initialize sockets and join multicast groups
		if(!this.initialize()){
			Util.programInfo("Failed To Initialize Application. Check Above Errors...");
			this.rmiShutdown(); 
			return;
		}
		else{
			Util.programInfo("Application Started With ID: " + this.serverID + 
					"\nEnter '" + shutDown + "' To Shutdown Application...");
		}
		
		this.peerInfo = new Peer(this);
		
		//initialize the threads
		this.initializeThreads();
		
		//enter loop for local
		this.enterLoopProcess();
		
		//sockets and threads shutdown
		this.shutdown();
		
		return;
	}

	public Peer(Peer peer) {
		backupFolder = peer.getBackupFolder();
		localInfoFolder = peer.getLocalInfoFolder();
		restoreFolder = peer.getRestoreFolder();
		chunkSize = peer.getChunkSize();
		confirmationTime = peer.getConfirmationTime();
		sendTime = peer.getSendTime();
		maxTries = peer.getMaxTries();
		randomTime = peer.getRandomTime();
		controlPackage = peer.getControlPackage();
		protocolVersion = peer.getProtocolVersion();
		chunkPackage = peer.getChunkPackage();
		random = peer.getRandom();
		serverID = peer.getServerID();
		accessPoint = peer.getAccessPoint();
		multicastChannelControlAddress = peer.getMulticastChannelBackupAddress();
		multicastChannelControlPort = peer.getMulticastChannelControlPort();
		multicastChannelBackupAddress = peer.getMulticastChannelBackupAddress();
		multicastChannelBackupPort = peer.getMulticastChannelBackupPort();
		multicastChannelRestoreAddress = peer.getMulticastChannelRestoreAddress();
		multicastChannelRestorePort = peer.getMulticastChannelRestorePort();
		socket = peer.getSocket();
	}

	private void enterLoopProcess(){
		Scanner reader = new Scanner(System.in);
		
		while(true){
			System.out.println("Enter a Request: ");
			String args = reader.nextLine(); 
			
			if(args.equals(shutDown)){
				System.out.println("Closing...");
				reader.close();
				return;
			}		
		}
	}
	
	private void shutdown(){
		//Close Sockets
		this.socket.close();
		this.socketMCC.close();
		this.socketMCB.close();
		this.socketMCR.close();
		//Close RMI
		this.rmiShutdown();			
	}

	private void rmiShutdown()
	{
		try{
			this.registry.unbind(this.accessPoint);
		} 
		catch (RemoteException | NotBoundException e) {
			Util.programInfo("Failed To Unbind RMI");
		}
		try {
			UnicastRemoteObject.unexportObject(this, true);
		}
		catch (NoSuchObjectException e) {
			Util.programInfo("Failed To Unexport Object");
		}
	}
	
	private boolean rmiConnect(){
		try {
			stubObject = (PeerInterface) UnicastRemoteObject.exportObject(this, 0);
			registry = LocateRegistry.getRegistry();
			registry.bind(this.accessPoint, stubObject);	
			Util.programInfo("Server ON With Access Point: " + this.accessPoint);
			return true;
		} 
		catch (Exception e) 
		{
			Util.programInfo("RMI Bind Failure");
			
			try {
				UnicastRemoteObject.unexportObject(this, true);
			}
			catch (NoSuchObjectException ex) {
				ex.getMessage();
				Util.programInfo("Failed To Unexport Object");
			}
			return false;
		}
	}
	
	private boolean parseArguments(String protocolVersion, String serverID, String accessPoint, String multicastChannelControl, String multicastChannelBackup, String multicastChannelRestore)
	{
		boolean test = true;
		
		//Save protocol version
		String[] arguments = protocolVersion.split("\\.");
		
		if(arguments.length != 2)
			test = false;
		else
		{
			try{
				Integer.parseInt(arguments[0]);
				Integer.parseInt(arguments[1]);
				this.protocolVersion = protocolVersion;;		
			}
			catch (NumberFormatException e) {
				e.getMessage();
				test = false;
			}
		}
		//Save server ID
		this.serverID = serverID;
		
		//Set Folder Paths with File.separator (Windows or Unix) And Test If Can Create
		backupFolder = ".." + File.separator + "peers" + File.separator + this.serverID + File.separator + "backup";
		localInfoFolder = ".." + File.separator + "peers" + File.separator + this.serverID + File.separator + "local";
		restoreFolder = ".." + File.separator + "peers" + File.separator + this.serverID + File.separator + "restore";

		File backupTemp = new File(backupFolder);
		File localTemp = new File(localInfoFolder);
		File restTemp = new File(restoreFolder);
		
		backupTemp.mkdirs();
		if(!backupTemp.exists() || !backupTemp.isDirectory()){
			Util.programInfo("Failed To Create 'backup' Folder: "+ backupFolder);
			test = false;
		}
		
		localTemp.mkdirs();
		if(!localTemp.exists() || !localTemp.isDirectory()){
			Util.programInfo("Failed To Create 'local' Folder: "+ localInfoFolder);
			test = false;
		}
		
		restTemp.mkdirs();
		if(!restTemp.exists() || !restTemp.isDirectory()){
			Util.programInfo("Failed To Create 'restore' Folder: "+ restoreFolder);
			test = false;
		}
		
		//Save the accessPoint
		this.accessPoint = accessPoint;
		
		//Retrieve addresses and port
		String[] multicastChannelControlArgs = Util.retrieveAddPort(multicastChannelControl);
		String[] multicastChannelBackupArgs = Util.retrieveAddPort(multicastChannelBackup);
		String[] multicastChannelRestoreArgs = Util.retrieveAddPort(multicastChannelRestore);
		
		//Checking arguments
		if(multicastChannelControlArgs.length < 2){
			Util.programInfo("ERROR: Invalid Multicast Channel Control Arguments");
			return false;
		}
		
		if(multicastChannelBackupArgs.length < 2){
			Util.programInfo("ERROR: Invalid Multicast Channel Backup Arguments");
			return false;
		}
		
		if(multicastChannelRestoreArgs.length < 2){
			Util.programInfo("ERROR: Invalid Multicast Channel Restore Arguments");
			return false;
		}
		
		//Check Collisions In Addresses
		if(multicastChannelControlArgs[0].equals(multicastChannelBackupArgs[0]) && multicastChannelControlArgs[1].equals(multicastChannelBackupArgs[1])){
			Util.programInfo("ERROR: Collision Between MCC and MCB");
			return false;
		}
		
		if(multicastChannelControlArgs[0].equals(multicastChannelRestoreArgs[0]) && multicastChannelControlArgs[1].equals(multicastChannelRestoreArgs[1])){
			Util.programInfo("ERROR: Collision Between MCC and MCR");
			return false;
		}
		
		if(multicastChannelBackupArgs[0].equals(multicastChannelRestoreArgs[0]) && multicastChannelBackupArgs[1].equals(multicastChannelRestoreArgs[1])){
			Util.programInfo("ERROR: Collision Between MCB and MCR");
			return false;
		}

		//Testing Multicast Channel Control Port
		try{
			this.multicastChannelControlPort = Integer.parseInt(multicastChannelControlArgs[1]);
		}
		catch(NumberFormatException n){
			Util.programInfo("ERROR: Invalid Port For Control: '" + multicastChannelControlArgs[1] + "'. Must Be a Number.");
			test = false;
		}
		
		//Multicast Channel Control Address
		try{
			this.multicastChannelControlAddress = InetAddress.getByName(multicastChannelControlArgs[0]);
		}
		catch(UnknownHostException e){
			Util.programInfo("ERROR: Failed to resolve multicast address for Control. \n" + "Please make sure multicast is available and the address is multicast");
			test = false;
		}
		
		//Testing Multicast Channel Backup Port
		try{
			this.multicastChannelBackupPort = Integer.parseInt(multicastChannelBackupArgs[1]);
		}
		catch(NumberFormatException n){
			Util.programInfo("ERROR: Invalid Port For Backup: '" + multicastChannelBackupArgs[1] + "'. Must Be a Number.");
			test = false;
		}
		
		//Multicast Channel Address for Backup
		try{
			this.multicastChannelBackupAddress = InetAddress.getByName(multicastChannelBackupArgs[0]);
		}
		catch(UnknownHostException e){
			Util.programInfo("ERROR: Failed To Resolve Multicast Address For Backup. \n" 
					+ "Please Make Sure Multicast Is Available And The Address Is Multicast");
			test = false;
		}
		
		//Testing Multicast Channel Restore port
		try{
			this.multicastChannelRestorePort = Integer.parseInt(multicastChannelRestoreArgs[1]);
		}
		catch(NumberFormatException n){
			Util.programInfo("ERROR: Invalid Port For Restore: '" + multicastChannelRestoreArgs[1] + "'. Must Be a Number.");
			test = false;
		}
		
		//Multicast Channel Adress For Restore
		try{
			this.multicastChannelRestoreAddress = InetAddress.getByName(multicastChannelRestoreArgs[0]);
		}
		catch(UnknownHostException e){
			Util.programInfo("ERROR: Failed To Resolve Multicast Address For Restore. \n"
					+ "Please Make Sure Multicast Is Available And Is a Multicast Address.");
			test = false;
		}
		return test;
	}
	
	
	private boolean initialize(){
		boolean test = true;
		
		//Initializing Random
		this.random = new Random();
		this.random.setSeed(System.currentTimeMillis());
		
		//Create Local Socket
		if(!createLocalSocket()){
			test = false;
			Util.programInfo("ERROR: Failed To Create Local Socket");
		}
		else Util.programInfo("Created Local Socket");
		
		if(!createMCC()){
			test = false;
			Util.programInfo("ERROR: Failed To Create Multicast Channel Control Socket");
		}
		else Util.programInfo("Created Multicast Channel Socket Control And Joined Group");
		
		if(!createMCB()){
			test = false;
			Util.programInfo("ERROR: Failed To Create Multicast Channel Backup Socket");
		}
		else Util.programInfo("Created Multicast Channel Socket Backup And Joined Group");
		
		if(!createMCR()){
			test = false;
			Util.programInfo("ERROR: Failed To Create Multicast Channel Restore Socket");
		}
		else Util.programInfo("Created Multicast Channel Socket Restore And Joined Group");
		
		return test;
	}
	
	private void initializeThreads(){
		this.threadMCC = new MulticastThread(MulticastThreadType.CONTROL, this.socket, this.socketMCC,  this.peerInfo);
		this.threadMCC.start();
		
		this.threadMCB = new MulticastThread(MulticastThreadType.BACKUP, this.socket, this.socketMCB, this.peerInfo);
		this.threadMCB.start();
		
		this.threadMCR = new MulticastThread(MulticastThreadType.RESTORE, this.socket, this.socketMCR, this.peerInfo);
		this.threadMCR.start();
	}
	
	private boolean createMCC(){
		try{
			this.socketMCC = new MulticastSocket(this.multicastChannelControlPort);
			this.socketMCC.joinGroup(this.multicastChannelControlAddress);
			return true;
		}
		catch(IOException e){return false;}
	}
	
	private boolean createMCB(){
		try{
			this.socketMCB = new MulticastSocket(this.multicastChannelBackupPort);
			this.socketMCB.joinGroup(this.multicastChannelBackupAddress);
			return true;
		}
		catch(IOException e){return false;}
	}
	
	private boolean createMCR(){
		try{
			this.socketMCR = new MulticastSocket(this.multicastChannelRestorePort);
			this.socketMCR.joinGroup(this.multicastChannelRestoreAddress);
			return true;
		}
		catch(IOException e){ return false; }
	}
	
	private boolean createLocalSocket(){
		try{
			this.socket = new DatagramSocket();
			return true;
		}
		catch(SocketException e){ return false; }	
	}
	
	private String getState(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n########## PEER STATE INFO " + this.peerInfo.getServerID() + " ##########\n");
		
		//About Peer
		long totalBytes = FileBackup.getBackupFolderSize(this.peerInfo.getBackupFolder()) + FileInfo.getLocalFolderSize(this.peerInfo.getBackupFolder());	
		long spaceBytes = (long)this.peerInfo.getDedicatedSpace();

		sb.append("\nPeer's Storage Capacity: ");
		sb.append(Long.toString(spaceBytes/1000) + " KBytes");
		
		sb.append("\nChunk Size: ");
		sb.append(this.peerInfo.getChunkSize()/1000 + " KBytes");
		
		sb.append("\nAmount Stored: ");	
		sb.append(totalBytes/1000 + " KBytes");
		
		sb.append("\nSpace Available: ");
		sb.append((spaceBytes - totalBytes)/1000 + " KBytes");
		
		//About Backup
		String backupInfo = this.getBackupInfo();
		sb.append(backupInfo);
		
		//About Local
		String localInfo = this.retrieveLocalInfo();
		sb.append(localInfo);

		return sb.toString();
	}

	private String getBackupInfo(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n\nBackup Folder Path (Where Peer Receives Backup): ");
		sb.append(this.peerInfo.getBackupFolder());
		
		File[] folder = new File(this.peerInfo.getBackupFolder()).listFiles();
		
		for(File child : folder)
		{
			FileStateInfo info = FileInfo.getInfo(child);
			
			if(info != null && info.getBackupSizeFile() > -1){
				sb.append("\nFileID: " + info.getFileID());
				sb.append(" ChunkNo: " + info.getChunkNo());
				sb.append(" Size: " + info.getBackupSizeFile());
				sb.append(" Desired Replication: " + info.getReplication());
				sb.append(" Perceived Replication: " + info.getPeers());				
			}
		}

		return sb.toString();
	}
	
	private String retrieveLocalInfo()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n\nLocal Info Folder Path (Where The Peer Initiates Backup): ");
		sb.append(this.peerInfo.getLocalInfoFolder());
		
		File[] folder = new File(this.peerInfo.getLocalInfoFolder()).listFiles();
		
		for(File child : folder){
			FileStateInfo info = FileInfo.getInfo(child);
			
			if(info != null){
				sb.append("\nPathname: " + info.getPath());
				sb.append(" FileID: " + info.getFileID());
				sb.append(" ChunkNo: " + info.getChunkNo());
				sb.append(" DesiredRep: " + info.getReplication());
				sb.append(" Perceived: " + info.getPeers());				
			}
		}

		return sb.toString();
	}
	
	@Override
	public void backup(File file, int replication) throws RemoteException 
	{
		Thread backupLocal = new BackupLocal(file, replication, this.peerInfo);
		backupLocal.start();
	}

	@Override
	public void restore(File file) throws RemoteException 
	{
		Thread restoreLocal = new RestoreLocal(file, this.peerInfo);
		restoreLocal.start();
	}

	@Override
	public void delete(File file) throws RemoteException 
	{
		Thread deleteLocal = new DeleteLocal(file, this.peerInfo);
		deleteLocal.start();
	}

	@Override
	public void manageStorage(int space) throws RemoteException 
	{
		Thread reclaimLocal = new ReclaimLocal(space, this.peerInfo);
		reclaimLocal.start();
	}

	@Override
	public String retrieveState() throws RemoteException 
	{
		return this.getState();
	}

	public String getBackupFolder() {
		return backupFolder;
	}

	public String getLocalInfoFolder() {
		return localInfoFolder;
	}

	public String getRestoreFolder() {
		return restoreFolder;
	}

	public long getDedicatedSpace() {
		return dedicatedSpace;
	}

	public int getChunkSize() {
		return chunkSize;
	}

	public int getConfirmationTime() {
		return confirmationTime;
	}

	public int getSendTime() {
		return sendTime;
	}

	public int getMaxTries() {
		return maxTries;
	}

	public int getRandomTime() {
		return randomTime;
	}

	public int getControlPackage() {
		return controlPackage;
	}

	public int getChunkPackage() {
		return chunkPackage;
	}

	public Random getRandom() {
		return random;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public String getServerID() {
		return serverID;
	}

	public String getAccessPoint() {
		return accessPoint;
	}

	public InetAddress getMulticastChannelControlAddress() {
		return multicastChannelControlAddress;
	}

	public int getMulticastChannelControlPort() {
		return multicastChannelControlPort;
	}

	public InetAddress getMulticastChannelBackupAddress() {
		return multicastChannelBackupAddress;
	}

	public int getMulticastChannelBackupPort() {
		return multicastChannelBackupPort;
	}

	public InetAddress getMulticastChannelRestoreAddress() {
		return multicastChannelRestoreAddress;
	}

	public int getMulticastChannelRestorePort() {
		return multicastChannelRestorePort;
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	public MulticastSocket getSocketMCC() {
		return socketMCC;
	}

	public MulticastSocket getSocketMCB() {
		return socketMCB;
	}

	public MulticastSocket getSocketMCR() {
		return socketMCR;
	}

	public MulticastThread getThreadMCC() {
		return threadMCC;
	}

	public MulticastThread getThreadMCB() {
		return threadMCB;
	}

	public MulticastThread getThreadMCR() {
		return threadMCR;
	}

	public PeerInterface getStubObject() {
		return stubObject;
	}

	public Registry getRegistry() {
		return registry;
	}

	public Peer getPeerInfo() {
		return peerInfo;
	}

	public void reclaimDedicatedSpace(long dedicatedSpace) {
		this.dedicatedSpace =- dedicatedSpace;
	}
}
