package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public class ROSMessageBasicPattern extends ROSMessagePattern {
	private String value;
	
	public ROSMessageBasicPattern(int line, int column, String variable, String value){
		super(line, column);
		super.setVariable(variable);
		this.value = value;
	}
	
	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
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
