package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

/**
 * Represents single ROS parameter with type.
 * 
 * @author Qingzhou Luo
 *
 */
public class ROSMOPParameter extends Node {
	private ROSType type;
	private String name = "";
	
	public ROSMOPParameter(int line, int column, ROSType type, String name){
		super(line, column);
		this.type = type;
		this.name = name;
	}

	public ROSType getType() { return type; }
	public void setType(ROSType type) { this.type = type; }

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public boolean equals(ROSMOPParameter param){
		return type.equals(param.getType()) && name.equals(param.getName());
	}
	
	@Override
	public int hashCode(){
		return name.hashCode();
	}
	
    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) {
        v.visit(this, arg);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return v.visit(this, arg);
    }
    
//	@Override
//	public String toString(){
//		String str = "";
//		
//		str = this.type.getTypeName() + " " + this.name; 
//		
//		return str;
//	}
}
