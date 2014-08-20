package rosmop.parser.ast.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.ROSMOPSpecFile;
import rosmop.parser.ast.mopspec.ROSBodyDeclaration;
import rosmop.parser.ast.mopspec.ROSEventDefinition;
import rosmop.parser.ast.mopspec.ROSFieldDeclaration;
import rosmop.parser.ast.mopspec.ROSMOPParameter;
import rosmop.parser.ast.mopspec.ROSMOPParameters;
import rosmop.parser.ast.mopspec.ROSMOPProperty;
import rosmop.parser.ast.mopspec.ROSMOPSpec;
import rosmop.parser.ast.mopspec.ROSMessageBasicPattern;
import rosmop.parser.ast.mopspec.ROSMessageComplexPattern;
import rosmop.parser.ast.mopspec.ROSType;
import rosmop.parser.ast.mopspec.ROSVariableDeclarator;
import rosmop.util.MessageParser;

/**
 * Visitor to generate monitor.cpp file
 * 
 * @author Cansu Erdogan
 * 
 */
public class CppGenerationVisitor implements VoidVisitor<Object> {

	protected final SourcePrinter printer = new SourcePrinter();
	HashMap<String, ArrayList<ROSEventDefinition>> addedTopics = new HashMap<String, ArrayList<ROSEventDefinition>>();
	private static boolean authenticated = false, initBlock = false;

	public static boolean isAuthenticated() {
		return authenticated;
	}

	public static void setAuthenticated(boolean authenticated) {
		CppGenerationVisitor.authenticated = authenticated;
	}

	public String getSource() {
		return printer.getSource();
	}

	protected void printSpecParameters(ROSMOPParameters parameters, Object arg) {
		printer.print("(");
		if (parameters != null) {
			for (Iterator<ROSMOPParameter> i = parameters.iterator(); i.hasNext();) {
				ROSMOPParameter t = i.next();
				t.accept(this, arg);
				if (i.hasNext()) {
					printer.print(", ");
				}
			}
		}
		printer.print(")");
	}

	protected void printParameterTypes(List<ROSType> args, Object arg) {
	}

	protected void printTypePatterns(List<ROSType> args, Object arg) {
	}

	public void visit(ROSType t, Object arg) {
	}

	public void visit(ROSMessageBasicPattern m, Object arg) {
	}

	public void visit(ROSMessageComplexPattern m, Object arg) {
	}

	public void visit(ROSMOPParameter parameter, Object arg) {
	}

	protected void printSpecModifiers(int modifiers) {
	}

	public void visit(Node node, Object arg) {
		throw new IllegalStateException(node.getClass().getName());
	}

	/**
	 * Visitor functions for ROSMOP components
	 */
	public void visit(ROSMOPSpecFile specFile, Object arg) {
		this.printer.printLn("#include \"" + GeneratorCommongUtil.MONITOR_FILE_NAME + ".h\"");

		if(isAuthenticated()){
			this.printer.printLn("#include <msg_auth/auth_subscribe_options.h>");
			this.printer.printLn("#include <msg_auth/hmac_utils.h>");		
			this.printer.printLn("#include \"msg_auth/hmac.h\"");
		}

		this.printer.printLn();

		this.printer.printLn("using namespace std;");
		this.printer.printLn("namespace rv");
		this.printer.printLn("{");
		this.printer.indent();

		//Print declared variables 
		this.printer.printLn("// Declarations of shared variables");
		for (ROSBodyDeclaration declaration : specFile.getAllDeclarations()) {
			declaration.accept(this, arg);
		}

		this.printer.printLn();

		this.printMonitorNamespace(specFile);

		this.printer.printLn();

		if(isAuthenticated())
			this.printAuthenticationCode();

		this.printer.printLn();

		this.printInit(specFile);

		this.printer.printLn();

		this.printConstructor(specFile);

		this.printer.printLn();

		if(isAuthenticated())
			this.printAuthCallBack();

		this.printer.printLn();

		//NOT NEEDED AFTER POINTER MAP
		/*	void RVMonitor::initPubSubPtr(ros::PublicationPtr &pub, SubscriptionPtr &sub){
			  pub_ptr = pub;
			  sub_ptr = sub;
			}
				this.printer.printLn("void " + GeneratorCommongUtil.MONITOR_CLASS_NAME +"::initPubSubPtr(ros::PublicationPtr &" + GeneratorCommongUtil.PUBLICATION_PTR_NAME 
						+ "_arg, SubscriptionPtr &" + GeneratorCommongUtil.SUBSCRIBE_PTR_NAME + "_arg)");
				this.printer.printLn("{");
				this.printer.indent();
				this.printer.printLn(GeneratorCommongUtil.PUBLICATION_PTR_NAME + " = " + GeneratorCommongUtil.PUBLICATION_PTR_NAME + "_arg;");
				this.printer.printLn(GeneratorCommongUtil.SUBSCRIBE_PTR_NAME + " = " + GeneratorCommongUtil.SUBSCRIBE_PTR_NAME + "_arg;");
				this.printer.unindent();
				this.printer.printLn("}");
				this.printer.printLn();
		 */

		//Print events
		Iterator<String> it = addedTopics.keySet().iterator();
		while (it.hasNext()) {
			String next = it.next();

			if(addedTopics.get(next).size() == 1){
				addedTopics.get(next).get(0).accept(this, arg);
			}else{
				printMergedCallback(addedTopics.get(next));
			}
		}

		//Print the property if exists
		printProperty(specFile);

		this.printer.printLn();
		this.printer.unindent();
		this.printer.printLn("}");
		this.printer.printLn();
		//		printer.unindent();
		//		printer.printLn("}");

	}

	private void printInit(ROSMOPSpecFile specFile) {
		boolean initPrinted = false;
		for( ROSMOPSpec spec : specFile.getSpecs()){
			if(spec.getInitblock() != null){
				if(!initPrinted){
					this.printer.printLn("void " + GeneratorCommongUtil.MONITOR_CLASS_NAME + "::" + "init()");
					this.printer.printLn("{");
					this.printer.indent();
					initPrinted = true;
					initBlock = true;
				}

				this.printer.printLn(spec.getInitblock().getContent());
			}
		}

		if(initPrinted){ 
			this.printer.unindent(); 
			this.printer.printLn("}"); 
		}
	}

	private void printAdvertisingOptions(ROSMOPSpecFile specFile) {

		this.printer.printLn("void initAdvertiseOptions(std::string topic, ros::AdvertiseOptions" + " &" + GeneratorCommongUtil.ADVERTISE_OPTIONS + ")");
		this.printer.printLn("{");
		this.printer.indent();

		//ops_pub: publisher registration

		//if(topic=="/landshark_control/base_velocity")
		//  ops_pub.init<geometry_msgs::TwistStamped>(topic,1000);
		boolean isFirst = true;
		ArrayList<String> noDoubles = new ArrayList<String>();

		for (ROSMOPSpec spec : specFile.getSpecs()) {
			ArrayList<ROSEventDefinition> publishEvts = new ArrayList<ROSEventDefinition>();
			ROSMOPProperty prop;

			publishEvts.addAll(spec.getEvents());

			prop = spec.getProperty();
			if(prop != null)
				publishEvts.addAll(prop.getPublishEvents());

			for (ROSEventDefinition event : publishEvts) {

				if(!noDoubles.contains(event.getTopicName())){
					if (!isFirst) {
						this.printer.print("else ");
					} else {
						isFirst = false;
					}

					if(!event.isAuthenticated()){
						this.printer.printLn("if (topic == \"" + event.getTopicName()+ "\") {");
					} else{
						this.printer.printLn("if (topic == \"" + event.getTopicName().substring(0, event.getTopicName().lastIndexOf("/hmac")) + "\") {");	
					}
					this.printer.indent();
					this.printer.printLn(GeneratorCommongUtil.ADVERTISE_OPTIONS + ".init<" + 
							event.getMessageType().replace("/", "::") + ">(topic, 1000);");
					this.printer.unindent();
					this.printer.printLn("}");


					if(isAuthenticated() && event.isAuthenticated()){
						this.printer.printLn("else if (topic == \"" + event.getTopicName() + "\") {");
						this.printer.indent();
						this.printer.printLn(GeneratorCommongUtil.ADVERTISE_OPTIONS + ".init<" + 
								GeneratorCommongUtil.AUTHENTICATED_MSG_TYPE + ">(topic, 1000);");
						this.printer.unindent();
						this.printer.printLn("}");
					}

					noDoubles.add(event.getTopicName());
				}
			}
		}

		this.printer.unindent();
		this.printer.printLn("}");
		this.printer.printLn();
	}

	private void printProperty(ROSMOPSpecFile specFile) {

		boolean isPrinted = false;
		for (ROSMOPSpec spec : specFile.getSpecs()) {
			if(spec.getProperty() != null){
				if(!isPrinted && spec.getProperty().getName().equalsIgnoreCase("rawmonitor")){
					this.printer.print("void" /*spec.getProperty().getType()*/+ " " + GeneratorCommongUtil.MONITOR_CLASS_NAME + "::" + spec.getProperty().getName() + "(){");
					this.printer.printLn();
					this.printer.indent();
					isPrinted = true;
				}

				printPropertyCode(spec.getProperty());
				//this.printer.printLn(spec.getProperty().getContent());
			}
		} 

		if(isPrinted){
			this.printer.unindent();
			this.printer.printLn("}");
		}
	}

	private void printAuthCallBack() {		
		this.printer.printLn("void " + GeneratorCommongUtil.MONITOR_CLASS_NAME + "::" + GeneratorCommongUtil.AUTHENTICATED_MSG_CALLBACK + "(const "+ GeneratorCommongUtil.AUTHENTICATED_MSG_TYPE + "::ConstPtr& message){");
		this.printer.indent();

		this.printer.printLn("if(impl_){");
		this.printer.indent();

		this.printer.printLn("if(impl_->hmac_.check_auth(*message, impl_->pub_id, impl_->id_seq_map )){");
		this.printer.indent();

		this.printer.printLn("ros::SerializedMessage m = to_serialized_message(*message);");
		this.printer.printLn("impl_->ops_.helper->call(m);");

		this.printer.unindent();
		this.printer.printLn("}");

		this.printer.unindent();
		this.printer.printLn("}");

		this.printer.unindent();
		this.printer.printLn("}");
	}

	private void printAuthenticationCode() {

		this.printer.printLn("struct " + GeneratorCommongUtil.MONITOR_CLASS_NAME + "::Impl {");
		this.printer.indent();

		this.printer.printLn("msg_auth::HMACUtil hmac_;");
		this.printer.printLn("msg_auth::AuthSubscribeOptions ops_;");
		this.printer.printLn("unsigned long pub_id;");
		this.printer.printLn("typedef boost::unordered_map<unsigned long, unsigned long> map;");
		this.printer.printLn("map id_seq_map;");
		this.printer.printLn("unsigned long id;");
		this.printer.printLn("unsigned long seq_num;");

		this.printer.printLn();										

		this.printer.printLn("Impl()");
		this.printer.indent();
		this.printer.printLn(": hmac_( key_length_, key_file_ )");
		this.printer.unindent();
		this.printer.printLn("{");
		this.printer.printLn("}");

		this.printer.printLn();

		this.printer.printLn("~Impl()");
		this.printer.printLn("{");
		this.printer.printLn("}");

		this.printer.unindent();
		this.printer.printLn("};");

		this.printer.printLn();

		this.printer.printLn("template <typename M>");
		this.printer.printLn("msg_auth::AuthenticatedMessage " + GeneratorCommongUtil.MONITOR_CLASS_NAME + "::make_auth_message(const M& message){");
		this.printer.indent();

		this.printer.printLn("ros::SerializedMessage m = auth_serialize_message<M>( message );");
		this.printer.printLn("msg_auth::AuthenticatedMessage m2;");
		this.printer.printLn("impl_->hmac_.make_auth(m, m2, impl_->id, impl_->seq_num );");
		this.printer.printLn("return m2;");

		this.printer.unindent();
		this.printer.printLn("}");
	}

	private void printMergedCallback(ArrayList<ROSEventDefinition> eventList) {

		String callback = "::mergedMonitorCallback_" + eventList.get(0).getTopicName().replace("/", "");

		printer.print("void " + GeneratorCommongUtil.MONITOR_CLASS_NAME +
				callback);
		String eventMsgType = eventList.get(0).getMessageType().replace("/", "::");
		printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ")");
		printer.printLn("{");
		printer.printLn();
		printer.indent();

		printParametersBindingAll(eventList, GeneratorCommongUtil.MONITORED_MSG_NAME);
		printer.printLn();

		for(ROSEventDefinition event : eventList){
			printActionCode(event);
			printer.printLn();
		}

		publishAndSerializeMsg(GeneratorCommongUtil.SERVERMANAGER_PTR_NAME, GeneratorCommongUtil.MONITOR_COPY_MSG_NAME);

		printer.unindent();
		printer.printLn();
		printer.printLn("}");
		printer.printLn();

		//authentication
		if(isAuthenticated() && eventList.get(0).getTopicName().endsWith("/hmac")){
			String acallback = "::authMergedMonitorCallback_" + eventList.get(0).getTopicName().replace("/", "");

			printer.print("void " + GeneratorCommongUtil.MONITOR_CLASS_NAME +
					acallback);
			printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ")");
			printer.printLn("{");
			printer.printLn();
			printer.indent();

			printParametersBindingAll(eventList, GeneratorCommongUtil.MONITORED_MSG_NAME);
			printer.printLn();

			for(ROSEventDefinition event : eventList){
				printActionCode(event);
				printer.printLn();
			}

			//	      msg_auth::AuthenticatedMessage am = make_auth_message(rv_msg);
			printer.printLn(GeneratorCommongUtil.AUTHENTICATED_MSG_TYPE + " am = make_auth_message(" + GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + ");");
			publishAndSerializeMsg(GeneratorCommongUtil.PUBLICATION_PTR_NAME, GeneratorCommongUtil.MONITOR_COPY_MSG_NAME);

			printer.unindent();
			printer.printLn();
			printer.printLn("}");
			printer.printLn();
		}

	}

	private void printMonitorNamespace(ROSMOPSpecFile specFile) {
		this.printer.printLn("namespace monitor");
		this.printer.printLn("{");
		this.printer.indent();

		this.printer.printLn("std::set<std::string> " + GeneratorCommongUtil.MONITOR_TOPICS_VAR + ";");
		this.printer.printLn("std::set<std::string> " + GeneratorCommongUtil.MONITOR_TOPICS_ALL + ";");
		this.printer.printLn("std::set<std::string> " + GeneratorCommongUtil.MONITOR_TOPICS_ENB + ";");
		this.printer.printLn("std::map<std::string,std::string> " + GeneratorCommongUtil.MONITOR_TOPICS_AND_TYPES + ";");

		this.printer.printLn();

		this.printer.printLn("void initMonitorTopics()");
		this.printer.printLn("{");
		this.printer.indent();

		this.printMonitorInsertion(specFile);

		this.printer.unindent();
		this.printer.printLn("}");

		this.printer.printLn();

		printAdvertisingOptions(specFile);

		this.printer.unindent();
		this.printer.printLn("}");		
	}

	private void printMonitorInsertion(ROSMOPSpecFile specFile) {

		String specName, topicName, msgType;
		ArrayList<ROSEventDefinition> publishEvts;
		ROSMOPProperty prop;

		for (ROSMOPSpec spec : specFile.getSpecs()) {
			specName = spec.getName();
			publishEvts = new ArrayList<ROSEventDefinition>();
			publishEvts.addAll(spec.getEvents());

			prop = spec.getProperty();
			if(prop != null)
				publishEvts.addAll(prop.getPublishEvents());

			for (ROSEventDefinition event : publishEvts) {
				topicName = event.getTopicName();
				msgType = event.getMessageType();

				if(!addedTopics.containsKey(topicName)){
					addedTopics.put(topicName, new ArrayList<ROSEventDefinition>());
					addedTopics.get(topicName).add(event);

					if(event.isAuthenticated()){
						this.printer.printLn(GeneratorCommongUtil.MONITOR_TOPICS_VAR + ".insert(\"" + topicName.substring(0, topicName.lastIndexOf("/hmac")) + "\");");
						this.printer.printLn(GeneratorCommongUtil.MONITOR_TOPICS_AND_TYPES + "[\"" + topicName.substring(0, topicName.lastIndexOf("/hmac")) + "\"] = \"" + msgType + "\";");
					}
					this.printer.printLn(GeneratorCommongUtil.MONITOR_TOPICS_VAR + ".insert(\"" + topicName + "\");");
					this.printer.printLn(GeneratorCommongUtil.MONITOR_TOPICS_AND_TYPES + "[\"" + topicName + "\"] = \"" + msgType + "\";");

				}else {
					addedTopics.get(topicName).add(event);
				}
			}

			this.printer.printLn(GeneratorCommongUtil.MONITOR_TOPICS_ALL + ".insert(\"" + specName + "\");");
			this.printer.printLn();
		}
	}

	public void visit(ROSMOPSpec spec, Object arg) {
	}

	public void visit(ROSEventDefinition event, Object arg) {
		printer.print("void " + GeneratorCommongUtil.MONITOR_CLASS_NAME +
				"::monitorCallback_" + event.getEventName());
		String eventMsgType = event.getMessageType().replace("/", "::");
		printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ")");
		printer.printLn("{");
		printer.printLn();
		printer.indent();

		printParametersBinding(event, GeneratorCommongUtil.MONITORED_MSG_NAME);
		printer.printLn();

		printActionCode(event);
		printer.printLn();

		publishAndSerializeMsg(GeneratorCommongUtil.SERVERMANAGER_PTR_NAME, GeneratorCommongUtil.MONITOR_COPY_MSG_NAME);

		printer.unindent();
		printer.printLn();
		printer.printLn("}");
		printer.printLn();

		//authentication
		if(isAuthenticated() && event.isAuthenticated()){
			printer.print("void " + GeneratorCommongUtil.MONITOR_CLASS_NAME +
					"::authMonitorCallback_" + event.getEventName());
			printer.print("(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ")");
			printer.printLn("{");
			printer.printLn();
			printer.indent();

			printParametersBinding(event, GeneratorCommongUtil.MONITORED_MSG_NAME);
			printer.printLn();

			printActionCode(event);
			printer.printLn();

			//	      msg_auth::AuthenticatedMessage am = make_auth_message(rv_msg);
			printer.printLn(GeneratorCommongUtil.AUTHENTICATED_MSG_TYPE + " am = make_auth_message(" + GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + ");");
			publishAndSerializeMsg(GeneratorCommongUtil.PUBLICATION_PTR_NAME, GeneratorCommongUtil.MONITOR_COPY_MSG_NAME);

			printer.unindent();
			printer.printLn();
			printer.printLn("}");
			printer.printLn();
		}
	}

	/**
	 * Print user provided action code
	 */
	private void printActionCode(ROSEventDefinition event) {
		// For now just prints out the whole string provided by user
		if(event.isUserDefined()){
			printer.printLn("if(monitor::" + GeneratorCommongUtil.MONITOR_TOPICS_ENB + ".find(\"" + event.getSpecName() + "\") != monitor::" + GeneratorCommongUtil.MONITOR_TOPICS_ENB + ".end())");
			printer.printLn("{");
			printer.indent();

			String preproc = event.getAction(), accum = "", message = null;
			int i1 = 0;

			while(i1 < preproc.length()){
				String st = preproc.substring(i1);
				if(st.indexOf("MESSAGE") != -1){ 
					accum += st.substring(0, st.indexOf("MESSAGE"));
					i1 += st.indexOf("MESSAGE");

					//the MESSAGE keyword
					message = preproc.substring(i1, preproc.indexOf(";", i1)+1);
					//					System.out.println(message);

					accum += GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + ";";
					//					System.out.println(accum);
					i1 += message.length();
				} else break;
			}

			if(!accum.equals("")) {
				//				System.out.println(accum + preproc.substring(i1));
				printer.print(accum + preproc.substring(i1));
			}			
			else printer.print(event.getAction());
			printer.printLn();
			printer.unindent();
			printer.printLn("}");
			printer.printLn();
		}
	}

	private void printPropertyCode(ROSMOPProperty property) {
		printer.printLn("if(monitor::" + GeneratorCommongUtil.MONITOR_TOPICS_ENB + ".find(\"" + property.getSpecName() + "\") != monitor::" + GeneratorCommongUtil.MONITOR_TOPICS_ENB + ".end())");
		printer.printLn("{");
		printer.indent();
		printer.print(property.getContent());
		//		System.out.println(property.getContent());
		printer.printLn();
		printer.unindent();
		printer.printLn("}");
		printer.printLn();
	}

	/**
	 * Generate code to bind message to user provided parameters
	 */
	private void printParametersBinding(ROSEventDefinition event, String msgParameterName) {
		// Generate code like:

		// geometry_msgs::TwistStamped rv_msg;
		// rv_msg.header = msg->header;
		// rv_msg.twist = msg->twist;

		// float& H = rv_msg.header;
		// double& A = rv_msg.twist.angular;
		// ...

		this.printer.printLn(event.getMessageTypeName() + " " + GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + ";");
		Map<String, String> msgMapping = MessageParser.parseMessage(event.getMessageType());
		for (String msgName : msgMapping.keySet()) {
			this.printer.printLn(GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + "." + msgName + " = " +
					msgParameterName + "->" + msgName + ";");
		}

		this.printer.printLn();
		this.printer.printLn();

		if(event.getParameters() != null){ //TODO not sure
			for (ROSMOPParameter parameter : event.getParameters()) {
				String boundEntry = event.getStringifiedPatterns().get(parameter.getName());
				if (boundEntry != null) {
					if(parameter.getType().getTypeName().endsWith("[]")){
						parameter.getType().setTypeName(parameter.getType().getTypeName().replace("[]", ""));
						parameter.getType().setTypeName("vector<" + parameter.getType().getTypeName() + ">");
					}
					this.printer.print(parameter.getType().getTypeName() + "& " + parameter.getName() + " = ");
					this.printer.print(GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + "." + boundEntry + ";");
					this.printer.printLn();
				}
			}
		}
	}

	private void printParametersBindingAll(ArrayList<ROSEventDefinition> eventList, String msgParameterName) {
		this.printer.printLn(eventList.get(0).getMessageTypeName() + " " + GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + ";");

		Map<String, String> msgMapping = new HashMap<String, String>();
		ArrayList<ROSMOPParameter> noDoubleParam = new ArrayList<ROSMOPParameter>();
		boolean incl;

		for(ROSEventDefinition event : eventList){
			msgMapping.putAll(MessageParser.parseMessage(event.getMessageType()));

			if(event.getParameters() != null){
				for (ROSMOPParameter parameter : event.getParameters()) {
					incl = false;

					for (int i = 0; i < noDoubleParam.size(); i++) {
						if(parameter.equals(noDoubleParam.get(i))){
							incl = true;
							break;
						}
					}

					if(!incl){
						noDoubleParam.add(parameter);
					}
				}
			}
		}
		for (String msgName : msgMapping.keySet()) {
			this.printer.printLn(GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + "." + msgName + " = " +
					msgParameterName + "->" + msgName + ";");
		}

		this.printer.printLn();
		this.printer.printLn();

		for(ROSEventDefinition event : eventList){
			if(event.getParameters() != null){ //TODO not sure
				for (ROSMOPParameter parameter : event.getParameters()) {
					if(noDoubleParam.contains(parameter)){

						String boundEntry = event.getStringifiedPatterns().get(parameter.getName());
						if (boundEntry != null) {
							if(parameter.getType().getTypeName().endsWith("[]")){
								parameter.getType().setTypeName(parameter.getType().getTypeName().replace("[]", ""));
								parameter.getType().setTypeName("vector<" + parameter.getType().getTypeName() + ">");
							}
							this.printer.print(parameter.getType().getTypeName() + "& " + parameter.getName() + " = ");
							this.printer.print(GeneratorCommongUtil.MONITOR_COPY_MSG_NAME + "." + boundEntry + ";");
							this.printer.printLn();
						}

						noDoubleParam.remove(parameter);
					}
				}
			}
		}
	}

	/**
	 * Generate code to serialize and publish message
	 */
	private void publishAndSerializeMsg(String serverMngr, String msgName) {
		// Generate code like:
		// ros::SerializedMessage serializedMsg = ros::serialization::serializeMessage(msgName);
		// publishPtr->publish(serializedMsg);
		String serializedMessage = "ros::SerializedMessage serializedMsg = ros::serialization::serializeMessage(" + msgName + ");";
		this.printer.print(serializedMessage);
		this.printer.printLn();
		serializedMessage = serverMngr + "->publish(" + GeneratorCommongUtil.TOPIC_PTR_NAME + ", serializedMsg);";
		this.printer.print(serializedMessage);
	}

	/**
	 * Generate code for constructor
	 */
	private void printConstructor(ROSMOPSpecFile specFile) {
		this.printer.printLn(GeneratorCommongUtil.MONITOR_CLASS_NAME + "::" + GeneratorCommongUtil.MONITOR_CLASS_NAME 
				+ "(string topic, ros::SubscribeOptions &" 
				+ GeneratorCommongUtil.SUBSCRIBE_OPTIONS + ")");
		this.printer.printLn("{");
		this.printer.indent();

		//	     topic_name = topic;
		//	     server_manager = rv::ServerManager::instance(
		this.printer.printLn(GeneratorCommongUtil.TOPIC_PTR_NAME + " = topic;");
		this.printer.printLn(GeneratorCommongUtil.SERVERMANAGER_PTR_NAME + " = rv::ServerManager::instance();");

		if(initBlock){
			this.printer.printLn();
			this.printer.printLn("init();");
		}

		this.printer.printLn();

		//ops_sub: subscriber registration

		// if(topic=="/landshark_control/base_velocity")
		// ops_sub.init<geometry_msgs::TwistStamped>(topic,1000,boost::bind(
		// &RVt::monitorCallbackLandsharkBaseVelocityReverse, this, _1));
		boolean isFirst = true;
		ArrayList<String> noDoubles = new ArrayList<String>();

		for (ROSMOPSpec spec : specFile.getSpecs()) {
			ArrayList<ROSEventDefinition> publishEvts = new ArrayList<ROSEventDefinition>();
			ROSMOPProperty prop;

			publishEvts.addAll(spec.getEvents());

			prop = spec.getProperty();
			if(prop != null)
				publishEvts.addAll(prop.getPublishEvents());

			for (ROSEventDefinition event : publishEvts) {

				if(!noDoubles.contains(event.getTopicName())){
					if (!isFirst) {
						this.printer.print("else ");
					} else {
						isFirst = false;
					}

					if(!event.isAuthenticated()){
						this.printer.printLn("if (topic == \"" + event.getTopicName() + "\") {");
					} else {
						this.printer.printLn("if (topic == \"" + event.getTopicName().substring(0, event.getTopicName().lastIndexOf("/hmac")) + "\") {");
					}
					this.printer.indent();

					if(addedTopics.get(event.getTopicName()).size() == 1){

						this.printer.printLn(GeneratorCommongUtil.SUBSCRIBE_OPTIONS + ".init<" + 
								event.getMessageType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
								GeneratorCommongUtil.MONITOR_CLASS_NAME +
								"::monitorCallback_" + event.getEventName() + ", this, _1));");
					}else{
						String callback = "::mergedMonitorCallback_" + event.getTopicName().replace("/", "");

						this.printer.printLn(GeneratorCommongUtil.SUBSCRIBE_OPTIONS + ".init<" + 
								event.getMessageType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
								GeneratorCommongUtil.MONITOR_CLASS_NAME +
								callback + ", this, _1));");
					}

					this.printer.unindent();
					this.printer.printLn("}");


					//		        else if (topic == "/landshark_control/base_velocity/hmac") {
					//		            ops_sub.init<msg_auth::AuthenticatedMessage>(topic, 1000, boost::bind(&RVMonitor::authCallBack, this, _1));
					//		            impl_->ops_.init<geometry_msgs::TwistStamped>(topic, 1000, boost::bind(&RVMonitor::authVelocity, this, _1));
					//		        }

					if(isAuthenticated() && event.isAuthenticated()){
						this.printer.printLn("else if (topic == \"" + event.getTopicName() + "\") {");
						this.printer.indent();

						this.printer.printLn(GeneratorCommongUtil.SUBSCRIBE_OPTIONS + ".init<" + 
								GeneratorCommongUtil.AUTHENTICATED_MSG_TYPE + ">(topic, 1000, boost::bind(&" + 
								GeneratorCommongUtil.MONITOR_CLASS_NAME +
								"::" + GeneratorCommongUtil.AUTHENTICATED_MSG_CALLBACK + ", this, _1));");

						if(addedTopics.get(event.getTopicName()).size() == 1){

							this.printer.printLn("impl_->ops_.init<" + 
									event.getMessageType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
									GeneratorCommongUtil.MONITOR_CLASS_NAME +
									"::authMonitorCallback_" + event.getEventName() + ", this, _1));");
						}else{
							String callback = "::authMergedMonitorCallback_" + event.getTopicName().replace("/", "");

							this.printer.printLn("impl_->ops_.init<" + 
									event.getMessageType().replace("/", "::") + ">(topic, 1000, boost::bind(&" + 
									GeneratorCommongUtil.MONITOR_CLASS_NAME +
									callback + ", this, _1));");
						}

						this.printer.unindent();
						this.printer.printLn("}");
					}

					noDoubles.add(event.getTopicName());
				}
			}
		}

		this.printer.unindent();
		this.printer.printLn("}");
		this.printer.printLn();
	}

	@Override
	public void visit(ROSBodyDeclaration declaration, Object arg) {
		if (declaration instanceof ROSFieldDeclaration) {
			ROSFieldDeclaration fieldDeclaration = (ROSFieldDeclaration) declaration; 
			this.printer.print(fieldDeclaration.getDeclaredType() + " ");
			for(ROSVariableDeclarator var : fieldDeclaration.getVariables()){
				this.printer.print(var.getId());
				if(var.getInitValue() != null)
					this.printer.print(" = " + var.getInitValue());

				if(fieldDeclaration.getVariables().indexOf(var) != fieldDeclaration.getVariables().size()-1)
					this.printer.print(", ");
			}
			this.printer.printLn(";");
		}
	}
}
