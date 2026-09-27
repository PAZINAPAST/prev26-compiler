package prev26lang.phase.imrgen;

import java.util.*;

import prev26lang.common.logger.*;
import prev26lang.common.report.*;
import prev26lang.phase.memory.*;

/**
 * Intermediate representation.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class IMR {

	/**
	 * Instruction of the intermediate representation.
	 */
	public static abstract class Instr implements Loggable {

		public abstract <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg);

	}

	/**
	 * Instruction denoting an expression.
	 */
	public static abstract class Expr extends Instr {
	}

	/**
	 * Binary operation (logical, relational, and arithmetic).
	 * 
	 * Evaluates the first subexpression, evaluates the second expression, performs
	 * the selected binary operation and returns its result.
	 */
	public static class BINOP extends Expr {

		public enum Oper {
			OR, AND, EQU, NEQ, LTH, GTH, LEQ, GEQ, ADD, SUB, MUL, DIV, MOD,
		}

		/** The operator. */
		public final Oper oper;

		/** The first operand. */
		public final Expr fstExpr;

		/** The second operand. */
		public final Expr sndExpr;

		/**
		 * Constructs a new binary operation.
		 * 
		 * @param oper    The operator.
		 * @param fstExpr The first operand.
		 * @param sndExpr The second operand.
		 */
		public BINOP(Oper oper, Expr fstExpr, Expr sndExpr) {
			this.oper = oper;
			this.fstExpr = fstExpr;
			this.sndExpr = sndExpr;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "BINOP(" + oper + ")");
			fstExpr.log(logger);
			sndExpr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "BINOP(" + oper + "," + fstExpr.toString() + "," + sndExpr.toString() + ")";
		}

	}

	/**
	 * Function call.
	 * 
	 * Evaluates arguments (the static link must be included) from left to right,
	 * calls the function denoted by the label provided and returns the function's
	 * result.
	 */
	public static class CALL extends Expr {

		/** The address of the function. */
		public final Expr addr;

		/** The offsets of arguments. */
		public final Vector<Long> offs;

		/** The values of arguments. */
		public final Vector<Expr> args;

		/**
		 * Constructs a function call.
		 * 
		 * @param addr The address of the function.
		 * @param offs The offsets of arguments.
		 * @param args The values of arguments.
		 */
		public CALL(Expr addr, Vector<Long> offs, Vector<Expr> args) {
			this.addr = addr;
			this.offs = new Vector<Long>(offs);
			this.args = new Vector<Expr>(args);
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "CALL");
			addr.log(logger);
			for (int a = 0; a < args.size(); a++)
				args.get(a).log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("CALL(");
			buffer.append(addr.toString());
			for (int a = 0; a < args.size(); a++) {
				buffer.append(",");
				buffer.append(offs.get(a).toString());
				buffer.append(":");
				buffer.append(args.get(a).toString());
			}
			buffer.append(")");
			return buffer.toString();
		}

	}

	/**
	 * Constant.
	 * 
	 * Returns the value of a constant.
	 */
	public static class CONST extends Expr {

		/** The value. */
		public final long value;

		/**
		 * Constructs a new constant.
		 * 
		 * @param value The value.
		 */
		public CONST(long value) {
			this.value = value;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", toString());
			logger.endElement();
		}

		@Override
		public String toString() {
			return "CONST(" + value + ")";
		}

	}

	/**
	 * Byte memory access.
	 * 
	 * Evaluates the address, reads the value from this address in the memory and
	 * returns the value read (but see {@link MOVE} as well.)
	 */
	public static class MEM1 extends Expr {

		/** The memory address. */
		public final Expr addr;

		/**
		 * Constucts a memory access.
		 * 
		 * @param addr The memory address.
		 */
		public MEM1(Expr addr) {
			this.addr = addr;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "MEM1");
			addr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "MEM1(" + addr.toString() + ")";
		}

	}

	/**
	 * Octa memory access.
	 * 
	 * Evaluates the address, reads the value from this address in the memory and
	 * returns the value read (but see {@link MOVE} as well.)
	 */
	public static class MEM8 extends Expr {

		/** The memory address. */
		public final Expr addr;

		/**
		 * Constucts a memory access.
		 * 
		 * @param addr The memory address.
		 */
		public MEM8(Expr addr) {
			this.addr = addr;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "MEM8");
			addr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "MEM8(" + addr.toString() + ")";
		}

	}

	/**
	 * Name.
	 * 
	 * Returns the address that the label is mapped to.
	 */
	public static class NAME extends Expr {

		/** The label. */
		public final MEM.Label label;

		/**
		 * Constructs a new name.
		 * 
		 * @param label The label.
		 */
		public NAME(MEM.Label label) {
			this.label = label;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "NAME(" + label.name + ")");
			logger.endElement();
		}

		@Override
		public String toString() {
			return "NAME(" + label.name + ")";
		}

	}

	/**
	 * Statement expression.
	 * 
	 * Executes the statement, evaluates the expression and returns its value.
	 */
	public static class SEXPR extends Expr {

		/** The statement. */
		public final Stmt stmt;

		/** The expression. */
		public final Expr expr;

		/**
		 * Constructs a statement expression.
		 * 
		 * @param stmt The statement.
		 * @param expr The expression.
		 */
		public SEXPR(Stmt stmt, Expr expr) {
			this.stmt = stmt;
			this.expr = expr;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "SEXPR");
			stmt.log(logger);
			expr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "SEXPR(" + stmt.toString() + "," + expr.toString() + ")";
		}

	}

	/**
	 * Temporary variable.
	 * 
	 * Returns the value of a temporary variable.
	 */
	public static class TEMP extends Expr {

		/** The temporary variable. */
		public final MEM.Temp temp;

		/**
		 * Constructs a temporary variable.
		 * 
		 * @param temp The temporary variable.
		 */
		public TEMP(MEM.Temp temp) {
			this.temp = temp;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "TEMP(T" + temp.temp + ")");
			logger.endElement();
		}

		@Override
		public String toString() {
			return "TEMP(T" + temp.temp + ")";
		}

	}

	/**
	 * Unary operation (logical, arithmetic).
	 * 
	 * Evaluates the value of the operand, performs the selected unary operation and
	 * return its result.
	 */
	public static class UNOP extends Expr {

		public enum Oper {
			NOT, NEG,
		}

		/** The operator. */
		public final Oper oper;

		/** The operand. */
		public final Expr subExpr;

		/**
		 * Constructs a unary operation.
		 * 
		 * @param oper    The operator.
		 * @param subExpr The operand.
		 */
		public UNOP(Oper oper, Expr subExpr) {
			this.oper = oper;
			this.subExpr = subExpr;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "UNOP(" + oper + ")");
			subExpr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "UNOP(" + oper + "," + subExpr.toString() + ")";
		}

	}

	/**
	 * Instruction denoting a statement.
	 */
	public static abstract class Stmt extends Instr {
	}

	/**
	 * Conditional jump.
	 * 
	 * Evaluates the condition, jumps to one address if the condition is nonzero or
	 * to other if the condition is zero.
	 */
	public static class CJUMP extends Stmt {

		/** The condition. */
		public Expr cond;

		/** The positive(if true) address. */
		public Expr posAddr;

		/** The negative(if false) address. */
		public Expr negAddr;

		/**
		 * Constructs a conditional jump.
		 * 
		 * @param cond     The condition.
		 * @param posLabel The positive label.
		 * @param negLabel The negative label.
		 */
		public CJUMP(Expr cond, Expr posLabel, Expr negLabel) {
			this.cond = cond;
			this.posAddr = posLabel;
			this.negAddr = negLabel;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "CJUMP");
			cond.log(logger);
			posAddr.log(logger);
			negAddr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "CJUMP(" + cond.toString() + "," + posAddr.toString() + "," + negAddr.toString() + ")";
		}

	}

	/**
	 * Expression statement.
	 * 
	 * Evaluates expression and throws the result away.
	 */
	public static class ESTMT extends Stmt {

		/** The expression. */
		public final Expr expr;

		/**
		 * Constructs an expression statement.
		 * 
		 * @param expr The expression.
		 */
		public ESTMT(Expr expr) {
			this.expr = expr;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "ESTMT");
			expr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "ESTMT(" + expr.toString() + ")";
		}

	}

	/**
	 * Unconditional jump.
	 * 
	 * Jumps to the address.
	 */
	public static class JUMP extends Stmt {

		/** The address. */
		public Expr addr;

		/**
		 * Constructs an uncoditional jump.
		 * 
		 * @param label The label.
		 */
		public JUMP(Expr addr) {
			this.addr = addr;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "JUMP");
			addr.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "JUMP(" + addr.toString() + ")";
		}

	}

	/**
	 * Label.
	 * 
	 * Does nothing.
	 */
	public static class LABEL extends Stmt {

		/** The label. */
		public MEM.Label label;

		/**
		 * Constructs a label.
		 * 
		 * @param label The label.
		 */
		public LABEL(MEM.Label label) {
			this.label = label;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "LABEL(" + label.name + ")");
			logger.endElement();
		}

		@Override
		public String toString() {
			return "LABEL(" + label.name + ")";
		}

	}

	/**
	 * Move operation.
	 * 
	 * Evaluates the destination, evaluates the source, and moves the source to the
	 * destination. If the root node of the destination is {@link MEM}, then the
	 * source is stored to the memory address denoted by the subtree of that
	 * {@link MEM} node. If the root node of the destination is {@link TEMP}, the
	 * source is stored in the temporary variable.
	 */
	public static class MOVE extends Stmt {

		/** The destination. */
		public final Expr dst;

		/** The source. */
		public final Expr src;

		/** Constructs a move operation. */
		public MOVE(Expr dst, Expr src) {
			this.dst = dst;
			this.src = src;
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "MOVE");
			dst.log(logger);
			src.log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			return "MOVE(" + dst.toString() + "," + src.toString() + ")";
		}

	}

	/**
	 * Sequence of statements.
	 * 
	 * Executes one statement after another.
	 */
	public static class STMTS extends Stmt {

		/** The sequence of statements. */
		public final Vector<Stmt> stmts;

		/**
		 * Constructs a sequence of statements.
		 * 
		 * @param stmts The sequence of statements.
		 */
		public STMTS(Vector<Stmt> stmts) {
			this.stmts = new Vector<Stmt>(stmts);
		}

		@Override
		public <Result, Arg> Result accept(Visitor<Result, Arg> visitor, Arg accArg) {
			return visitor.visit(this, accArg);
		}

		@Override
		public void log(XMLLogger logger) {
			logger.begElement("imc");
			logger.addAttribute("instruction", "STMTS");
			for (int s = 0; s < stmts.size(); s++)
				stmts.get(s).log(logger);
			logger.endElement();
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("STMTS(");
			for (int s = 0; s < stmts.size(); s++) {
				if (s > 0)
					buffer.append(",");
				buffer.append(stmts.get(s).toString());
			}
			buffer.append(")");
			return buffer.toString();
		}

	}

	/**
	 * An abstract visitor of the intermediate representation.
	 * 
	 * @author sliva
	 *
	 * @param <Result> The result the visitor produces.
	 * @param <Arg>    The argument the visitor carries around.
	 */
	public static interface Visitor<Result, Arg> {

		public default Result visit(BINOP binOp, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(CALL call, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(CJUMP cjump, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(CONST constant, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(ESTMT eStmt, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(JUMP jump, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(LABEL label, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(MEM1 mem, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(MEM8 mem, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(MOVE move, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(NAME name, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(SEXPR sExpr, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(STMTS stmts, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(TEMP temp, Arg visArg) {
			throw new Report.InternalError();
		}

		public default Result visit(UNOP unOp, Arg visArg) {
			throw new Report.InternalError();
		}

	}

}
