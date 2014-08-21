package rosmop;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import rosmop.parser.ast.ROSMOPSpecFile;
import rosmop.parser.ast.mopspec.Event;
import rosmop.parser.ast.mopspec.MonitorFile;
import rosmop.parser.ast.mopspec.ROSBodyDeclaration;
import rosmop.parser.ast.mopspec.ROSEventDefinition;
import rosmop.parser.ast.mopspec.ROSFieldDeclaration;
import rosmop.parser.ast.mopspec.ROSMOPSpec;
import rosmop.parser.ast.mopspec.ROSVariableDeclarator;
import rosmop.parser.ast.mopspec.Specification;
import rosmop.parser.main_parser.ROSMOPParser;
import rosmop.util.Tool;

public class Main {

	public static void main(String[] args) {
		try {

			if(args.length == 0){
				throw new ROSMOPException("Make sure you provide at least "
						+ "one .rv file or a folder of .rv files.");
			}

			//            String logicPluginDirPath = Tool.polishPath(readLogicPluginDir(basePath));
			//            
			//            File dirLogicPlugin = new File(logicPluginDirPath);
			//            
			//            if(!dirLogicPlugin.exists()){
			//                throw new LogicException(
			//                    "Unrecoverable error: please place plugins in the default plugins directory:plugins");
			//            }

			String pathToFile = "", pathToOutputNoExt;
			File fileToGetPath;
			MonitorFile readyToProcess;
			/*
			1- "." means current directory (all .rv files in the directory)
			2- if it ends with "/" or "\" process the given directory (all .rv files in the directory)
			3- one .rv file
			 */
			if (args.length == 1) {
				if(args[0].equalsIgnoreCase(".")){
					fileToGetPath = new File(System.getProperty("user.dir"));
					pathToFile = fileToGetPath.getAbsolutePath();
					//					/home/cans-u/workspace/rosmop
//					System.out.println(pathToFile);
					pathToOutputNoExt = pathToFile + File.separator + "rvmonitor";
//					System.out.println(pathToOutputNoExt);
					readyToProcess = processDirOfFiles(pathToFile);
				} else if(args[0].endsWith(File.separator)){
					fileToGetPath = new File(args[0]);
					pathToFile = fileToGetPath.getAbsolutePath();
					//					/home/cans-u/Desktop
//					System.out.println(pathToFile);
					pathToOutputNoExt = pathToFile + File.separator + "rvmonitor";
//					System.out.println(pathToOutputNoExt);
					readyToProcess = processDirOfFiles(pathToFile);
				} else {
					if (!checkArguments(args)) {
						throw new ROSMOPException("Unrecognized file type! The ROSMOP specification file should have .rv as the extension.");
					}
					fileToGetPath = new File(args[0]);
					pathToFile = fileToGetPath.getAbsolutePath();
					//					/home/cans-u/Desktop/deneme.rv
//					System.out.println(pathToFile);
					pathToOutputNoExt = pathToFile.substring(0, pathToFile.lastIndexOf(File.separator)+1) + "rvmonitor";
//					System.out.println(pathToOutputNoExt);
					readyToProcess = ROSMOPParser.parse(pathToFile);
				}
			}
			/*
			 * multiple .rv files 
			 */
			else {
				//					output file is going to be written in the first file's dir
				fileToGetPath = new File(args[0]);
				pathToFile = fileToGetPath.getAbsolutePath();
				//					/home/cans-u/Desktop/deneme.rv
//				System.out.println(pathToFile);
				pathToOutputNoExt = pathToFile.substring(0, pathToFile.lastIndexOf(File.separator)+1) + "rvmonitor";
//				System.out.println(pathToOutputNoExt);
				readyToProcess = processMultipleFiles(args);
			}

			//			CSpecification rvcParser = new RVParserAdapter(readyToProcess);

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

	private static MonitorFile processMultipleFiles(String[] args) throws ROSMOPException {
		MonitorFile specFile = new MonitorFile();
		try {
			if (!checkArguments(args)) {
				throw new ROSMOPException("Unrecognized file type! The ROSMOP specification file should have .rv as the extension.");
			}
			for (String arg : args) {
				MonitorFile f = ROSMOPParser.parse(arg);
				specFile.addSpecifications(f.getSpecifications());
				specFile.addPreamble(f.getPreamble());
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
	 * @return one MonitorFile including all the specifications 
	 * 			from all .rv files in the input directory
	 * @throws ROSMOPException
	 */
	private static MonitorFile processDirOfFiles(String arg) throws ROSMOPException {
		MonitorFile specFile = new MonitorFile();

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
				MonitorFile f = ROSMOPParser.parse(file.getAbsolutePath());
				specFile.addSpecifications(f.getSpecifications());
				specFile.addPreamble(f.getPreamble());
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
	private static boolean checkDuplicatedEventsDeclarations(MonitorFile specFile) {
		Set<String> events = new HashSet<String>();
		Set<String> declarations = new HashSet<String>();

		for (Specification spec : specFile.getSpecifications()) {
			for (Event event : spec.getEvents()) {
				if (!events.add(event.getName())) return false;

				//				TODO: handle declarations as fielddecl in event, 
				//				then check for duplicates!!
				//				this way you dont have to keep decls in specfile
				//				if(!declarations.add(event.getDefinition())) return false;
			}

			//			for (ROSBodyDeclaration declaration : spec.getDeclarations()) {
			//				ROSFieldDeclaration field = (ROSFieldDeclaration) declaration;
			//				for(ROSVariableDeclarator variable : field.getVariables()){
			//					if (!declarations.add(variable.getId())) return false;
			//				}
			//			}
		}

		return true;
	}
}
