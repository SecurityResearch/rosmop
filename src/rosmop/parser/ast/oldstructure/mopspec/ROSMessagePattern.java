package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.Node;

/**
 * ROS message pattern. Super class for basic pattern and complex pattern.
 * 
 * @author Qingzhou Luo
 *
 */
public abstract class ROSMessagePattern extends Node {
	
	private String variable;

	public ROSMessagePattern(int line, int column) {
		super(line, column);
	}
	
	public String getVariable() { return variable; }
	public void setVariable(String variable) { this.variable = variable; }
}
