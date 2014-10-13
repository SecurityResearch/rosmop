package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public class ROSBlock extends Node {
	
	String content;
	String specName;
	int line, column;
	
	public ROSBlock (int line, int column, String content, String specName){
		super(line, column);
		
		this.content = content;
		this.specName = specName;
	}
	
	public String getContent() {
		return content;
	}

	public String getSpecName() {
		return specName;
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
