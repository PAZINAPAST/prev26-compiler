package prev26lang.phase.seman;

import java.util.function.*;

import prev26lang.*;
import prev26lang.common.logger.*;
import prev26lang.common.report.*;
import prev26lang.phase.*;
import prev26lang.phase.abstr.*;

/**
 * Semantic analysis phase.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class SemAn extends Phase {

	/**
	 * Phase construction.
	 */
	public SemAn() {
		super("seman");
	}

	// Attribute DefAt:

	/** Tester for nodes that represent name usage. */
	private static final Predicate<AST.Node> validForDefAt = //
			(AST.Node node) -> (node instanceof AST.NameType) || (node instanceof AST.NameExpr);

	/** Mapping of names to definitions. */
	public static final AST.Attribute<AST.Defn> defAtAttr = new AST.Attribute<AST.Defn>(validForDefAt);

	// Attribute IsType:

	/** Tester for nodes defining types. */
	private static final Predicate<AST.Node> validForIsType = //
			(AST.Node node) -> (node instanceof AST.Type) || //
					((node instanceof AST.Defn) && ((node instanceof AST.TypDefn)) == true);

	/** Attribute specifying what type is defined by a node. */
	public static final AST.Attribute<TYP.Type> isTypeAttr = new AST.Attribute<TYP.Type>(validForIsType);

	// Attribute ofType:

	/** Tester for nodes that can be typed. */
	private static final Predicate<AST.Node> validForOfType = //
			(AST.Node node) -> (node instanceof AST.Expr) || //
					((node instanceof AST.Defn) && ((node instanceof AST.TypDefn)) == false);

	/** Attribute specifying what is a type of a node. */
	public static final AST.Attribute<TYP.Type> ofTypeAttr = new AST.Attribute<TYP.Type>(validForOfType);

	// Attribute IsConst:

	/** Tester for nodes describing constant expressions. */
	private static final Predicate<AST.Node> validForIsConst = //
			(AST.Node node) -> (node instanceof AST.Expr);

	/** Attribute specifying whether an expression consists of constants only. */
	public static final AST.Attribute<Boolean> isConstAttr = new AST.Attribute<Boolean>(validForIsConst);

	// Attribute IsAddr:

	/** Tester for nodes describing addressable expression. */
	private static final Predicate<AST.Node> validForIsAddr = //
			(AST.Node node) -> (node instanceof AST.Expr);

	/** Attribute specifying whether an expression is addressable or not. */
	public static final AST.Attribute<Boolean> isAddrAttr = new AST.Attribute<Boolean>(validForIsAddr);

	// ===== LOGGER =====

	/**
	 * Semantic analysis logger.
	 */
	public static class Logger extends Abstr.Logger {

		/**
		 * Constructs a new semantic analysis logger.
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
			if (validForDefAt.test(node)) {
				final AST.Defn defn = defAtAttr.get(node);
				if (defn == null) {
					if (Compiler.devMode()) {
						xmlLogger.begElement("defat");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				} else {
					xmlLogger.begElement("defat");
					xmlLogger.addAttribute("id", Integer.toString(defn.id));
					Location location = Abstr.locAttr.get(defn);
					location.log(xmlLogger);
					xmlLogger.endElement();
				}
			}
			if (validForIsType.test(node)) {
				final TYP.Type type = isTypeAttr.get(node);
				if (type == null) {
					if (Compiler.devMode()) {
						xmlLogger.begElement("istype");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				} else {
					xmlLogger.begElement("istype");
					type.accept(new TYP.Logger(xmlLogger), false);
					xmlLogger.endElement();
				}
			}
			if (validForOfType.test(node)) {
				final TYP.Type type = ofTypeAttr.get(node);
				if (type == null) {
					if (Compiler.devMode()) {
						xmlLogger.begElement("oftype");
						xmlLogger.addAttribute("none", "");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				} else {
					xmlLogger.begElement("oftype");
					type.accept(new TYP.Logger(xmlLogger), false);
					xmlLogger.endElement();
				}
			}
			if (validForIsConst.test(node)) {
				final Boolean isConst = isConstAttr.get(node);
				if (isConst == null) {
					if (Compiler.devMode()) {
						xmlLogger.begElement("isconst");
						xmlLogger.addAttribute("value", "none");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				} else {
					xmlLogger.begElement("isconst");
					xmlLogger.addAttribute("value", isConst.toString());
					xmlLogger.endElement();
				}
			}
			if (validForIsAddr.test(node)) {
				final Boolean isAddr = isAddrAttr.get(node);
				if (isAddr == null) {
					if (Compiler.devMode()) {
						xmlLogger.begElement("isaddr");
						xmlLogger.addAttribute("value", "none");
						xmlLogger.endElement();
					} else
						throw new Report.InternalError();
				} else {
					xmlLogger.begElement("isaddr");
					xmlLogger.addAttribute("value", isAddr.toString());
					xmlLogger.endElement();
				}
			}
		}

	}

}
