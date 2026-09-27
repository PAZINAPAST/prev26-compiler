package prev26lang.phase.seman;

import java.util.*;

import prev26lang.common.report.*;
import prev26lang.phase.abstr.*;
import prev26lang.phase.abstr.AST.*;
import prev26lang.phase.seman.SemAn;
import prev26lang.phase.seman.NameResolver.*;
import prev26lang.phase.seman.TYP.*;

/**
 * Type constructor.
 * 
 * Sprehodi se po AST drevesu in vsaki deklaraciji(typedefe,deklaracije spremenljivk in funkcij)
 *  konstruira ustrezen tipski objekt na podalgi tipske sintakse v izvorni kodi.
 * 
 * Za konstruiranje tipov uporabljamo knjižnico TYP.java
 * 
 * Rezultat se shrani kot atribut vozlišča v AST drevesu z ukazom:
 * SemAn.isTypeAttr.put(AST.Node, TYP.Type). 
 *  {Ukaz specificira katerega tip je definiran by vozlišče}
 */


public class TypeConstructor implements AST.FullVisitor<TYP.Type, TypeConstructor.Mode> {

    private boolean hasMain = false;
    private boolean isRoot = true;
    private int depth = 0;
    public static final java.util.Map<TYP.RecType, AST.RecType> astRecordMap = new java.util.HashMap<>();

    public TypeConstructor() {}

    enum Mode {
        BUILD, RESOLVE,
    }

    /**
     * Type construktor gradi tipe v dveh prehodih.
     * BUILD -> naredi placeholderje za imena in preveri ali program vsebuje funkcijo main. 
     * RESOLVE -> napolni dejasnke tipe in jih poveže
     * 
     * S tem da razdelimo na dve fazi nam omogoči, da se lahko tip sklicuje sam nase ali na tip, ki je definiran kasneje v bloku.
     */
    @Override
    public TYP.Type visit(AST.Nodes<? extends Node> nodes, Mode arg) {
        
        depth++;
        for (final AST.Node node : nodes) node.accept(this, Mode.BUILD);
        for (final AST.Node node : nodes) node.accept(this, Mode.RESOLVE);
        if (depth == 1 && !hasMain) {
            throw new Report.Error(nodes.location(), 
                "No main function defined. The program must have a main function defined.");
        }
        depth--;
        return null;
      
    }

    /**
     * Konstruiranje osnovnih tipov.
     * TYP:5 -> [int]ISTYPE = int
     * TYP:6 -> [char]ISTYPE = char
     * TYP:7 -> [bool]ISTYPE = bool
     * TYP:8 -> [void]ISTYPE = void
     */
    @Override
    public TYP.Type visit(AST.AtomType atomType, Mode arg) {
        if(arg == Mode.RESOLVE) {
            TYP.Type type;

            switch (atomType.type) {
                case INT:
                    type = TYP.IntType.type;
                    break;
                case BOOL:
                    type = TYP.BoolType.type;
                    break;
                case CHAR:
                    type = TYP.CharType.type;
                    break;
                case VOID:
                    type = TYP.VoidType.type;
                    break;
                default:
                    type = null;
                
            }
            return SemAn.isTypeAttr.put(atomType, type);
        }

        return null;
    }

    /**
     * Konstruiranje tabele
     * TYP:10 -> 
     *  [int]VALUE = n   0 < n < 2^63   [T]ISTYPE = t   t isnt void
     * ----------------------------------------------------------------------------
     *  [[int] T]ISTYPE = arr(n x t)
     */
    @Override
    public TYP.Type visit(AST.ArrType arrType, Mode arg) {
        if(arg == Mode.RESOLVE) {
            TYP.Type el = arrType.elemType.accept(this, arg);
            //System.out.println("numElems string: '" + arrType.numElems + "'");
            Long size = Long.parseLong(arrType.numElems);
            //System.out.println("Size: " + size);

            if(size <= 0) {
                throw new Report.Error(arrType.location(), "Array size must be positive");
                
            }
            if(el instanceof TYP.VoidType) {
                throw new Report.Error(arrType.location(), "Array cannot be of void type");
                
            }

            TYP.ArrType type = new TYP.ArrType(el, size);
            return SemAn.isTypeAttr.put(arrType, type);
        }

        return null;
    }

    /**
     * Konstruiranje kazalca
     * TYP:9 ->
     *  [T]ISTYPE = t   t isElement (Tau \ {void})
     * ----------------------------------------------
     *  [^T]ISTYPE = ptr(t)
     */
    @Override
    public TYP.Type visit(AST.PtrType ptrType, Mode arg) {
        if(arg == Mode.RESOLVE) {
            TYP.Type base = ptrType.baseType.accept(this, arg); 

            if(base instanceof TYP.VoidType) {
                throw new Report.Error(ptrType.location(), "Pointer cannot be of void type");
            }

            TYP.PtrType type = new TYP.PtrType(base);
            return SemAn.isTypeAttr.put(ptrType, type);
        }

        return null;
    }

    /**
     * Konstruiraj imenski tip
     * Ni sklepnega zapisa vendar pa se konstruira tako:
     *  Najprej pridobimo vozlišče kjer je definiran ta imenski tip
     *  Nato najdemo definicijo tega imenskega tipa,
     *  Razrešimo pravi tip imenskega tipa in jo pripnemo temu vozlišču
     * 
     * Vrne napako če imenski tip ni definiran
     */
    @Override
    public TYP.Type visit(AST.NameType nameType, Mode arg) {
        if(arg == Mode.RESOLVE) {
            AST.Node def = SemAn.defAtAttr.get(nameType);

            if( def instanceof AST.TypDefn) {
                TYP.Type type = SemAn.isTypeAttr.get(def);
                return SemAn.isTypeAttr.put(nameType, type);
            }

            throw new Report.Error(nameType.location(), "Unable to resolve type name:" + nameType.name);
        }

        return null;
    }


    /**
     * Konstrukcija strukture
     * TYP:11 ->
     * [[ Ti ]]ISTYPE = τi τi ̸≡ void
     * ---------------------------------------------------------------------------------------------------------------
     * [[ ( id 1 : T1 , id 2 : T2 , . . . , idc : Tc ) ]]ISTYPE = struct id1 id2 ... idc (τ1 τ2 . . . τc)
     * [[ idi ]]BINDS = idi : Ti  [[ idi ]]OFTYPE = τi
     * [[ idi ]]ISCONST = false [[ idi ]]ISADDR = true
     */
    @Override
    public TYP.Type visit(AST.StrType strType, Mode arg) {
        if(arg == Mode.RESOLVE) {
            List<TYP.Type> elemTypes = new ArrayList<>();

            for (AST.Node comp : strType.comps) {
                TYP.Type compType = comp.accept(this, arg);

                if(compType instanceof TYP.VoidType) {
                    throw new Report.Error(comp.location(), "Struct component cannot be of void type");
                }

                elemTypes.add(compType);

            }

            TYP.StrType type = new TYP.StrType(elemTypes);
            astRecordMap.put(type, strType);
            SemAn.isTypeAttr.put(strType, type);
            return type;
        }

        return null;
    }

    /**
     * Konstrukcija unije
     * TYP:12 ->
     * [[ Ti ]]ISTYPE = τi τi ̸≡ void
     * ---------------------------------------------------------------------------------------------------------------
     * [[ ( id 1 : T1 , id 2 : T2 , . . . , idc : Tc ) ]]ISTYPE = union id1 id2 ... idc (τ1 τ2 . . . τc)
     * [[ idi ]]BINDS = idi : Ti  [[ idi ]]OFTYPE = τi
     * [[ idi ]]ISCONST = false   [[ idi ]]ISADDR = true
     */
    @Override
    public TYP.Type visit(AST.UniType uniType, Mode arg) {
        if(arg == Mode.RESOLVE) {
            List<TYP.Type> elemTypes = new ArrayList<>();

            for (Node comp : uniType.comps) {
                TYP.Type compType = comp.accept(this, arg);

                if(compType instanceof TYP.VoidType) {
                    throw new Report.Error(comp.location(), "Union component cannot be of void type");
                }

                elemTypes.add(compType);

            }

            TYP.UniType type = new TYP.UniType(elemTypes);
            astRecordMap.put(type, uniType);
            SemAn.isTypeAttr.put(uniType, type);
            return type;
        }

        return null;
    }


    /**
     * Konstrukcija funkcije
     * TYP:13 ->
     * [[ Ti ]]ISTYPE = τi  [ T ]]ISTYPE = τ   τi ≡ τ′i   τ ≡ τ′
     * τ′i ∈ {int, char, bool} ∪ {ptr(τ ),fun(¯τ → τ )|τ¯ ∈ T ∗ ∧ τ ∈ T }
     * τ′ ∈ {int, char, bool, void} ∪ {ptr(τ ),fun(¯τ → τ )|τ¯ ∈ T ∗ ∧ τ ∈ T }
     * -----------------------------------------------------------------------------------------------------------
     * [[ ( : T1 , T2 , . . . , Tp : T ) ]]ISTYPE = fun(τ1τ2 . . . τp → τ )
     */
    @Override
    public TYP.Type visit(AST.FunType funType, Mode arg) {
        if(arg == Mode.RESOLVE) {
            List<TYP.Type> parameters = new ArrayList<>();
            TYP.Type returnType = funType.resType.accept(this, arg);

            for(AST.Node param : funType.parTypes) {
                TYP.Type paramType = param.accept(this, arg);

                if(paramType instanceof TYP.VoidType) {
                    throw new Report.Error(param.location(), "Function parameter cannot be of void type");
                } else if ( paramType instanceof TYP.StrType) {
                    throw new Report.Error(param.location(), "Function parameter cannot be of struct type");
                } else if ( paramType instanceof TYP.UniType) {
                    throw new Report.Error(param.location(), "Function parameter cannot be of union type");
                } else if ( paramType instanceof TYP.ArrType) {
                    throw new Report.Error(param.location(), "Function parameter cannot be of array type");
                }

                parameters.add(paramType);
            }

            if(returnType instanceof TYP.StrType) {
                throw new Report.Error(funType.resType, "Function cannot return struct type");
            } else if ( returnType instanceof TYP.UniType) {
                throw new Report.Error(funType.resType, "Function cannot return union type");
            } else if ( returnType instanceof TYP.ArrType) {
                throw new Report.Error(funType.resType, "Function cannot return array type");
            }

            TYP.FunType type = new TYP.FunType(parameters, returnType);
            SemAn.isTypeAttr.put(funType, type);
            return type;
        }

        return null;
    }

    /**
     * Kosntrukcija tipa iz deklaracije tipa
     * TYP:2 ->
     *  [[ id ]]BINDS = typ id = T [[ T ]]ISTYPE = τ
     * -------------------------------------------------------
     *  [[ typ id = T ]]OFTYPE = void
     *  [[ id ]]ISTYPE = name(id, τ )
     */
    @Override
    public TYP.Type visit(AST.TypDefn typDefn, Mode arg) {
        // V BUILD fazi nastavi placeholder
        if( arg == Mode.BUILD) {
            TYP.NameType placeholder = new TYP.NameType(typDefn.name);
            SemAn.isTypeAttr.put(typDefn, placeholder);
            return placeholder;
        } else if ( arg == Mode.RESOLVE) { // V RESOLVE razreši pravi tip in ga seta z sezActualType()
            TYP.Type aType = typDefn.type.accept(this, arg);

            TYP.NameType placeholder = (TYP.NameType) SemAn.isTypeAttr.get(typDefn);
            placeholder.setActType(aType);
            //SemAn.ofTypeAttr.put(typDefn, TYP.VoidType.type);
            return null;
        } 

        return null;
    }

    /**
     * Konstruira/nastavi tip vozlišča
     * 
     */
    @Override
    public TYP.Type visit(AST.CompDefn compDefn, Mode arg) {
        if (arg == Mode.RESOLVE) {
            TYP.Type aType = compDefn.type.accept(this, arg);
            SemAn.ofTypeAttr.put(compDefn, aType);
            return aType;
        }

        return null;
    }

    /**
     * KOnstrukcija tipa iz deklaracije spremenljivke
     * TYP:3 ->
     * [[ id ]]BINDS = var id : T [[ T ]]ISTYPE = τ τ ̸≡ void
     * ----------------------------------------------------------------------------------
     *  [[ var id : T ]]OFTYPE = void
     *  [[ id ]]OFTYPE = τ [[ id ]]ISCONST = false [[ id ]]ISADDR = true
     */
    @Override
    public TYP.Type visit(AST.VarDefn varDefn, Mode arg) {
        if (arg == Mode.RESOLVE) {
            TYP.Type aType = varDefn.type.accept(this,arg);
            
            if(aType instanceof TYP.VoidType) {
                throw new Report.Error(varDefn.location(), "Variable cannot be of void type");
            }
            //System.out.println("Setting type for var " + varDefn.name + " -> " + aType);
            SemAn.ofTypeAttr.put(varDefn, aType);
            return aType;
        }

        return null;
    }

    /**
     * Konstrukcija iz deklaracije parametrov
     * Ne smejo biti Tipa void, struct, union, array
     */
    @Override
    public TYP.Type visit(AST.ParDefn parDefn, Mode arg) {
        if (arg == Mode.RESOLVE) {
            TYP.Type type = parDefn.type.accept(this, arg);
            if(type instanceof TYP.VoidType) {
                throw new Report.Error(parDefn.location(), "Parameters cannot be of void type");
            } else if ( type instanceof TYP.StrType) {
                throw new Report.Error(parDefn.location(), "Parameters cannot be of struct type");
            } else if ( type instanceof TYP.UniType) {
                throw new Report.Error(parDefn.location(), "Parameters cannot be of union type");
            } else if ( type instanceof TYP.ArrType) {
                throw new Report.Error(parDefn.location(), "Parameters cannot be of array type");
            }

            SemAn.ofTypeAttr.put(parDefn, type);
            return type;
        }

        return null;
    }

    /**
     * Konstrukcija iz deklaracije funkcije
     * TYP:1 in TYP:4 ->
     * [[ Di ]]OFTYPE = void
     * ∃m: Dm = fun main ( ) : int = E1 , E2 , . . . , Ee
     * -------------------------------------------------------------
     * [[ D1 D2 . . . Dd ]]OFTYPE = void
     * 
     * 
     * [[ id ]]BINDS = fun id ( id 1 : T1, id 2 : T2, . . . , id p : Tp) : T [= E1 , E2 , . . . , Ee]?
     * [[ Ti ]]ISTYPE = τi   τi ≡ τ′i    [[ T ]]ISTYPE = τ τ ≡ τ′
     * τ′i ∈ {int, char, bool} ∪ {ptr(τ ),fun(¯τ → τ )|τ¯ ∈ T ∗ ∧ τ ∈ T }
     * τ′ ∈ {int, char, bool, void} ∪ {ptr(τ ),fun(¯τ → τ )|τ¯ ∈ T ∗ ∧ τ ∈ T }
     * [[ Ei]]OFTYPE ∈ T   [[ Ee ]]OFTYPE = τ′′    τ′ ≡ τ′′
     * -------------------------------------------------------------------------------------------------------
     * [[ fun id ( id 1 : T1, id 2 : T2, . . . , id p : Tp) : T [= E1 , E2 , . . . , Ee]? ]]OFTYPE = void
     * [[ id ]]OFTYPE = fun(τ1 τ2 . . . τn → τ )    [[ id ]]ISCONST = false    [[ id ]]ISADDR = false
     * [[ idi ]]BINDS = idi : Ti   [[ idi ]]OFTYPE = τi
     * [[ idi ]]ISCONST = false [[ idi ]]ISADDR = true
     */
    @Override
    public TYP.Type visit(AST.DefFunDefn funDefn, Mode arg) {
        if (arg == Mode.BUILD) {
            //System.out.println("BUILD: " + funDefn.name);
            if (funDefn.name.equals("main")) {
                if (hasMain) {
                    throw new Report.Error(funDefn, "Multiple definitions of main function");
                }
                hasMain = true;
                //System.out.println("BUILD: " + funDefn.name);
            }

            for (AST.Node par : funDefn.pars) {
                par.accept(this, arg);
            }

            funDefn.type.accept(this, arg);

            if (funDefn.expr != null) {
                funDefn.expr.accept(this, arg);
            }

        } else if (arg == Mode.RESOLVE) {
            List<TYP.Type> paramTypes = new ArrayList<>();
            for (AST.Node param : funDefn.pars) {
                TYP.Type paramType = param.accept(this, arg);
                paramTypes.add(paramType);
            }

            TYP.Type resType = funDefn.type.accept(this, arg);
            TYP.FunType type = new TYP.FunType(paramTypes, resType);
            SemAn.ofTypeAttr.put(funDefn, type);

            if (funDefn.name.equals("main")) {
                if (!paramTypes.isEmpty()) {
                    throw new Report.Error(funDefn.location(), "Main function cannot have parameters");
                }
                if (!resType.actualType().equals(TYP.IntType.type)) {
                    throw new Report.Error(funDefn.location(), "Main function must return int");
                }
            }

            
            if (funDefn.expr != null) funDefn.expr.accept(this, arg);
        }
        return null;
    }

    /**
     * Konstrukcija zunanje funkcije
     * Gremo čez parametre in tip rezultata nato pa shranimo tip vozlišča
     */
    @Override
    public TYP.Type visit(AST.ExtFunDefn extFunDefn, Mode arg) {
        if (arg == Mode.RESOLVE) {
            List<TYP.Type> paramTypes = new ArrayList<>();
            for (AST.Node param : extFunDefn.pars) {
                TYP.Type paramType = param.accept(this, arg);
                paramTypes.add(paramType);
            }

            TYP.Type resType = extFunDefn.type.accept(this, arg);
            TYP.FunType type = new TYP.FunType(paramTypes, resType);
            SemAn.ofTypeAttr.put(extFunDefn, type);
            return type;
        }

        return null;
    }
    
}