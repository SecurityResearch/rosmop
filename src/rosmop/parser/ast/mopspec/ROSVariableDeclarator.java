package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public final class ROSVariableDeclarator extends Node {
	
	private String declaredName;
	private String initValue;

    public ROSVariableDeclarator(int line, int column, String id, String init) {
        super(line, column);
        this.declaredName = id;
        this.initValue = init;
    }

    public String getId() {
        return declaredName;
    }

    public String getInitValue() {
        return initValue;
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
