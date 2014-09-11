package rosmop.parser.ast.mopspec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private final String action;

	private List<Variable> parameters;
	private HashMap<String, String> patternMap;

	//	TODO: need specname??


	/**
	 * Construct an Event out of its component parts.
	 * @param modifiers Strings that change the meaning of the event.
	 * @param name The name of the event to monitor.
	 * @param definition The descrption of what the event is on, e.g. its parameters.
	 * @param action The action to take on encountering the event.
	 */
	public Event(final List<String> modifiers, final String name, 
			final List<String> definitionModifiers, final String definition, final String topic, 
			final String msgType, final String pattern, final String action) {
		this.modifiers = Collections.unmodifiableList(new ArrayList<String>(modifiers));
		this.name = name;
		this.definitionModifiers = Collections.unmodifiableList(
				new ArrayList<String>(definitionModifiers));
		this.definition = definition;
		this.topic = topic;
		this.msgType = msgType;
		this.pattern = pattern;
		this.action = action;
		
		parameterize();
		matchParametersToPattern();
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
		
		String eventMsgType = msgType.replace("/", "::");
		definition = "(const " + eventMsgType + "::ConstPtr& " + GeneratorCommongUtil.MONITORED_MSG_NAME + ")";
	}

	private void matchParametersToPattern() {
		pattern = pattern.substring(1, pattern.length()-1);
		// get rid of multiline comments!!
		pattern = pattern.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)","");
		String[] pairs = pattern.trim().split(",");
		String msgField, varName;
		if(!pattern.isEmpty()){
			patternMap = new HashMap<String, String>();
			for (String string : pairs) {
				varName = string.substring(string.lastIndexOf(":")+1, string.length()).trim();
				string = string.substring(0, string.lastIndexOf(":")).trim();

				msgField = string.trim();

				patternMap.put(varName, msgField);
				//				System.out.println(msgField + ":" + varName);
			}
		}
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

}
