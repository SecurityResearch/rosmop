package rosmop.parser.ast.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.ROSMOPSpecFile;
import rosmop.parser.ast.mopspec.ROSBodyDeclaration;
import rosmop.parser.ast.mopspec.ROSEventDefinition;
import rosmop.parser.ast.mopspec.ROSFieldDeclaration;
import rosmop.parser.ast.mopspec.ROSIncludeDeclaration;
import rosmop.parser.ast.mopspec.ROSMOPParameter;
import rosmop.parser.ast.mopspec.ROSMOPProperty;
import rosmop.parser.ast.mopspec.ROSMOPSpec;
import rosmop.parser.ast.mopspec.ROSType;

/**
 * Visitor to generate monitor.h file
 * 
 * @author Qingzhou Luo
 *
 */
public class HeaderGenerationVisitor implements VoidVisitor<Object> {

	protected final SourcePrinter printer = new SourcePrinter();
	HashMap<String, ArrayList<ROSEventDefinition>> addedTopics = new HashMap<String, ArrayList<ROSEventDefinition>>();
	ArrayList<String> noDoubles = new ArrayList<String>();

	public String getSource() {
		return printer.getSource();
	}

	@Override
	public void visit(Node node, Object arg) {
	}

	@Override
	public void visit(ROSMOPSpecFile specFile, Object arg) {
		this.printDefAndInclude(specFile);
		this.printer.printLn();
		this.printer.printLn();
		this.printer.printLn("namespace rv");
		this.printer.printLn("{");
		this.printer.indent();

		// Inner monitor class declaration
		this.printer.printLn("class " + GeneratorCommongUtil.MONITOR_CLASS_NAME);
		this.printer.printLn("{");
		this.printer.indent();

		//public methods
		this.printer.printLn("public:");
		this.printer.indent();

		//Constructor
		this.printer.printLn(GeneratorCommongUtil.MONITOR_CLASS_NAME + "(std::string topic, ros::SubscribeOptions &" 
				+ GeneratorCommongUtil.SUBSCRIBE_OPTIONS + ");");

		//Deconstructor
		this.printer.printLn("~" + GeneratorCommongUtil.MONITOR_CLASS_NAME + "();");
		this.printer.printLn();

		//ros::SubscribeOptions ops_sub;
		//ros::AdvertiseOptions ops_pub;
		//SubscriptionPtr sub_ptr;
		//ros::PublicationPtr pub_ptr;
		//this.printer.printLn("ros::SubscribeOptions " + GeneratorCommongUtil.SUBSCRIBE_OPTIONS + ";");
		//this.printer.printLn("ros::AdvertiseOptions " + GeneratorCommongUtil.ADVERTISE_OPTIONS + ";");

		//NO NEED AFTER POINTER MAP
		/*		this.printer.printLn("SubscriptionPtr " + GeneratorCommongUtil.SUBSCRIBE_PTR_NAME + ";");
		this.printer.printLn("ros::PublicationPtr " + GeneratorCommongUtil.PUBLICATION_PTR_NAME + ";");

		//initPubSubPtr
		this.printer.printLn("void initPubSubPtr(ros::PublicationPtr &" + GeneratorCommongUtil.PUBLICATION_PTR_NAME 
				+ "_arg, SubscriptionPtr &" + GeneratorCommongUtil.SUBSCRIBE_PTR_NAME + "_arg);");

		this.printer.printLn();
		this.printer.printLn();
		 */

		fillAddedTopics(specFile);

		for (ROSMOPSpec spec : specFile.getSpecs()) {
			spec.accept(this, arg);
		}

		if(CppGenerationVisitor.isAuthenticated())
			generateAuthCallback();

		printProperty(specFile);

		this.printer.printLn("void init();");

		this.printer.printLn();

		this.printer.unindent();
		this.printer.printLn("private:");
		this.printer.indent();

		//        std::string topic_name;
		//        boost::shared_ptr<rv::ServerManager> server_manager;

		this.printer.printLn("std::string " + GeneratorCommongUtil.TOPIC_PTR_NAME + ";");
		this.printer.printLn("boost::shared_ptr<rv::ServerManager> " + GeneratorCommongUtil.SERVERMANAGER_PTR_NAME + ";");
		this.printer.printLn();

		if(CppGenerationVisitor.isAuthenticated())
			printAuthenticationRelated();

		this.printer.unindent();

		this.printer.unindent();
		this.printer.printLn("};");
		this.printer.unindent();

		this.printer.printLn("}");
		this.printer.unindent();

		this.printer.printLn();
		this.printer.printLn("#endif");
	}

	private void printAuthenticationRelated() {
		this.printer.printLn("struct Impl;");
		this.printer.printLn("boost::scoped_ptr<Impl> impl_;");

		this.printer.printLn();

		this.printer.printLn("template <typename M>");
		this.printer.printLn("inline ros::SerializedMessage auth_serialize_message(const M& message){");

		this.printer.indent();

		this.printer.printLn("using namespace ros::serialization;");
		this.printer.printLn("ros::SerializedMessage m;");
		this.printer.printLn("m.num_bytes = serializationLength(message);");
		this.printer.printLn("m.buf.reset(new uint8_t[m.num_bytes]);");

		this.printer.printLn();

		this.printer.printLn("OStream s(m.buf.get(), (uint32_t)m.num_bytes);");
		this.printer.printLn("m.message_start = s.getData();");
		this.printer.printLn("serialize(s, message);");

		this.printer.printLn();

		this.printer.printLn("return m;");

		this.printer.unindent();
		this.printer.printLn("}");

		this.printer.printLn();

		this.printer.printLn("template <typename M>");
		this.printer.printLn(GeneratorCommongUtil.AUTHENTICATED_MSG_TYPE + " make_auth_message(const M& message);");		
	}

	private void generateAuthCallback() {
		//		void authCallBack( const msg_auth::AuthenticatedMessage::ConstPtr& message);
		this.printer.print("void " + GeneratorCommongUtil.AUTHENTICATED_MSG_CALLBACK);
		this.printer.print("(const " + GeneratorCommongUtil.AUTHENTICATED_MSG_TYPE + "::ConstPtr& message);");
		this.printer.printLn();		
	}

	private void printProperty(ROSMOPSpecFile specFile) {
		boolean isPrinted = false;
		for (ROSMOPSpec spec : specFile.getSpecs()) {
			if(spec.getProperty() != null){
				if(spec.getProperty().getName().equalsIgnoreCase("rawmonitor")){
					if(!isPrinted){
						this.printer.print("void" /*spec.getProperty().getType()*/ + " ");
						this.printer.printLn(spec.getProperty().getName() + "();");
						isPrinted = true;
					}
				}
			}
		}
	}

	@Override
	public void visit(ROSMOPSpec spec, Object arg) {

		ArrayList<ROSEventDefinition> allEvents = new ArrayList<ROSEventDefinition>();
		ROSMOPProperty prop;

		allEvents.addAll(spec.getEvents());

		prop = spec.getProperty();
		if(prop != null){
			allEvents.addAll(prop.getPublishEvents());
		}

		for (ROSEventDefinition event : allEvents) {
			generateCallbacks(event, arg);
		}

		for (ROSBodyDeclaration declaration : spec.getDeclarations()) {
			declaration.accept(this, arg);
		}
	}

	private void generateCallbacks(ROSEventDefinition event, Object arg) {

		if(!noDoubles.contains(event.getTopicName())){
			if(addedTopics.get(event.getTopicName()).size() == 1){
				event.accept(this, arg);
			}else{
				String callback = "mergedMonitorCallback_" + event.getTopicName().replace("/", "");

				this.printer.print("void " + callback);
				String eventMsgType = event.getMessageType().replace("/", "::");
				this.printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ");");
				this.printer.printLn();

				if(CppGenerationVisitor.isAuthenticated() && event.isAuthenticated()){
					this.printer.print("void auth" + Character.toUpperCase(callback.charAt(0)) + callback.substring(1));
					this.printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ");");
					this.printer.printLn();
				}
			}
			noDoubles.add(event.getTopicName());
		}
	}

	private void fillAddedTopics(ROSMOPSpecFile specFile) {

		String topicName;
		ArrayList<ROSEventDefinition> allEvents;
		ROSMOPProperty prop;

		for (ROSMOPSpec spec : specFile.getSpecs()) {
			allEvents = new ArrayList<ROSEventDefinition>();
			allEvents.addAll(spec.getEvents());

			prop = spec.getProperty();
			if(prop != null){
				allEvents.addAll(prop.getPublishEvents());
			}

			for (ROSEventDefinition event : allEvents) {
				topicName = event.getTopicName();

				if(!addedTopics.containsKey(topicName)){
					addedTopics.put(topicName, new ArrayList<ROSEventDefinition>());
					addedTopics.get(topicName).add(event);
				}else {
					addedTopics.get(topicName).add(event);
				}
			}
		}
	}

	@Override
	public void visit(ROSMOPParameter parameter, Object arg) {
	}

	@Override
	public void visit(ROSEventDefinition event, Object arg) {
		this.printer.print("void monitorCallback_" + event.getEventName());
		String eventMsgType = event.getMessageType().replace("/", "::");
		this.printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ");");
		this.printer.printLn();

		if(CppGenerationVisitor.isAuthenticated() && event.isAuthenticated()){
			this.printer.print("void authMonitorCallback_" + event.getEventName());
			this.printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ");");
			this.printer.printLn();
		}
	}

	@Override
	public void visit(ROSType type, Object arg) {
	}

	private void printDefAndInclude(ROSMOPSpecFile specFile) {
		// We may need to pass specification name
		printer.printLn("#ifndef RVCPP_RVMONITOR_H");
		printer.printLn("#define RVCPP_RVMONITOR_H");
		printer.printLn();

		//		#include "rv/xmlrpc_manager.h"
		//		#include "rv/connection_manager.h"
		//		#include "rv/subscription.h"
		//		#include "std_msgs/String.h"
		//		#include "ros/subscribe_options.h"
		//		#include "ros/advertise_options.h"
		//		#include "ros/callback_queue.h"
		//		#include "geometry_msgs/TwistStamped.h"
		//		#include "sensor_msgs/JointState.h"
		//		#include <landshark_msgs/PaintballTrigger.h>
		//		#include <landshark_msgs/JointControl.h>
		//		#include <landshark_msgs/BoolStamped.h>
		//		#include <landshark_msgs/BatteryState.h>
		//		#include <landshark_msgs/NavigateToWayPointsActionGoal.h>
		//		#include <landshark_msgs/SpoofStatusList.h>
		//		#include <rosgraph_msgs/Log.h>
		//		#include <sensor_msgs/Image.h>
		//		#include <sensor_msgs/CompressedImage.h>
		//		#include <sensor_msgs/Imu.h>
		//		#include <sensor_msgs/NavSatFix.h>
		//		#include "msg_auth/AuthenticatedMessage.h"

		printer.printLn("#include \"rv/xmlrpc_manager.h\"");
		printer.printLn("#include \"rv/connection_manager.h\"");
		printer.printLn("#include \"rv/server_manager.h\"");
		printer.printLn("#include \"rv/subscription.h\"");
		printer.printLn("#include \"ros/publication.h\"");
		printer.printLn("#include \"std_msgs/String.h\"");
		printer.printLn("#include \"ros/subscribe_options.h\"");
		printer.printLn("#include \"ros/advertise_options.h\"");
		printer.printLn("#include \"ros/callback_queue.h\"");
		printer.printLn("#include <rosgraph_msgs/Log.h>");

		// Print all referred message types
		// printer.printLn("#include \"geometry_msgs/TwistStamped.h\"");
		// printer.printLn("#include \"sensor_msgs/JointState.h\"");
		Set<String> union = new HashSet<String>();
		for (ROSMOPSpec spec : specFile.getSpecs()) {
			union.addAll(spec.getAllMsgTypes());
		}

		for (String msgType : union) {
			printer.printLn("#include \"" + msgType + ".h\"");
		}

		for(ROSIncludeDeclaration includes : specFile.getIncludeDeclarations()){
			printer.printLn(includes.getLibrary());
		}

		//		printer.printLn("#include <landshark_msgs/PaintballTrigger.h>");
		//		printer.printLn("#include <landshark_msgs/JointControl.h>");
		//		printer.printLn("#include <landshark_msgs/BoolStamped.h>");
		//		printer.printLn("#include <landshark_msgs/BatteryState.h>");
		//		printer.printLn("#include <landshark_msgs/NavigateToWayPointsActionGoal.h>");
		//		printer.printLn("#include <landshark_msgs/SpoofStatusList.h>");
		//		printer.printLn("#include <sensor_msgs/Image.h>");
		//		printer.printLn("#include <sensor_msgs/CompressedImage.h>");
		//		printer.printLn("#include <sensor_msgs/Imu.h>");
		//		printer.printLn("#include <sensor_msgs/NavSatFix.h>");
		if(CppGenerationVisitor.isAuthenticated())
			printer.printLn("#include <msg_auth/AuthenticatedMessage.h>");
		printer.printLn("#include <boost/scoped_ptr.hpp>");
		printer.printLn("#include <ros/serialization.h>");
	}

	@Override
	public void visit(ROSBodyDeclaration declaration, Object arg) {
		//		if (declaration instanceof ROSFieldDeclaration) {
		//			ROSFieldDeclaration fieldDeclaration = (ROSFieldDeclaration)declaration; 
		//			this.printer.print("external " + fieldDeclaration.getDeclaredType() + " "
		//					 + fieldDeclaration.getDeclaredName());
		//			this.printer.printLn(";");
		//		}
	}

}
