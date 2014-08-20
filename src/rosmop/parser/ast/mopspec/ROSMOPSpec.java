package rosmop.parser.ast.mopspec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

/**
 * Class represents one single rosmop specification.
 * 
 * @author Qingzhou Luo
 *
 */
public class ROSMOPSpec extends Node implements Comparable<ROSMOPSpec> {

	private List<String> languageModifiers;
	private String name;
	private String languageParameters;
	private List<ROSBodyDeclaration> declarations;
	private String initblock;
	private List<ROSEventDefinition> events;
	private List<ROSMOPProperty> properties;

	public ROSMOPSpec(int line, int column){
		super(line, column);
		this.events = new ArrayList<ROSEventDefinition>();
		this.declarations = new ArrayList<ROSBodyDeclaration>();
	}

	public ROSMOPSpec(int line, int column, List<String> languageModifiers, String name, String languageParameters, 
			List<ROSEventDefinition> events, List<ROSBodyDeclaration> declarations, List<ROSMOPProperty> properties, String init){
		super(line, column);
		this.setLanguageModifiers(languageModifiers);
		this.name = name;
		this.setLanguageParameters(languageParameters);
		this.events = events;
		this.declarations = declarations;
		this.setProperties(properties);
		this.initblock = init;
	}

	public String getInitblock() {
		return initblock;
	}

	public void setInitblock(String initblock) {
		this.initblock = initblock;
	}

	public List<ROSEventDefinition> getEvents() {
		return this.events;
	}

	public List<ROSBodyDeclaration> getDeclarations() {
		return this.declarations;
	}

	/**
	 * Get non-repeated message types
	 */
	//TODO: handle properties correctly!!!
	public Set<String> getAllMsgTypes() {
		Set<String> allMsgTypes = new HashSet<String>();

		ArrayList<ROSEventDefinition> allEvents = new ArrayList<ROSEventDefinition>();
		allEvents.addAll(this.getEvents());
		if(this.getProperties() != null){
			for(ROSMOPProperty prop : this.getProperties())
				allEvents.addAll(prop.getPublishEvents());
		}

		for (ROSEventDefinition e : allEvents) {
			allMsgTypes.add(e.getMessageType());
		}
		return allMsgTypes;
	}

	/**
	 * Get non-repeated topic names
	 */
//	TODO: handle properties correctly!!!
	public Set<String> getAllTopicNames() {
		Set<String> topics = new HashSet<String>();

		ArrayList<ROSEventDefinition> allEvents = new ArrayList<ROSEventDefinition>();
		allEvents.addAll(this.getEvents());
		if(this.getProperties() != null){
			for(ROSMOPProperty prop : this.getProperties())
				allEvents.addAll(prop.getPublishEvents());
		}

		for (ROSEventDefinition e : allEvents) {
			topics.add(e.getTopicName());
		}
		return topics;
	}

	public void setEvents(List<ROSEventDefinition> events) {
		this.events = events;
	}

	public void setDeclarations(List<ROSBodyDeclaration> declarations) {
		this.declarations = declarations;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<ROSMOPProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<ROSMOPProperty> properties) {
		this.properties = properties;
	}

	public String getLanguageParameters() {
		return languageParameters;
	}

	public void setLanguageParameters(String languageParameters) {
		this.languageParameters = languageParameters;
	}

	public List<String> getLanguageModifiers() {
		return languageModifiers;
	}

	public void setLanguageModifiers(List<String> languageModifiers) {
		this.languageModifiers = languageModifiers;
	}

	public int compareTo(ROSMOPSpec o){
		return getName().compareTo(o.getName());
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
