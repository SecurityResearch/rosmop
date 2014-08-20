package rosmop.parser.ast.mopspec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a list of ROS parameters with types.
 * 
 * @author Qingzhou Luo
 *
 */
public class ROSMOPParameters implements Iterable<ROSMOPParameter> {

	ArrayList<ROSMOPParameter> parameters;

	public ROSMOPParameters() {
		this.parameters = new ArrayList<ROSMOPParameter>();
	}
	
	public ROSMOPParameters(List<ROSMOPParameter> params) {
		this.parameters = new ArrayList<ROSMOPParameter>();

		if(params != null){
			for(ROSMOPParameter param : params){
				this.add(param);
			}
		}
	}

	public ROSMOPParameters(ROSMOPParameters params) {
		this.parameters = new ArrayList<ROSMOPParameter>();

		if(params != null){
			for(ROSMOPParameter param : params){
				this.add(param);
			}
		}
	}
	
	public ArrayList<ROSMOPParameter> getParameters() { return parameters; }
	public void setParameters(ArrayList<ROSMOPParameter> parameters) { this.parameters = parameters; }
	
	public void add(ROSMOPParameter p) {
		if (this.getParam(p.getName()) == null) {
			ROSMOPParameter p2 = p;
			if (p.getType().getTypeName().charAt(p.getType().getTypeName().length() - 1) == '+') {
				//right now we only have one type
				//that's why BaseTypePattern -> ROSType
				ROSType t2 = new ROSType(p.getType().getBeginLine(), p.getType().getBeginColumn(), p.getType().getTypeName().substring(0,
						p.getType().getTypeName().length() - 1));
				p2 = new ROSMOPParameter(p.getBeginLine(), p.getBeginColumn(), t2, p.getName());
			}
			this.parameters.add(p2);
		}
	}
	
	public void addAll(ROSMOPParameters set) {
		if (set == null)
			return;
		for (ROSMOPParameter p : set.parameters) {
			if (this.getParam(p.getName()) == null)
				this.parameters.add(p);
		}
	}

	public void addAll(List<ROSMOPParameter> set) {
		if (set == null)
			return;
		for (ROSMOPParameter p : set) {
			if (this.getParam(p.getName()) == null) {
				ROSMOPParameter p2 = p;
				if (p.getType().getTypeName().charAt(p.getType().getTypeName().length() - 1) == '+') {
					ROSType t2 = new ROSType(p.getType().getBeginLine(), p.getType().getBeginColumn(), p.getType().getTypeName().substring(0,
							p.getType().getTypeName().length() - 1));
					p2 = new ROSMOPParameter(p.getBeginLine(), p.getBeginColumn(), t2, p.getName());
				}
				this.parameters.add(p2);
			}
		}
	}

	/**
	 * Find a parameter with the given name
	 * 
	 * @param name
	 *            a parameter name
	 */
	public ROSMOPParameter getParam(String name) {
		ROSMOPParameter ret = null;

		for (ROSMOPParameter param : this.parameters) {
			if (param.getName().compareTo(name) == 0) {
				ret = param;
				break;
			}
		}
		return ret;
	}
	
	static public ROSMOPParameters unionSet(ROSMOPParameters set1, ROSMOPParameters set2) {
		ROSMOPParameters ret = new ROSMOPParameters();

		if (set1 != null)
			ret.addAll(set1);

		if (set2 != null)
			ret.addAll(set2);
		return ret;
	}

	static public ROSMOPParameters intersectionSet(ROSMOPParameters set1, ROSMOPParameters set2) {
		ROSMOPParameters ret = new ROSMOPParameters();

		for (ROSMOPParameter p1 : set1) {
			for (ROSMOPParameter p2 : set2) {
				if (p1.getName().compareTo(p2.getName()) == 0) {
					ret.add(p1);
					break;
				}
			}
		}
		return ret;
	}

	public ROSMOPParameters sortParam(ROSMOPParameters set) {
		ROSMOPParameters ret = new ROSMOPParameters();

		for (ROSMOPParameter p : this.parameters) {
			if (set.contains(p))
				ret.add(p);
		}

		for (ROSMOPParameter p : set.parameters) {
			if (!ret.contains(p))
				ret.add(p);
		}
		return ret;
	}
	
	public boolean contains(ROSMOPParameter p) {
		return (this.getParam(p.getName()) != null);
	}

	public boolean contains(ROSMOPParameters set) {
		for (ROSMOPParameter p : set.parameters) {
			if (!this.contains(p))
				return false;
		}
		return true;
	}

	public int size() {
		return this.parameters.size();
	}
	
	/**
	 * Compare a list of parameters with this one to see if they contains the
	 * same parameters
	 * 
	 * @param set
	 *            ROSMOPParameters
	 */
	public boolean equals(Object set) {
		if (!(set instanceof ROSMOPParameters))
			return false;
		
		return this.equals((ROSMOPParameters) set);
	}

	public boolean equals(ROSMOPParameters set) {
		if(set == null)
			return false;
		if (this.size() != set.size())
			return false;
		return this.contains(set) && set.contains(this);
	}
	
	public boolean matchTypes(ROSMOPParameters set){
		if(this.size() != set.size())
			return false;
		for (int i = 0; i < this.parameters.size(); i++) {
			if (this.parameters.get(i).getType().getTypeName().equals(set.get(i).getType().getTypeName())){
				return false;
			}
		}				
		
		return true;
	}
	

	public ROSMOPParameter get(int i) {
		if (i < 0 || i >= this.parameters.size())
			return null;
		return this.parameters.get(i);
	}
	
	@Override
	public Iterator<ROSMOPParameter> iterator() {
		return this.parameters.iterator();
	}
	
	public String parameterString() {
		String ret = "";

		for (ROSMOPParameter param : this.parameters) {
			ret += ", " + param.getName();
		}
		if (ret.length() != 0)
			ret = ret.substring(2);
		return ret;
	}

	public int hashCode() {
		int code = 0;
				
		for (ROSMOPParameter param : this.parameters) {
			code ^= param.hashCode();
		}
		
		return code;
	}

	public String toString(){
		return this.parameters.toString();
	}
	
	public List<ROSMOPParameter> toList(){
		return parameters;
	}
}
