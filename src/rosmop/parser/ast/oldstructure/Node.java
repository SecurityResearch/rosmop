package rosmop.parser.ast;

import rosmop.parser.ast.visitor.CppGenerationVisitor;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.HeaderGenerationVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

/**
 * @author Cansu Erdogan
 * 
 */
public abstract class Node {

	protected final int beginLine;
	protected final int beginColumn;
	protected final int endLine;
	protected final int endColumn;

	/**
	 * This attribute can store additional information from semantic analysis.
	 */
	protected Object data;

	public Node(int line, int column) {
		this.beginLine = line;
		this.beginColumn = column;
		this.endLine = line;
		this.endColumn = column;
	}

	public Node(int beginLine, int beginColumn, int endLine, int endColumn) {
		this.beginLine = beginLine;
		this.beginColumn = beginColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}

	/**
	 * Use this to retrieve additional information associated to this node.
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Use this to store additional information to this node.
	 */
	public void setData(Object data) {
		this.data = data;
	}

	public final int getBeginLine() {
		return beginLine;
	}

	public final int getBeginColumn() {
		return beginColumn;
	}

	public final int getEndLine() {
		return endLine;
	}

	public final int getEndColumn() {
		return endColumn;
	}

	public <A> void accept(VoidVisitor<A> v, A arg) {
		v.visit(this, arg);
	}

	public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
		return v.visit(this, arg);
	}
	
	public String toCppFile() {
		CppGenerationVisitor visitor = new CppGenerationVisitor();
		accept(visitor, null);
		return visitor.getSource();
	}
	
	public String toHeaderFile() {
		HeaderGenerationVisitor visitor = new HeaderGenerationVisitor();
		accept(visitor, null);
		return visitor.getSource();
	}

}
