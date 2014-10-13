package rosmop.parser.ast.mopspec;

import java.util.List;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public final class ROSFieldDeclaration extends ROSBodyDeclaration {
	
	private String declaredType;
    private List<ROSVariableDeclarator> variables;
	
	public ROSFieldDeclaration(int line, int column) {
		super(line, column);
	}

	public ROSFieldDeclaration(int line, int column, String type, List<ROSVariableDeclarator> variables) {
		super(line, column);
		this.declaredType = type;
        this.variables = variables;
	}
	
	public String getDeclaredType() {
		return this.declaredType;
	}
	
	public List<ROSVariableDeclarator> getVariables() {
		return variables;
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
