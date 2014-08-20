package rosmop.parser.ast.mopspec;

import java.util.Map;

import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public class ROSMessageComplexPattern extends ROSMessagePattern {

	private Map<String, ROSMessagePattern> valueMap;

	public ROSMessageComplexPattern(int line, int column, String variable, Map<String, ROSMessagePattern> valueMap){
		super(line, column);
		super.setVariable(variable);
		this.valueMap = valueMap;
	}
	
	public Map<String, ROSMessagePattern> getValueMap() {
		return this.valueMap;
	}

	public void setValueMap(Map<String, ROSMessagePattern> valueMap) {
		this.valueMap = valueMap;
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
