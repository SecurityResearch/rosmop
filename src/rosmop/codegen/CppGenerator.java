package rosmop.codegen;

import java.io.FileNotFoundException;
import java.util.HashMap;

import rosmop.ROSMOPException;
import rosmop.RVParserAdapter;
import rosmop.parser.ast.mopspec.Event;
import rosmop.parser.ast.mopspec.Variable;
import rosmop.util.Tool;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;

public class CppGenerator {

	protected final static SourcePrinter printer = new SourcePrinter();

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

		// print init()
		if(HeaderGenerator.hasInit){
			printer.printLn("void " + GeneratorUtil.MONITOR_CLASS_NAME + "::init(){\n");
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

		// print reset()
		for (CSpecification rvcParser : toWrite.keySet()) {
			if(toWrite.get(rvcParser) != null){
				printer.printLn(((String) toWrite.get(rvcParser).properties.get("reset")).replace("void\n__RVC_", "void " + GeneratorUtil.MONITOR_CLASS_NAME + "::__RVC_"));
				printer.printLn();
			}
		}

		// print constructor
		printConstructor(toWrite);

		// print monitor callback functions
		printMonitorCallbacks(toWrite);

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

	private static void printMonitorInsertion(HashMap<CSpecification, LogicPluginShellResult> toWrite) {
		printer.printLn("void initMonitorTopics()");
		printer.printLn("{");
		printer.indent();

		for (String topic : HeaderGenerator.addedTopics.keySet()) {
			printer.printLn(GeneratorUtil.MONITOR_TOPICS_VAR + ".insert(\"" + topic + "\");");
			printer.printLn(GeneratorUtil.MONITOR_TOPICS_AND_TYPES + "[\"" + topic + "\"] = \"" + HeaderGenerator.addedTopics.get(topic).get(0).getMsgType() + "\";");
		}
		printer.printLn();
		for (CSpecification rvcParser : toWrite.keySet()) {
			printer.printLn(GeneratorUtil.MONITOR_TOPICS_ALL + ".insert(\"" + rvcParser.getSpecName() + "\");");
		}
		printer.printLn();
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

		for (String topic : HeaderGenerator.addedTopics.keySet()) {
			if (!isFirst) {
				printer.print("else ");
			} else {
				isFirst = false;
			}

			printer.printLn("if (topic == \"" + topic + "\") {");

			printer.indent();
			printer.printLn(GeneratorUtil.ADVERTISE_OPTIONS + ".init<" + 
					HeaderGenerator.addedTopics.get(topic).get(0).getMsgType().replace("/", "::") + ">(topic, 1000);");
			printer.unindent();
			printer.printLn("}");
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

		for(String topic : HeaderGenerator.addedTopics.keySet()){
			if (!isFirst) printer.print("else ");
			else isFirst = false;

			printer.printLn("if (topic == \"" + topic + "\") {");
			printer.indent();

			if(HeaderGenerator.addedTopics.get(topic).size() == 1){

				printer.printLn(GeneratorUtil.SUBSCRIBE_OPTIONS + ".init<" + 
						HeaderGenerator.addedTopics.get(topic).get(0).getMsgType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
						GeneratorUtil.MONITOR_CLASS_NAME +
						"::monitorCallback_" + HeaderGenerator.addedTopics.get(topic).get(0).getName() + ", this, _1));");
			}else{
				String callback = "::mergedMonitorCallback_" + topic.replace("/", "");

				printer.printLn(GeneratorUtil.SUBSCRIBE_OPTIONS + ".init<" + 
						HeaderGenerator.addedTopics.get(topic).get(0).getMsgType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
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

	private static void printMonitorCallbacks(
			HashMap<CSpecification, LogicPluginShellResult> toWrite) {

		for (CSpecification rvcParser : toWrite.keySet()) {
			for (Event event : ((RVParserAdapter) rvcParser).getEventsList()) {
				if(HeaderGenerator.addedTopics.get(event.getTopic()).size() > 1){
					printer.printLn("void " + GeneratorUtil.MONITOR_CLASS_NAME + "::mergedMonitorCallback_" + event.getTopic().replace("/", "") + 
							"(const " + event.getMsgType().replace("/", "::") + 
							"::ConstPtr& " + GeneratorUtil.MONITORED_MSG_NAME + ")");
					printer.printLn("{");
					printer.printLn();
					printer.indent();

					//						printParametersBindingAll(HeaderGenerator.addedTopics.get(topic), GeneratorUtil.MONITORED_MSG_NAME);
					printer.printLn();

					for(Event mergeevents : HeaderGenerator.addedTopics.get(event.getTopic())){
						if(toWrite.get(rvcParser) == null || !rvcParser.getEvents().keySet().contains(mergeevents.getName())){
							printActionCode(mergeevents);
						}else{
							printRVMGeneratedFunction(mergeevents);
						}
						printer.printLn();
					}
					HeaderGenerator.addedTopics.get(event.getTopic()).clear();

					publishAndSerializeMsg();

					printer.unindent();
					printer.printLn();
					printer.printLn("}");
					printer.printLn();

				}else if(HeaderGenerator.addedTopics.get(event.getTopic()).size() > 0){
					printer.printLn("void " + GeneratorUtil.MONITOR_CLASS_NAME + "::monitorCallback_" + event.getName() + 
							"(const " + event.getMsgType().replace("/", "::") + 
							"::ConstPtr& " + GeneratorUtil.MONITORED_MSG_NAME + ")");
					printer.printLn("{");
					printer.printLn();
					printer.indent();

					//				printParametersBinding(HeaderGenerator.addedTopics.get(topic).get(0), GeneratorUtil.MONITORED_MSG_NAME);
					printer.printLn();

					if(toWrite.get(rvcParser) == null || !rvcParser.getEvents().keySet().contains(event.getName())){
						printActionCode(event);
					}else{
						printRVMGeneratedFunction(event);
					}
					printer.printLn();
					HeaderGenerator.addedTopics.get(event.getTopic()).remove(event);

					publishAndSerializeMsg();

					printer.unindent();
					printer.printLn();
					printer.printLn("}");
					printer.printLn();
				}
			}

			if(toWrite.get(rvcParser) != null){
				String str = (String) toWrite.get(rvcParser).properties.get("event functions");
//				String[] classstr = str.trim().split("void\n__RVC_");
//				int c = 1;
//				for (String string : classstr) {
//					System.out.println(c++ + "*****\n" + string);
//				}
				printer.printLn(str.replace("void\n__RVC_", "void " + GeneratorUtil.MONITOR_CLASS_NAME + "::__RVC_"));
				printer.printLn();
			}
		}
	}

	private static void printActionCode(Event event) {
		printer.printLn("if(monitor::" + GeneratorUtil.MONITOR_TOPICS_ENB + ".find(\"" + event.getSpecName() + "\") != monitor::" + GeneratorUtil.MONITOR_TOPICS_ENB + ".end())");
		//		printer.printLn("{");
		//		printer.indent();

		printer.print(event.getAction());
		printer.printLn();
		//		printer.unindent();
		//		printer.printLn("}");
		printer.printLn();
	}

	private static void printRVMGeneratedFunction(Event mergeevents) {
		//		__RVC_safeTrigger_checkPoint(std::string monitored_name, double monitored_position)
		printer.printLn("if(monitor::" + GeneratorUtil.MONITOR_TOPICS_ENB + 
				".find(\"" + mergeevents.getSpecName() + "\") != monitor::" + GeneratorUtil.MONITOR_TOPICS_ENB + ".end())");
		printer.printLn("{");
		printer.printLn();
		printer.indent();

		printer.print("__RVC_" + mergeevents.getSpecName() + "_" + mergeevents.getName() + "(");
		if(mergeevents.getParameters() != null && mergeevents.getParameters().size() != 0){
			int c = mergeevents.getParameters().size()-1;
			for (Variable var : mergeevents.getParameters()) {
				printer.print(var.getDeclaredName() + ((c-- > 0) ? ", " : ""));
			}
		}
		printer.printLn(");");

		printer.unindent();
		printer.printLn();
		printer.printLn("}");
		printer.printLn();		
	}

	private static void publishAndSerializeMsg() {
		// ros::SerializedMessage serializedMsg = ros::serialization::serializeMessage(msgName);
		// publishPtr->publish(serializedMsg);
		String serializedMessage = "ros::SerializedMessage serializedMsg = ros::serialization::serializeMessage(" + GeneratorUtil.MONITOR_COPY_MSG_NAME + ");";
		printer.print(serializedMessage);
		printer.printLn();
		serializedMessage = GeneratorUtil.SERVERMANAGER_PTR_NAME + "->publish(" + GeneratorUtil.TOPIC_PTR_NAME + ", serializedMsg);";
		printer.print(serializedMessage);
	}

}
