package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public class ROSBodyDeclaration  extends Node {

	public ROSBodyDeclaration(int line, int column) {
		super(line, column);
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
