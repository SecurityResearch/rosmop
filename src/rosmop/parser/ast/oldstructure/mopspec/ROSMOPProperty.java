package rosmop.parser.ast.mopspec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rosmop.ROSMOPException;
import rosmop.parser.ast.Node;
import rosmop.parser.ast.visitor.GeneratorCommongUtil;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public class ROSMOPProperty extends Node {

	String name;
	String content;
	final List<ROSMOPPropertyHandler> handlers;
	String specName;
	int line, column;
	
	ArrayList<ROSEventDefinition> publishEvents = new ArrayList<ROSEventDefinition>();
	
	public ROSMOPProperty (int line, int column, String name, String content,  final List<ROSMOPPropertyHandler> handlers, String specName){
		super(line, column);
		this.line = line;
		this.column = column;
		
		this.name = name;
		this.content = content;
		this.handlers = Collections.unmodifiableList(new ArrayList<ROSMOPPropertyHandler>(handlers));
		this.specName = specName;

		createEventOutOfPublish();
	}

	public String getName() { return name; }
	public String getContent() { return content; }
    public List<ROSMOPPropertyHandler> getHandlers() {
        return handlers;
    }


	private void createEventOutOfPublish(){

		String preproc = this.content, publish, message, serialize, accum = "", topic, msgType;
		int i1 = 0, count = 1, i2, i3;

		while(i1 < preproc.length()){
			String st = preproc.substring(i1);
//			System.out.println(st);
			if(st.indexOf("PUBLISH") != -1){ 
				accum += st.substring(0, st.indexOf("PUBLISH"));
				i1 += st.indexOf("PUBLISH");

				//the whole PUBLISH statement (whole line)
				publish = preproc.substring(i1, preproc.indexOf(";", i1)+1);
//				System.out.println("=================="+publish);
				
				i2 = publish.indexOf(",");
				//topic name
//				topic = publish.substring(publish.indexOf("(\"")+2, i2);
				topic = publish.substring(publish.indexOf("(")+1, i2);
				topic = topic.trim();
				topic = topic.replaceAll("\"", "");
//				System.out.println(topic);

				i3 = publish.lastIndexOf(",")+1;
				//message variable
				message = publish.substring(i3, publish.lastIndexOf(")"));
				message = message.trim();
//				System.out.println("***"+message);
				
				//message type
				msgType = publish.substring(i2+1, i3-1);
				msgType = msgType.trim();
				msgType = msgType.replaceAll("\"", "");
//				System.out.println(msgType);
				
				ROSEventDefinition pubevent = new ROSEventDefinition(line, column, "publish"+message+count, topic, msgType.replace("::", "/"), null, null, null, specName, false);
				publishEvents.add(pubevent);
				
				serialize = "ros::SerializedMessage serializedMsg" + count +" = ros::serialization::serializeMessage(" + message + ");\n" 
						+ GeneratorCommongUtil.SERVERMANAGER_PTR_NAME + "->publish(\"" + topic + "\", serializedMsg" + count +");";
				
				accum += serialize;
				
				i1 += publish.length();
				count++;
			} else break;
		}
		
		this.content = accum + preproc.substring(i1);
//		System.out.println(this.content);
	}
	
	public ArrayList<ROSEventDefinition> getPublishEvents() {
		return publishEvents;
	}

	@Override
	public <A> void accept(VoidVisitor<A> v, A arg) {
		v.visit(this, arg);
	}

	@Override
	public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
		return v.visit(this, arg);
	}

	public String getSpecName() {
		return specName;
	}

}

