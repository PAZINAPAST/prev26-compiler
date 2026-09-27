package prev26lang.phase.abstr;

import java.util.function.*;

import prev26lang.*;
import prev26lang.common.logger.*;
import prev26lang.common.report.*;
import prev26lang.phase.*;

/**
 * Abstract syntax phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Abstr extends Phase {

	/** The abstract syntax tree. */
	public static AST.Nodes<AST.FullDefn> tree;

	/**
	 * Phase construction.
	 */
	public Abstr() {
		super("abstr");
	}

	// Attribute Location:

	/** Tester for locatable nodes. */
	private static final Predicate<AST.Node> validForLocation = //
			(AST.Node node) -> (node instanceof AST.Node);

	/** Mapping of nodes to locations. */
	public static final AST.Attribute<Location> locAttr = new AST.Attribute<>(validForLocation);

	// ===== LOGGER =====

	/**
	 * Abstract syntax logger.
	 */
	public static class Logger extends AST.Logger {

		/**
		 * Constructs a new abstract syntax logger.
		 * 
		 * @param xmlLogger The underlying XML logger.
		 */
		public Logger(final XMLLogger xmlLogger) {
			super(xmlLogger);
		}

		/**
		 * Logs attribute values of a single abstract syntax tree node.
		 * 
		 * This method is called by {@link AST.Logger} for every abstract syntax tree
		 * node visited.
		 * 
		 * @param xmlLogger The XML logger the log should be written to.
		 * @param node      The abstract syntax tree node visited by {@link AST.Logger}.
		 */
		public void logAttrs(final XMLLogger xmlLogger, final AST.Node node) {
			if (validForLocation.test(node)) {
				Location location = locAttr.get(node);
				if (location == null) {
					if (Compiler.devMode()) {
						xmlLogger.begElement("location");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				} else {
					if ((node instanceof AST.Nodes nodes) && (nodes.size() == 0)) {
					} else
						location.log(xmlLogger);
				}
			}
		}

	}

}
