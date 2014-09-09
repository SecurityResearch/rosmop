package rosmop.util;

import java.io.File;
import java.io.FileWriter;

import rosmop.ROSMOPException;

public class Tool {
	public static boolean isSpecFile(String path){
		return path.endsWith(".rv");
	}

	public static String getFileName(String path){
		int i = path.lastIndexOf(File.separator);
//		int j = path.lastIndexOf(".");
		return path.substring(i+1, path.length());        
	}

	public static void writeFile(String content, String location) throws ROSMOPException {
		if (content == null || content.length() == 0)
			return;

//		int i = location.lastIndexOf(File.separator);
//		String filePath = ""; 
		try {
//			filePath = location.substring(0, i + 1) + Tool.getFileName(location) + suffix;
			FileWriter f = new FileWriter(location);
			f.write(content);
			f.close();
		} catch (Exception e) {
			throw new ROSMOPException(e.getMessage());
		}

		System.out.println(Tool.getFileName(location) + " is generated");
	}
}