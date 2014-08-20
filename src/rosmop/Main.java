package rosmop;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import rosmop.parser.ast.ROSMOPSpecFile;
import rosmop.parser.ast.mopspec.ROSBodyDeclaration;
import rosmop.parser.ast.mopspec.ROSEventDefinition;
import rosmop.parser.ast.mopspec.ROSFieldDeclaration;
import rosmop.parser.ast.mopspec.ROSMOPSpec;
import rosmop.parser.ast.mopspec.ROSVariableDeclarator;
import rosmop.parser.main_parser.ROSMOPParser;
import rosmop.util.Tool;

public class Main {

	public static void main(String[] args) {
		try {

			if(args.length == 0){
				throw new ROSMOPException("Make sure you provide at least "
						+ "one .rv file or a folder of .rv files.");
			}
			
			CSpecification rvcParser = new RVParserAdapter(ROSMOPParser.parse(new InputStreamReader(new FileInputStream(args[0]))));
			
			System.out.println("success");
//			ROSMOPSpecFile specFile;
//			File file = null;
//			boolean diffName = false;
//			
//			/*
//				1- "." means current directory (all .rv files in the directory)
//				2- if it ends with "/" or "\" process the given directory (all .rv files in the directory)
//				3- one .rv file
//			 */
//			if (args.length == 1) {
//				if(args[0].equalsIgnoreCase(".")){
//					file = new File("rvmonitor.rv");
//					specFile = processDirOfFiles(System.getProperty("user.dir"));
//				} else if(args[0].endsWith(File.separator)){
//					file = new File(args[0]+"rvmonitor.rv");
//					specFile = processDirOfFiles(args[0]);
//				} else {
//					if (!checkArguments(args)) {
//						throw new ROSMOPException("Unrecognized file type! The ROSMOP specification file should have .rv as the extension.");
//					}
//					file = new File(args[0]);
//					diffName = true;
//					specFile = ROSMOPParser.parse(file);
//				}
//			} else {
//				file = new File(args[0]);
//				diffName = true;
//				specFile = processMultipleFiles(args);
//			}
//			String filePath = "";
//			if (file.getParent() != null) {
//				filePath = file.getParent() + File.separator + "rvmonitor.rv";
//			} else {
//				filePath = "rvmonitor.rv";
//			}
//			Tool.writeFile(specFile.toCppFile(), diffName ? filePath : file.getAbsolutePath(), ".cpp");
//			Tool.writeFile(specFile.toHeaderFile(), diffName ? filePath : file.getAbsolutePath(), ".h");
//			
//			diffName = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ROSMOPSpecFile processMultipleFiles(String[] args) throws ROSMOPException {
		ROSMOPSpecFile specFile = new ROSMOPSpecFile(0, 0);
		try {
			if (!checkArguments(args)) {
				throw new ROSMOPException("Unrecognized file type! The ROSMOP specification file should have .rv as the extension.");
			}
			for (String arg : args) {
				File file = new File(arg);
				ROSMOPSpecFile f = ROSMOPParser.parse(file);
				specFile.addSpecs(f.getSpecs());
				specFile.addIncludes(f.getIncludeDeclarations());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!checkDuplicatedEventsDeclarations(specFile)) {
			throw new ROSMOPException("Duplicate event names or field declarations"); 
		}
		return specFile;
	}

	/**
	 * Called when the input is a directory (ends with a file separator)
	 * 
	 * @param arg name of the input directory
	 * @return one ROSMOPSpecFile including all the specifications 
	 * 			from all .rv files in the input directory
	 * @throws ROSMOPException
	 */
	private static ROSMOPSpecFile processDirOfFiles(String arg) throws ROSMOPException {
		ROSMOPSpecFile specFile = new ROSMOPSpecFile(0, 0);

		try {
			File folder = new File(arg);
			File[] listOfFiles = folder.listFiles();
			
			//get rid of hidden files/folders & directories in the folder
			//(won't be processed)
			ArrayList<File> onlyFiles = new ArrayList<File>(Arrays.asList(listOfFiles));
			for (int i = 0; i < onlyFiles.size(); i++) {
				if(onlyFiles.get(i).isDirectory() || onlyFiles.get(i).getName().startsWith(".")){
					onlyFiles.remove(i);
				}
			}
			
			File noDirs[] = new File[onlyFiles.size()];
			noDirs = onlyFiles.toArray(noDirs);
			
			//all remaining files should have the extension .rv
			if (!checkArguments(noDirs)) {
				throw new ROSMOPException("Unrecognized file type! The ROSMOP specification file should have .rv as the extension.");
			}
			
			for (File file : noDirs) {
				ROSMOPSpecFile f = ROSMOPParser.parse(file);
				specFile.addSpecs(f.getSpecs());
				specFile.addIncludes(f.getIncludeDeclarations());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (!checkDuplicatedEventsDeclarations(specFile)) {
			throw new ROSMOPException("Duplicate event names or field declarations"); 
		}
		
		return specFile;
	}

	private static boolean checkArguments(String[] args) {
		if(args.length == 0) return false;
		for (String arg : args) {
			if(!Tool.isSpecFile(arg)){
				return false;
			}
		}
		return true;
	}
	
	private static boolean checkArguments(File[] args) {
		if(args.length == 0) return false;
		for (File arg : args) {
			if(!Tool.isSpecFile(arg.getName())){
				return false;
			}
		}
		return true;
	}

	
	/**
	 * In case of multiple .rv files as input, all of them are gathered
	 * and processed as one big specification file with multiple specifications.
	 * Therefore, event names and field declarations should have unique names.
	 * 
	 * @param specFile The specification file which is the collection of 
	 * 			all .rv specifications provided 
	 * @return true if there are no duplicate names
	 */
	private static boolean checkDuplicatedEventsDeclarations(ROSMOPSpecFile specFile) {
		Set<String> events = new HashSet<String>();
		Set<String> declarations = new HashSet<String>();
		
		for (ROSMOPSpec spec : specFile.getSpecs()) {
			for (ROSEventDefinition event : spec.getEvents()) {
				if (!events.add(event.getEventName())) return false;
			}
			
			for (ROSBodyDeclaration declaration : spec.getDeclarations()) {
				ROSFieldDeclaration field = (ROSFieldDeclaration) declaration;
				for(ROSVariableDeclarator variable : field.getVariables()){
					if (!declarations.add(variable.getId())) return false;
				}
			}
		}

		return true;
	}
}
