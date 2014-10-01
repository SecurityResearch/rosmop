package rosmop.parser.ast.mopspec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rosmop.codegen.GeneratorUtil;
import rosmop.parser.ast.visitor.GeneratorCommongUtil;

/**
 * An event monitored in the output program.
 */
public class Event {

	private final List<String> modifiers;
	private final String name;
	private final List<String> definitionModifiers;
	private String definition; //parameters
	private final String topic, msgType;
	private String pattern;
	private String action;

	private final String specName;
	private List<Variable> parameters;
	private HashMap<String, String> patternMap;
	private List<Event> publishKeywordEvents = null;

	/**
	 * Construct an Event out of its component parts.
	 * @param modifiers Strings that change the meaning of the event.
	 * @param name The name of the event to monitor.
	 * @param definition The descrption of what the event is on, e.g. its parameters.
	 * @param action The action to take on encountering the event.
	 */
	public Event(final List<String> modifiers, final String name, 
			final List<String> definitionModifiers, final String definition, final String topic, 
			final String msgType, final String pattern, final String action, final String specName) {
		this.modifiers = Collections.unmodifiableList(new ArrayList<String>(modifiers));
		this.name = name;
		this.definitionModifiers = Collections.unmodifiableList(
				new ArrayList<String>(definitionModifiers));
		this.definition = definition;
		this.topic = topic;
		this.msgType = msgType;
		this.pattern = pattern;
		this.action = action;
		this.specName = specName;

		parameterize();
		matchParametersToPattern();
		replaceMESSAGE();
		createEventOutOfPublish();
	}

	private void parameterize() {
		// get rid of multiline comments!!
		definition = definition.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");

		String tmp = definition.substring(1, definition.length()-1);
		String[] vars = tmp.trim().split(",");
		if(!tmp.isEmpty()){
			parameters = new ArrayList<Variable>();
			patternMap = new HashMap<String, String>();
			for (String string : vars) {
				//				System.out.println(string.trim());
				if(string.trim().startsWith("//")) continue;
				parameters.add(new Variable(string.trim()));
			}
		}
	}

	private void matchParametersToPattern() {
		pattern = pattern.substring(1, pattern.length()-1);
		// get rid of multiline comments!!
		pattern = pattern.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
		System.out.println(pattern);
		if(!pattern.isEmpty())
			patternMap = new HashMap<String, String>();
		String s;
		while(pattern.contains("{")){
			s = pattern.substring(pattern.lastIndexOf("{"), pattern.indexOf("}")+1);
			System.out.println("inside " + s);
			pattern = pattern.replace(s, "");
			System.out.println("left " + pattern);

			String[] pairs = s.substring(1, s.length()-1).trim().split(",");
			String msgField, varName;
			for (String string : pairs) {
				varName = string.substring(string.lastIndexOf(":")+1, string.length()).trim();
				string = string.substring(0, string.lastIndexOf(":")).trim();

				msgField = string.trim();

				patternMap.put(varName, msgField);
				System.out.println(msgField + ":" + varName);
			}
		}
		//		String[] pairs = pattern.trim().split(",");
		//		String msgField, varName;
		//		if(!pattern.isEmpty()){
		//			patternMap = new HashMap<String, String>();
		//			for (String string : pairs) {
		//				varName = string.substring(string.lastIndexOf(":")+1, string.length()).trim();
		//				string = string.substring(0, string.lastIndexOf(":")).trim();
		//
		//				msgField = string.trim();
		//
		//				patternMap.put(varName, msgField);
		////				System.out.println(msgField + ":" + varName);
		//			}
		//		}
	}

	private void replaceMESSAGE(){
		String preproc = action, accum = "", message = null;
		int i1 = 0;

		while(i1 < preproc.length()){
			String st = preproc.substring(i1);
			if(st.indexOf("MESSAGE") != -1){ 
				accum += st.substring(0, st.indexOf("MESSAGE"));
				i1 += st.indexOf("MESSAGE");

				//the MESSAGE keyword
				message = preproc.substring(i1, preproc.indexOf(";", i1)+1);
				//					System.out.println(message);

				accum += GeneratorUtil.MONITOR_COPY_MSG_NAME + ";";
				//					System.out.println(accum);
				i1 += message.length();
			} else break;
		}

		if(!accum.equals("")) {
			action = accum + preproc.substring(i1);
		}
	}

	private void createEventOutOfPublish(){

		String preproc = action, publish, message, serialize, accum = "", topic, msgType;
		int i1 = 0, count = 1, i2, i3;

		while(i1 < preproc.length()){
			if(publishKeywordEvents == null)
				publishKeywordEvents = new ArrayList<Event>();

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

				Event pubevent = new Event(new ArrayList<String>(), "publish"+message+count, new ArrayList<String>(), "()", topic, msgType.replace("::", "/"), "{}", "{}", specName);
				publishKeywordEvents.add(pubevent);

				serialize = "ros::SerializedMessage serializedMsg" + count +" = ros::serialization::serializeMessage(" + message + ");\n" 
						+ GeneratorCommongUtil.SERVERMANAGER_PTR_NAME + "->publish(\"" + topic + "\", serializedMsg" + count +");";

				accum += serialize;

				i1 += publish.length();
				count++;
			} else break;
		}

		action = accum + preproc.substring(i1);
		//		System.out.println(this.content);
	}

	public String classifyMsgType() {
		return msgType.replace("/", "::");
	}

	/**
	 * Strings that affect the meaning of the event.
	 * @return An unmodifiable list of strings.
	 */
	public List<String> getModifiers() {
		return modifiers;
	}

	/**
	 * The name of the event.
	 * @return The event's name, which is also used in the logic states.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Modifiers after the event, but before the event definition/parameters.
	 * @return The event modifiers applying to the parameters.
	 */
	public List<String> getDefinitionModifiers() {
		return definitionModifiers;
	}

	/**
	 * The event's parameters.
	 * @return The parameters of the event, described in a language-specific way.
	 */
	public String getDefinition() {
		return definition;
	}

	public String getTopic() {
		return topic;
	}

	public String getMsgType() {
		return msgType;
	}

	public String getPattern() {
		return pattern;
	}

	/**
	 * Language-specific code to take on encountering the event.
	 * @return Code in the target language to run on encountering the event.
	 */
	public String getAction() {
		return action;
	}

	public String getSpecName() {
		return specName;
	}

	public List<Variable> getParameters() {
		return parameters;
	}

	public List<Event> getPublishKeywordEvents() {
		return publishKeywordEvents;
	}

	public HashMap<String, String> getPatternMap() {
		return patternMap;
	}

}
