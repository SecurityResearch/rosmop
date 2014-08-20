package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;
import rosmop.parser.ast.Node;

/**
 * Basic type in ROS. May need to be changed to abstract class if more types
 * need to be added.
 * 
 * @author Qingzhou Luo
 * 
 */
public class ROSType extends Node {
	private String typeName;
	
	public ROSType(int line, int column, String typeName){
		super(line, column);
		this.typeName = typeName;
	}

	public String getTypeName() { return typeName; }
	public void setTypeName(String typeName) { this.typeName = typeName; }
	
	public boolean equals(Object o){
		if(!(o instanceof ROSType)){
			return false;
		}
		
		ROSType t2 = (ROSType) o;
		
		return typeName.equals(t2.getTypeName());
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
