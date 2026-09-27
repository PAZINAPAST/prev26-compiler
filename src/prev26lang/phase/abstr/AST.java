package prev26lang.phase.abstr;

import java.util.*;
import java.util.function.*;

import prev26lang.*;
import prev26lang.common.logger.*;
import prev26lang.common.report.*;

/**
 * Abstract syntax tree.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class AST {

	/** (Unused but included to keep javadoc happy.) */
	private AST() {
		throw new Report.InternalError();
	}

	// ===== AST NODES =====

	// ----- Trees ----

	/**
	 * An abstract node of an abstract syntax tree.
	 */
	public static abstract class Node implements Locatable {

		/** The number of nodes constructed so far. */
		private static int numNodes = 0;

		/** The unique id of this node. */
		public final int id;

		/**
		 * Constructs a node of an abstract syntax tree.
		 */
		public Node() {
			id = numNodes++;
		}

		/**
		 * Returns the location of a part of the source file.
		 * 
		 * @return The location of a part of the source file.
		 */
		public Location location() {
			return Abstr.locAttr.get(this);
		}

		/**
		 * The acceptor method.
		 * 
		 * @param <Result>   The result type.
		 * @param <Argument> The argument type.
		 * @param visitor    The visitor accepted by this acceptor.
		 * @param arg        The argument.
		 * @return The result.
		 */
		public abstract <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg);

	}

	/**
	 * A sequence of nodes of an abstract syntax tree.
	 * 
	 * @param <N> The type of nodes stored in this sequence.
	 */
	public static class Nodes<N extends Node> extends Node implements Iterable<N> {

		/** The nodes stored in this sequence. */
		private final N[] nodes;

		/**
		 * Constructs a sequence of nodes.
		 */
		public Nodes() {
			this(new Vector<N>());
		}

		/**
		 * Constructs a sequence of nodes.
		 * 
		 * @param nodes The nodes stored in this sequence (no {@code null}s allowed).
		 */
		@SuppressWarnings("unchecked")
		public Nodes(final List<N> nodes) {
			super();
			this.nodes = (N[]) (new Node[nodes.size()]);
			int index = 0;
			for (final N n : nodes)
				this.nodes[index++] = n;
		}

		/**
		 * Returns the node at the specified position in this sequence.
		 * 
		 * @param index The index of the node to return.
		 * @return The node at the specified index.
		 */
		public final N get(final int index) {
			return nodes[index];
		}

		/**
		 * Returns the first node.
		 * 
		 * @return The first node.
		 */
		public final N first() {
			if (nodes.length == 0)
				throw new InternalError();
			return nodes[0];
		}

		/**
		 * Returns the last node.
		 * 
		 * @return The last node.
		 */
		public final N last() {
			if (nodes.length == 0)
				throw new InternalError();
			return nodes[nodes.length - 1];
		}

		/**
		 * Returns the number of nodes in this sequence.
		 * 
		 * @return The number of nodes in this sequence.
		 */
		public final int size() {
			return nodes.length;
		}

		// Iterable<N>

		@Override
		public void forEach(final Consumer<? super N> action) throws NullPointerException {
			for (final N n : this)
				action.accept(n);
		}

		@Override
		public Iterator<N> iterator() {
			return new NodesIterator();
		}

		// Iterator.

		/**
		 * Iterator over nodes with the removal operation blocked.
		 * 
		 * It is assumed that the underlying array of nodes is immutable.
		 */
		private final class NodesIterator implements Iterator<N> {

			/** Constructs a new iterator. */
			private NodesIterator() {
			}

			/** The index of the next node to be returned. */
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < nodes.length;
			}

			@Override
			public N next() throws NoSuchElementException {
				if (index < nodes.length)
					return nodes[index++];
				else
					throw new NoSuchElementException("");
			}

			@Override
			public void remove() {
				throw new Report.InternalError();
			}

			@Override
			public void forEachRemaining(final Consumer<? super N> action) {
				while (hasNext())
					action.accept(next());
			}

		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ----- Definitions -----

	/**
	 * A definition of a name.
	 */
	public static abstract class Defn extends Node {

		/** The defined name. */
		public final String name;

		/** The type of the defined name. */
		public final Type type;

		/**
		 * Constructs a definition of a name.
		 * 
		 * @param name The defined name.
		 * @param type The type of the defined name.
		 */
		public Defn(final String name, final Type type) {
			super();
			this.name = name;
			this.type = type;
		}

	}

	/**
	 * A stand-alone definition of a name.
	 */
	public static abstract class FullDefn extends Defn {

		/**
		 * Constructs a stand-alone definition of a name.
		 * 
		 * @param name The defined name.
		 * @param type The type of the defined name.
		 */
		public FullDefn(final String name, final Type type) {
			super(name, type);
		}

	}

	/**
	 * A definition of a name which is a part of a stand-alone definition of another
	 * name.
	 */
	public static abstract class PartDefn extends Defn {

		/**
		 * Constructs a definition of a name which is a part of a stand-alone definition
		 * of another name.
		 * 
		 * @param name The defined name.
		 * @param type The type of the defined name.
		 */
		public PartDefn(final String name, final Type type) {
			super(name, type);
		}

	}

	/**
	 * A definition of a type.
	 */
	public static class TypDefn extends FullDefn {

		/**
		 * Constructs a definition of a type.
		 * 
		 * @param name The name of the this type.
		 * @param type The type of the this type.
		 */
		public TypDefn(final String name, final Type type) {
			super(name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a variable.
	 */
	public static class VarDefn extends FullDefn {

		/**
		 * Constructs a definition of a variable.
		 * 
		 * @param name The name of the this variable.
		 * @param type The type of the this variable.
		 */
		public VarDefn(final String name, final Type type) {
			super(name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a function.
	 */
	public static abstract class FunDefn extends FullDefn {

		/** The parameters. */
		public final Nodes<ParDefn> pars;

		/**
		 * Constructs a definition of a function.
		 * 
		 * @param name The name of this function.
		 * @param pars The parameters (no {@code null}s allowed).
		 * @param type The result type of this function.
		 */
		public FunDefn(final String name, final Nodes<ParDefn> pars, final Type type) {
			super(name, type);
			this.pars = pars;
		}

	}

	/**
	 * A definition of a defined function.
	 */
	public static class DefFunDefn extends FunDefn {

		/** The expression. */
		public final Expr expr;

		/**
		 * Constructs a definition of a defined function.
		 * 
		 * @param name The name of this function.
		 * @param pars The parameters (no {@code null}s allowed).
		 * @param type The result type of this function.
		 * @param expr The expression.
		 */
		public DefFunDefn(final String name, final Nodes<ParDefn> pars, final Type type, final Expr expr) {
			super(name, pars, type);
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of an external function.
	 */
	public static class ExtFunDefn extends FunDefn {

		/**
		 * Constructs a definition of a defined function.
		 * 
		 * @param name The name of this function.
		 * @param pars The parameters (no {@code null}s allowed).
		 * @param type The result type of this function.
		 */
		public ExtFunDefn(final String name, final Nodes<ParDefn> pars, final Type type) {
			super(name, pars, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A parameter definition.
	 */
	public static class ParDefn extends PartDefn {

		/**
		 * Constructs a parameter definition.
		 * 
		 * @param name The name of this parameter.
		 * @param type The type of this parameter.
		 */
		public ParDefn(final String name, final Type type) {
			super(name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a record component.
	 */
	public static class CompDefn extends PartDefn {

		/**
		 * Constructs a definition of a record component.
		 * 
		 * @param name The name of this record component.
		 * @param type The type of this record component.
		 */
		public CompDefn(final String name, final Type type) {
			super(name, type);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ----- Types -----

	/**
	 * A type.
	 */
	public static abstract class Type extends Node {

		/**
		 * Constructs a type.
		 */
		public Type() {
			super();
		}

	}

	/**
	 * An atomic type.
	 */
	public static class AtomType extends Type {

		/** The atomic types. */
		public enum Type {
			/** Type {@code int}. */
			INT,
			/** Type {@code char}. */
			CHAR,
			/** Type {@code bool}. */
			BOOL,
			/** Type {@code void}. */
			VOID,
		};

		/** The kind of this atomic type. */
		public final Type type;

		/**
		 * Constructs an atomic type.
		 * 
		 * @param type The atomic type.
		 */
		public AtomType(final Type type) {
			super();
			this.type = type;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An array type.
	 */
	public static class ArrType extends Type {

		/** The type of elements in this array. */
		public final Type elemType;

		/** The number of elements. */
		public final String numElems;

		/**
		 * Constructs an array type.
		 * 
		 * @param elemType The type of elements in this array.
		 * @param numElems The number of elements.
		 */
		public ArrType(final Type elemType, final String numElems) {
			super();
			this.elemType = elemType;
			this.numElems = numElems;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A pointer type.
	 */
	public static class PtrType extends Type {

		/** The base type pointers of this type point to. */
		public final Type baseType;

		/**
		 * Constructs a pointer type.
		 * 
		 * @param baseType The base type pointers of this type point to.
		 */
		public PtrType(final Type baseType) {
			super();
			this.baseType = baseType;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A record type, i.e., either a struct or union.
	 */
	public static abstract class RecType extends Type {

		/** The components. */
		public final Nodes<CompDefn> comps;

		/**
		 * Constructs a record type.
		 * 
		 * @param comps The components of this union (no {@code null}s allowed).
		 */
		public RecType(final Nodes<CompDefn> comps) {
			super();
			this.comps = comps;
		}

	}

	/**
	 * A struct type.
	 */
	public static class StrType extends RecType {

		/**
		 * Constructs a struct type.
		 * 
		 * @param comps The components of this struct (no {@code null}s allowed).
		 */
		public StrType(final Nodes<CompDefn> comps) {
			super(comps);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A union type.
	 */
	public static class UniType extends RecType {

		/**
		 * Constructs a union type.
		 * 
		 * @param comps The components of this union (no {@code null}s allowed).
		 */
		public UniType(final Nodes<CompDefn> comps) {
			super(comps);
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A definition of a function type.
	 */
	public static class FunType extends Type {

		/** Types of parameters. */
		public final Nodes<Type> parTypes;

		/** A type of the function result. */
		public final Type resType;

		/**
		 * Constructs a function type.
		 * 
		 * @param parTypes The types of parameters.
		 * @param type     The result type.
		 */
		public FunType(final Nodes<Type> parTypes, final Type type) {
			super();
			this.parTypes = parTypes;
			this.resType = type;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A type name.
	 */
	public static class NameType extends Type {

		/** The name. */
		public final String name;

		/**
		 * Constructs a type name.
		 * 
		 * @param name The name of type.
		 */
		public NameType(final String name) {
			super();
			this.name = name;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ----- Expressions -----

	/**
	 * An expression.
	 */
	public static abstract class Expr extends Node {

		/**
		 * Constructs an expression.
		 */
		public Expr() {
			super();
		}

	}

	/**
	 * An array access expression.
	 */
	public static class ArrExpr extends Expr {

		/** The array. */
		public final Expr arrExpr;

		/** The index. */
		public final Expr idx;

		/**
		 * Constructs an array access expression.
		 * 
		 * @param arrExpr The array.
		 * @param idx     The index.
		 */
		public ArrExpr(final Expr arrExpr, final Expr idx) {
			super();
			this.arrExpr = arrExpr;
			this.idx = idx;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An assign expression.
	 */
	public static class AsgnExpr extends Expr {

		/** The first subexpression. */
		public final Expr fstExpr;

		/** The second subexpression. */
		public final Expr sndExpr;

		/**
		 * Constructs an assign expression.
		 * 
		 * @param fstExpr The first subexpression.
		 * @param sndExpr The second subexpression.
		 */
		public AsgnExpr(final Expr fstExpr, final Expr sndExpr) {
			super();
			this.fstExpr = fstExpr;
			this.sndExpr = sndExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Atom expression, i.e., a constant.
	 */
	public static class AtomExpr extends Expr {

		/** Types. */
		public enum Type {
			/** Constant of type {@code int}. */
			INT,
			/** Constant of type {@code char}. */
			CHAR,
			/** Constant of type {@code bool}. */
			BOOL,
			/** Constant of type {@code void}. */
			VOID,
			/** Constant of a pointer type. */
			PTR,
			/** Constant of type {@code ^char}. */
			STR,
		};

		/** The type of a constant. */
		public final Type type;

		/** The value of a constant. */
		public final String value;

		/**
		 * Constructs an atom expression, i.e., a constant.
		 * 
		 * @param type  The type of this constant.
		 * @param value The value of this constant.
		 */
		public AtomExpr(final Type type, final String value) {
			super();
			this.type = type;
			this.value = value;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A binary expression.
	 */
	public static class BinExpr extends Expr {

		/** Operators. */
		public enum Oper {
			/** Logical or. */
			OR,
			/** Logical and. */
			AND,
			/** Equal. */
			EQU,
			/** Not equal. */
			NEQ,
			/** Less than. */
			LTH,
			/** Greater than. */
			GTH,
			/** Less of equal. */
			LEQ,
			/** Greater or equal. */
			GEQ,
			/** Multiply. */
			MUL,
			/** Divide. */
			DIV,
			/** Modulo. */
			MOD,
			/** Add. */
			ADD,
			/** Subtract. */
			SUB,
		};

		/** The operator. */
		public final Oper oper;

		/** The first subexpression. */
		public final Expr fstExpr;

		/** The second subexpression. */
		public final Expr sndExpr;

		/**
		 * Constructs a binary expression.
		 * 
		 * @param oper    The operator.
		 * @param fstExpr The first subexpression.
		 * @param sndExpr The second subexpression.
		 */
		public BinExpr(final Oper oper, final Expr fstExpr, final Expr sndExpr) {
			super();
			this.oper = oper;
			this.fstExpr = fstExpr;
			this.sndExpr = sndExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A function call.
	 */
	public static class CallExpr extends Expr {

		/** The function. */
		public Expr funExpr;

		/** The arguments. */
		public final Nodes<Expr> argExprs;

		/**
		 * Constructs a function call.
		 * 
		 * @param funExpr  The function.
		 * @param argExprs The arguments (no {@code null}s allowed).
		 */
		public CallExpr(final Expr funExpr, final Nodes<Expr> argExprs) {
			super();
			this.funExpr = funExpr;
			this.argExprs = argExprs;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A cast expression.
	 */
	public static class CastExpr extends Expr {

		/** The type. */
		public final Type type;

		/** The expression. */
		public final Expr expr;

		/**
		 * Constructs a cast expression.
		 * 
		 * @param type The type.
		 * @param expr The expression.
		 */
		public CastExpr(final Type type, final Expr expr) {
			super();
			this.type = type;
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A component access expression.
	 */
	public static class CompExpr extends Expr {

		/** The container. */
		public final Expr recExpr;

		/** The component name. */
		public final String name;

		/**
		 * Constructs a component access expression.
		 * 
		 * @param recExpr The container.
		 * @param name    The component name.
		 */
		public CompExpr(final Expr recExpr, final String name) {
			super();
			this.recExpr = recExpr;
			this.name = name;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A name (of a variable or a function).
	 */
	public static class NameExpr extends Expr {

		/** The name. */
		public String name;

		/**
		 * Constructs a name (of a variable or a function).
		 * 
		 * @param name The name.
		 */
		public NameExpr(final String name) {
			super();
			this.name = name;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A prefix expression.
	 */
	public static class PfxExpr extends Expr {

		/** Operators. */
		public enum Oper {
			/** Sign. */
			ADD,
			/** Sign. */
			SUB,
			/** Negation. */
			NOT,
			/** Reference. */
			PTR,
		};

		/** The operator. */
		public final Oper oper;

		/** The subexpression. */
		public final Expr subExpr;

		/**
		 * Constructs a prefix expression.
		 * 
		 * @param subExpr The operator.
		 * @param expr    The subexpression.
		 */
		public PfxExpr(final Oper subExpr, final Expr expr) {
			super();
			this.oper = subExpr;
			this.subExpr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A suffix expression.
	 */
	public static class SfxExpr extends Expr {

		/** Operators. */
		public enum Oper {
			/** Dereference. */
			PTR,
		};

		/** The operator. */
		public final Oper oper;

		/** The subexpression. */
		public final Expr subExpr;

		/**
		 * Constructs a suffix expression.
		 * 
		 * @param subExpr The operator.
		 * @param expr    The subexpression.
		 */
		public SfxExpr(final Oper subExpr, final Expr expr) {
			super();
			this.oper = subExpr;
			this.subExpr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A {@code sizeof} expression.
	 */
	public static class SizeExpr extends Expr {

		/** The type. */
		public final Type type;

		/**
		 * Constructs a {code sizeof} expression.
		 * 
		 * @param type The type.
		 */
		public SizeExpr(final Type type) {
			super();
			this.type = type;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A sequence of subexpressions.
	 */
	public static class Exprs extends Expr {

		/** The expressions. */
		public final Nodes<Expr> exprs;

		/**
		 * Constructs a sequence of subexpressions.
		 * 
		 * @param subExprs The subexpressions.
		 */
		public Exprs(final Nodes<Expr> exprs) {
			super();
			this.exprs = exprs;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An if-then statement.
	 */
	public static class IfThenExpr extends Expr {

		/** The condition. */
		public final Expr condExpr;

		/** The expression in the then branch. */
		public final Expr thenExpr;

		/**
		 * Constructs an if-then expression.
		 * 
		 * @param condExpr The condition.
		 * @param thenExpr The expression in the then branch (no {@code null}s allowed).
		 */
		public IfThenExpr(final Expr condExpr, final Expr thenExpr) {
			super();
			this.condExpr = condExpr;
			this.thenExpr = thenExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * An if-then-else expression.
	 */
	public static class IfThenElseExpr extends IfThenExpr {

		/** The expression in the else branch. */
		public final Expr elseExpr;

		/**
		 * Constructs an if-then-else expression.
		 * 
		 * @param cond     The condition.
		 * @param thenExpr The expression in the then branch (no {@code null}s allowed).
		 * @param elseExpr The expression in the else branch (no {@code null}s allowed).
		 */
		public IfThenElseExpr(final Expr cond, final Expr thenExpr, final Expr elseExpr) {
			super(cond, thenExpr);
			this.elseExpr = elseExpr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A while expression.
	 */
	public static class WhileExpr extends Expr {

		/** The condition. */
		public final Expr condExpr;

		/** The inner expression. */
		public final Expr expr;

		/**
		 * Constructs a while expression.
		 * 
		 * @param condExpr The condition.
		 * @param expr     The inner expression.
		 */
		public WhileExpr(final Expr condExpr, final Expr expr) {
			super();
			this.condExpr = condExpr;
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A let expression.
	 */
	public static class LetExpr extends Expr {

		/** The definitions. */
		public final Nodes<FullDefn> defns;

		/** The inner expression. */
		public final Expr expr;

		/**
		 * Constructs a let statement.
		 * 
		 * @param defns The definitions.
		 * @param expr  The inner expression.
		 */
		public LetExpr(final Nodes<FullDefn> defns, final Expr expr) {
			super();
			this.defns = defns;
			this.expr = expr;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ===== VISITORS =====

	/**
	 * An abstract syntax tree visitor.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public interface Visitor<Result, Argument> {

		// ----- Trees -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(Nodes<? extends Node> nodes) {
			return visit(nodes, null);
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(Nodes<? extends Node> nodes, Argument arg) {
			throw new Report.InternalError();
		}

		// ----- Definitions -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(TypDefn typDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(VarDefn varDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(DefFunDefn defFunDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ExtFunDefn extFunDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ParDefn parDefn, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CompDefn compDefn, Argument arg) {
			throw new Report.InternalError();
		}

		// ----- Types -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(AtomType atomType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ArrType arrType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(PtrType ptrType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(StrType strType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(UniType uniType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(FunType funType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(NameType nameType, Argument arg) {
			throw new Report.InternalError();
		}

		// ----- Expressions -----

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(ArrExpr arrExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(AsgnExpr asgnExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(AtomExpr atomExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(BinExpr binExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CallExpr callExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CastExpr castExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CompExpr compExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(NameExpr nameExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(PfxExpr pfxExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(SfxExpr sfxExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(SizeExpr sizeExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(Exprs exprs, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(IfThenExpr ifThenExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(IfThenElseExpr ifTheElseExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(WhileExpr whileExpr, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(LetExpr letExpr, Argument arg) {
			throw new Report.InternalError();
		}

	}

	/**
	 * An abstract syntax tree visitor that does nothing.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public static interface NullVisitor<Result, Argument> extends Visitor<Result, Argument> {

		// ----- Trees -----

		@Override
		public default Result visit(Nodes<? extends Node> nodes, Argument arg) {
			return null;
		}

		// ----- Definitions -----

		@Override
		public default Result visit(TypDefn typDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(VarDefn varDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(DefFunDefn defFunDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ExtFunDefn extFunDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ParDefn parDefn, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CompDefn compDefn, Argument arg) {
			return null;
		}

		// ----- Types -----

		@Override
		public default Result visit(AtomType atomType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ArrType arrType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(PtrType ptrType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(StrType strType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(UniType uniType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(FunType funType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(NameType nameType, Argument arg) {
			return null;
		}

		// ----- Expressions -----

		@Override
		public default Result visit(ArrExpr arrExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(AsgnExpr asgnExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(AtomExpr atomExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(BinExpr binExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CallExpr callExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CastExpr castExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(CompExpr compExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(NameExpr nameExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(PfxExpr pfxExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(SfxExpr sfxExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(SizeExpr sizeExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(Exprs exprs, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(IfThenExpr ifThenExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(IfThenElseExpr ifThenElseExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(WhileExpr whileExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(LetExpr letExpr, Argument arg) {
			return null;
		}

	}

	/**
	 * An abstract syntax tree visitor that traverses the entire abstract syntax
	 * tree.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public interface FullVisitor<Result, Argument> extends Visitor<Result, Argument> {

		// ----- Trees -----

		@Override
		public default Result visit(Nodes<? extends Node> nodes, Argument arg) {
			for (final Node node : nodes)
				if ((node != null) || (!Compiler.devMode()))
					node.accept(this, arg);
			return null;
		}

		// ----- Definitions -----

		@Override
		public default Result visit(TypDefn typDefn, Argument arg) {
			if ((typDefn.type != null) || (!Compiler.devMode()))
				typDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(VarDefn varDefn, Argument arg) {
			if ((varDefn.type != null) || (!Compiler.devMode()))
				varDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(DefFunDefn defFunDefn, Argument arg) {
			if ((defFunDefn.pars != null) || (!Compiler.devMode()))
				defFunDefn.pars.accept(this, arg);
			if ((defFunDefn.type != null) || (!Compiler.devMode()))
				defFunDefn.type.accept(this, arg);
			if ((defFunDefn.expr != null) || (!Compiler.devMode()))
				defFunDefn.expr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(ExtFunDefn extFunDefn, Argument arg) {
			if ((extFunDefn.pars != null) || (!Compiler.devMode()))
				extFunDefn.pars.accept(this, arg);
			if ((extFunDefn.type != null) || (!Compiler.devMode()))
				extFunDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(ParDefn parDefn, Argument arg) {
			if ((parDefn.type != null) || (!Compiler.devMode()))
				parDefn.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CompDefn compDefn, Argument arg) {
			if ((compDefn.type != null) || (!Compiler.devMode()))
				compDefn.type.accept(this, arg);
			return null;
		}

		// ----- Types -----

		@Override
		public default Result visit(AtomType atomType, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(ArrType arrType, Argument arg) {
			if ((arrType.elemType != null) || (!Compiler.devMode()))
				arrType.elemType.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(PtrType ptrType, Argument arg) {
			if ((ptrType.baseType != null) || (!Compiler.devMode()))
				ptrType.baseType.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(StrType strType, Argument arg) {
			if ((strType.comps != null) || (!Compiler.devMode()))
				strType.comps.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(UniType uniType, Argument arg) {
			if ((uniType.comps != null) || (!Compiler.devMode()))
				uniType.comps.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(FunType funType, Argument arg) {
			if ((funType.parTypes != null) || (!Compiler.devMode()))
				funType.parTypes.accept(this, arg);
			if ((funType.resType != null) || (!Compiler.devMode()))
				funType.resType.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(NameType nameType, Argument arg) {
			return null;
		}

		// ----- Expressions -----

		@Override
		public default Result visit(ArrExpr arrExpr, Argument arg) {
			if ((arrExpr.arrExpr != null) || (!Compiler.devMode()))
				arrExpr.arrExpr.accept(this, arg);
			if ((arrExpr.idx != null) || (!Compiler.devMode()))
				arrExpr.idx.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(AsgnExpr asgnExpr, Argument arg) {
			if ((asgnExpr.fstExpr != null) || (!Compiler.devMode()))
				asgnExpr.fstExpr.accept(this, arg);
			if ((asgnExpr.sndExpr != null) || (!Compiler.devMode()))
				asgnExpr.sndExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(AtomExpr atomExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(BinExpr binExpr, Argument arg) {
			if ((binExpr.fstExpr != null) || (!Compiler.devMode()))
				binExpr.fstExpr.accept(this, arg);
			if ((binExpr.sndExpr != null) || (!Compiler.devMode()))
				binExpr.sndExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CallExpr callExpr, Argument arg) {
			if ((callExpr.funExpr != null) || (!Compiler.devMode()))
				callExpr.funExpr.accept(this, arg);
			if ((callExpr.argExprs != null) || (!Compiler.devMode()))
				callExpr.argExprs.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CastExpr castExpr, Argument arg) {
			if ((castExpr.type != null) || (!Compiler.devMode()))
				castExpr.type.accept(this, arg);
			if ((castExpr.expr != null) || (!Compiler.devMode()))
				castExpr.expr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(CompExpr compExpr, Argument arg) {
			if ((compExpr.recExpr != null) || (!Compiler.devMode()))
				compExpr.recExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(NameExpr nameExpr, Argument arg) {
			return null;
		}

		@Override
		public default Result visit(PfxExpr pfxExpr, Argument arg) {
			if ((pfxExpr.subExpr != null) || (!Compiler.devMode()))
				pfxExpr.subExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(SfxExpr sfxExpr, Argument arg) {
			if ((sfxExpr.subExpr != null) || (!Compiler.devMode()))
				sfxExpr.subExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(SizeExpr sizeExpr, Argument arg) {
			if ((sizeExpr.type != null) || (!Compiler.devMode()))
				sizeExpr.type.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(Exprs exprs, Argument arg) {
			if ((exprs.exprs != null) || (!Compiler.devMode()))
				exprs.exprs.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(IfThenExpr ifThenExpr, Argument arg) {
			if ((ifThenExpr.condExpr != null) || (!Compiler.devMode()))
				ifThenExpr.condExpr.accept(this, arg);
			if ((ifThenExpr.thenExpr != null) || (!Compiler.devMode()))
				ifThenExpr.thenExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(IfThenElseExpr ifThenElseExpr, Argument arg) {
			if ((ifThenElseExpr.condExpr != null) || (!Compiler.devMode()))
				ifThenElseExpr.condExpr.accept(this, arg);
			if ((ifThenElseExpr.thenExpr != null) || (!Compiler.devMode()))
				ifThenElseExpr.thenExpr.accept(this, arg);
			if ((ifThenElseExpr.elseExpr != null) || (!Compiler.devMode()))
				ifThenElseExpr.elseExpr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(WhileExpr whileExpr, Argument arg) {
			if ((whileExpr.condExpr != null) || (!Compiler.devMode()))
				whileExpr.condExpr.accept(this, arg);
			if ((whileExpr.expr != null) || (!Compiler.devMode()))
				whileExpr.expr.accept(this, arg);
			return null;
		}

		@Override
		public default Result visit(LetExpr letExpr, Argument arg) {
			if ((letExpr.defns != null) || (!Compiler.devMode()))
				letExpr.defns.accept(this, arg);
			if ((letExpr.expr != null) || (!Compiler.devMode()))
				letExpr.expr.accept(this, arg);
			return null;
		}

	}

	// ===== ATTRIBUTES =====

	/**
	 * An attribute of the abstract syntax tree node.
	 *
	 * @param <Value> Values associated with nodes.
	 */
	public static class Attribute<Value> {

		/** Mapping of nodes to values. */
		private final Vector<Value> mapping;

		/** Checker for testing whether a node is a valid key. */
		private Predicate<Node> keyChecker;

		/** A flag than makes this attribute immutable. */
		private boolean locked;

		/**
		 * Constructs a new attribute.
		 * 
		 * @param keyChecker Checker for testing whether a node is a valid key.
		 */
		public Attribute(Predicate<Node> keyChecker) {
			this.mapping = new Vector<Value>();
			this.keyChecker = keyChecker;
			this.locked = false;
		}

		/**
		 * Associates a value with the specified abstract syntax tree node.
		 * 
		 * @param node  The specified abstract syntax tree node.
		 * @param value The value.
		 * @return The value.
		 */
		public Value put(final Node node, final Value value) {
			if (locked)
				throw new Report.InternalError();
			if (!(keyChecker.test(node)))
				throw new Report.InternalError();
			int id = node.id;
			while (id >= mapping.size())
				mapping.setSize(id + 1000);
			mapping.set(id, value);
			return value;
		}

		/**
		 * Returns a value associated with the specified abstract syntax tree node.
		 * 
		 * @param node The specified abstract syntax tree node.
		 * @return The value (or {@code null} if the value is not found).
		 */
		public Value get(final Node node) {
			if (!(keyChecker.test(node)))
				throw new Report.InternalError();
			int id = node.id;
			if (id >= mapping.size())
				return null;
			return mapping.get(id);
		}

		/**
		 * Makes this attribute immutable.
		 */
		public void lock() {
			locked = true;
		}

	}

	// ===== LOGGER =====

	/**
	 * Abstract syntax tree logger.
	 * 
	 * This logger traverses the abstract syntax tree and produces an XML
	 * description of it.
	 */
	public static abstract class Logger implements Visitor<Object, String> {

		/** The logger the log should be written to. */
		public final XMLLogger xmlLogger;

		/**
		 * Construct a new visitor with a logger the log should be written to.
		 * 
		 * @param xmlLogger The logger the log should be written to.
		 */
		public Logger(final XMLLogger xmlLogger) {
			this.xmlLogger = xmlLogger;
		}

		/**
		 * A wrapper for accepting this visitor.
		 * 
		 * Makes the given node accept this visitor. Unless in development mode where
		 * the missing node is replaced by a placeholder, the internal error is thrown
		 * if the node is missing, i.e., when {@code null} pointer is given instead of a
		 * node.
		 * 
		 * @param node          The node that should accept this visitor.
		 * @param elemClassName The name of the class of the name accepting this visitor
		 *                      (needed only if the node is of type
		 *                      {@link prev26lang.phase.abstr.AST.Nodes}).
		 */
		private void safeAccept(final Node node, final String elemClassName) {
			if (node == null) {
				if (Compiler.devMode()) {
					xmlLogger.begElement("astnode");
					xmlLogger.addAttribute("none", "");
					xmlLogger.endElement();
				} else
					throw new Report.InternalError();
			} else
				node.accept(this, elemClassName);
		}

		/**
		 * Logs the attached attributes of a given abstract syntax tree node.
		 * 
		 * @param xmlLogger The logger the log should be written to.
		 * @param node      The node that the attributes must be logged for.
		 */
		public abstract void logAttrs(final XMLLogger xmlLogger, final Node node);

		// ----- Trees -----

		@Override
		public Object visit(Nodes<? extends Node> nodes) {
			return visit(nodes, "Nodes<Defn>");
		}

		@Override
		public Object visit(Nodes<? extends Node> nodes, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(nodes.id));
			xmlLogger.addAttribute("label", elemClassName);
			final String subElemClassName = elemClassName.replaceFirst("^[A-Za-z0-9]*<", "").replaceFirst(">$", "");
			logAttrs(xmlLogger, nodes);
			for (Node node : nodes)
				safeAccept(node, subElemClassName);
			xmlLogger.endElement();
			return null;
		}

		// ----- Definitions -----

		@Override
		public Object visit(TypDefn typDefn, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(typDefn.id));
			xmlLogger.addAttribute("label", typDefn.getClass().getSimpleName());
			xmlLogger.addAttribute("name", typDefn.name);
			logAttrs(xmlLogger, typDefn);
			safeAccept(typDefn.type, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(VarDefn varDefn, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(varDefn.id));
			xmlLogger.addAttribute("label", varDefn.getClass().getSimpleName());
			xmlLogger.addAttribute("name", varDefn.name);
			logAttrs(xmlLogger, varDefn);
			safeAccept(varDefn.type, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(DefFunDefn defFunDefn, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(defFunDefn.id));
			xmlLogger.addAttribute("label", defFunDefn.getClass().getSimpleName());
			xmlLogger.addAttribute("name", defFunDefn.name);
			logAttrs(xmlLogger, defFunDefn);
			safeAccept(defFunDefn.pars, "Nodes<ParDefn>");
			safeAccept(defFunDefn.type, null);
			safeAccept(defFunDefn.expr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(ExtFunDefn extFunDefn, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(extFunDefn.id));
			xmlLogger.addAttribute("label", extFunDefn.getClass().getSimpleName());
			xmlLogger.addAttribute("name", extFunDefn.name);
			logAttrs(xmlLogger, extFunDefn);
			safeAccept(extFunDefn.pars, "Nodes<ParDefn>");
			safeAccept(extFunDefn.type, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(ParDefn parDefn, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(parDefn.id));
			xmlLogger.addAttribute("label", parDefn.getClass().getSimpleName());
			xmlLogger.addAttribute("name", parDefn.name);
			logAttrs(xmlLogger, parDefn);
			safeAccept(parDefn.type, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(CompDefn compDefn, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(compDefn.id));
			xmlLogger.addAttribute("label", compDefn.getClass().getSimpleName());
			xmlLogger.addAttribute("name", compDefn.name);
			logAttrs(xmlLogger, compDefn);
			safeAccept(compDefn.type, null);
			xmlLogger.endElement();
			return null;
		}

		// ----- Types -----

		@Override
		public Object visit(AtomType atomType, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(atomType.id));
			xmlLogger.addAttribute("label", atomType.getClass().getSimpleName());
			xmlLogger.addAttribute("name", atomType.type.name());
			logAttrs(xmlLogger, atomType);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(ArrType arrType, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(arrType.id));
			xmlLogger.addAttribute("label", arrType.getClass().getSimpleName());
			xmlLogger.addAttribute("name", arrType.getClass().getSimpleName() + "[" + arrType.numElems + "]");
			logAttrs(xmlLogger, arrType);
			safeAccept(arrType.elemType, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(PtrType ptrType, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(ptrType.id));
			xmlLogger.addAttribute("label", ptrType.getClass().getSimpleName());
			logAttrs(xmlLogger, ptrType);
			safeAccept(ptrType.baseType, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(StrType strType, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(strType.id));
			xmlLogger.addAttribute("label", strType.getClass().getSimpleName());
			logAttrs(xmlLogger, strType);
			safeAccept(strType.comps, "Nodes<CompDefn>");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(UniType uniType, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(uniType.id));
			xmlLogger.addAttribute("label", uniType.getClass().getSimpleName());
			logAttrs(xmlLogger, uniType);
			safeAccept(uniType.comps, "Nodes<CompDefn>");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(FunType funType, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(funType.id));
			xmlLogger.addAttribute("label", funType.getClass().getSimpleName());
			logAttrs(xmlLogger, funType);
			safeAccept(funType.parTypes, "Nodes<Type>");
			safeAccept(funType.resType, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(NameType nameType, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(nameType.id));
			xmlLogger.addAttribute("label", nameType.getClass().getSimpleName());
			xmlLogger.addAttribute("name", nameType.name);
			logAttrs(xmlLogger, nameType);
			xmlLogger.endElement();
			return null;
		}

		// ----- Expressions -----

		@Override
		public Object visit(ArrExpr arrExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(arrExpr.id));
			xmlLogger.addAttribute("label", arrExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, arrExpr);
			safeAccept(arrExpr.arrExpr, null);
			safeAccept(arrExpr.idx, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(AsgnExpr asgnExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(asgnExpr.id));
			xmlLogger.addAttribute("label", asgnExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, asgnExpr);
			safeAccept(asgnExpr.fstExpr, null);
			safeAccept(asgnExpr.sndExpr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(AtomExpr atomExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(atomExpr.id));
			xmlLogger.addAttribute("label", atomExpr.getClass().getSimpleName());
			xmlLogger.addAttribute("name", atomExpr.value);
			logAttrs(xmlLogger, atomExpr);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(BinExpr binExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(binExpr.id));
			xmlLogger.addAttribute("label", binExpr.getClass().getSimpleName());
			xmlLogger.addAttribute("name", binExpr.oper.name());
			logAttrs(xmlLogger, binExpr);
			safeAccept(binExpr.fstExpr, null);
			safeAccept(binExpr.sndExpr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(CallExpr callExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(callExpr.id));
			xmlLogger.addAttribute("label", callExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, callExpr);
			safeAccept(callExpr.funExpr, null);
			safeAccept(callExpr.argExprs, "Nodes<Expr>");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(CastExpr castExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(castExpr.id));
			xmlLogger.addAttribute("label", castExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, castExpr);
			safeAccept(castExpr.expr, null);
			safeAccept(castExpr.type, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(CompExpr compExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(compExpr.id));
			xmlLogger.addAttribute("label", compExpr.getClass().getSimpleName());
			xmlLogger.addAttribute("name", compExpr.name);
			logAttrs(xmlLogger, compExpr);
			safeAccept(compExpr.recExpr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(NameExpr nameExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(nameExpr.id));
			xmlLogger.addAttribute("label", nameExpr.getClass().getSimpleName());
			xmlLogger.addAttribute("name", nameExpr.name);
			logAttrs(xmlLogger, nameExpr);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(PfxExpr pfxExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(pfxExpr.id));
			xmlLogger.addAttribute("label", pfxExpr.getClass().getSimpleName());
			xmlLogger.addAttribute("name", pfxExpr.oper.name());
			logAttrs(xmlLogger, pfxExpr);
			safeAccept(pfxExpr.subExpr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(SfxExpr sfxExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(sfxExpr.id));
			xmlLogger.addAttribute("label", sfxExpr.getClass().getSimpleName());
			xmlLogger.addAttribute("name", sfxExpr.oper.name());
			logAttrs(xmlLogger, sfxExpr);
			safeAccept(sfxExpr.subExpr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(SizeExpr sizeExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(sizeExpr.id));
			xmlLogger.addAttribute("label", sizeExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, sizeExpr);
			safeAccept(sizeExpr.type, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(Exprs exprs, String elemClassName) {
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(exprs.id));
			xmlLogger.addAttribute("label", exprs.getClass().getSimpleName());
			logAttrs(xmlLogger, exprs);
			if ((exprs.exprs != null) || (!Compiler.devMode()))
				exprs.exprs.accept(this, "Nodes<Expr>");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(IfThenExpr ifThenExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(ifThenExpr.id));
			xmlLogger.addAttribute("label", ifThenExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, ifThenExpr);
			safeAccept(ifThenExpr.condExpr, null);
			safeAccept(ifThenExpr.thenExpr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(IfThenElseExpr ifThenElseExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(ifThenElseExpr.id));
			xmlLogger.addAttribute("label", ifThenElseExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, ifThenElseExpr);
			safeAccept(ifThenElseExpr.condExpr, null);
			safeAccept(ifThenElseExpr.thenExpr, null);
			safeAccept(ifThenElseExpr.elseExpr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(WhileExpr whileExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(whileExpr.id));
			xmlLogger.addAttribute("label", whileExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, whileExpr);
			safeAccept(whileExpr.condExpr, null);
			safeAccept(whileExpr.expr, null);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(LetExpr letExpr, String elemClassName) {
			if (xmlLogger == null)
				return null;
			xmlLogger.begElement("astnode");
			xmlLogger.addAttribute("id", Integer.toString(letExpr.id));
			xmlLogger.addAttribute("label", letExpr.getClass().getSimpleName());
			logAttrs(xmlLogger, letExpr);
			safeAccept(letExpr.defns, "Nodes<Defn>");
			safeAccept(letExpr.expr, null);
			xmlLogger.endElement();
			return null;
		}

	}

}
