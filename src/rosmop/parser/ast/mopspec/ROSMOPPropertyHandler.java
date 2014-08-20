package rosmop.parser.ast.mopspec;

import rosmop.parser.ast.Node;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

public class ROSMOPPropertyHandler extends Node {
	
	private final String state;
    private final String action;
    
    int line, column;
    
    /**
     * Construct a PropertyHandler out of its component elements.
     * @param state The state to invoke the handler on.
     * @param action The language-specific action to take on entering the state.
     */
    public ROSMOPPropertyHandler(int line, int column, final String state, final String action) {
    	super(line, column);
    	this.state = state;
        this.action = action;
    }
    
    /**
     * The state to apply the action to.
     * @return The String name of the state this handler is related to.
     */
    public String getState() {
        return state;
    }
    
    /**
     * The language-specific action to apply on reaching the state.
     * @return Language-specific code for the action.
     */
    public String getAction() {
        return action;
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
