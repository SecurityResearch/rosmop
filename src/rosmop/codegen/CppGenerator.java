package rosmop.codegen;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import rosmop.ROSMOPException;
import rosmop.RVParserAdapter;
import rosmop.parser.ast.mopspec.Event;
import rosmop.util.Tool;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;

public class CppGenerator {

	protected final static SourcePrinter printer = new SourcePrinter();
	protected static HashMap<String, ArrayList<Event>> addedTopics = new HashMap<String, ArrayList<Event>>();

	public static void generateCpp(HashMap<CSpecification, LogicPluginShellResult> toWrite, String outputPath) throws FileNotFoundException, ROSMOPException{
		String cppFile = "rvmonitor.cpp";
		String hFile = "rvmonitor.h";

		printer.printLn("#include \"" + hFile + "\"");

		printer.printLn();
		printer.printLn("using namespace std;");
		printer.printLn("namespace rv");
		printer.printLn("{");
		printer.indent();

		// Print declared variables 
		printer.printLn("// Declarations of shared variables");
		for (CSpecification rvcParser : toWrite.keySet()) {
			printer.printLn(rvcParser.getDeclarations());

			printer.printLn();

			if(toWrite.get(rvcParser) != null){
				printer.printLn((String) toWrite.get(rvcParser).properties.get("state declaration"));
				printer.printLn((String) toWrite.get(rvcParser).properties.get("categories"));
				printer.printLn((String) toWrite.get(rvcParser).properties.get("monitoring body"));
				printer.printLn();
			}
		}

		printMonitorNamespace(toWrite);

		if(HeaderGenerator.hasInit){
			printer.printLn("void init(){\n");
			printer.indent();

			for (CSpecification rvcParser : toWrite.keySet()) {
				if(!((RVParserAdapter) rvcParser).getInit().isEmpty()){
					printer.printLn(((RVParserAdapter) rvcParser).getInit());
				}
			}

			printer.unindent();
			printer.printLn("}");
			printer.printLn();
		}

		for (CSpecification rvcParser : toWrite.keySet()) {
			if(toWrite.get(rvcParser) != null){
				printer.printLn((String) toWrite.get(rvcParser).properties.get("reset"));
				printer.printLn();
			}
		}


		// print constructor
		printConstructor(toWrite);

		for (CSpecification rvcParser : toWrite.keySet()) {
			if(toWrite.get(rvcParser) != null){
			printer.printLn((String) toWrite.get(rvcParser).properties.get("event functions"));
			printer.printLn();
			} else {
				// TODO: print raw monitor events!!
			}
		}

		printer.printLn();
		printer.unindent();
		printer.printLn("}");
		printer.printLn();

		Tool.writeFile(printer.getSource(), outputPath+cppFile);
	}

	private static void printMonitorNamespace(HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		printer.printLn("namespace monitor");
		printer.printLn("{");
		printer.indent();

		printer.printLn("std::set<std::string> " + GeneratorUtil.MONITOR_TOPICS_VAR + ";");
		printer.printLn("std::set<std::string> " + GeneratorUtil.MONITOR_TOPICS_ALL + ";");
		printer.printLn("std::set<std::string> " + GeneratorUtil.MONITOR_TOPICS_ENB + ";");
		printer.printLn("std::map<std::string,std::string> " + GeneratorUtil.MONITOR_TOPICS_AND_TYPES + ";");

		printer.printLn();

		printMonitorInsertion(toWrite);

		printAdvertisingOptions(toWrite);

		printer.unindent();
		printer.printLn("}");	
		printer.printLn();
	}

	private static void printAdvertisingOptions(HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		printer.printLn("void initAdvertiseOptions(std::string topic, ros::AdvertiseOptions" + " &" + GeneratorUtil.ADVERTISE_OPTIONS + ")");
		printer.printLn("{");
		printer.indent();

		//ops_pub: publisher registration

		//if(topic=="/landshark_control/base_velocity")
		//  ops_pub.init<geometry_msgs::TwistStamped>(topic,1000);
		boolean isFirst = true;

		for (String topic : addedTopics.keySet()) {
			if (!isFirst) {
				printer.print("else ");
			} else {
				isFirst = false;
			}

			printer.printLn("if (topic == \"" + topic + "\") {");

			printer.indent();
			printer.printLn(GeneratorUtil.ADVERTISE_OPTIONS + ".init<" + 
					addedTopics.get(topic).get(0).getMsgType().replace("/", "::") + ">(topic, 1000);");
			printer.unindent();
			printer.printLn("}");
		}

		printer.unindent();
		printer.printLn("}");
		printer.printLn();		
	}

	private static void printMonitorInsertion(HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		String topicName, msgType;

		printer.printLn("void initMonitorTopics()");
		printer.printLn("{");
		printer.indent();

		for (CSpecification rvcParser : toWrite.keySet()) {
			for (Event event : ((RVParserAdapter) rvcParser).getEventsList()) {
				topicName = event.getTopic();
				msgType = event.getMsgType();

				if(!addedTopics.containsKey(topicName)){
					addedTopics.put(topicName, new ArrayList<Event>());
					addedTopics.get(topicName).add(event);

					printer.printLn(GeneratorUtil.MONITOR_TOPICS_VAR + ".insert(\"" + topicName + "\");");
					printer.printLn(GeneratorUtil.MONITOR_TOPICS_AND_TYPES + "[\"" + topicName + "\"] = \"" + msgType + "\";");

				}else {
					addedTopics.get(topicName).add(event);
				}
			}

			printer.printLn(GeneratorUtil.MONITOR_TOPICS_ALL + ".insert(\"" + rvcParser.getSpecName() + "\");");
			printer.printLn();
		}

		printer.unindent();
		printer.printLn("}");
		printer.printLn();
	}

	/**
	 * Generate code for constructor
	 * @param toWrite 
	 */
	private static void printConstructor(HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		printer.printLn(GeneratorUtil.MONITOR_CLASS_NAME + "::" + GeneratorUtil.MONITOR_CLASS_NAME 
				+ "(string topic, ros::SubscribeOptions &" 
				+ GeneratorUtil.SUBSCRIBE_OPTIONS + ")");
		printer.printLn("{");
		printer.indent();

		//	     topic_name = topic;
		//	     server_manager = rv::ServerManager::instance(
		printer.printLn(GeneratorUtil.TOPIC_PTR_NAME + " = topic;");
		printer.printLn(GeneratorUtil.SERVERMANAGER_PTR_NAME + " = rv::ServerManager::instance();");

		if(HeaderGenerator.hasInit){
			printer.printLn();
			printer.printLn("init();");
		}

		printer.printLn();

		//ops_sub: subscriber registration

		// if(topic=="/landshark_control/base_velocity")
		// ops_sub.init<geometry_msgs::TwistStamped>(topic,1000,boost::bind(
		// &RVt::monitorCallbackLandsharkBaseVelocityReverse, this, _1));
		boolean isFirst = true;

		for(String topic : addedTopics.keySet()){
			if (!isFirst) printer.print("else ");
			else isFirst = false;

			printer.printLn("if (topic == \"" + topic + "\") {");
			printer.indent();

			if(addedTopics.get(topic).size() == 1){

				printer.printLn(GeneratorUtil.SUBSCRIBE_OPTIONS + ".init<" + 
						addedTopics.get(topic).get(0).getMsgType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
						GeneratorUtil.MONITOR_CLASS_NAME +
						"::monitorCallback_" + addedTopics.get(topic).get(0).getName() + ", this, _1));");
			}else{
				String callback = "::mergedMonitorCallback_" + topic.replace("/", "");

				printer.printLn(GeneratorUtil.SUBSCRIBE_OPTIONS + ".init<" + 
						addedTopics.get(topic).get(0).getMsgType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
						GeneratorUtil.MONITOR_CLASS_NAME +
						callback + ", this, _1));");
			}

			printer.unindent();
			printer.printLn("}");
		}

		printer.unindent();
		printer.printLn("}");
		printer.printLn();
	}
}
