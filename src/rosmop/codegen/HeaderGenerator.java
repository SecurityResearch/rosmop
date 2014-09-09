package rosmop.codegen;

import java.io.FileNotFoundException;
import java.util.List;

import rosmop.ROSMOPException;
import rosmop.RVParserAdapter;
import rosmop.parser.ast.mopspec.Event;
import rosmop.parser.ast.mopspec.ROSEventDefinition;
import rosmop.parser.ast.visitor.CppGenerationVisitor;
import rosmop.parser.ast.visitor.GeneratorCommongUtil;
import rosmop.util.Tool;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;
import com.runtimeverification.rvmonitor.logicpluginshells.LogicPluginShellResult;

public class HeaderGenerator {
	
	protected final static SourcePrinter printer = new SourcePrinter();
	
	public static void generateHeader(CSpecification rvcParser, LogicPluginShellResult sr, String outputPath) throws FileNotFoundException, ROSMOPException{
		String rvcPrefix = "__RVCPP_";
		String specName = (String) sr.properties.get("specName");
		String constSpecName = (String) sr.properties.get("constSpecName");

		String hFile = rvcPrefix + specName + "Monitor.h";
		String hDef = rvcPrefix + constSpecName + "MONITOR_H";

//		FileOutputStream hfos = new FileOutputStream(new File(hFile));
//		PrintStream hos = new PrintStream(hfos);
		
		
		printer.printLn("#ifndef " + hDef);
		printer.printLn("#define " + hDef + "\n");
		printer.printLn(rvcParser.getIncludes());
		
		printer.printLn();
		printer.printLn("namespace rv");
		printer.printLn("{");
		printer.indent();

		// Inner monitor class declaration
		printer.printLn("class " + GeneratorUtil.MONITOR_CLASS_NAME);
		printer.printLn("{");
		printer.indent();

		//public methods
		printer.printLn("public:");
		printer.indent();

		//Constructor
		printer.printLn(GeneratorUtil.MONITOR_CLASS_NAME + "(std::string topic, ros::SubscribeOptions &" 
				+ GeneratorUtil.SUBSCRIBE_OPTIONS + ");");

		//Deconstructor
		printer.printLn("~" + GeneratorUtil.MONITOR_CLASS_NAME + "();");
		printer.printLn();
		
		generateCallbacks(((RVParserAdapter) rvcParser).getEventsList());
		
		if(!((RVParserAdapter) rvcParser).getInit().isEmpty())
			printer.printLn("void init();");
		printer.printLn((String) sr.properties.get("header declarations"));
		
		printer.printLn();

		printer.unindent();
		printer.printLn("private:");
		printer.indent();

		//        std::string topic_name;
		//        boost::shared_ptr<rv::ServerManager> server_manager;

		printer.printLn("std::string " + GeneratorUtil.TOPIC_PTR_NAME + ";");
		printer.printLn("boost::shared_ptr<rv::ServerManager> " + GeneratorUtil.SERVERMANAGER_PTR_NAME + ";");
		printer.printLn();

		printer.unindent();

		printer.unindent();
		printer.printLn("};");
		printer.unindent();

		printer.printLn("}");
		printer.unindent();

		printer.printLn();
		
		printer.printLn("#endif");
		
		Tool.writeFile(printer.getSource(), outputPath+hFile);
	}
	
	private static void generateCallbacks(List<Event> events) {

		for (Event event : events) {
			printer.print("void monitorCallback_" + event.getName());
			String eventMsgType = event.getMsgType().replace("/", "::");
			printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ");");
			printer.printLn();
		}
	}
	
	
//	namespace rv
//	{
//	    class RVMonitor
//	    {
//	        public:
//	            RVMonitor(std::string topic, ros::SubscribeOptions &ops_sub);
//	            ~RVMonitor();
//
//	            void monitorCallback_checkPoint(const sensor_msgs::JointState::ConstPtr& monitored_msg);
//	            void monitorCallback_safeTrigger(const landshark_msgs::PaintballTrigger::ConstPtr& monitored_msg);
//	            void init();
//
//	        private:
//	            std::string topic_name;
//	            boost::shared_ptr<rv::ServerManager> server_manager;
//
//	    };
//	}

}
