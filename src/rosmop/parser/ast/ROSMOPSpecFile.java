package rosmop.parser.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rosmop.parser.ast.mopspec.ROSBodyDeclaration;
import rosmop.parser.ast.mopspec.ROSMOPSpec;
import rosmop.parser.ast.visitor.GenericVisitor;
import rosmop.parser.ast.visitor.VoidVisitor;

/**
 * Class represents the entire rosmop file.
 * 
 * @author Qingzhou Luo
 * 
 */
public class ROSMOPSpecFile extends Node {
	private List<ROSMOPSpec> specList = null;
	private String includeDeclarations;

	public ROSMOPSpecFile(int line, int column) {
		super(line, column);
		this.specList = new ArrayList<ROSMOPSpec>();
	}

	public ROSMOPSpecFile(int line, int column, List<ROSMOPSpec> specList, String includeDecl) {
		super(line, column);
		this.specList = specList;
		this.includeDeclarations = includeDecl;
	}

	public List<ROSMOPSpec> getSpecs() {
		return this.specList;
	}
	
	public void addSpecs(List<ROSMOPSpec> specs) {
		this.specList.addAll(specs);
	}
	
	public String getIncludeDeclarations() {
		return includeDeclarations;
	}

	public void setIncludeDeclarations(
			String includeDeclarations) {
		this.includeDeclarations = includeDeclarations;
	}

	public void addIncludes(String includeDeclarations) {
		this.includeDeclarations += includeDeclarations;
	}
	
//	TODO: is this necessary?
	public Set<ROSBodyDeclaration> getAllDeclarations() {
		Set<ROSBodyDeclaration> union = new HashSet<ROSBodyDeclaration>();
		for (ROSMOPSpec spec : this.getSpecs()) {
			union.addAll(spec.getDeclarations());
		}
		return union;
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
