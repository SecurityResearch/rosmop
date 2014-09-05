package rosmop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShell;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;
import com.runtimeverification.rvmonitor.logicpluginshells.cfg.CCFG;
import com.runtimeverification.rvmonitor.logicpluginshells.fsm.CFSM;
import com.runtimeverification.rvmonitor.logicpluginshells.tfsm.CTFSM;
import com.runtimeverification.rvmonitor.logicrepository.LogicException;
import com.runtimeverification.rvmonitor.logicrepository.LogicRepositoryData;
import com.runtimeverification.rvmonitor.logicrepository.parser.logicrepositorysyntax.LogicRepositoryType;
import com.runtimeverification.rvmonitor.logicrepository.parser.logicrepositorysyntax.PropertyType;
import com.runtimeverification.rvmonitor.logicrepository.plugins.LogicPluginFactory;
import com.runtimeverification.rvmonitor.util.RVMException;

import rosmop.parser.ast.mopspec.Event;
import rosmop.parser.ast.mopspec.MonitorFile;
import rosmop.parser.ast.mopspec.Specification;
import rosmop.parser.ast.mopspec.Variable;
import rosmop.parser.main_parser.ROSMOPParser;
import rosmop.util.Tool;

public class Main {

	static String logicPluginDirPath, pathToOutputNoExt;

	public static void main(String[] args) {
		try {

			if(args.length == 0){
				throw new ROSMOPException("Make sure you provide at least "
						+ "one .rv file or a folder of .rv files.");
			}

			logicPluginDirPath = readLogicPluginDir();

			String pathToFile = "";
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
					processDirOfFiles(pathToFile);
				} else if(args[0].endsWith(File.separator)){
					fileToGetPath = new File(args[0]);
					pathToFile = fileToGetPath.getAbsolutePath();
					//					/home/cans-u/Desktop
					//					System.out.println(pathToFile);
					pathToOutputNoExt = pathToFile + File.separator + "rvmonitor";
					//					System.out.println(pathToOutputNoExt);
					processDirOfFiles(pathToFile);
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
					process(readyToProcess);
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
				processMultipleFiles(args);
			}

			//			CSpecification rvcParser = (CSpecification) new RVParserAdapter(readyToProcess);
			//			LogicRepositoryData cmgDataOut = sendToLogicRepository(rvcParser, logicPluginDirPath);
			//			outputCode(cmgDataOut, rvcParser, pathToOutputNoExt);  

			//			Tool.writeFile(specFile.toCppFile(), diffName ? filePath : file.getAbsolutePath(), ".cpp");
			//			Tool.writeFile(specFile.toHeaderFile(), diffName ? filePath : file.getAbsolutePath(), ".h");
			//			
			//			diffName = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void process(MonitorFile readyToProcess){
		CSpecification rvcParser = (CSpecification) new RVParserAdapter(readyToProcess);
		LogicRepositoryData cmgDataOut = null;
		try {
			//raw monitor
			if(rvcParser.getFormalism() != null)
				cmgDataOut = sendToLogicRepository(rvcParser, logicPluginDirPath);
			outputCode(cmgDataOut, rvcParser, pathToOutputNoExt); 
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void processMultipleFiles(String[] args) {
		Set<String> events = new HashSet<String>();
		Set<String> declarations = new HashSet<String>();

		try {
			if (!checkArguments(args)) {
				throw new ROSMOPException("Unrecognized file type! The ROSMOP specification file should have .rv as the extension.");
			}
			for (String arg : args) {
				MonitorFile f = ROSMOPParser.parse(arg);

				/* In case of multiple .rv files as input, all of the specifications 
				 * are gathered and checked for duplicate event names and field declarations;
				 * they should have unique names.*/
				for (Specification spec : f.getSpecifications()) {
					for (Event event : spec.getEvents()) {
						if (!events.add(event.getName())) 
							throw new ROSMOPException("Duplicate event names");
					}

					for (Variable var : spec.getSpecDeclarations()) {
						if(!declarations.add(var.getDeclaredName())) 
							throw new ROSMOPException("Duplicate field declarations");
					}
				}

				process(f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when the input is a directory (ends with a file separator)
	 * 
	 * @param arg name of the input directory
	 * @return one MonitorFile including all the specifications 
	 * 			from all .rv files in the input directory
	 * @throws ROSMOPException
	 */
	private static void processDirOfFiles(String arg) throws ROSMOPException {
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
			String fileNames[] = new String[noDirs.length];
			for (int i = 0; i < fileNames.length; i++) {
				fileNames[i] = noDirs[i].getAbsolutePath();
			}

			processMultipleFiles(fileNames);

		} catch (Exception e) {
			e.printStackTrace();
		}
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

	/**
	 * Generates the proper name for the logic plugin directory.
	 * @param basePath The location of the project.
	 * @return The location of the logic plugins.
	 */
	static public String readLogicPluginDir() {
		String logicPluginDirPath = System.getenv("LOGICPLUGINPATH");
		if (logicPluginDirPath == null || logicPluginDirPath.length() == 0) {
			try {
				throw new LogicException(
						"Unrecoverable error: please set LOGICPLUGINPATH variable to refer to the plugins directory");
			} catch (LogicException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return logicPluginDirPath;
	}

	/**
	 * Send the specification to the logic repository. The appropriate logic repository plugins
	 * are run on the code.
	 * @param rvcParser Extracted information from the RVM C file being used.
	 * @param logicPluginDirPath The location at which to find the logic repository plugins.
	 * @return The output of the logic plugins.
	 */
	static public LogicRepositoryData sendToLogicRepository(CSpecification rvcParser, 
			String logicPluginDirPath) throws LogicException {
		LogicRepositoryType cmgXMLIn = new LogicRepositoryType();
		PropertyType logicProperty = new PropertyType();

		// Get Logic Name and Client Name
		String logicName = rvcParser.getFormalism();
		if (logicName == null || logicName.length() == 0) {
			throw new LogicException("no logic names");
		}

		cmgXMLIn.setSpecName(rvcParser.getSpecName());

		logicProperty.setFormula(rvcParser.getFormula());
		logicProperty.setLogic(logicName);

		cmgXMLIn.setClient("CMonGen");
		StringBuilder events = new StringBuilder();
		for(String event : rvcParser.getEvents().keySet()){
			events.append(event);
			events.append(" ");
		}
		cmgXMLIn.setEvents(events.toString().trim());

		StringBuilder categories = new StringBuilder();
		for(String category : rvcParser.getHandlers().keySet()){
			categories.append(category);
			categories.append(" ");
		}
		cmgXMLIn.setCategories(categories.toString().trim());

		PropertyType prop = new PropertyType();
		prop.setLogic(rvcParser.getFormalism());
		prop.setFormula(rvcParser.getFormula());

		cmgXMLIn.setProperty(prop);

		LogicRepositoryData cmgDataIn = new LogicRepositoryData(cmgXMLIn);

		// Find a logic plugin and apply it
		ByteArrayOutputStream logicPluginResultStream 
		= LogicPluginFactory.process(logicPluginDirPath, logicName, cmgDataIn);

		// Error check
		if (logicPluginResultStream == null || logicPluginResultStream.size() == 0) {
			throw new LogicException("Unknown Error from Logic Plugins");
		}
		return new LogicRepositoryData(logicPluginResultStream);
	}

	/**
	 * Output code for the monitor. Creates a C and a H file, optionally compiles to LLVM.
	 * @param cmgDataOut The output of the logic repository plugins.
	 * @param rvcParser Extracted information from the RVM C file.
	 * @throws RVMException 
	 */
	static private void outputCode(LogicRepositoryData cmgDataOut, CSpecification rvcParser, 
			String outputPath) throws LogicException, FileNotFoundException, RVMException {
		if(cmgDataOut != null){
			LogicRepositoryType logicOutputXML = cmgDataOut.getXML();

			LogicPluginShellResult sr = evaluateLogicPluginShell(logicOutputXML, rvcParser, false);

			String rvcPrefix = (String) sr.properties.get("rvcPrefix");
			String specName = (String) sr.properties.get("specName");
			String constSpecName = (String) sr.properties.get("constSpecName");

			String cFile = rvcPrefix + specName + "Monitor.c";
			String hFile = rvcPrefix + specName + "Monitor.h";
			String hDef = rvcPrefix + constSpecName + "MONITOR_H";

			File cFileHandle = new File(cFile);
			FileOutputStream cfos = new FileOutputStream(cFileHandle);
			PrintStream cos = new PrintStream(cfos);

			FileOutputStream hfos = new FileOutputStream(new File(hFile));
			PrintStream hos = new PrintStream(hfos);
			hos.println("#ifndef " + hDef);
			hos.println("#define " + hDef + "\n");
			if(!((RVParserAdapter) rvcParser).getInit().isEmpty())
				hos.println("void init();");
			hos.println(sr.properties.get("header declarations"));
			hos.println("#endif");

			cos.println(rvcParser.getIncludes());
			cos.println("#include <stdlib.h>");
			cos.println(sr.properties.get("state declaration"));
			cos.println(rvcParser.getDeclarations());
			cos.println(sr.properties.get("categories"));
			cos.println(sr.properties.get("reset"));
			cos.println(sr.properties.get("monitoring body"));
			if(!((RVParserAdapter) rvcParser).getInit().isEmpty())
				cos.println("void init()\n" + ((RVParserAdapter) rvcParser).getInit());
			cos.println(sr.properties.get("event functions"));

			System.out.println(cFile + " and " + hFile + " have been generated");

			cos.close();
			hos.close();
		} else{
			String rvcPrefix = "__RAW_";
			String specName = rvcParser.getSpecName() + "_";
			String constSpecName = specName.toUpperCase();

			String cFile = rvcPrefix + specName + "Monitor.c";
			String hFile = rvcPrefix + specName + "Monitor.h";
			String hDef = rvcPrefix + constSpecName + "MONITOR_H";

			StringBuilder headerDecs = new StringBuilder();
			StringBuilder eventFuncs = new StringBuilder();

			for(String eventName : rvcParser.getEvents().keySet()){
				headerDecs.append("void\n");
				eventFuncs.append("void\n");
				String funcDecl = rvcPrefix + specName + eventName + rvcParser.getParameters().get(eventName);
				headerDecs.append(funcDecl + ";\n");
				eventFuncs.append(funcDecl + "\n");
				eventFuncs.append("{\n");
				eventFuncs.append(rvcParser.getEvents().get(eventName) + "\n"); 
				eventFuncs.append("}\n\n");
			}

			File cFileHandle = new File(cFile);
			FileOutputStream cfos = new FileOutputStream(cFileHandle);
			PrintStream cos = new PrintStream(cfos);

			FileOutputStream hfos = new FileOutputStream(new File(hFile));
			PrintStream hos = new PrintStream(hfos);
			hos.println("#ifndef " + hDef);
			hos.println("#define " + hDef + "\n");
			if(!((RVParserAdapter) rvcParser).getInit().isEmpty())
				hos.println("void init();");
			hos.println(headerDecs.toString());
			hos.println("#endif");

			cos.println(rvcParser.getIncludes());
			cos.println("#include <stdlib.h>");
			cos.println(rvcParser.getDeclarations());
			if(!((RVParserAdapter) rvcParser).getInit().isEmpty())
				cos.println("void init()\n" + ((RVParserAdapter) rvcParser).getInit());
			cos.println(eventFuncs.toString());

			System.out.println(cFile + " and " + hFile + " have been generated");

			cos.close();
			hos.close();
		}
	}

	/**
	 * Evaluate the appropriate logic plugin shell on the logic formalism.
	 * @param logicOutputXML The result of the logic repository plugins.
	 * @param rvcParser The extracted information from the C file.
	 * @return The result of applying the appropriate logic plugin shell to the parameters.
	 * @throws LogicException Something went wrong in applying the logic plugin shell.
	 * @throws RVMException 
	 */
	private static LogicPluginShellResult evaluateLogicPluginShell(
			LogicRepositoryType logicOutputXML, CSpecification rvcParser, boolean parametric)
					throws LogicException, RVMException {
		//TODO: make this reflective instead of using a switch over type
		String logic = logicOutputXML.getProperty().getLogic().toLowerCase();
		LogicPluginShell shell;

		if("fsm".equals(logic)) {
			shell = new CFSM((com.runtimeverification.rvmonitor.c.rvc.CSpecification) rvcParser, parametric);
		}
		else if("tfsm".equals(logic)) {
			shell = new CTFSM((com.runtimeverification.rvmonitor.c.rvc.CSpecification) rvcParser, parametric);
		}
		else if("cfg".equals(logic)) {
			shell = new CCFG((com.runtimeverification.rvmonitor.c.rvc.CSpecification) rvcParser, parametric);
		}
		else {
			throw new LogicException("Only finite logics and CFG are currently supported");
		}

		return shell.process(logicOutputXML, logicOutputXML.getEvents());
	}
}
