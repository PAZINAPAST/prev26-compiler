package prev26lang.phase.seman;

import java.util.*;
import prev26lang.common.report.*;
import prev26lang.phase.abstr.*;
import prev26lang.phase.abstr.AST.*;
import prev26lang.phase.seman.SemAn;
import prev26lang.phase.seman.TYP.*;
import prev26lang.phase.seman.TypeConstructor;


/**
 * Type checker
 * 
 * Za razliko od Type Constructorja , Type checker pokriva predvsem izraze
 * (AtomExpr, BinExpr, PfxExpr, SfxExpr, ArrExpr, ...) in kontrolne kosntrukte
 *  (IfThenExpr, WhileExpr, LetExpr).
 *  Vsako vozlišče obdela v istem vzorcu:
 *  Najprej rekurzivno preveri podizraze,
 *  nato preveri semantično pravilo zase,
 *  na koncu nastavi svoje tri atribute in vrne svoj tip.
 */

public class TypeChecker implements AST.FullVisitor<TYP.Type, TypeChecker.Mode> {

    public TypeChecker() {
    }

    enum Mode {
        RESOLVE,
    }
    

    /**
     * Funckija za preverjanje strukturne ekvivalence dveh tipov
     */
    private static boolean equivalent(TYP.Type a, TYP.Type b) {
        a = a.actualType();
        b = b.actualType();
        if(a == b) {
            return true;
        }
        
        if(a.getClass() != b.getClass()) {
            return false;
        }

        if(a instanceof TYP.IntType) return true;
        if(a instanceof TYP.BoolType) return true;
        if(a instanceof TYP.CharType) return true;
        if(a instanceof TYP.VoidType) return true;


        if(a instanceof TYP.ArrType) {
            TYP.ArrType arrA = (TYP.ArrType) a;
            TYP.ArrType arrB = (TYP.ArrType) b;

            return equivalent(arrA.elemType, arrB.elemType) && arrA.numElems.equals(arrB.numElems);
        }

        if(a instanceof TYP.StrType) {
            TYP.StrType strA = (TYP.StrType) a;
            TYP.StrType strB = (TYP.StrType) b;

            if(strA.compTypes.size() != strB.compTypes.size()) {
                return false;
            }

            for(int i = 0; i < strA.compTypes.size(); i++) {
                if(!equivalent(strA.compTypes.get(i), strB.compTypes.get(i))) {
                    return false;
                }
            }

            return true;
        }

        if(a instanceof TYP.PtrType) {
            TYP.PtrType ptrA = (TYP.PtrType) a;
            TYP.PtrType ptrB = (TYP.PtrType) b;

            return equivalent(ptrA.baseType, ptrB.baseType);
        }

        if(a instanceof TYP.UniType) {
            TYP.UniType uniA = (TYP.UniType) a;
            TYP.UniType uniB = (TYP.UniType) b;

            if(uniA.compTypes.size() != uniB.compTypes.size()) {
                return false;
            }

            for(int i = 0; i < uniA.compTypes.size(); i++) {
                if(!equivalent(uniA.compTypes.get(i), uniB.compTypes.get(i))) {
                    return false;
                }
            }

            return true;
        }

        if(a instanceof TYP.FunType) {
            TYP.FunType funA = (TYP.FunType) a;
            TYP.FunType funB = (TYP.FunType) b;

            if(funA.parTypes.size() != funB.parTypes.size()) {
                return false;
            }

            for(int i = 0; i < funA.parTypes.size(); i++) {
                if(!equivalent(funA.parTypes.get(i), funB.parTypes.get(i))) {
                    return false;
                }
            }

            return equivalent(funA.resType, funB.resType);
        }

        return false;
    }

    /**
     * Atomski izrazi
     * TYP:14 ->
     * [[ int ]]OFTYPE = int
     * [[ int ]]ISCONST = true
     * [[ int ]]ISADDR = false
     * 
     * TYP:15 ->
     *  [[ char ]]OFTYPE = char
     * [[ char ]]ISCONST = true
     * [[ char ]]ISADDR = false
     * 
     * TYP:16->
     * [[ true ]]OFTYPE = bool
     * [[ true ]]ISCONST = true
     * [[ true ]]ISADDR = false
     * 
     * TYP:17 -> 
     * [[ false ]]OFTYPE = bool
     * [[ false ]]ISCONST = true
     * [[ false ]]ISADDR = false
     * 
     * TYP:18 ->
     * [[ string ]]OFTYPE = ptr(char)
     * [[ string ]]ISCONST = true
     * [[ string ]]ISADDR = false
     * 
     * TYP:19 ->
     * [[ nil ]]OFTYPE = ptr(void)
     * [[ nil ]]ISCONST = true
     * [[ nil ]]ISADDR = false
     * 
     * TYP:20 -> 
     * [[ none ]]OFTYPE = void
     * [[ none ]]ISCONST = true
     * [[ none ]]ISADDR = false
     * 
     */
    @Override
    public TYP.Type visit(AST.AtomExpr atomExpr, Mode arg) {
        TYP.Type t;

        switch(atomExpr.type) {
            case INT:
                t = TYP.IntType.type;
                break;
            case BOOL:
                t = TYP.BoolType.type;
                break;
            case CHAR:
                t = TYP.CharType.type;
                break;
            case VOID:
                t = TYP.VoidType.type;
                break;
            case PTR:
                t = TYP.PtrType.type;
                break;
            case STR:
                t = TYP.PtrType.type;
                break;
            default:
                t = null;
                break;
            
        }

        SemAn.ofTypeAttr.put(atomExpr, t);
        SemAn.isAddrAttr.put(atomExpr, false);
        SemAn.isConstAttr.put(atomExpr, true);

        return t;
    }

    /**
     * Primer:
     *      var i: int
     *      var j : int
     * 
     *      i = 1,
     *      j = i + i
     * 
     * Pri assing in pri računu je i NameExpr
     * 
     * Pravila:
     *  Če ime nima definicije je nedifinirano ime.
     *  Če je definicija deklaracije tipa ne more biti v izrazu.
     *  Nato poiskusimo dobiti kakšnega tipa je vozlišče.
     *  Če nima tipa pomeni da konstruktor ni skonstruiral tipa.
     *  
     *  NameExpr je naslovljiv le če deklariran kot spremenljivka ali kot parameter.
     */
    @Override
    public TYP.Type visit(AST.NameExpr nameExpr, Mode arg) {
        AST.Defn def = SemAn.defAtAttr.get(nameExpr);
        if (def == null) {
            throw new Report.Error(nameExpr.location(), "Undefined name: " + nameExpr.name);
        }
        if (def instanceof AST.TypDefn) {
            throw new Report.Error(nameExpr.location(), "Name '" + nameExpr.name + "' is a type, not a variable or function.");
        }
   
        TYP.Type type = SemAn.ofTypeAttr.get(def);
        if (type == null) {
            throw new Report.Error("No type attribute for definition: " + def.getClass().getSimpleName());
        }

        boolean addr = (def instanceof AST.VarDefn) || (def instanceof AST.ParDefn);

        SemAn.ofTypeAttr.put(nameExpr, type);
        SemAn.isAddrAttr.put(nameExpr, addr);
        SemAn.isConstAttr.put(nameExpr, false);
        return type;
    }

    /**
     * Preglde in zapis atributov za prefix izraze
     * 
     * TYP:21 ->
     * [[ E ]]OFTYPE = τ   τ ≡ int   op ∈ {+, -}
     * ------------------------------------------------------------------------------------------
     * [[ op E ]]OFTYPE = int   [[ op E ]]ISCONST = [[ E ]]ISCONST    [[ op E ]]ISADDR = false
     * 
     * TYP:22 ->
     * [[ E ]]OFTYPE = τ   τ ≡ bool    op ∈ {not}
     * ------------------------------------------------------------------------------------------------
     * [[ op E ]]OFTYPE = bool   [[ op E ]]ISCONST = [[ E ]]ISCONST    [[ op E ]]ISADDR = false
     * 
     * TYP:28 ->
     * [[ E ]]OFTYPE = τ    [[ E ]]ISADDR = true     τ ̸≡ void
     * -------------------------------------------------------------------------------------------------------
     * [[ ^ E ]]OFTYPE = ptr(τ )    [[ ^ E ]]ISCONST = false      [[ ^ E ]]ISADDR = false
     * (Kot &p v c-ju)
     */
    @Override
    public TYP.Type visit(AST.PfxExpr pfExpr, Mode arg) {
        AST.PfxExpr.Oper op = pfExpr.oper; //pridobim operatorja
        TYP.Type result;
        TYP.Type sub = pfExpr.subExpr.accept(this, arg); //podizraz
        boolean subIsConst = SemAn.isConstAttr.get(pfExpr.subExpr); //Ali je podizraz konstanta
        boolean resultConst;
        boolean resultAddr = false;


        switch(op) {
            case ADD:
                if(sub.actualType() != TYP.IntType.type) {
                    throw new Report.Error(pfExpr.location(), "Unary + operator can only be applied to integers.");
                }
                result = TYP.IntType.type;
                resultConst = subIsConst;
                break;
            case SUB:
                if(sub.actualType() != TYP.IntType.type) {
                    throw new Report.Error(pfExpr.location(), "Unary - operator can only be applied to integers.");
                }
                result = TYP.IntType.type;
                resultConst = subIsConst;
                break;
            case NOT:
                if(sub.actualType() != TYP.BoolType.type) {
                    throw new Report.Error(pfExpr.location(), "Unary ! operator can only be applied to booleans.");
                }
                result = TYP.BoolType.type;
                resultConst = subIsConst;
                break;
            case PTR:
                if(!SemAn.isAddrAttr.get(pfExpr.subExpr)) {
                    throw new Report.Error(pfExpr.location(), "Cannot take pointer of non-addressable expression.");
                }
                if(sub.actualType() instanceof TYP.VoidType) {
                    throw new Report.Error(pfExpr.location(), "Cannot take pointer of void type.");
                }
                result = new TYP.PtrType(sub);
                resultConst = false;
                break;
            default:
                throw new Report.Error(pfExpr.location(), "Unknown prefix operator.");

        }
        

        SemAn.ofTypeAttr.put(pfExpr, result);
        SemAn.isAddrAttr.put(pfExpr, resultAddr);
        SemAn.isConstAttr.put(pfExpr, resultConst);

        return result;
    }

    /**
     * Sufix izrazi
     * (dereferenciranje pointerja) kot *p v C-ju
     * 
     * TYP:27 ->
     *  [[ E ]]OFTYPE ≡ ptr(τ )   [[ E ]]ISCONST = false      τ ̸≡ void
     * --------------------------------------------------------------------------------------------
     * [[ E ^ ]]OFTYPE = τ    [[ E ^ ]]ISCONST = false     [[ E ^ ]]ISADDR = true
     */
    @Override
    public TYP.Type visit(AST.SfxExpr sfExpr, Mode arg) {
        TYP.Type sub = sfExpr.subExpr.accept(this, arg);
        TYP.Type actualSub = sub.actualType();


        if(!(actualSub instanceof TYP.PtrType)) {
            throw new Report.Error(sfExpr.location(), "Cannot dereference non-pointer type.");
        }

        TYP.PtrType ptrSub = (TYP.PtrType) actualSub;
        TYP.Type result = ptrSub.baseType;

        if(result.actualType() instanceof TYP.VoidType) {
            throw new Report.Error(sfExpr.location(), "Cannot dereference void type.");
        }
        

        SemAn.ofTypeAttr.put(sfExpr, result);
        SemAn.isAddrAttr.put(sfExpr, true);
        SemAn.isConstAttr.put(sfExpr, false);

        return result;

    }

    /**
     * Binarni izrazi
     * 
     * TYP:23 ->
     * [ E1 ]]OFTYPE = τ1    [[ E2 ]]OFTYPE = τ2     op ∈ {and, or}
     * τ1 ≡ bool    τ2 ≡ bool
     * ----------------------------------------------------------------------------------------------------------------------------------
     * [[ E1 op E2 ]]OFTYPE = bool
     * [[ E1 op E2 ]]ISCONST = [[ E1 ]]ISCONST ∧ [[ E2 ]]ISCONST     [[ E1 op E2 ]]ISADDR = false
     * 
     * TYP:24 ->
     *  [ E1 ]]OFTYPE = τ1   [[ E2 ]]OFTYPE = τ2    op ∈ {*, /, %, +, -}
     *  τ1 ≡ int   τ2 ≡ int
     * -------------------------------------------------------------------------------------------------------------
     * [[ E1 op E2 ]]OFTYPE = int
     * [[ E1 op E2 ]]ISCONST = [[ E1 ]]ISCONST ∧ [[ E2 ]]ISCONST       [[ E1 op E2 ]]ISADDR = false
     * 
     * TYP:25 ->
     *  [[ E1 ]]OFTYPE = τ1 [[ E2 ]]OFTYPE = τ2 op ∈ {==, !=, <, >, <=, >=}
     *  τ1 ≡ τ′1   τ2 ≡ τ′2   τ1 ≡ τ2
     *  τ′1, τ′2 ∈ {int, char, bool} ∪ {ptr(τ ),fun(¯τ → τ )|τ¯ ∈ T ∗ ∧ τ ∈ T }
     * ------------------------------------------------------------------------------------------------------------------
     * [[ E1 op E2 ]]OFTYPE = bool
     * [[ E1 op E2 ]]ISCONST = [[ E1 ]]ISCONST ∧ [[ E2 ]]ISCONST     [[ E1 op E2 ]]ISADDR = false

     * 
     */
    @Override
    public TYP.Type visit(AST.BinExpr binExpr, Mode arg) {
        TYP.Type fst = binExpr.fstExpr.accept(this, arg);
        TYP.Type snd = binExpr.sndExpr.accept(this, arg);


        
        switch(binExpr.oper) {
            case AND:
            case OR:
                if((fst.actualType() != TYP.BoolType.type) || (snd.actualType() != TYP.BoolType.type)) {
                    throw new Report.Error(binExpr.location(), "Logical operators can only be applied to booleans.");
                }
                SemAn.ofTypeAttr.put(binExpr, TYP.BoolType.type);                    
                SemAn.isAddrAttr.put(binExpr, false);
                //SemAn.isConstAttr.put(binExpr, SemAn.isConstAttr.get(binExpr.fstExpr) && SemAn.isConstAttr.get(binExpr.sndExpr));
                break;

            case MUL:
            case DIV:
            case MOD:
            case ADD:
            case SUB:
                if((fst.actualType() != TYP.IntType.type) || (snd.actualType() != TYP.IntType.type)) {
                    System.out.println(fst.actualType().toString());
                    System.out.println(snd.actualType().toString());
                    throw new Report.Error(binExpr.location(), "Arithmetic operators can only be applied to integers.");
                }
                SemAn.ofTypeAttr.put(binExpr, TYP.IntType.type);
                //SemAn.isAddrAttr.put(binExpr, false);
                //SemAn.isConstAttr.put(binExpr, SemAn.isConstAttr.get(binExpr.fstExpr) && SemAn.isConstAttr.get(binExpr.sndExpr));
                break;
            case EQU:
            case NEQ:
            case LTH:
            case LEQ:
            case GTH:
            case GEQ:
                if(!equivalent(fst, snd)) {
                    throw new Report.Error(binExpr.location(), "Comparison operators can only be applied to compatible types.");
                }
                TYP.Type actualFst = fst.actualType();
                if(!(actualFst instanceof TYP.IntType || actualFst instanceof TYP.BoolType || actualFst instanceof TYP.CharType || actualFst instanceof TYP.PtrType)) {
                    throw new Report.Error(binExpr.location(), "Comparison operators can only be applied to integers, booleans, characters, or pointers.");
                }
                SemAn.ofTypeAttr.put(binExpr, TYP.BoolType.type);
                //SemAn.isAddrAttr.put(binExpr, false);
                //SemAn.isConstAttr.put(binExpr, SemAn.isConstAttr.get(binExpr.fstExpr) && SemAn.isConstAttr.get(binExpr.sndExpr));
                break;
            default:
                throw new Report.Error(binExpr.location(), "Unknown binary operator.");
        }
        SemAn.isAddrAttr.put(binExpr, false);
        SemAn.isConstAttr.put(binExpr, SemAn.isConstAttr.get(binExpr.fstExpr) && SemAn.isConstAttr.get(binExpr.sndExpr));
    
        return SemAn.ofTypeAttr.get(binExpr);
    }  


    /**
     * ARRAY izraz (Pridobivanje ali zapisovanje elementa v tabelo torej arr[i])
     * 
     * TYP:26 ->
     *  [[ E1 ]]OFTYPE ≡ arr(n × τ1)   [[ E2 ]]OFTYPE = τ2   τ2 ≡ int   [[ E1 ]]ISADDR = true
     * ------------------------------------------------------------------------------------------------------------------
     * [[ E1 [ E2 ] ]]OFTYPE = τ1    [[ E1 [ E2 ] ]]ISCONST = false    [[ E1 [ E2 ] ]]ISADDR = true
     * 
     */
    @Override
    public TYP.Type visit(AST.ArrExpr arrExpr, Mode arg) {
        TYP.Type arrType = arrExpr.arrExpr.accept(this, arg);
        TYP.Type idxType = arrExpr.idx.accept(this, arg);
        TYP.Type actualArr = arrType.actualType();
        TYP.Type actualIdx = idxType.actualType();

        if (!(actualArr instanceof TYP.ArrType)) {
            throw new Report.Error(arrExpr.location(), "Array indexing requires an array expression.");
        }
        if (!(actualIdx instanceof TYP.IntType)) {
            throw new Report.Error(arrExpr.location(), "Array index must be of type int.");
        }
        TYP.ArrType arr = (TYP.ArrType) actualArr;
        TYP.Type elemType = arr.elemType;

        boolean isConst = SemAn.isConstAttr.get(arrExpr.arrExpr) && SemAn.isConstAttr.get(arrExpr.idx);

        boolean isAddr = SemAn.isAddrAttr.get(arrExpr.arrExpr);

        SemAn.ofTypeAttr.put(arrExpr, elemType);
        SemAn.isConstAttr.put(arrExpr, isConst);
        SemAn.isAddrAttr.put(arrExpr, isAddr);

        return elemType;
    }


    /**
     * Izraz za pridobitev komponente (a.id)
     * 
     * TYP:29 -> 
     * [[ E ]]OFTYPE ≡ structid1id2...idc(τ1τ2 . . . τc)     [[ E ]]ISADDR = true    id = idi
     * ----------------------------------------------------------------------------------------------------
     * [[ E . id ]]OFTYPE = τi   [[ E . id ]]ISCONST = false   [[ E . id ]]ISADDR = true
     * 
     * TYP:30 ->
     * [[ E ]]OFTYPE ≡ unionid1id2...idc(τ1τ2 . . . τc)    [[ E ]]ISADDR = true    id = idi
     * ----------------------------------------------------------------------------------------------------
     * [[ E . id ]]OFTYPE = τi   [[ E . id ]]ISCONST = false   [[ E . id ]]ISADDR = true
     * 
     */    
    @Override
    public TYP.Type visit(AST.CompExpr compExpr, Mode arg) {

        TYP.Type recType = compExpr.recExpr.accept(this, arg);
        TYP.Type actual = recType.actualType();

        if (!(actual instanceof TYP.RecType)){

            throw new Report.Error(compExpr.location(), "Component access requires a struct or union.");
        }

        TYP.RecType rec = (TYP.RecType) actual;

        AST.RecType astRec = TypeConstructor.astRecordMap.get(rec);
        if (astRec == null) {
            throw new Report.InternalError();
        }

        AST.CompDefn foundComp = null;
        for (AST.Node comp : astRec.comps) {
            AST.CompDefn compDefn = (AST.CompDefn) comp;
            if (compDefn.name.equals(compExpr.name)) {
                foundComp = compDefn;
                break;
            }
        }

        if (foundComp == null) {
            throw new Report.Error(compExpr.location(), "Component '" + compExpr.name + "' does not exist.");
        }
       
        TYP.Type compType = SemAn.ofTypeAttr.get(foundComp);

        if (compType == null) {
            throw new Report.InternalError();
        }

        boolean isAddr = SemAn.isAddrAttr.get(compExpr.recExpr);
        
        boolean isConst = SemAn.isConstAttr.get(compExpr.recExpr);

        SemAn.ofTypeAttr.put(compExpr, compType);
        SemAn.isAddrAttr.put(compExpr, isAddr);
        SemAn.isConstAttr.put(compExpr, isConst);
        return compType;
    }
    
    /**
     * Klic funkcije
     * TYP:31 ->
     *  [[ E ]]OFTYPE = fun(τ1τ2 . . . τa → τ )   [[ Ei]]OFTYPE = τ′i   τ′i ≡ τi
     * ----------------------------------------------------------------------------------------
     * [[ E ( E1 , E2 , . . . , Ea ) ]]OFTYPE = τ
     * [[ E ( E1 , E2 , . . . , Ea ) ]]ISCONST = false
     * [[ E ( E1 , E2 , . . . , Ea ) ]]ISADDR = false
     * 
     */
    @Override
    public TYP.Type visit(AST.CallExpr callExpr, Mode arg) {
        TYP.Type funType = callExpr.funExpr.accept(this, arg);
        TYP.Type actualFun = funType.actualType();

        if (!(actualFun instanceof TYP.FunType)) {
            throw new Report.Error(callExpr.location(), "Call expression must be a function.");
        }
        TYP.FunType ft = (TYP.FunType) actualFun;

        if (ft.parTypes.size() != callExpr.argExprs.size()) {
            throw new Report.Error(callExpr.location(), "Wrong number of arguments.");
        }

        for (int i = 0; i < callExpr.argExprs.size(); i++) {
            TYP.Type argType = callExpr.argExprs.get(i).accept(this, arg);
            if (!equivalent(argType, ft.parTypes.get(i))) {
                throw new Report.Error(callExpr.argExprs.get(i).location(), "Argument type mismatch.");
            }
        }

        // Result attributes
        SemAn.ofTypeAttr.put(callExpr, ft.resType);
        SemAn.isAddrAttr.put(callExpr, false);   // function result is an rvalue
        SemAn.isConstAttr.put(callExpr, false);  // function call is never a compile-time constant

        return ft.resType;
    }

    /**
     * Zavijanje tipov / Catsing izraz
     * 
     * TYP:33 ->
     * [[ E ]]OFTYPE ̸≡ void [[ T ]]ISTYPE ̸≡ void
     * ------------------------------------------------------------------------
     * [[ E as T ]]OFTYPE = [[ T ]]ISTYPE
     * [[ E as T ]]ISCONST = [[ E ]]ISCONST
     * [[ E as T ]]ISADDR = [[ E ]]ISADDR

     */
    @Override
    public TYP.Type visit(AST.CastExpr castExpr, Mode arg) {
        TYP.Type exprType = castExpr.expr.accept(this, arg);
        TYP.Type targetType = SemAn.isTypeAttr.get(castExpr.type);

        if (targetType == null) {
            throw new Report.Error(castExpr.location(), "Cannot resolve target type.");
        }

        TYP.Type actualTarget = targetType.actualType();
        TYP.Type actualExpr = exprType.actualType();

        if (actualTarget instanceof TYP.VoidType && !(actualExpr instanceof TYP.PtrType)) {
            throw new Report.Error(castExpr.location(), "Cannot cast to void.");
        }

        boolean isConst = SemAn.isConstAttr.get(castExpr.expr);

        SemAn.ofTypeAttr.put(castExpr, targetType);
        SemAn.isAddrAttr.put(castExpr, false);
        SemAn.isConstAttr.put(castExpr, isConst);
        return targetType;
    }

    /**
     * Izraz za določanje velikosti
     * 
     * TYP:32->
     *  [[ T ]]ISTYPE ̸≡ void
     * ---------------------------------------------
     * [[ sizeof T ]]OFTYPE = int
     * [[ sizeof T ]]ISCONST = true
     * [[ sizeof T ]]ISADDR = false
     */
    @Override
    public TYP.Type visit(AST.SizeExpr sizeExpr, Mode arg) {

        TYP.Type t = SemAn.isTypeAttr.get(sizeExpr.type);
        
        if (t == null) {
            throw new Report.Error(sizeExpr.location(), "Cannot resolve type for sizeof.");
        }

        TYP.Type actual = t.actualType();

        SemAn.ofTypeAttr.put(sizeExpr, TYP.IntType.type);
        SemAn.isAddrAttr.put(sizeExpr, false);
        SemAn.isConstAttr.put(sizeExpr, true);
        return TYP.IntType.type;
    }

    /**
     * Izrazi
     * TYP:34 ->
     *  [[ Ei ]OFTYPE ∈ T
     * ---------------------------------------------------------------------------------------------------
     * [[ ( E1 , E2 , . . . , Ee ) ]]OFTYPE = [[ Ee ]]OFTYPE
     * [[ ( E1 , E2 , . . . , Ee ) ]]ISCONST = [[ E1 ]]ISCONST ∧ [[ E2 ]]ISCONST ∧ . . . ∧ [[ Ee ]]ISCONST
     * [[ ( E1 , E2 , . . . , Ee ) ]]ISADDR = [[ Ee ]]ISADDR
     */
    @Override
    public TYP.Type visit(AST.Exprs exprs, Mode arg) {

        if (exprs.exprs.size() == 0) {
            throw new Report.Error(exprs.location(), "Empty expression sequence is not allowed.");
        }

        TYP.Type lastType = null;
        boolean allConst = true;

        for (AST.Expr e : exprs.exprs) {
            lastType = e.accept(this, arg);
            allConst = allConst && SemAn.isConstAttr.get(e);
        }


        SemAn.ofTypeAttr.put(exprs, lastType);
        SemAn.isAddrAttr.put(exprs, false);
        SemAn.isConstAttr.put(exprs, allConst);
        return lastType;
    }

    /**
     * IFTHEN izraz
     * 
     * TYP:37->
     * [[ E ]]OFTYPE ≡ bool    [[ Ei ]]OFTYPE ∈ T
     * --------------------------------------------------------------------------------------
     * [[ if E then E1 , E2 , . . . , Ee end ]]OFTYPE = void
     * [[ if E then E1 , E2 , . . . , Ee end ]]ISCONST = false
     * [[ if E then E1 , E2 , . . . , Ee end ]]ISADDR = false

     */
    @Override
    public TYP.Type visit(AST.IfThenExpr ifThenExpr, Mode arg) {
        
        TYP.Type condType = ifThenExpr.condExpr.accept(this, arg);

        if (!(condType.actualType() instanceof TYP.BoolType)) {
            throw new Report.Error(ifThenExpr.location(), "Condition must be of type bool.");
        }

        TYP.Type thenType = ifThenExpr.thenExpr.accept(this, arg);
        boolean constCond = SemAn.isConstAttr.get(ifThenExpr.condExpr);
        boolean constThen = SemAn.isConstAttr.get(ifThenExpr.thenExpr);

        SemAn.ofTypeAttr.put(ifThenExpr, thenType);
        SemAn.isAddrAttr.put(ifThenExpr, false);
        SemAn.isConstAttr.put(ifThenExpr, constCond && constThen);
        return thenType;
    }

    /**
     * IF THEN ELSE izraz
     * TYP:38 ->
     *  [[ E ]]OFTYPE ≡ bool    [[ Ei ]]OFTYPE ∈ T    [[ E′i ]]OFTYPE ∈ T
     * ---------------------------------------------------------------------------------
     *  [[ if E then E1 , E2 , . . . , Ee else E′1 , E′2 , . . . , E′e′ end ]]OFTYPE = void
     *  [[ if E then E1 , E2 , . . . , Ee else E′1 , E′2 , . . . , E′e′ end ]]ISCONST = false
     *  [[ if E then E1 , E2 , . . . , Ee else E′1 , E′2 , . . . , E′e′ end ]]ISADDR = false
     */
    @Override
    public TYP.Type visit(AST.IfThenElseExpr ifThenElseExpr, Mode arg) {

        TYP.Type condType = ifThenElseExpr.condExpr.accept(this, arg);

        if (!(condType.actualType() instanceof TYP.BoolType)) {
            throw new Report.Error(ifThenElseExpr.location(), "Condition must be of type bool.");
        }

        TYP.Type thenType = ifThenElseExpr.thenExpr.accept(this, arg);
        TYP.Type elseType = ifThenElseExpr.elseExpr.accept(this, arg);

        if (!equivalent(thenType, elseType)) {
            throw new Report.Error(ifThenElseExpr.location(), "Then and else branches must have the same type.");
        }

        boolean constCond = SemAn.isConstAttr.get(ifThenElseExpr.condExpr);
        boolean constThen = SemAn.isConstAttr.get(ifThenElseExpr.thenExpr);
        boolean constElse = SemAn.isConstAttr.get(ifThenElseExpr.elseExpr);

        SemAn.ofTypeAttr.put(ifThenElseExpr, thenType);
        SemAn.isAddrAttr.put(ifThenElseExpr, false);
        SemAn.isConstAttr.put(ifThenElseExpr, constCond && constThen && constElse);
        return thenType;
    }

    /**
     * While izraz
     * 
     * TYP:36 ->
     * [[ E ]]OFTYPE ≡ bool   [[ Ei ]]OFTYPE ∈ T
     * -------------------------------------------------------------
     * [[ while E do E1 , E2 , . . . , Ee end ]]OFTYPE = void
     * [[ while E do E1 , E2 , . . . , Ee end ]]ISCONST = false
     * [[ while E do E1 , E2 , . . . , Ee end ]]ISADDR = false
     */
    @Override
    public TYP.Type visit(AST.WhileExpr whileExpr, Mode arg) {
        
        TYP.Type condType = whileExpr.condExpr.accept(this, arg);

        if (!(condType.actualType() instanceof TYP.BoolType)) {
            throw new Report.Error(whileExpr.location(), "Condition must be of type bool.");
        }

        whileExpr.expr.accept(this, arg);

        SemAn.ofTypeAttr.put(whileExpr, TYP.VoidType.type);
        SemAn.isAddrAttr.put(whileExpr, false);
        SemAn.isConstAttr.put(whileExpr, false);
        return TYP.VoidType.type;
    }

    /**
     * Let izraz
     * TYP:39 ->
     *  [[ Di ]]OFTYPE = void [[ Ei ]]OFTYPE ∈ T [[ Ee ]]OFTYPE = τ
     * -----------------------------------------------------------------------------------
     * [[ let D1 D2 . . . Dd in E1 , E2 , . . . , Ee end ]]OFTYPE = τ
     * [[ let D1 D2 . . . Dd in E1 , E2 , . . . , Ee end ]]ISCONST = false
     * [[ let D1 D2 . . . Dd in E1 , E2 , . . . , Ee end ]]ISADDR = false
     */
    @Override
    public TYP.Type visit(AST.LetExpr letExpr, Mode arg) {

        for(AST.Defn d : letExpr.defns) {
            d.accept(this, arg);
        }
        
        TYP.Type bodyType = letExpr.expr.accept(this, arg);
        boolean constBody = SemAn.isConstAttr.get(letExpr.expr);

        SemAn.ofTypeAttr.put(letExpr, bodyType);
        SemAn.isAddrAttr.put(letExpr, false);
        SemAn.isConstAttr.put(letExpr, constBody);
        return bodyType;
    }

    /**
     * Deklaracija definicije
     * TYP:4 ->
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
    public TYP.Type visit(AST.DefFunDefn defFunDefn, Mode arg) {

        TYP.FunType funType = (TYP.FunType) SemAn.ofTypeAttr.get(defFunDefn);
        if (funType == null) {
            throw new Report.InternalError(); 
        }
        
        defFunDefn.expr.accept(this, arg);

        Exprs body = (Exprs) defFunDefn.expr;
        int size = body.exprs.size();
        /*
        for (int i = 0; i < size - 1; i++) {
            AST.Expr e = body.exprs.get(i);
            TYP.Type stmtType = SemAn.ofTypeAttr.get(e);

            if (!(stmtType.actualType() instanceof TYP.VoidType)) {
                throw new Report.Error(e.location(), "Non-last expression in function body must be of type void.");
            }
        }*/
        AST.Expr last = body.exprs.get(size - 1);
        TYP.Type lastType = SemAn.ofTypeAttr.get(last);

        if (!equivalent(lastType, funType.resType)) {
            throw new Report.Error(last.location(), "Last expression type does not match function return type.");
        }

        return null;
    }

    @Override
    public TYP.Type visit(AST.ExtFunDefn extFunDefn, Mode arg) {
        
        return null;
    }

    /**
     * Prireditveni izraz
     * TYP:35 ->
     *  [[ E1 ]]OFTYPE = τ1 [[ E2 ]]OFTYPE = τ2 τ1 ≡ τ2
     * τ1, τ2 ∈ {int, char, bool} ∪ {ptr(τ ),fun(¯τ → τ )|τ¯ ∈ T ∗ ∧ τ ∈ T }
     * [[ E1 ]]ISADDR = true
     * ------------------------------------------------------------------------------------------------
     * [[ E1 = E2 ]]OFTYPE = void
     * [[ E1 = E2 ]]ISCONST = false
     * [[ E1 = E2 ]]ISADDR = false
     */
    @Override
    public TYP.Type visit(AST.AsgnExpr asgnExpr, Mode arg) {
        TYP.Type leftType = asgnExpr.fstExpr.accept(this, arg);
        TYP.Type rightType = asgnExpr.sndExpr.accept(this, arg);

        
        Boolean leftAddr = SemAn.isAddrAttr.get(asgnExpr.fstExpr);
        if (leftAddr == null || !leftAddr) {
            throw new Report.Error(asgnExpr.location(), "Left-hand side of assignment must be addressable.");
        }

        
        if (!equivalent(leftType, rightType)) {
            throw new Report.Error(asgnExpr.location(), "Assignment type mismatch.");
        }

        
        SemAn.ofTypeAttr.put(asgnExpr, TYP.VoidType.type);
        SemAn.isAddrAttr.put(asgnExpr, false);
        SemAn.isConstAttr.put(asgnExpr, false);
        return TYP.VoidType.type;
    }
}

