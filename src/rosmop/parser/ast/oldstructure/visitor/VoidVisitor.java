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
public interface VoidVisitor<A> {

	public void visit(Node node, A arg);

	//- ROSMOP components

	public void visit(ROSMOPSpecFile spec, A arg);

	public void visit(ROSMOPSpec spec, A arg);

	public void visit(ROSMOPParameter parameter, A arg); //or parameters?

	public void visit(ROSEventDefinition event, A arg);

//	public void visit(PropertyAndHandlers p, A arg);
	
//	public void visit(Formula f, A arg);

	//later on add C++ specific visit methods
	
	public void visit(ROSType type, A arg);
	
	public void visit(ROSBodyDeclaration declaration, A arg);
}
