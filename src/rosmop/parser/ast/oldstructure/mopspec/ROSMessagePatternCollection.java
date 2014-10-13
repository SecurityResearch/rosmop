package rosmop.parser.ast.mopspec;

import java.util.HashMap;

/**
 * Represents the collection of patterns specified in event specification
 * Utility class for parsing purposes, not used in ROSEventDefinition
 * 
 * @author Cansu Erdogan
 * 
 */
public class ROSMessagePatternCollection {

	HashMap<String, ROSMessagePattern> patterns;
	HashMap<String, String> stringifiedPatterns;

	public ROSMessagePatternCollection() {
		patterns = new HashMap<String, ROSMessagePattern>();
		stringifiedPatterns = new HashMap<String, String>();
	}

	@Override
	public String toString() {
		return this.patterns.toString();
	}

	public HashMap<String, ROSMessagePattern> getPatterns() {
		return patterns;
	}

	public void setPatterns(HashMap<String, ROSMessagePattern> patterns) {
		this.patterns = patterns;
	}

	public HashMap<String, String> getStringifiedPatterns() {
		return stringifiedPatterns;
	}

	public void setStringifiedPatterns(HashMap<String, String> stringifiedPatterns) {
		this.stringifiedPatterns = stringifiedPatterns;
	}

}
