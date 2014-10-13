package rosmop.parser.ast.visitor;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.ROSMOPSpecFile;
import rosmop.parser.ast.mopspec.ROSBodyDeclaration;
import rosmop.parser.ast.mopspec.ROSEventDefinition;
import rosmop.parser.ast.mopspec.ROSMOPParameter;
import rosmop.parser.ast.mopspec.ROSMOPSpec;
import rosmop.parser.ast.mopspec.ROSType;

/**
 * @author Cansu Erdogan
 * 
 */
public interface GenericVisitor<R, A> {

	public R visit(Node node, A arg);

	// - ROSMOP components

	public R visit(ROSMOPSpecFile specFile, A arg);

	public R visit(ROSMOPSpec spec, A arg);

	public R visit(ROSMOPParameter parameter, A arg);

	public R visit(ROSEventDefinition event, A arg);

	// public R visit(PropertyAndHandlers p, A arg);

	// public R visit(Formula f, A arg);

	// later on add C++ specific visit methods

	public R visit(ROSType type, A arg);
	
	public R visit(ROSBodyDeclaration declaration, A arg);
}
