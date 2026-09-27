package prev26lang.phase.memory;

import java.util.function.*;

import prev26lang.*;
import prev26lang.common.logger.*;
import prev26lang.common.report.*;
import prev26lang.phase.*;
import prev26lang.phase.abstr.*;
import prev26lang.phase.seman.*;

/**
 * Memory layout phase: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class Memory extends Phase {

	// Attribute Frame:

	/** Tester for nodes describing function definitions, i.e., for frames. */
	private static final Predicate<AST.Node> validForFrame = //
			(AST.Node node) -> (node instanceof AST.DefFunDefn);

	/** Maps function declarations to frames. */
	public static final AST.Attribute<MEM.Frame> frameAttr = new AST.Attribute<MEM.Frame>(validForFrame);

	// Attribute Access:

	/** Tester for nodes describing variables, i.e., for accesses. */
	private static final Predicate<AST.Node> validForAccess = //
			(AST.Node node) -> ((node instanceof AST.VarDefn) || (node instanceof AST.ParDefn)
					|| (node instanceof AST.CompDefn));

	/** Maps variable declarations to accesses. */
	public static final AST.Attribute<MEM.Access> accessAttr = new AST.Attribute<MEM.Access>(validForAccess);

	// Attribute String:

	/** Tester for nodes describing strings, i.e., for initialization values. */
	private static final Predicate<AST.Node> validForString = //
			(AST.Node node) -> ((node instanceof AST.AtomExpr)
					&& (((AST.AtomExpr) node).type == AST.AtomExpr.Type.STR));

	/** Maps variable declarations to accesses. */
	public static final AST.Attribute<MEM.AbsAccess> stringAttr = new AST.Attribute<MEM.AbsAccess>(validForString);

	/**
	 * Phase construction.
	 */
	public Memory() {
		super("memory");
	}

	// ===== LOGGER =====

	/**
	 * Memory layout logger.
	 */
	public static class Logger extends SemAn.Logger {

		/**
		 * Constructs a new memory layout logger.
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
		@Override
		public void logAttrs(final XMLLogger xmlLogger, final AST.Node node) {
			super.logAttrs(xmlLogger, node);
			if (validForFrame.test(node)) {
				switch (frameAttr.get(node)) {
				case null -> {
					if (Compiler.devMode()) {
						xmlLogger.begElement("frame");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				}
				case final MEM.Frame frame -> {
					xmlLogger.begElement("frame");
					xmlLogger.addAttribute("label", frame.label.name);
					xmlLogger.addAttribute("depth", Long.toString(frame.depth));
					xmlLogger.addAttribute("locssize", Long.toString(frame.locsSize));
					xmlLogger.addAttribute("argssize", Long.toString(frame.argsSize));
					xmlLogger.addAttribute("size", Long.toString(frame.size) + "+(tmps+regs)");
					xmlLogger.addAttribute("FP", frame.FP.toString());
					xmlLogger.addAttribute("RV", frame.RV.toString());
					xmlLogger.endElement();
				}
				}
			}
			if (validForAccess.test(node)) {
				switch (accessAttr.get(node)) {
				case null -> {
					if (Compiler.devMode()) {
						xmlLogger.begElement("access");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				}
				case final MEM.AbsAccess absAccess -> {
					xmlLogger.begElement("access");
					xmlLogger.addAttribute("size", Long.toString(absAccess.size));
					xmlLogger.addAttribute("label", absAccess.label.name);
					if (absAccess.init != null)
						xmlLogger.addAttribute("init", absAccess.init);
					xmlLogger.endElement();
				}
				case final MEM.RelAccess relAccess -> {
					xmlLogger.begElement("access");
					xmlLogger.addAttribute("size", Long.toString(relAccess.size));
					xmlLogger.addAttribute("offset", Long.toString(relAccess.offset));
					if (relAccess.depth != -1)
						xmlLogger.addAttribute("depth", Long.toString(relAccess.depth));
					xmlLogger.endElement();
				}
				default -> {
					throw new Report.InternalError();
				}
				}
			}
			if (validForString.test(node)) {
				switch (stringAttr.get(node)) {
				case null -> {
					if (Compiler.devMode()) {
						xmlLogger.begElement("access");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				}
				case final MEM.AbsAccess absAccess -> {
					xmlLogger.begElement("access");
					xmlLogger.addAttribute("size", Long.toString(absAccess.size));
					xmlLogger.addAttribute("label", absAccess.label.name);
					xmlLogger.addAttribute("init", absAccess.init);
					xmlLogger.endElement();
				}
				}
			}
		}

	}

}