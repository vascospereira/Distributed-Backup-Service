# Distributed System Configuration #

## To COMPILE in UNIX: ##

 * using a terminal, navigate to the project's root folder
 * cd to "scripts" subfolder and enter "chmod 755 compile.sh"	
 * followed by "./compile.sh"

## To COMPILE in WINDOWS: ##

 * through Command Prompt, navigate to root project folder
 * cd to "scripts" subfolder and enter "compile.bat"	

## To START a Peer in UNIX: ##

 * navigate to the project's "src" folder
 * through the terminal type: "rmiregistry &" to start the RMI registry process
 * in the same terminal, or other pointing to the same directory, type:	
		<br/>"java peer.Peer \<protocolVersion> \<serverID> \<accessPoint> \<MC>:\<MCPort> \<MCB>:\<MCBPort> \<MCR>:\<MCRestore>"
	<br/>example: 
	java peer.Peer 1.0 1 remote 224.0.0.3:3333 224.0.0.3:4444 224.0.0.3:5555

## To START a Peer in WINDOWS: ##

 * open Comand Prompt, navigate to project's "src" folder
 * enter "start rmiregistry" to start the RMI registry process
 * in the same Command Prompt, or other pointing to the same directory, type:	
		<br/>"java peer.Peer \<protocolVersion> \<serverID> \<accessPoint> \<MC>:\<MCPort> \<MCB>:\<MCBPort> \<MCR>:\<MCRestore>"
	<br/>example: 
	java peer.Peer 1.0 1 remote 224.0.0.3:3333 224.0.0.3:4444 224.0.0.3:5555

## To RUN the TestApp in UNIX: ##

 * navigate to the project's "src" folder
 * through the terminal type:
		<br/>"java test.TestApp <peer_access_point> BACKUP <filepath> <replication_degree>" or
		<br/>"java test.TestApp <peer_access_point> RESTORE <filepath>" or
		<br/>"java test.TestApp <peer_access_point> DELETE <filepath>" or
		<br/>"java test.TestApp <peer_access_point> RECLAIM <space>" or
		<br/>"java test.TestApp <peer_access_point> STATE" 
	<br/>backup example: 
	java test.TestApp remote backup ../testfiles/nature.jpg 1
    
## To RUN the TestApp in WINDOWS: ##

 * open Comand Prompt, navigate to project's "src" folder
 * through the Comand Prompt type:
		<br/>"java test.TestApp <peer_access_point> BACKUP <filepath> <replication_degree>" or
		<br/>"java test.TestApp <peer_access_point> RESTORE <filepath>" or
		<br/>"java test.TestApp <peer_access_point> DELETE <filepath>" or
		<br/>"java test.TestApp <peer_access_point> RECLAIM <space>" or
		<br/>"java test.TestApp <peer_access_point> STATE" 
	<br/>backup example: 
	java test.TestApp remote backup ..\testfiles\nature.jpg 1

## DESCRIPTION of local files created by the peer application: ##

	In 'peers' folder, each peer createas a new folder, which the name is the Peer own identifier, creates also a:

 * 'backup' folder: Saves every backup initiated by other peers.
 * 'local' folder: Saves every information related to local initiated backups.
 * 'restore' folder: Saves every restored file.
