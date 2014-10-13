package rosmop.parser.ast.mopspec;

import java.util.HashMap;

import rosmop.parser.ast.visitor.CppGenerationVisitor;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;
import rosmop.parser.ast.Node;

/**
 * ROS event definition.
 * 
 * @author Qingzhou Luo
 * 
 */

/*
 * (void Monitor::monitorCallback[eventName](const [messageType--with
 * ::]::ConstPtr& msg) //don't know if it's always ConstPtr&)
 * 
 * 
 * void Monitor::monitorCallbackControlMessage(const
 * geometry_msgs::TwistStamped::ConstPtr& msg) { //According to the input
 * pattern, decide which parameters will be declared
 * 
 * std_msgs::Header H = msg->header; geometry_msgs::Vector3 A =
 * msg->twist->angular; double X = msg->twist->linear->x; double Y =
 * msg->twist->linear->y; double Z = msg->twist->linear->z;
 * 
 * 
 * ([messageType--with ::] rv_msg;)
 * 
 * geometry_msgs::TwistStamped rv_msg; rv_msg.header = H; rv_msg.twist.angular =
 * A; rv_msg.twist.linear.x = X; rv_msg.twist.linear.y = Y;
 * rv_msg.twist.linear.z = Z;
 * 
 * ros::SerializedMessage m = ros::serialization::serializeMessage(rv_msg);
 * pub_ptr->publish(m); }
 */

public class ROSEventDefinition extends Node {

	private String eventName;
	private String topicName;
	private String messageType;
	private String action;
	private ROSMOPParameters parameters;
	private HashMap<String, ROSMessagePattern> patterns;
	private String specName;
	private boolean isUserDefined;
	private boolean isAuthenticated;

	private HashMap<String, String> stringifiedPatterns;

	public ROSEventDefinition(int line, int column) {
		super(line, column);
	}

	public ROSEventDefinition(int line, int column, String eventName,
			String topicName, String messageType, ROSMOPParameters parameters,
			HashMap<String, ROSMessagePattern> patterns, String action, String specName, boolean userdefined) {
		super(line, column);
		this.eventName = eventName;
		this.topicName = topicName;
		this.messageType = messageType;
		this.parameters = parameters;
		this.patterns = patterns;
		this.action = action;
		this.specName = specName;
		this.isUserDefined = userdefined;
		
		if(this.topicName.endsWith("/hmac")){
			setAuthenticated(true);
			CppGenerationVisitor.setAuthenticated(true);
		} else setAuthenticated(false);
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
		if(this.topicName.endsWith("/hmac") && !CppGenerationVisitor.isAuthenticated()){
			CppGenerationVisitor.setAuthenticated(true);
		}
	}

	public String getMessageType() {
		return messageType;
	}
	
	public String getMessageTypeName() {
		return messageType.replace("/", "::");
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public ROSMOPParameters getParameters() {
		return parameters;
	}

	public void setParameters(ROSMOPParameters parameters) {
		this.parameters = parameters;
	}

	public HashMap<String, ROSMessagePattern> getPatterns() {
		return patterns;
	}

	public void setPatterns(HashMap<String, ROSMessagePattern> patterns) {
		this.patterns = patterns;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getSpecName() {
		return specName;
	}

	public void setSpecName(String specName) {
		this.specName = specName;
	}

	public HashMap<String, String> getStringifiedPatterns() {
		return stringifiedPatterns;
	}

	public void setStringifiedPatterns(HashMap<String, String> stringifiedPatterns) {
		this.stringifiedPatterns = stringifiedPatterns;
	}

	// @Override
	// public String toString(){
	//
	// String str = "";
	//
	// str += this.eventName + "\t";
	// str += this.parameters + "\t";
	// str += this.messageType + "\t";
	// str += this.topicName + "\t";
	// str += this.patterns + "\t";
	// str += "\n{ " + this.action + " }\n";
	//
	// return str;
	// }

	public boolean isUserDefined() {
		return isUserDefined;
	}

	public void setUserDefined(boolean isUserDefined) {
		this.isUserDefined = isUserDefined;
	}

	public boolean isAuthenticated() {
		return isAuthenticated;
	}

	public void setAuthenticated(boolean isAuthenticated) {
		this.isAuthenticated = isAuthenticated;
	}

	@Override
	public <A> void accept(VoidVisitor<A> v, A arg) {
		v.visit(this, arg);
	}

	@Override
	public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
		return v.visit(this, arg);
	}
}
