package fileManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import util.Util;

public abstract class FileBackup {

	public synchronized static File createChunk(String name, byte[] buff) {
		File file = new File(name);
		// Creating directories if non existing
		file.mkdirs();
		// Delete file if exists
		file.delete();

		if (!file.exists() && file.isDirectory())
			Util.programInfo("Failed To Create Chunk File: " + name);
		try {
			FileOutputStream fos = new FileOutputStream(name);
			fos.write(buff);
			fos.close();
		} catch (IOException e) {
			Util.programInfo("Failed To Write To Chunk " + file.getName());
			return null;
		}
		return file;
	}

	public synchronized static byte[] readChunk(File file) {
		int length = (int) file.length();
		FileInputStream fis;
		byte[] buff = new byte[length];
		// Opening file
		try {
			fis = new FileInputStream(file);
			fis.read(buff);
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e1) {
			return null;
		}
		// Closing
		try {
			fis.close();
		} catch (IOException e) {
			return buff;
		}
		return buff;
	}

	public synchronized static void deleteBackupFiles(String folder, String fileID) {
		File localInfoFolder = new File(folder);
		File[] files = localInfoFolder.listFiles();

		for (File child : files) {
			String fileName = child.getName();
			String[] args1 = fileName.split(Util.SEPARATOR);
			String[] args2 = fileName.split("\\.");
			String ext = "." + args2[1];

			if (args1[0].equals(fileID) && (ext.equals(Util.CHUNK_EXTENSION) || ext.equals(Util.INFO_EXTENSION))) {
				while (child.exists())
					child.delete();
			}
		}
	}

	public synchronized static void deleteBackupFile(String folder, String fileID, String chunkNo) {
		String backupFile = createChunkPath(folder, fileID, chunkNo);
		File file = new File(backupFile);
		file.delete();
	}

	public synchronized static long getBackupFolderSize(String folder) {
		long folderLength = 0;
		File file = new File(folder);
		File[] files = file.listFiles();

		for (File child : files) {
			String[] args = child.getName().split("\\.");

			if (args.length == 2) {
				String ext = "." + args[1];

				if (ext.equals(Util.CHUNK_EXTENSION))
					folderLength += child.length();
			}
		}

		return folderLength;
	}

	public static String createChunkPath(String folder, String fileID, int chunkNo) {
		return folder + File.separatorChar + fileID + Util.SEPARATOR + chunkNo + Util.CHUNK_EXTENSION;
	}

	public static String createChunkPath(String folder, String fileID, String chunkNo) {
		return folder + File.separatorChar + fileID + Util.SEPARATOR + chunkNo + Util.CHUNK_EXTENSION;
	}

	public static String createRestorePath(String folder, String name) {
		return folder + File.separator + name;
	}
}
