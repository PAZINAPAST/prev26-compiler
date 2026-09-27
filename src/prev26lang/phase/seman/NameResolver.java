package prev26lang.phase.seman;

import java.util.*;

import prev26lang.common.report.*;
import prev26lang.phase.abstr.*;
import prev26lang.phase.abstr.AST.*;
import prev26lang.phase.seman.SemAn;

/**
 * Name resolver.
 * 
 * The name resolver connects each node of a abstract syntax tree where a name
 * is used with the node where it is defined. The only exceptions are struct and
 * union component names which are connected with their definitions by the type
 * resolver. The results of the name resolver are stored in
 * {@link prev26lang.phase.seman.SemAn#defAtAttr}.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class NameResolver implements AST.FullVisitor<Object, NameResolver.Phase> {

	/** Constructs a new name resolver. */
	public NameResolver() {
	}

	enum Phase { 
		DEFINE,
		RESOLVE,
	}


//------------------------------------------------------------------------------------------------------
// Osnovni loop:
// DEFINE -> definiramo/vstavimo imena spremenljivk v simbolno table v trenutnem scope-u | Ukaz: symTable.ins(String, AST.Defn)
// RESOLVE -> rezreši poveži imena spremenljivk v izrazih z njihovimi definicijami | Ukaz -> symTable.fnd(String)
// symTable.newScope() - ustvari nov scope
// symTable.oldScope() - pobriše trenutni scope in gre na enega višje
//------------------------------------------------------------------------------------------------------


	@Override
	public Object visit(AST.Nodes<? extends Node> nodes, NameResolver.Phase arg) {
		for ( final Node node : nodes) node.accept(this, NameResolver.Phase.DEFINE);

		for ( final Node node : nodes) node.accept(this, NameResolver.Phase.RESOLVE);

		return null;
	}


	/** The symbol table. */
	private SymbTable symbTable = new SymbTable();

	//??? TODO ???
//------------------------------------------------------------------------------------------------------
// Definicija tipov
// DEFINE: zapišemo ime definiranega tipa v simbolno tabelo
// RESOLVE: Gremo naprej razrešiti tip tega definiranega tip
//------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.TypDefn typDefn, Phase arg) {
		if (arg == Phase.DEFINE) {
			try {
				symbTable.ins(typDefn.name, typDefn);
			} catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(typDefn.location(), "Type name '" + typDefn.name + "' is already defined within this scope.");
			}
		}

		if (arg == Phase.RESOLVE) {
			typDefn.type.accept(this, arg);
		}

		return null;
	}


//---------------------------------------------------------------------------------------------------------
// Definicija spremenljivk
// DEFINE: Vstavimo ime definirane spremenljivke v simbolno tabelo
// RESOLVE: Razrešimo dejanski tip te spremenjvike zato uporabimo accept metodo nad nejnim tipom
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.VarDefn varDefn, Phase arg) {
		if (arg == Phase.DEFINE) {
			try {
				symbTable.ins(varDefn.name, varDefn);
			} catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(varDefn.location(), "Variable name '" + varDefn.name + "' is already defined within this scope.");
			}
		}

		if (arg == Phase.RESOLVE) {
			varDefn.type.accept(this,arg);
		}

		return null;
	}

//---------------------------------------------------------------------------------------------------------
// Definicija definirane funckije
// DEFINE: V simbolno tabelo zapišemo ime funckije
// RESOLVE: Razrešimo tipe v expressionih ,ki so v tej funckiji in razrešimo oziroma povežemo tip in ime z tipom rezultata
// V vsakem primeru  funkcija ustvari svoj scope simbolni tabeli, ko gremo iz funkcije zapremo scope
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.DefFunDefn defFunDefn, Phase arg) {
		if (arg == Phase.DEFINE) {
			try {
				symbTable.ins(defFunDefn.name, defFunDefn);
			} catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(defFunDefn.location(), "Function name '"+ defFunDefn.name +"' is already defined within this scope.");
			}
			//defFunDefn.pars.accept(this, arg);
		}

		symbTable.newScope();

		defFunDefn.pars.accept(this, arg);

		if(arg == Phase.RESOLVE) {
			defFunDefn.type.accept(this, arg);
			defFunDefn.expr.accept(this, arg);
		}

		symbTable.oldScope();

		return null;
	}


//---------------------------------------------------------------------------------------------------------
// Definicija zunanje funkcije
// DEFINE: Shranimo ime funcije v simbolno tableo
// RESOLVE: Razrešimo tip rezultata funckije
// vsakič se moramo sprehoditi tudi po parametrih enako naredimo nov scope
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.ExtFunDefn extFunDefn, Phase arg) {
		if (arg == Phase.DEFINE) {
			try {
				symbTable.ins(extFunDefn.name, extFunDefn);
			} catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(extFunDefn.location(), "Function name '"+ extFunDefn.name +"' is already defined within this scope.");
			}
		}

		symbTable.newScope();

		extFunDefn.pars.accept(this, arg);

		if(arg == Phase.RESOLVE) {
			extFunDefn.type.accept(this, arg);
		}

		symbTable.oldScope();

		return null;
	}


//---------------------------------------------------------------------------------------------------------
// Parametri funkcije
// DEFINE: Definiramo ime parametra in ga zapišemo v simbolno tabelo
// RESOLVE: Potopimo se v tip parametra
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.ParDefn parDefn, Phase arg) {
		if (arg == Phase.DEFINE) {
			try {
				symbTable.ins(parDefn.name, parDefn);
			} catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(parDefn.location(), "Parameter name '"+ parDefn.name +"' is already defined within this scope.");
			}
		}

		if (arg == Phase.RESOLVE) {
			parDefn.type.accept(this, arg);
		}

		return null;
	}

//---------------------------------------------------------------------------------------------------------
// Komponente v strukturah/unijah
// DEFINE: Imena komponent zapišemo v simbolno tabelo
// RESOLVE: Potopimo se globlje v tip te komponente
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.CompDefn compDefn, Phase arg) {
		if (arg == Phase.DEFINE) {
			try {
				symbTable.ins(compDefn.name, compDefn);
			} catch (SymbTable.CannotInsNameException e) {
				throw new Report.Error(compDefn.location(), "Component name '" + compDefn.name +"' is already defined within this scope.");
			}
		}

		if (arg == Phase.RESOLVE) {
			compDefn.type.accept(this, arg);
		}

		return null;
	}


	// TYPES

//---------------------------------------------------------------------------------------------------------
// Ne naredimo ničesar
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.AtomType atomType, Phase arg) {
		return null;
	}

//---------------------------------------------------------------------------------------------------------
//  Potopimo se globlje v dejanski tip arraya
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.ArrType arrType, Phase arg) {

		if( arrType.elemType != null) {
			arrType.elemType.accept(this, arg);
		}

		return null;
	}

//---------------------------------------------------------------------------------------------------------
// Potopimo se globlje v dejanksi tip pointerja
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.PtrType ptrType, Phase arg) {
		if( ptrType.baseType != null) {
			ptrType.baseType.accept(this, arg);
		}

		return null;
	}

//---------------------------------------------------------------------------------------------------------
// Struktura
// RESOLVE: V razreševanju naredimo novi scope in gremo razreševati komponente
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.StrType strType, Phase arg) {
		
		if(arg == Phase.RESOLVE) {
			symbTable.newScope();
			strType.comps.accept(this, arg);
			symbTable.oldScope();
		}

		return null;

	} 

//---------------------------------------------------------------------------------------------------------
// Unija
// RESOLVE: v fazi razreševanja gremo opraviti pregled komponent in naredimo nov scope
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.UniType uniType, Phase arg) {
		
		if(arg == Phase.RESOLVE) {
			symbTable.newScope();
			uniType.comps.accept(this, arg);
			symbTable.oldScope();
		}

		return null;

	} 

//---------------------------------------------------------------------------------------------------------
// Pomožna funkcija da se spustiomo globlje v parametre in tip rezultata funkcije
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.FunType funType, Phase arg) {
		if(funType.parTypes != null) {
			funType.parTypes.accept(this, arg);
		}
		if(funType.resType != null) {
			funType.resType.accept(this, arg);
		}

		return null;
	}

//---------------------------------------------------------------------------------------------------------
// Imenski tip
// RESOLVE: Poišče ime v simbolni tabeli in z ukazom SemAn.defAttr.put() poveže uporabo tipa z dejansko definicijo
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.NameType nameType, Phase arg) {
		if (arg == Phase.RESOLVE) {
			try {
				AST.Defn defn = symbTable.fnd(nameType.name);
				SemAn.defAtAttr.put(nameType, defn);
			} catch(NameResolver.SymbTable.CannotFndNameException e) {
				throw new Report.Error(nameType.location(), "Name '" + nameType.name + "' is not defined within the currently active scope or any scope enclosing it.");
			}
		}

		return null;
	}

	// EXPRESSIONS

	@Override
	public Object visit(AST.ArrExpr arrExpr, Phase arg) {
		
		arrExpr.arrExpr.accept(this, arg);
		arrExpr.idx.accept(this, arg);
		

		return null;
	}

	@Override
	public Object visit(AST.AsgnExpr asgnExpr, Phase arg) {
		
		asgnExpr.fstExpr.accept(this, arg);
		asgnExpr.sndExpr.accept(this, arg);
		

		return null;
	}

	@Override
	public Object visit(AST.AtomExpr atomExpr, Phase arg) {
		return null;
	}


	@Override
	public Object visit(AST.BinExpr binExpr, Phase arg) {

		binExpr.fstExpr.accept(this, arg);
		
		binExpr.sndExpr.accept(this, arg);
		

		return null;
	}

	@Override
	public Object visit(AST.CallExpr callExpr, Phase arg) {
		
		callExpr.funExpr.accept(this, arg);
		if(callExpr.argExprs != null) {
			callExpr.argExprs.accept(this, arg);
		}
		
		return null;
	}	

	@Override
	public Object visit(AST.CompExpr compExpr, Phase arg) {
		if (compExpr.recExpr != null) {
			compExpr.recExpr.accept(this, arg);
		}
		return null;
	}

//---------------------------------------------------------------------------------------------------------
// Imenski izraz
// RESOLVE: Razreši in poveže uporabo z dejansko definicijo
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.NameExpr nameExpr, Phase arg) {
		if(arg == Phase.RESOLVE) {
			try {
				AST.Defn defn = symbTable.fnd(nameExpr.name);
				//System.out.println("Resolved " + nameExpr.name + " -> " + defn.getClass().getSimpleName() + " at depth " + symbTable.currDepth());
				SemAn.defAtAttr.put(nameExpr, defn);
			} catch(NameResolver.SymbTable.CannotFndNameException e) {
				throw new Report.Error(nameExpr.location(), "Name '" + nameExpr.name + "' is not defined within the currently active scope or any scope enclosing it.");
			}
		}
		return null;
	}

//---------------------------------------------------------------------------------------------------------
// V preostanku kode se le sprehajam po vozliščih
//---------------------------------------------------------------------------------------------------------
	@Override
	public Object visit(AST.PfxExpr pfxExpr, Phase arg) {
		
		pfxExpr.subExpr.accept(this, arg);
		
		return null;
	}

	@Override
	public Object visit(AST.SfxExpr sfxExpr, Phase arg) {
		
		sfxExpr.subExpr.accept(this, arg);
		
		return null;
	}

	@Override
	public Object visit(AST.SizeExpr sizeExpr, Phase arg) {
		
		if(sizeExpr.type != null) {
			sizeExpr.type.accept(this, arg);
		}

		return null;
	}

	@Override
	public Object visit(AST.Exprs exprs, Phase arg) {
		if(exprs.exprs.size() > 0) {
			for (Node node : exprs.exprs) {
				node.accept(this, arg);
			}
		}

		return null;
	}

	@Override
	public Object visit(AST.IfThenExpr ifThenExpr, Phase arg) {
		
		if(ifThenExpr.condExpr != null) {
			ifThenExpr.condExpr.accept(this, arg);
		}
		if(ifThenExpr.thenExpr != null) {
			ifThenExpr.thenExpr.accept(this, arg);
		}

		return null;
	}

	@Override
	public Object visit(AST.IfThenElseExpr ifThenElseExpr, Phase arg) {
		
		if(ifThenElseExpr.condExpr != null) {
			ifThenElseExpr.condExpr.accept(this, arg);
		}
		if(ifThenElseExpr.thenExpr != null) {
			ifThenElseExpr.thenExpr.accept(this, arg);
		}
		if(ifThenElseExpr.elseExpr != null) {
			ifThenElseExpr.elseExpr.accept(this, arg);
		}

		return null;
	}

	@Override
	public Object visit(AST.WhileExpr whileExpr, Phase arg) {

		if(whileExpr.condExpr != null) {
			whileExpr.condExpr.accept(this, arg);
		}
		if(whileExpr.expr != null) {
			whileExpr.expr.accept(this, arg);
		}

		return null;
	}

	@Override
	public Object visit(AST.LetExpr letExpr, Phase arg) {
		
		symbTable.newScope();

		if(arg == Phase.RESOLVE) {
			for(Node node : letExpr.defns) {
				node.accept(this, Phase.DEFINE);
			}

			for(Node node : letExpr.defns) {
				node.accept(this, Phase.RESOLVE);
			}

			letExpr.expr.accept(this, arg);

		}

		symbTable.oldScope();
		return null;
	}
	// ===== SYMBOL TABLE =====

	/**
	 * A symbol table.
	 */
	public class SymbTable {

		/**
		 * A symbol table record denoting a definition of a name within a certain scope.
		 */
		private class ScopedDefn {

			/** The depth of the scope the definition belongs to. */
			public final int depth;

			/** The definition. */
			public final AST.Defn defn;

			/**
			 * Constructs a new record denoting a definition of a name within a certain
			 * scope.
			 * 
			 * @param depth The depth of the scope the definition belongs to.
			 * @param defn  The definition.
			 */
			public ScopedDefn(int depth, AST.Defn defn) {
				this.depth = depth;
				this.defn = defn;
			}

		}

		/**
		 * A mapping of names into lists of records denoting definitions at different
		 * scopes. At each moment during the lifetime of a symbol table, the definition
		 * list corresponding to a particular name contains all definitions that name
		 * within currently active scopes: the definition at the inner most scope is the
		 * first in the list and is visible, the other definitions are hidden.
		 */
		private final HashMap<String, LinkedList<ScopedDefn>> allDefnsOfAllNames;

		/**
		 * The list of scopes. Each scope is represented by a list of names defined
		 * within it.
		 */
		private final LinkedList<LinkedList<String>> scopes;

		/** The depth of the currently active scope. */
		private int currDepth;

		/** Whether the symbol table can no longer be modified or not. */
		private boolean lock;

		/**
		 * Constructs a new symbol table.
		 */
		public SymbTable() {
			allDefnsOfAllNames = new HashMap<String, LinkedList<ScopedDefn>>();
			scopes = new LinkedList<LinkedList<String>>();
			currDepth = 0;
			lock = false;
			newScope();
		}

		/**
		 * Returns the depth of the currently active scope.
		 * 
		 * @return The depth of the currently active scope.
		 */
		public int currDepth() {
			return currDepth;
		}

		/**
		 * Inserts a new definition of a name within the currently active scope or
		 * throws an exception if this name has already been defined within this scope.
		 * Once the symbol table is locked, any attempt to insert further definitions
		 * results in an internal error.
		 * 
		 * @param name The name.
		 * @param defn The definition.
		 * @throws CannotInsNameException Thrown if this name has already been defined
		 *                                within the currently active scope.
		 */
		public void ins(String name, AST.Defn defn) throws CannotInsNameException {
			if (lock)
				throw new Report.InternalError();

			LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.get(name);
			if (allDefnsOfName == null) {
				allDefnsOfName = new LinkedList<ScopedDefn>();
				allDefnsOfAllNames.put(name, allDefnsOfName);
			}

			if (!allDefnsOfName.isEmpty()) {
				ScopedDefn defnOfName = allDefnsOfName.getFirst();
				if (defnOfName.depth == currDepth)
					throw new CannotInsNameException();
			}

			allDefnsOfName.addFirst(new ScopedDefn(currDepth, defn));
			scopes.getFirst().addFirst(name);
		}

		/**
		 * Returns the currently visible definition of the specified name. If no
		 * definition of the name exists within these scopes, an exception is thrown.
		 * 
		 * @param name The name.
		 * @return The definition.
		 * @throws CannotFndNameException Thrown if the name is not defined within the
		 *                                currently active scope or any scope enclosing
		 *                                it.
		 */
		public AST.Defn fnd(String name) throws CannotFndNameException {
			LinkedList<ScopedDefn> allDefnsOfName = allDefnsOfAllNames.get(name);
			if (allDefnsOfName == null)
				throw new CannotFndNameException();

			if (allDefnsOfName.isEmpty())
				throw new CannotFndNameException();

			return allDefnsOfName.getFirst().defn;
		}

		/** Used for selecting the range of scopes. */
		public enum XScopeSelector {
			/** All live scopes. */
			ALL,
			/** Currently active scope. */
			ACT,
		}

		/**
		 * Constructs a new scope within the currently active scope. The newly
		 * constructed scope becomes the currently active scope.
		 */
		public void newScope() {
			if (lock)
				throw new Report.InternalError();

			currDepth++;
			scopes.addFirst(new LinkedList<String>());
		}

		/**
		 * Destroys the currently active scope by removing all definitions belonging to
		 * it from the symbol table. Makes the enclosing scope the currently active
		 * scope.
		 */
		public void oldScope() {
			if (lock)
				throw new Report.InternalError();

			if (currDepth == 0)
				throw new Report.InternalError();

			for (String name : scopes.getFirst()) {
				allDefnsOfAllNames.get(name).removeFirst();
			}
			scopes.removeFirst();
			currDepth--;
		}

		/**
		 * Prevents further modifications of this symbol table.
		 */
		public void lock() {
			lock = true;
		}

		/**
		 * An exception thrown when the name cannot be inserted into a symbol table.
		 */
		@SuppressWarnings("serial")
		public class CannotInsNameException extends Exception {

			/**
			 * Constructs a new exception.
			 */
			private CannotInsNameException() {
			}

		}

		/**
		 * An exception thrown when the name cannot be found in the symbol table.
		 */
		@SuppressWarnings("serial")
		public class CannotFndNameException extends Exception {

			/**
			 * Constructs a new exception.
			 */
			private CannotFndNameException() {
			}

		}

	}

}
