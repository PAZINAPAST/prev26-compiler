package prev26lang.phase.imrgen;

import java.util.function.*;

import prev26lang.*;
import prev26lang.common.logger.*;
import prev26lang.common.report.*;
import prev26lang.phase.*;
import prev26lang.phase.abstr.*;
import prev26lang.phase.memory.*;

/**
 * Intermediate representation generation phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class ImrGen extends Phase {

	// Attribute EntryLabel:

	/** Tester for nodes describing function definitions, i.e., for entry labels. */
	private static final Predicate<AST.Node> validForBodyEntryLabel = //
			(AST.Node node) -> (node instanceof AST.DefFunDefn);

	/** The entry label, i.e., to enter a function's body (from the prologue). */
	public static final AST.Attribute<MEM.Label> bodyEntryLabelAttr = new AST.Attribute<MEM.Label>(
			validForBodyEntryLabel);

	// Attribute ExitLabel:

	/** Tester for nodes describing function definitions, i.e., for exit labels. */
	private static final Predicate<AST.Node> validForBodyExitLabel = //
			(AST.Node node) -> (node instanceof AST.DefFunDefn);

	/** The exit label, i.e., to leave a function's body (to the epilogue). */
	public static final AST.Attribute<MEM.Label> bodyExitLabelAttr = new AST.Attribute<MEM.Label>(
			validForBodyExitLabel);

	// Attribute RawIR (generated intermediate representation) for expressions:

	/**
	 * Tester for nodes that intermediate representation made of expressions is
	 * generated for.
	 */
	private static final Predicate<AST.Node> validForExprIMR = //
			(AST.Node node) -> (node instanceof AST.Expr);

	/** Maps expressions to generated intermediate code. */
	public static final AST.Attribute<IMR.Expr> genExprIMRAttr = new AST.Attribute<IMR.Expr>(validForExprIMR);

	// Attribute RawIR (generated intermediate representation) for functions:

	/**
	 * Tester for nodes that intermediate representation for functions is generated
	 * for.
	 */
	private static final Predicate<AST.Node> validForFunIMR = //
			(AST.Node node) -> (node instanceof AST.DefFunDefn);

	/** Maps functions to generated intermediate code. */
	public static final AST.Attribute<IMR.Stmt> genStmtIMRAttr = new AST.Attribute<IMR.Stmt>(validForFunIMR);

	/**
	 * Phase construction.
	 */
	public ImrGen() {
		super("imrgen");
	}

	// ===== LOGGER =====

	/**
	 * Logger of the intermediate representation.
	 */
	public static class Logger extends Memory.Logger {

		/**
		 * Constructs a new intermediate code (generated) visitor.
		 * 
		 * @param xmlLogger The underlying XML logger.
		 */
		public Logger(final XMLLogger xmlLogger) {
			super(xmlLogger);
		}

		/**
		 * Logs all attributes of a node.
		 * 
		 * @param node The node.
		 */
		@Override
		public void logAttrs(final XMLLogger xmlLogger, final AST.Node node) {
			super.logAttrs(xmlLogger, node);
			if (validForBodyEntryLabel.test(node)) {
				xmlLogger.begElement("genimr");
				xmlLogger.begElement("entrylabel");
				switch (bodyEntryLabelAttr.get(node)) {
				case null -> {
					if (!Compiler.devMode()) {
						throw new Report.InternalError();
					}
				}
				case final MEM.Label label -> {
					xmlLogger.addAttribute("label", label.name);
				}
				}
				xmlLogger.endElement();
				xmlLogger.endElement();
			}
			if (validForBodyExitLabel.test(node)) {
				xmlLogger.begElement("genimr");
				xmlLogger.begElement("exitlabel");
				switch (bodyExitLabelAttr.get(node)) {
				case null -> {
					if (!Compiler.devMode()) {
						throw new Report.InternalError();
					}
				}
				case final MEM.Label label -> {
					xmlLogger.addAttribute("label", label.name);
				}
				}
				xmlLogger.endElement();
				xmlLogger.endElement();
			}
			if (validForExprIMR.test(node)) {
				switch (genExprIMRAttr.get(node)) {
				case null -> {
					if (Compiler.devMode()) {
						xmlLogger.begElement("genimr");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				}
				case final IMR.Expr genIMR -> {
					genIMR.log(xmlLogger);
				}
				}
			}
			if (validForFunIMR.test(node)) {
				switch (genStmtIMRAttr.get(node)) {
				case null -> {
					if (Compiler.devMode()) {
						xmlLogger.begElement("genimr");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				}
				case final IMR.Stmt genIMR -> {
					genIMR.log(xmlLogger);
				}
				}
			}
		}

	}

}
