package prev26lang.phase.seman;

import java.util.*;
import java.util.function.*;

import prev26lang.common.logger.*;
import prev26lang.common.report.*;
import prev26lang.*;

/**
 * Internal representation of types.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class TYP {

	/** (Unused but included to keep javadoc happy.) */
	private TYP() {
		throw new Report.InternalError();
	}

	// ===== TYPE NODES =====

	/**
	 * A type.
	 */
	public static abstract class Type {

		/** The number of nodes constructed so far. */
		private static int numNodes = 0;

		/** The unique id of this node. */
		public final int id;

		/** Constructs a new type. */
		public Type() {
			id = numNodes++;
		}

		/**
		 * Returns the actual type, i.e., not the renamed one.
		 * 
		 * @return The actual type, i.e., not the renamed one.
		 */
		public Type actualType() {
			return this;
		}

		/**
		 * Returns the actual type, i.e., not the renamed one.
		 * 
		 * @param types The types encountered during the evaluation of the actual type.
		 * @return The actual type, i.e., not the renamed one.
		 */
		public Type actualType(final HashSet<NameType> types) {
			return this;
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
	 * A sequence of types.
	 * 
	 * @param <T> The type of types stored in this sequence.
	 */
	public static class Types<T extends Type> extends Type implements Iterable<T> {

		/** The types stored in this sequence. */
		private final T[] types;

		/**
		 * Constructs a sequence of types.
		 */
		public Types() {
			this(new Vector<T>());
		}

		/**
		 * Constructs a sequence of types.
		 * 
		 * @param types The types stored in this sequence (no {@code null}s allowed).
		 */
		@SuppressWarnings("unchecked")
		public Types(final List<T> types) {
			this.types = (T[]) (new Type[types.size()]);
			int index = 0;
			for (final T t : types)
				this.types[index++] = t;
		}

		/**
		 * Returns the type at the specified position in this sequence.
		 * 
		 * @param index The index of the type to return.
		 * @return The type at the specified index.
		 */
		public final T get(final int index) {
			return types[index];
		}

		/**
		 * Returns the number of types in this sequence.
		 * 
		 * @return The number of types in this sequence.
		 */
		public final int size() {
			return types.length;
		}

		// Iterable<T>

		@Override
		public void forEach(final Consumer<? super T> action) throws NullPointerException {
			for (final T t : this)
				action.accept(t);
		}

		@Override
		public Iterator<T> iterator() {
			return new TypesIterator();
		}

		// Iterator.

		/**
		 * Iterator over types with the removal operation blocked.
		 * 
		 * It is assumed that the underlying array of types is immutable.
		 */
		private final class TypesIterator implements Iterator<T> {

			/** Constructs a new iterator. */
			private TypesIterator() {
			}

			/** The index of the next type to be returned. */
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < types.length;
			}

			@Override
			public T next() throws NoSuchElementException {
				if (index < types.length)
					return types[index++];
				else
					throw new NoSuchElementException("");
			}

			@Override
			public void remove() {
				throw new Report.InternalError();
			}

			@Override
			public void forEachRemaining(final Consumer<? super T> action) {
				while (hasNext())
					action.accept(next());
			}

		}

		@Override
		public String toString() {
			final StringBuffer str = new StringBuffer();
			str.append("(");
			boolean fst = true;
			for (Type type : types) {
				if (!fst)
					str.append(",");
				str.append(type.toString());
				fst = false;
			}
			str.append(")");
			return str.toString();
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
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A data type with values.
	 */
	public static abstract class ValueType extends Type {

		/** Constructs a new data type with values. */
		public ValueType() {
		}

	}

	/**
	 * A data type with simple values.
	 */
	public static abstract class SimpleType extends ValueType {

		/** Constructs a new data type with simple values. */
		public SimpleType() {
		}

	}

	/**
	 * A data type with atom values.
	 */
	public static abstract class AtomType extends SimpleType {

		/** Constructs a new data type with atom values. */
		public AtomType() {
		}

	}

	/**
	 * Integer type.
	 */
	public static class IntType extends AtomType {

		/** An object of {@code SemIntType} class. */
		public static final IntType type = new IntType();

		/** Constructs a new integer type. */
		private IntType() {
		}

		@Override
		public String toString() {
			return "int";
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Character type.
	 */
	public static class CharType extends AtomType {

		/** An object of {@code SemCharType} class. */
		public static final CharType type = new CharType();

		/** Constructs a new charater type. */
		private CharType() {
		}

		@Override
		public String toString() {
			return "char";
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Boolean type.
	 */
	public static class BoolType extends AtomType {

		/** An object of {@code SemBoolType} class. */
		public static final BoolType type = new BoolType();

		/** Constructs a new boolean type. */
		private BoolType() {
		}

		@Override
		public String toString() {
			return "bool";
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Void type.
	 */
	public static class VoidType extends Type {

		/** An object of {@code SemVoidType} class. */
		public static final VoidType type = new VoidType();

		/** Constructs a void type. */
		private VoidType() {
		}

		@Override
		public String toString() {
			return "void";
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * Array type.
	 */
	public static class ArrType extends ValueType {

		/** The type of elements in this array. */
		public final Type elemType;

		/** The size of the array. */
		public final Long numElems;

		/**
		 * Constructs an array type.
		 * 
		 * @param elemType The type of elements in this array.
		 * @param numElems The size of the array.
		 */
		public ArrType(final Type elemType, final Long numElems) {
			this.elemType = elemType;
			this.numElems = numElems;
		}

		@Override
		public String toString() {
			return "array[" + numElems + "]" + elemType.toString();
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A pointer type.
	 */
	public static class PtrType extends SimpleType {

		/** An object of {@code SemPointerType(null)} class. */
		public static final PtrType type = new PtrType(VoidType.type);

		/** The base type (or {@code null} if it denotes <code>nil</code>). */
		public final Type baseType;

		/**
		 * Constructs a pointer type.
		 * 
		 * @param baseType The base type.
		 */
		public PtrType(final Type baseType) {
			this.baseType = baseType;
		}

		@Override
		public String toString() {
			return "^" + (baseType == null ? "" : baseType.toString());
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A record type.
	 */
	public static abstract class RecType extends ValueType {

		/** Types of components. */
		public final Types<Type> compTypes;

		/**
		 * Constructs a new record type.
		 * 
		 * @param compTypes Types of components (no {@code null}s allowed).
		 */
		public RecType(final List<Type> compTypes) {
			this.compTypes = new Types<Type>(compTypes);
		}

	}

	/**
	 * A struct type.
	 */
	public static class StrType extends RecType {

		/**
		 * Constructs a new struct type.
		 * 
		 * @param compTypes Types of components (no {@code null}s allowed).
		 */
		public StrType(final List<Type> compTypes) {
			super(compTypes);
		}

		@Override
		public String toString() {
			final StringBuffer str = new StringBuffer();
			str.append("struct");
			str.append(compTypes.toString());
			return str.toString();
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
		 * Constructs a new union type.
		 * 
		 * @param compTypes Types of components (no {@code null}s allowed).
		 */
		public UniType(final List<Type> compTypes) {
			super(compTypes);
		}

		@Override
		public String toString() {
			final StringBuffer str = new StringBuffer();
			str.append("union");
			str.append(compTypes.toString());
			return str.toString();
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

		/** The actual type this named type represents. */
		private Type actType = null;

		/**
		 * Constructs a new type name.
		 * 
		 * @param name The name.
		 */
		public NameType(final String name) {
			this.name = name;
		}

		/**
		 * Defines the actual type this named type represents.
		 * 
		 * @param type The actual type this named type represents.
		 */
		public void setActType(final Type type) {
			if (this.actType != null)
				throw new Report.InternalError();
			this.actType = type;
		}

		/**
		 * Returns the actual type this named type represents.
		 * 
		 * @return The actual type this named type represents.
		 */
		public Type type() {
			if (this.actType == null)
				throw new Report.InternalError();
			return actType;
		}

		@Override
		public Type actualType() {
			if (this.actType == null)
				throw new Report.InternalError();
			final HashSet<NameType> types = new HashSet<NameType>();
			types.add(this);
			return this.actType.actualType(types);
		}

		@Override
		public Type actualType(final HashSet<NameType> types) {
			if (this.actType == null)
				throw new Report.InternalError();
			if (types.contains(this))
				throw new Report.InternalError();
			types.add(this);
			return this.actType.actualType(types);
		}

		@Override
		public String toString() {
			return name;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	/**
	 * A function type.
	 */
	public static class FunType extends SimpleType {

		/** Parameters. */
		public final Types<Type> parTypes;

		/** A result type. */
		public final Type resType;

		/**
		 * Constructs a new function type.
		 * 
		 * @param parTypes The parameters (no {@code null}s allowed).
		 * @param resType  The result type.
		 */
		public FunType(final List<Type> parTypes, final Type resType) {
			this.parTypes = new Types<Type>(parTypes);
			this.resType = resType;
		}

		@Override
		public <Result, Argument> Result accept(Visitor<Result, Argument> visitor, Argument arg) {
			return visitor.visit(this, arg);
		}

	}

	// ===== VISITORS =====

	/**
	 * A visitor of semantic representation of a type.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public static interface Visitor<Result, Argument> {

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(Types<? extends Type> types, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(IntType intType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(CharType charType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(BoolType boolType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(VoidType voidType, Argument arg) {
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
		public default Result visit(NameType nameType, Argument arg) {
			throw new Report.InternalError();
		}

		@SuppressWarnings({ "doclint:missing" })
		public default Result visit(FunType funType, Argument arg) {
			throw new Report.InternalError();
		}

	}

	/**
	 * A visitor of semantic representation of a type that does nothing.
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public static class NullVisitor<Result, Argument> implements Visitor<Result, Argument> {

		/**
		 * Constructs a new visitor of semantic representation of a type that does
		 * nothing.
		 */
		public NullVisitor() {
		}

		@Override
		public Result visit(IntType intType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(CharType charType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(BoolType boolType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(VoidType voidType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(ArrType arrType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(PtrType ptrType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(StrType strType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(UniType uniType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(NameType nameType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(FunType funType, Argument arg) {
			return null;
		}

	}

	/**
	 * A visitor of semantic representation of a type that traverses the entire
	 * semantic representation of a type (but stops before entering a loop).
	 * 
	 * @param <Result>   The result type.
	 * @param <Argument> The argument type.
	 */
	public static class FullVisitor<Result, Argument> implements Visitor<Result, Argument> {

		/** The set of all name types already visited. */
		private final HashSet<NameType> visited = new HashSet<NameType>();

		/**
		 * Constructs a new visitor of semantic representation of a type that traverses
		 * the entire semantic representation of a type.
		 */
		public FullVisitor() {
		}

		@Override
		public Result visit(Types<? extends Type> types, Argument arg) {
			for (TYP.Type type : types)
				type.accept(this, arg);
			return null;
		}

		@Override
		public Result visit(IntType intType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(CharType charType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(BoolType boolType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(VoidType voidType, Argument arg) {
			return null;
		}

		@Override
		public Result visit(ArrType arrType, Argument arg) {
			arrType.elemType.accept(this, arg);
			return null;
		}

		@Override
		public Result visit(PtrType ptrType, Argument arg) {
			ptrType.baseType.accept(this, arg);
			return null;
		}

		@Override
		public Result visit(StrType strType, Argument arg) {
			strType.compTypes.accept(this, arg);
			return null;
		}

		@Override
		public Result visit(UniType uniType, Argument arg) {
			uniType.compTypes.accept(this, arg);
			return null;
		}

		@Override
		public Result visit(NameType nameType, Argument arg) {
			if (!visited.contains(nameType)) {
				visited.add(nameType);
				nameType.actType.accept(this, arg);
			}
			return null;
		}

		@Override
		public Result visit(FunType funType, Argument arg) {
			funType.parTypes.accept(this, arg);
			funType.resType.accept(this, arg);
			return null;
		}

	}

	// ===== LOGGER =====

	/**
	 * Type logger.
	 * 
	 * This logger traverses the type tree and produces an XML description of it.
	 */
	public static class Logger extends FullVisitor<Object, Boolean> {

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

		/***
		 * 
		 * A wrapper for accepting this visitor.
		 * 
		 * Makes the given type accept this visitor.Unless in development mode where*
		 * the missing type is replaced by a placeholder, the internal error is
		 * thrown*if the type is missing, i.e.,when {@code null} pointer is given
		 * instead of an object.
		 * 
		 * @param type The type that should accept this visitor.
		 * @param full {@code true}if the type should be printed out in-depth or
		 *             {@code false}if the type should be printed with the top-most node
		 *             only (applicable only for
		 *             {@link prev26lang.phase.seman.TYP.NameType}).
		 */

		private void safeAccept(final Type type, final Boolean full) {
			if (type == null) {
				if (Compiler.devMode()) {
					xmlLogger.begElement("typnode");
					xmlLogger.addAttribute("none", "");
					xmlLogger.endElement();
				} else
					throw new Report.InternalError();
			} else
				type.accept(this, full);
		}

		@Override
		public Object visit(Types<? extends Type> types, Boolean full) {
			for (TYP.Type type : types) {
				safeAccept(type, full);
			}
			return null;
		}

		@Override
		public Object visit(IntType intType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(intType.id));
			xmlLogger.addAttribute("label", "INT");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(CharType charType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(charType.id));
			xmlLogger.addAttribute("label", "CHAR");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(BoolType boolType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(boolType.id));
			xmlLogger.addAttribute("label", "BOOL");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(VoidType voidType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(voidType.id));
			xmlLogger.addAttribute("label", "VOID");
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(ArrType arrType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(arrType.id));
			xmlLogger.addAttribute("label", "ARRAY[" + arrType.numElems + "]");
			safeAccept(arrType.elemType, false);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(PtrType ptrType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(ptrType.id));
			xmlLogger.addAttribute("label", "POINTER");
			safeAccept(ptrType.baseType, false);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(StrType strType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(strType.id));
			xmlLogger.addAttribute("label", "STRUCT");
			safeAccept(strType.compTypes, false);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(UniType uniType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(uniType.id));
			xmlLogger.addAttribute("label", "UNION");
			safeAccept(uniType.compTypes, false);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(NameType nameType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(nameType.id));
			xmlLogger.addAttribute("label", "NAME " + nameType.name);
			if (full)
				safeAccept(nameType.type(), false);
			xmlLogger.endElement();
			return null;
		}

		@Override
		public Object visit(FunType funType, Boolean full) {
			xmlLogger.begElement("typnode");
			xmlLogger.addAttribute("id", Integer.toString(funType.id));
			xmlLogger.addAttribute("label", "FUNCTION");
			funType.parTypes.accept(this, false);
			safeAccept(funType.resType, false);
			xmlLogger.endElement();
			return null;
		}

	}

}
