package rosmop;

import java.util.Map;
import java.util.HashMap;

import com.runtimeverification.rvmonitor.c.rvc.CSpecification;

import rosmop.parser.ast.mopspec.*;

/**
 * Wrap the language-neutral specification in a way to easily make accessible the data the C code 
 * generators need.
 */
public class RVParserAdapter implements CSpecification {

	private final MonitorFile file;
	private final Specification wrapped;

	/**
	 * Wrap the given specification.
	 * @param wrapped The specification to wrap.
	 */
	public RVParserAdapter(final MonitorFile file) {
		this.file = file;
		wrapped = file.getSpecifications().get(0);
	}

	@Override
	public String getIncludes() {
		return file.getPreamble();
	}

	@Override
	public String getSpecName() {
		return wrapped.getName();
	}

	@Override
	public HashMap<String, String> getEvents() {
		HashMap<String, String> events = new HashMap<String, String>();
		for(Event event : wrapped.getEvents()) {
			events.put(event.getName(), event.getAction());
		}
		return events;
	}

	@Override
	public HashMap<String, String> getParameters() {
		HashMap<String, String> parameters = new HashMap<String, String>();
		for(Event event : wrapped.getEvents()) {
			parameters.put(event.getName(), event.getDefinition());
		}
		return parameters;
	}

	@Override
	public HashMap<String, String> getPParameters() {
		HashMap<String, String> parameters = new HashMap<String, String>();
		for(Event event : wrapped.getEvents()) {
			String params = event.getDefinition().trim();
			// Use a comma if there is at least one parameter.
			String separator = params.matches(".*[a-zA-Z]+.*") ? ", " : "";
			params = params.substring(0, params.length() - 1) + separator + "void* key)";
			parameters.put(event.getName(), params);
		}
		return parameters;
	}

	@Override
	public HashMap<String, String> getHandlers() {
		HashMap<String, String> handlers = new HashMap<String, String>();
		for(PropertyHandler handler : wrapped.getProperties().get(0).getHandlers()) {
			handlers.put(handler.getState(), handler.getAction());
		}
		return handlers;
	}

	@Override
	public String getDeclarations() {
		return wrapped.getLanguageDeclarations();
	}

	@Override
	public String getFormalism() {
		if(!wrapped.getProperties().isEmpty())
			return wrapped.getProperties().get(0).getName();
		else return null;
	}

	@Override
	public String getFormula() {
		if(!wrapped.getProperties().isEmpty())
			return wrapped.getProperties().get(0).getSyntax();
		else return null;
	}
	
	public String getInit() {
		return wrapped.getInit();
	}
}
