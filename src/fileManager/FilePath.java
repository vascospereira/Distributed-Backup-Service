package fileManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import util.Util;
import message.Message;

public abstract class FilePath {

	public static synchronized boolean createPathFile(String name, String path) {
		File file = new File(name);
		// Creating directories if non existing
		file.mkdirs();
		// Delete file if exists
		file.delete();

		if (!file.exists() && file.isDirectory())
			Util.programInfo("Failed To Create Path File: " + name);

		try {
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bf = new BufferedWriter(fw);
			bf.write(path);
			bf.close();
		} catch (IOException e) {
			Util.programInfo("Failed To Concat To File: " + file.getName());
			return false;
		}

		return true;
	}

	public synchronized static void deletePathFiles(String folder, String fileID) {
		File localInfoFolder = new File(folder);
		File[] files = localInfoFolder.listFiles();

		for (File child : files) {
			if (checkFormat(child.getName())) {
				String fileName = child.getName();
				String[] args2 = fileName.split("\\.");
				String ext = "." + args2[1];
				String[] file = fileName.split(Util.SEPARATOR);
				if (file[0].equals(fileID) && ext.equals(Util.PATH_EXTENSION)) {
					while (child.exists())
						child.delete();
				}
			}
		}
	}

	public static synchronized String readPath(File file) {
		if (!checkFormat(file.getName()))
			return null;
		BufferedReader bir;

		try {
			// Opening files
			bir = new BufferedReader(new FileReader(file));
			String temp = "";
			// Read line to check header
			temp = bir.readLine();

			if (temp == null) {
				bir.close();
				return null;
			}
			// Close rename and delete
			bir.close();
			return temp;
		} catch (IOException e) {
			return null;
		}
	}

	public static String createFilePathPath(String folder, String fileID, int chunkNo) {
		return folder + File.separatorChar + fileID + Util.SEPARATOR + chunkNo + Util.PATH_EXTENSION;
	}

	private static boolean checkFormat(String name) {
		String[] file = name.split(Util.SEPARATOR);

		if (file.length != 2 || file[0].length() != Message.SHA_BYTES)
			return false;

		String[] extension = file[1].split("\\.");

		if (extension.length != 2)
			return false;

		String fileExtension = "." + extension[1];

		if (!Util.isInteger(extension[0]) || !fileExtension.equals(Util.PATH_EXTENSION))
			return false;

		return true;
	}
}
