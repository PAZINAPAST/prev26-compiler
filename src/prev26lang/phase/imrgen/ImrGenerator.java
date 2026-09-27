package prev26lang.phase.imrgen;

import java.util.*;

import prev26lang.phase.abstr.*;
import prev26lang.phase.abstr.AST.Nodes;
import prev26lang.phase.memory.*;
import prev26lang.phase.memory.SizeEncoder;
import prev26lang.phase.seman.*;
import prev26lang.common.report.*;


/**
 * Intermediate code generator.
 * 
 * 
 */

public class ImrGenerator implements AST.FullVisitor<Object, ImrContext> {
    private static final SizeEncoder sizeEncoder = new SizeEncoder();
    //public Map<MEM.Label, MEM.Frame> mapFrame = new HashMap<>();

    /**
     * Ustvari nov kontekst in se sprehodi po AST drevesu z atributi
     */
    @Override
    public Object visit(Nodes<? extends AST.Node> nodes, ImrContext con) {

        if(con == null) {
            con = new ImrContext(null, 0L);
        }

        for(final AST.Node node : nodes) {  
            node.accept(this, con);
        }

        return null;
    }

    /**
     * Definicija Funkcije.
     */
    @Override
    public Object visit(AST.DefFunDefn fun, ImrContext con) {
        
        //Pridobimo frame funkcije in njeno globino

        MEM.Frame frame = Memory.frameAttr.get(fun);
        Long d = frame.depth;

        // Ustvarimo nov kontekst
        ImrContext c = new ImrContext(frame, d);
        //System.out.println("IMRGEN: function " + fun.name + " depth = " + frame.depth);

        /**
         * Nastavimo labelo za vstop vv funkcijo kot labelo iz frame-a funkcije
         * Ustvarimo novo labelo za izhod iz funkcije
         * Tudi tako jih nastavimo preko funkcij:
         * 
         *  @ImrGen.bodyEntryLabelAttr.put(fun, l1);
         *  @ImrGen.bodyExitLabelAttr.put(fun, l2);
         */
        MEM.Label l1 = frame.label;
        MEM.Label l2 = new MEM.Label();

        ImrGen.bodyEntryLabelAttr.put(fun, l1);
        ImrGen.bodyExitLabelAttr.put(fun, l2);

        //rekurzivno naprej visitam-o drevo v telo funkcije
        IMR.Expr body = (IMR.Expr) fun.expr.accept(this, c);

        //Naredimo nov Temp za rezultat funkcije
        MEM.Temp resTemp = new MEM.Temp();


        /**
         * Ustavrimo novo tabelo statementov. Ti predstavljajo kodo te funkcije.
         * 
         * IMR.LABEL(l1) je labela z imenom funkcije / vhodna labela
         * IMR.MOVE(new IMR.TEMP(resTemp), body) premakne IMR kodo ozirmoa rezultat body v resTemp
         * IMR.MOVE( new IMR.TEMP(frame.RV), new IMR.TEMP(resTemp)) premakne iz temporary resTemp v return value iz frame-a funkcije
         * IMR.JUMP(new IMR.NAME(l2)) - premaknemo se na izhodno labelo funkcije
         * IMR.LABEL(l2) - izhodna labela funkcije
         * 
         */
        Vector<IMR.Stmt> stmts = new Vector<>();
        stmts.add(new IMR.LABEL(l1));
        stmts.add(new IMR.MOVE(new IMR.TEMP(resTemp), body));
        stmts.add(new IMR.MOVE( new IMR.TEMP(frame.RV), new IMR.TEMP(resTemp)));
        stmts.add(new IMR.JUMP(new IMR.NAME(l2)));
        stmts.add(new IMR.LABEL(l2));

        IMR.Stmt r = new IMR.STMTS(stmts);
        ImrGen.genStmtIMRAttr.put(fun, r);


        return null;

    }

    /**
     * Atomski izrazi
     * 
     * SEM:1 -> (String)
     * [[ string ]]^M_ADDR =⟨addr(string),M⟩
     * 
     * SEM:6 -> (pointer)
     * [[nil]]^M_EXPR=⟨0,M⟩
     * 
     * SEM:7 -> (bool)
     * [[true]]^M_EXPR=⟨1,M⟩
     * 
     * SEM:8 -> (bool)
     * [[false]]^M_EXPR=⟨0,M⟩
     * 
     * SEM:9 -> (char)
     * [[char ]]^M_EXPR=⟨val(char),M⟩
     * 
     * SEM:10 -> (int)
     * [[int ]]^M_EXPR=⟨val(int),M⟩
     */
    @Override
    public IMR.Expr visit(AST.AtomExpr atom , ImrContext con) {
        IMR.Expr c = null;

        switch(atom.type) {
            case INT:
                c = new IMR.CONST(Long.parseLong(atom.value));
                break;
            case CHAR:
                char ch = atom.value.charAt(1);
                c = new IMR.CONST((long) ch);
                break;
            case BOOL:
                if(atom.value.equals("true")) {
                    c = new IMR.CONST(1);
                } else {
                    c = new IMR.CONST(0);
                }

                break;
            case STR:
                MEM.AbsAccess s = Memory.stringAttr.get(atom);
                c = new IMR.NAME(s.label);
                break;
            case VOID:
            case PTR:
                c = new IMR.CONST(0);
                break;
            default:
                throw new Report.Error("Unknown atom type");
        }

        con.lastExpr = c;
        ImrGen.genExprIMRAttr.put(atom, c);

        return c;

    }

    /**
     * Assign expression
     * 
     * SEM:24 ->
     * [[ E1 ]]^M_ADDR = ⟨n1,M′⟩     [[ E2 ]]^M′_EXPR = ⟨n2,M′′⟩
     *              {   n2      a = n1
     * ∀a:M′′′[a] = {   M′′[a]  otherwise
     * ----------------------------------------------------------------
     * [[ E1 = E2 ]]^M_EXPR = ⟨ 0,M′′′ ⟩
     * 
     */
    @Override
    public Object visit(AST.AsgnExpr ass, ImrContext con) {

        // Getting the address of the first expression
        IMR.Expr rawAddr = addressOf(ass.fstExpr, con);

        // Pridobitev tipa prvega izraza
        TYP.Type fst = SemAn.ofTypeAttr.get(ass.fstExpr);
        Long size = fst.accept(sizeEncoder, null);
        
        //odvisno od velikosti tipa določimo 1 ali 8 bitni memory dostop
        IMR.Expr dst;
        if(size == 1) {
            dst = new IMR.MEM1(rawAddr);
        } else {
            dst = new IMR.MEM8(rawAddr);
        }

        // Izračun vmesne kode drugega expressiona
        IMR.Expr src = (IMR.Expr) ass.sndExpr.accept(this, con);

        // Premik desne vrednosti na  naslov dst
        IMR.Stmt move = new IMR.MOVE(dst, src);

        /**
         * Zavijemo statement v expression in shranimo izraz kot atribut:
         * 
         *  @ImrGen.genExprIMRAttr.put(ass,r)
         * 
         */
        IMR.Expr r = new IMR.SEXPR(move, new IMR.CONST(0L));
        ImrGen.genExprIMRAttr.put(ass, r);
        return r;
    }


    /**
     * Imenski izrazi
     */
    @Override
    public IMR.Expr visit(AST.NameExpr name, ImrContext con) {

        //Pridobi definicijo imenske spremenljivke
        AST.Defn def = SemAn.defAtAttr.get(name);

        /**
         * Če je definicija funkcije:
         *  - pridobi Labelo funkcije
         *  - shrani jo v IMR.NAME
         * 
         *  Shrani generirano IMR kodo:
         *      @ImrGen.genExprIMRAttr.put(name, spr)
         */
        if (def instanceof AST.DefFunDefn funDef) {
            MEM.Frame f = Memory.frameAttr.get(funDef);
            IMR.Expr spr = new IMR.NAME(f.label);
            ImrGen.genExprIMRAttr.put(name, spr);
            return spr;
        }
        if (def instanceof AST.ExtFunDefn extDef) {
            MEM.Label l = new MEM.Label(extDef.name);
            IMR.Expr spr = new IMR.NAME(l);
            ImrGen.genExprIMRAttr.put(name, spr);
            return spr;
        }

        //Pridobimo tip dostopa do spremenljivke
        MEM.Access access = Memory.accessAttr.get(def);
        IMR.Expr addr;

        /**
         * Če je dostop absoluten:
         *  - labelo shranimo v IMR.NAME (ta nas premakne na naslov te labele)
         * 
         * Če je dostop relativen:
         *  
         *  Če je spremenljivka definirana v funkcijah višje moramo najprej priti do 
         *  funkcije kjer je spremenljivka definirana preko static linkov.
         * 
         * Če je definirana spremenljivka v enaki funkciji je osnova enaka FP od funkcije ki je ovit v temp
         * 
         *  naslov se izračuna z seštevanjem baze z offsetom spremenljivke
         * 
         */
        if (access instanceof MEM.AbsAccess abs) {
            addr = new IMR.NAME(abs.label);
        } else if (access instanceof MEM.RelAccess rel) {
            IMR.Expr base;
            if (rel.depth < con.frame.depth) {
                base = getStaticLink(rel.depth, con);
            } else {
                base = new IMR.TEMP(con.frame.FP);
            }
            addr = new IMR.BINOP(IMR.BINOP.Oper.ADD, base, new IMR.CONST(rel.offset));
        } else {
            throw new Report.Error("Wrong type in Name Expr");
        }

        
        
        // Pridobimo tip izraza ime
        TYP.Type typ = SemAn.ofTypeAttr.get(name);
        
        //Razreši imenski tip
        while (typ instanceof TYP.NameType) {
            typ = ((TYP.NameType) typ).type();
        }

        IMR.Expr result;
        if (typ instanceof TYP.ArrType) {
            // Array: variable -> address; parameter -> load pointer
            if (def instanceof AST.ParDefn) {
                result = new IMR.MEM8(addr);   // load pointer from parameter slot
            } else {
                result = addr;                 // address of array variable is the base pointer
            }
        } else {
            long size = typ.accept(sizeEncoder, null);
            result = (size == 1) ? new IMR.MEM1(addr) : new IMR.MEM8(addr);
        }

        // Shranimo kodo k vozlišču
        ImrGen.genExprIMRAttr.put(name, result);
        return result;
    }

    /**
     * Binarni izrazi
     * 
     *  Postopek je enak za vse:
     *  - razrešita se prvi in drugi izraz reurzivno:
     *          @bin.fstExpr.accept(this, con);
     *          @bin.sndExpr.accept(this,con);
     * 
     *  - Nato odvisno od operatorja generiramo IMR.BINOP in shranimo generirano kodo vozlišču tega izraza
     */
    @Override
    public IMR.Expr visit(AST.BinExpr bin, ImrContext con) {
        IMR.Expr fst = (IMR.Expr) bin.fstExpr.accept(this, con);
        IMR.Expr snd = (IMR.Expr) bin.sndExpr.accept(this, con);

        IMR.BINOP binop = null;

        switch(bin.oper) {
            case OR: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.OR, fst, snd);
                break;
            case AND: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.AND, fst, snd);
                break;
            case EQU: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.EQU, fst, snd);
                break;
            case NEQ: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.NEQ, fst, snd);
                break;
            case LTH: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.LTH, fst, snd);
                break;
            case GTH: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.GTH, fst, snd);
                break;
            case LEQ: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.LEQ, fst, snd);
                break;
            case GEQ: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.GEQ, fst, snd);
                break;
            case ADD: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.ADD, fst, snd);
                break;
            case SUB: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.SUB, fst, snd);
                break;
            case MUL: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.MUL, fst, snd);
                break;
            case DIV: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.DIV, fst, snd);
                break;
            case MOD: 
                binop = new IMR.BINOP(IMR.BINOP.Oper.MOD, fst, snd);
                break;
            default:
                throw new Report.Error("Something is wrong with binary expr.");
        }

        ImrGen.genExprIMRAttr.put(bin, binop);
        return binop;
    }

    /**
     * Izrazi
     */
    @Override
    public IMR.Expr visit(AST.Exprs exp, ImrContext con) {
        
        // Vektor statementov
        Vector<IMR.Stmt> stmts = new Vector<>();
        IMR.Expr l = null;
        int ix = 0;
        //MEM.Frame f = con.frame;
        //System.out.println( "Ime: " +   f.label + " exprs size: " + exp.exprs.size());

        /**
         * gremo čez vse expressione pridobimo intermediate code expression in 
         * ga shranimo v stmts vektor.
         *
         *  Shranimo ga v expression ,ki se izvede in njegov rezultat se ne shrani. 
         */

        for( AST.Expr e : exp.exprs) {
            l = (IMR.Expr) e.accept(this, con);
            if(ix < exp.exprs.size() - 1) {
                //System.out.println("Se naredi stmts.");
                stmts.add(new IMR.ESTMT(l));
            }
            ix++;
        }

        
        /**
         * Če je vektor prazen vrnemo null.
         * 
         * Drugače vektor damo v vektor STMTS in dodamo v SEXPR, kjer poleg da se izvedejo statementi se zadnji expression izračuna in 
         *  vrne rezultat tega expressiona 
         */
        if(stmts.isEmpty()) {/*System.out.println("le en expr"); */return l;}
        return new IMR.SEXPR(new IMR.STMTS(stmts), l);

    }


    /**
     * If-Then-Expr
     * 
     * SEM:25 -> (če je pogoj true)
     * [[ E ]]^M_EXPR = ⟨true, M0⟩    [[ E1 ]]^M0_EXPR = ⟨n1, M1⟩ ... [[ Es ]]^Ms−1_EXPR =  ns, Ms⟩
     * ----------------------------------------------------------------------------------------------
     * [[ if E then E1 , E2 , . . . , Es end ]]EXPR = ⟨0, Ms⟩
     * 
     * 
     * SEM:26 -> (če je pogoj false)
     * [[ E ]]^M_EXPR = ⟨false, M0⟩
     * -----------------------------------------------------------------------
     * [[ if E then E1 , E2 , . . . , Es end ]]EXPR = ⟨0, M0⟩

     */
    @Override
    public IMR.Expr visit(AST.IfThenExpr expr, ImrContext con) {

        /**
         * Pridobimo IMR kodo pogoja
         * Nastavimo then , else in end labelo,
         * Nato pa še temporary za shranitev rezultata
         */
        IMR.Expr cond = (IMR.Expr) expr.condExpr.accept(this, con);
        MEM.Label thenLabel = new MEM.Label();
        MEM.Label elseLabel = new MEM.Label();
        MEM.Label endLabel = new MEM.Label();
        MEM.Temp resultTemp = new MEM.Temp();

        //If loop zapakiramo v conditional jump
        IMR.Stmt cjump = new IMR.CJUMP(cond, new IMR.NAME(thenLabel), new IMR.NAME(elseLabel)); 

        /**
         * Če je res evalviramo thenExpr in pridobimo IMR kodo
         * Premaknemo vrednost v temp ResultTemp in nato dodamo še skok na konec if
         * 
         */
        IMR.Expr thenVal = (IMR.Expr) expr.thenExpr.accept(this, con);
        IMR.Stmt thenMove = new IMR.MOVE(new IMR.TEMP(resultTemp), thenVal);
        IMR.Stmt thenjump = new IMR.JUMP(new IMR.NAME(endLabel));

        /**
         * Če pogoj ne stoji, v rezultat damo 0
         */
        IMR.Stmt elseMove = new IMR.MOVE(new IMR.TEMP(resultTemp), new IMR.CONST(0L));


        //Skonstruiramo if then kodo v vektorju stmts
        Vector<IMR.Stmt> stmts = new Vector<>();
        stmts.add(cjump);
        stmts.add(new IMR.LABEL(thenLabel));
        stmts.add(thenMove);
        stmts.add(thenjump);
        stmts.add(new IMR.LABEL(elseLabel));
        stmts.add(elseMove);
        stmts.add(new IMR.LABEL(endLabel));

        // shranimo kodo in na koncu dodamo rezultat ki ga vrnemo
        IMR.Stmt a = new IMR.STMTS(stmts);
        IMR.Expr r = new IMR.SEXPR(a, new IMR.TEMP(resultTemp));
        ImrGen.genExprIMRAttr.put(expr, r);
        return r;
    }
   

    /**
     * If-then-else izraz
     * 
     * SEM:27 -> (pogoj je true)
     * [[ E ]]^M_EXPR = ⟨true, M0⟩      [[ E1 ]]^M0_EXPR = ⟨n1, M1⟩ . . . [[ Es ]]^Ms−1_EXPR = ⟨ns, Ms⟩
     * -------------------------------------------------------------------------------------------------------
     * [[ if E then E1 , E2 , . . . , Es else E′1 , E′2 , . . . , E′s′ end ]]EXPR = ⟨0, Ms⟩
     * 
     * SEM:28 -> (pogoj je false)
     * [[ E ]]^M_EXPR = ⟨false, M0⟩    [[ E′1]]^M0_EXPR = ⟨n1, M1⟩ . . . [[ E′s′ ]]^Ms′−1_EXPR = ⟨ns′ , Ms′ ⟩
     * ------------------------------------------------------------------------------------------------------------------
     * [[ if E then E1 , E2 , . . . , Es else E′1 , E′2 , . . . , E′s′ end ]]EXPR = ⟨0, Ms′ ⟩
     * 
     * 
     */
    @Override
    public IMR.Expr visit(AST.IfThenElseExpr expr, ImrContext con) {

        /**
         * Pridobimo IMR kodo pogoja
         * Nastavimo then , else in end labelo,
         * Nato pa še temporary za shranitev rezultata
         */
        IMR.Expr cond = (IMR.Expr) expr.condExpr.accept(this, con);
        MEM.Label thenLabel = new MEM.Label();
        MEM.Label elseLabel = new MEM.Label();
        MEM.Label endLabel = new MEM.Label();
        MEM.Temp resultTemp = new MEM.Temp();

        //If loop zapakiramo v conditional jump
        IMR.Stmt cjump = new IMR.CJUMP(cond, new IMR.NAME(thenLabel), new IMR.NAME(elseLabel)); 

        /**
         * Če je res evalviramo thenExpr in pridobimo IMR kodo
         * Premaknemo vrednost v temp ResultTemp in nato dodamo še skok na konec if
         * 
         */
        IMR.Expr thenVal = (IMR.Expr) expr.thenExpr.accept(this, con);
        IMR.Stmt thenMove = new IMR.MOVE(new IMR.TEMP(resultTemp), thenVal);
        IMR.Stmt thenjump = new IMR.JUMP(new IMR.NAME(endLabel));

        /**
         * Če ni res evalviramo elseExpr in pridobimo IMR kodo
         * Premaknemo vrednost v temp ResultTemp
         * 
         */
        IMR.Expr elseVal = (IMR.Expr) expr.elseExpr.accept(this, con);
        IMR.Stmt elseMove = new IMR.MOVE(new IMR.TEMP(resultTemp), elseVal);

        //Skonstruiramo if then kodo v vektorju stmts
        Vector<IMR.Stmt> stmts = new Vector<>();
        stmts.add(cjump);
        stmts.add(new IMR.LABEL(thenLabel));
        stmts.add(thenMove);
        stmts.add(thenjump);
        stmts.add(new IMR.LABEL(elseLabel));
        stmts.add(elseMove);
        stmts.add(new IMR.LABEL(endLabel));

        // shranimo kodo in na koncu dodamo rezultat ki ga vrnemo
        IMR.Stmt a = new IMR.STMTS(stmts);
        IMR.Expr r = new IMR.SEXPR(a, new IMR.TEMP(resultTemp));
        ImrGen.genExprIMRAttr.put(expr, r);
        return r;
    }

    /**
     * While izraz
     * 
     * SEM:29 -> (pogoj je true)
     * [[ E ]]^M_EXPR = ⟨true, M0⟩    [[ E1 ]]^M0_EXPR = ⟨n1, M1⟩ ... [[ Es ]]^Ms−1_EXPR = ⟨ns, Ms⟩
     * --------------------------------------------------------------------------------------------------------------
     * [[ while E do E1 , E2 , . . . , Es end ]]^M_EXPR = [[ while E do E1 , E2 , . . . , Es end ]]^Ms_EXPR
     * 
     * SEM:30 -> (pogoj za false)
     * [[ E ]]^M_EXPR = ⟨false, M0⟩
     * ----------------------------------------------------------------------------------
     * [[ while E do E1 , E2 , . . . , Es end ]]^M_EXPR = ⟨0, M0⟩
     * 
     * 
     */
    @Override
    public IMR.Expr visit(AST.WhileExpr exp, ImrContext con) {
        /**
         * Definiramo labele za pogoj, telo in konec
         */
        MEM.Label condLabel = new MEM.Label();
        MEM.Label bodyLabel = new MEM.Label();
        MEM.Label endLabel = new MEM.Label();

        /**
         * Evaluate-amo pogoj in nato
         * pogoj, bodyLabel in ednLabel ovijemo v conditional jump
         */
        IMR.Expr cond = (IMR.Expr) exp.condExpr.accept(this, con);
        IMR.Stmt cjump = new IMR.CJUMP(cond, new IMR.NAME(bodyLabel), new IMR.NAME(endLabel));

        /**
         * Evalviramo telo while izraza
         * Telo zavijemo v izraze katere rezultate zavržemo
         * Ponovno skočimo v pogoj
         */
        IMR.Expr body = (IMR.Expr) exp.expr.accept(this, con);
        IMR.Stmt bodyStmt = new IMR.ESTMT(body);
        IMR.Stmt bodyJump = new IMR.JUMP(new IMR.NAME(condLabel));


        /**
         * Ves while loop spravimo v vektor statementov
         */
        Vector<IMR.Stmt> stmts = new Vector<>();
        stmts.add(new IMR.LABEL(condLabel));
        stmts.add(cjump);
        stmts.add(new IMR.LABEL(bodyLabel));
        stmts.add(bodyStmt);
        stmts.add(bodyJump);
        stmts.add(new IMR.LABEL(endLabel));

        /**
         * while loop shranimo v izraze in SEXPR kjer se izvedejo vsi izrazi zadnji expression pa vrne rezultat 0
         */
        IMR.Stmt loop = new IMR.STMTS(stmts);
        IMR.Expr r = new IMR.SEXPR(loop, new IMR.CONST(0L));
        ImrGen.genExprIMRAttr.put(exp, r);
        return r;
    }

    /**
     * Let izraz
     * 
     * SEM:32 ->
     * -------------------------------------------------------------------------------------------------
     * [[ let D1 D2 . . . Dn in E1 , E2 , . . . , Es end ]]^M_EXPR = [[ E1 , E2 , . . . , Es ]]^M_EXPR
     * 
     * 
     */
    @Override
    public IMR.Expr visit(AST.LetExpr letExpr, ImrContext con) {

        //sprehodimo se po definicijah in če je definirana funkcija se potopimo globlje z metodo accept(this, con)
        for( AST.Defn defn : letExpr.defns) {
            if(defn instanceof AST.DefFunDefn funDef) {
                funDef.accept(this, con);

            }

        }
        
        //Evalviramo izraze in jih shranimo kot atrubit vozlišča
        IMR.Expr body = (IMR.Expr) letExpr.expr.accept(this, con);
        ImrGen.genExprIMRAttr.put(letExpr, body);
        return body;
    }

    /**
     * Klic funkcije
     * 
     * SEM:19 ->
     * [[ E ]]^M_EXPR = ⟨fun, M0⟩     [[ E1 ]]^M0_EXPR = ⟨n1, M1⟩ . . . [[ Em ]]^Mm−1_EXPR = ⟨nm, Mm⟩
     * --------------------------------------------------------------------------------------------------
     * [[ E(E1, . . . ,Em) ]]MEXPR = ⟨fun(n1, . . . , nm), Mm⟩
     * 
     */
    @Override
    public IMR.Expr visit(AST.CallExpr call, ImrContext con) {
        
        /**
         * Pridobimo kodo za naslov funkcije
         * In pridobimo samo funkcijo
         */
        IMR.Expr funAddr;
        AST.Expr fexpr = call.funExpr;

        if(fexpr instanceof AST.NameExpr name) {
            AST.Defn def = SemAn.defAtAttr.get(name);

            if(def instanceof AST.DefFunDefn defFun) {
                MEM.Frame frame = Memory.frameAttr.get(defFun);
                funAddr = new IMR.NAME(frame.label);
            } else if(def instanceof AST.ExtFunDefn extFun) {
                MEM.Label label = new MEM.Label(extFun.name);
                funAddr = new IMR.NAME(label);
            } else {
                throw new Report.Error("Call does not refer to a function.");

            }
        } else {
            funAddr = (IMR.Expr) fexpr.accept(this, con);
        }

        /**
         * Preverimo ali potrebuje klic vsebovati static link
         */
        boolean needsStaticLink = false;
        long callDepth = -1;
        if(fexpr instanceof AST.NameExpr name) {
            AST.Defn def = SemAn.defAtAttr.get(name);
            
            if(def instanceof AST.DefFunDefn defFun) {
                MEM.Frame callFrame = Memory.frameAttr.get(defFun);
                callDepth = callFrame.depth;
                if(callDepth > 0) {
                    needsStaticLink = true;
                }
            }
        }

        /**
         * Nastavimo dva vektorja,
         * en za offsete drugi za algoritme, element pri argumetih ima offset na istem indeksu.
         * 
         * Če potrebujemo statični link dodamo to na začetek nato pa gremo čez vse argumente.
         */
        Vector<Long> offs = new Vector<>();
        Vector<IMR.Expr> args = new Vector<>();
        long currOff = 0;

        if(needsStaticLink) {
            IMR.Expr staticLink = getStaticLink(callDepth - 1, con);
            args.add(staticLink);
            offs.add(currOff);
            currOff += 8;
        }


        for(AST.Expr arg : call.argExprs) {

            /**
             * Vsak argument gremo posebaj evalvirat in pridoniti IMR kodo
             * dodamo argument i njegov offset v vektoja
             */
            IMR.Expr argVal = (IMR.Expr) arg.accept(this, con);
            args.add(argVal);
            offs.add(currOff);


            /**
             * Preverimo tip argumenta in izračunamo koliko prostora zavzame element in popravimo nov offset
             */
            TYP.Type argType = SemAn.ofTypeAttr.get(arg);
            TYP.Type resolved = argType;

            while(resolved instanceof TYP.NameType) {
                resolved = ((TYP.NameType) resolved).type();

            }

            long argSize;

            if(resolved instanceof TYP.ArrType || resolved instanceof TYP.StrType || resolved instanceof TYP.UniType || resolved instanceof TYP.FunType) {
                argSize = 8L;
            } else {
                argSize = argType.accept(sizeEncoder, null);
            }

            
            long padded = (argSize % 8 == 0) ? argSize : ((argSize / 8) + 1) * 8;
            currOff += padded;
        }

        if(con.frame != null && currOff > con.frame.argsSize) {
            throw new Report.Error("Outgoing argument size exceeds frame.argsSize");
        }

        // Ovijemo naslov funcije in ofsete in argumente  v IMR.CALL kodo
        IMR.Expr r = new IMR.CALL(funAddr, offs, args);
        ImrGen.genExprIMRAttr.put(call, r);
        return r;
    }

    /**
     * Izraz za dostop do elementa v arrayu
     * 
     * SEM:17 ->
     * [[ E1[E2] ]]^M_ADDR = ⟨a, M′⟩ 
     * --------------------------------------------------
     * [[ E1[E2] ]]^M_EXPR = ⟨M′[a], M′⟩
     * 
     */
    @Override
    public IMR.Expr visit(AST.ArrExpr arr, ImrContext con) {
        //Pridobimo IMR kodo za dostop do naslova elementa
        IMR.Expr elemAddr = addressOf(arr, con);

        // Pridobimo tip elementa v Arrayu
        TYP.Type arrType = SemAn.ofTypeAttr.get(arr.arrExpr);
        if(arrType instanceof TYP.NameType) {
            arrType = ((TYP.NameType) arrType).type();
            //System.out.println("DEBUG: Array type is a name type, actual type: " + arrType);
        }

        // Izračunamo velikost
        long elemSize = ((TYP.ArrType) arrType).elemType.accept(sizeEncoder, null);

        //Shranimo v strukturo ki prebere naslov iz tega naslova prebere element in ga vrne
        IMR.Expr r = (elemSize == 1) ? new IMR.MEM1(elemAddr) : new IMR.MEM8(elemAddr);
        ImrGen.genExprIMRAttr.put(arr, r);
        return r;
    }

    /**
     * Izraz za dostop do komponent
     * 
     * [[ E.identifier ]]^M_ADDR = ⟨a, M′⟩
     * ----------------------------------------------------------------------
     * [[ E.identifier ]]^M_EXPR = ⟨M′[a], M′⟩
     * 
     */
    @Override
    public IMR.Expr visit(AST.CompExpr comp, ImrContext con) {

        // IMR koda za dostop do komponente v rec tipu
        IMR.Expr fieldAddr = addressOf(comp, con);

        // Pridobimo tip komponente
        TYP.Type filedType = SemAn.ofTypeAttr.get(comp);

        // Preverimo velikost tipa in ga Zavijemo v MEM1 ali MEM8 (Ti shranijo naslov, bere iz naslova in vrne vrednost na tem naslovu)
        long size = filedType.accept(sizeEncoder, null);
        IMR.Expr r = (size == 1) ? new IMR.MEM1(fieldAddr) : new IMR.MEM8(fieldAddr);
        ImrGen.genExprIMRAttr.put(comp, r);
        return r;

    }


    /**
     * Castanje tipo
     * 
     * SEM:20 ->
     * [[ E ]]^M_EXPR = ⟨n, M′⟩    [[ T ]]ISTYPE = τ    τ ̸≡ bool ∧ τ ̸≡ char
     * -----------------------------------------------------------------------------
     * [[ E as T ]]^M_EXPR = ⟨n, M′⟩
     * 
     * SEM:21 ->
     * [[ E ]]^M_EXPR = ⟨n, M′⟩     [[ T ]]ISTYPE = bool
     * ---------------------------------------------------------------------------
     * [[ E as T ]]^M_EXPR = ⟨n mod 2, M′⟩
     * 
     * SEM:22 ->
     * [[ E ]]^M_EXPR = ⟨n, M′⟩    [[ T ]]ISTYPE = char
     * ----------------------------------------------------------------------
     * [[ E as T} ]]^M_EXPR = ⟨n mod 256, M′⟩
     * 
     * 
     */
    @Override
    public IMR.Expr visit(AST.CastExpr cast, ImrContext con) {

        // Pridobimo IMR kodo  podizraza, ki bo castan
        IMR.Expr sub = (IMR.Expr) cast.expr.accept(this, con);

        // Katerega tipa je tip v katerege bomo izraz castali
        TYP.Type targetType = SemAn.ofTypeAttr.get(cast);

        IMR.Expr r;

        // Castamo po pravilih nad metodo
        if(targetType instanceof TYP.BoolType) {
            r = new IMR.BINOP(IMR.BINOP.Oper.MOD, sub, new IMR.CONST(2L));

        } else if (targetType instanceof TYP.CharType) {
            r = new IMR.BINOP(IMR.BINOP.Oper.MOD, sub, new IMR.CONST(256L));
        } else {
            r = sub;
        }

        ImrGen.genExprIMRAttr.put(cast, r);
        return r;
    }

    /**
     * Prefixni izrazi
     * 
     * SEM:11 ->
     * [[ E ]]^M_EXPR = ⟨n, M′⟩ op ∈ {not, +, -}
     * -----------------------------------------------
     * [[ op E ]]^M_EXPR = ⟨op n, M′⟩
     * 
     * SEM:13 ->
     * [[ E ]]^M_ADDR = ⟨n, M′⟩
     * ---------------------------------------- 
     * [[ ^E ]]^M_EXPR = ⟨n, M′⟩
     * 
     */
    @Override
    public IMR.Expr visit(AST.PfxExpr pfx, ImrContext con) {

        // Pridobimo IMR kodo podizraza
        IMR.Expr sub = (IMR.Expr) pfx.subExpr.accept(this, con);
        IMR.Expr r;

        /**
         * Odvisno od operatorja zgeneriramo kodo primerno operatorju
         */
        switch(pfx.oper) {
            case ADD:
                r = sub;
                break;
            case SUB:
                r = new IMR.UNOP(IMR.UNOP.Oper.NEG, sub);
                break;
            case NOT:
                r = new IMR.UNOP(IMR.UNOP.Oper.NOT, sub);
                break;
            case PTR:
                r = addressOf(pfx.subExpr, con);
                break;
            default:
                throw new Report.Error("Invalid prefix");

        }

        ImrGen.genExprIMRAttr.put(pfx, r);
        return r;
    }

    /**
     * Izrazi s sufixom
     * 
     * SEM:14 ->
     * [[ E ]]^M_EXPR = ⟨n, M′⟩
     * ---------------------------------------
     * [[ E^ ]]^M_EXPR = ⟨M′[n], M′⟩
     * 
     * 
     */
    @Override
    public IMR.Expr visit(AST.SfxExpr sfx, ImrContext con) {

        //Pridobitev IMR kode za podizraz
        IMR.Expr addr = (IMR.Expr) sfx.subExpr.accept(this, con);

        //Pridobimo dejanksi tip podizraza
        TYP.Type ptrType = SemAn.ofTypeAttr.get(sfx.subExpr);

        /**
         * Preverimo če je pointer in če je shranimo naslov in beremo iz naslova
         */
        if (!(ptrType instanceof TYP.PtrType pt)) throw new Report.Error("Dereference of non-pointer");
        long elemSize = pt.baseType.accept(sizeEncoder, null);

        IMR.Expr r = (elemSize == 1) ? new IMR.MEM1(addr) : new IMR.MEM8(addr);
        ImrGen.genExprIMRAttr.put(sfx, r);
        return r;
    }

    /**
     * Size izraz
     * 
     * SEM:23 ->
     * [[ T ]]ISTYPE = τ
     * --------------------------------------
     * [[ sizeof T ]]^M_EXPR = ⟨sizeof(T), M⟩
     * 
     * 
     */
    @Override
    public IMR.Expr visit(AST.SizeExpr se, ImrContext con) {
        //Pridobimo dejanski tip in shranimo njegovo velikost
        TYP.Type typ = SemAn.ofTypeAttr.get(se.type);

        long size = typ.accept(sizeEncoder, null);

        IMR.Expr r = new IMR.CONST(size);
        ImrGen.genExprIMRAttr.put(se, r);
        return r;
    }

    /**
     * 
     * Pomožna metoda za dodeljevanje naslovov imenksim izrazom in
     * voveljevanjem naslovov kjer se nahajajo komponente v rec tipih in arrayih
     */

    public IMR.Expr addressOf(AST.Expr expr, ImrContext con) {
        if(expr  instanceof AST.NameExpr name) {
            return addressOf(name, con);
        } else if(expr instanceof AST.ArrExpr arr) {
            return addressOf(arr, con);
        } else if(expr instanceof AST.CompExpr comp) {
            return addressOf(comp, con);
        } else {
            throw new Report.Error("Wrong type in addressOf method");
        }
    }

    /**
     * Pomožna metod aza naslavljanje imenksih tipov
     * 
     * SEM:2 ->
     * addr(identifier ) = a
     * ------------------------------------------------------
     * [[ identifier ]]^M_ADDR = ⟨a, M⟩
     * 
     */
    private IMR.Expr addressOf(AST.NameExpr name, ImrContext con) {

        // Pridobimo definicijo imenksega tipa i tip dostopa
        AST.Defn def = SemAn.defAtAttr.get(name);
        MEM.Access access = Memory.accessAttr.get(def);

        /**
         * Če je AbsAccess se ustvari IMR.NAME shrani in vrne na naslov labele
         * Če pa je RelAccess pa  izračunamo bazo, ki je mogoče v drugi funkciji definirana kot je uporabljena
         * nato pa dodamo le še offset in vrnemo naslov
         */
        if(access instanceof MEM.AbsAccess abs) {
            return new IMR.NAME(abs.label);
        } else if(access instanceof MEM.RelAccess rel) {
            //long currentDepth = con.frame.depth;
            IMR.Expr base;
            if(rel.depth < con.frame.depth) {
                base = getStaticLink(rel.depth, con);
            } else {
                base = new IMR.TEMP(con.frame.FP);
            }

            long offset = rel.offset;
            
           
            //System.out.println("Accessing " + name.name + " rel.depth=" + rel.depth + " con.depth=" + con.depth);
            IMR.Expr off = new IMR.CONST(offset);
            return new IMR.BINOP(IMR.BINOP.Oper.ADD, base, off);
        }else {
            throw new Report.Error("Wrong accessing.");
        }
    }

    /**
     * Pomožna funkcija za izpis izrazov
     */
    private String exprToString(IMR.Expr expr) {
        if (expr instanceof IMR.CONST) return "" + ((IMR.CONST) expr).value;
        if (expr instanceof IMR.TEMP) return "T" + ((IMR.TEMP) expr).temp.temp;
        if (expr instanceof IMR.NAME) return ((IMR.NAME) expr).label.name;
        if (expr instanceof IMR.MEM1) return "MEM1(" + exprToString(((IMR.MEM1) expr).addr) + ")";
        if (expr instanceof IMR.MEM8) return "MEM8(" + exprToString(((IMR.MEM8) expr).addr) + ")";
        if (expr instanceof IMR.BINOP) {
            IMR.BINOP bin = (IMR.BINOP) expr;
            return "(" + exprToString(bin.fstExpr) + " " + bin.oper + " " + exprToString(bin.sndExpr) + ")";
        }
        if (expr instanceof IMR.UNOP) {
            IMR.UNOP un = (IMR.UNOP) expr;
            return un.oper + "(" + exprToString(un.subExpr) + ")";
        }
        return expr.toString();
    }

    /**
     * Pomožna funkcija za pridobivanjen naslovov elementov v arrayu
     * 
     * SEM:3 ->
     * [[ E1 ]]^M_ADDR = ⟨n1, M′⟩    [[ E2 ]]^M′_EXPR = ⟨n2, M′′⟩     [[ E1 ]]OFTYPE = arr(n × τ )
     * ----------------------------------------------------------------------------------------------------------
     * [[ E1[E2] ]]^M_ADDR = ⟨n1 + n2 ∗ sizeof(τ ), M′′⟩
     */

    private IMR.Expr addressOf(AST.ArrExpr arr, ImrContext con) {

        //Pridobimo IMR naslova začetka seznama
        IMR.Expr baseAddr = (IMR.Expr) arr.arrExpr.accept(this, con);
        //System.out.println("DEBUG: Addressing array element. Base address: " + exprToString(baseAddr));

        //Pridobimo IMR za naslov indeksa
        IMR.Expr index = (IMR.Expr) arr.idx.accept(this, con);
        //System.out.println("DEBUG: Addressing array element. Index: " + exprToString(index));

        // Pridobimo podatkovni tip iz katerega je array
        TYP.Type arrType = SemAn.ofTypeAttr.get(arr.arrExpr);
        //System.out.println("DEBUG: Addressing array element. Array type: " + arrType);


        while(arrType instanceof TYP.NameType) {
            arrType = ((TYP.NameType) arrType).type();
        }
        
        if (!(arrType instanceof TYP.ArrType atype)) throw new Report.Error(arr.location(), "Expected array type");
        //System.out.println("DEBUG: Addressing array element. Element type: " + atype.elemType);

        //Pridobimo velikost tipa in izračunamo lokacijo po pravilu operacijske semantike
        long elemSize = atype.elemType.accept(sizeEncoder, null);
        //System.out.println("DEBUG: Addressing array element. Element size: " + elemSize);
        IMR.Expr off = new IMR.BINOP(IMR.BINOP.Oper.MUL, index, new IMR.CONST(elemSize));
        //System.out.println("DEBUG: Addressing array element. Offset: " + exprToString(off));
        return new IMR.BINOP(IMR.BINOP.Oper.ADD, baseAddr, off);

    }


    /**
     * Pomožna metoda za pridobivanje statičnih povezav
     * 
     *  Pridobimo fp od trenutne funkcije v kateri smo,
     *  nato pa izračunamo koliko nivojev višje se moramo premakniti da pridemo do željene funkcije
     */
   private IMR.Expr getStaticLink(long targetDepth, ImrContext con) {
        IMR.Expr fp = new IMR.TEMP(con.frame.FP);
        //System.out.println("DEBUG: Getting static link. Current depth: " + con.frame.depth + ", target depth: " + targetDepth);
        long diff = con.frame.depth - targetDepth;
        for(int i = 0; i < diff; i++) {
            fp = new IMR.MEM8(fp);
        }

        return fp;
   }


    /**
     * Pomožna metoda za naslavljanje komponent v record tipu
     * 
     * SE;:4 ->
     * [[ E ]]^M_ADDR = ⟨n1, M′⟩
     * ----------------------------------
     * [[ E.identifier ]]^M_ADDR = ⟨n1 + addr(identifier ), M′⟩
     * 
     */
    private IMR.Expr addressOf(AST.CompExpr comp, ImrContext con) {

        //Pridobimo IMR kodo za dostop do komponente
        IMR.Expr baseAddr = addressOf(comp.recExpr, con);
    
        //Prdiobimo tip record tipa
        TYP.Type contType = SemAn.ofTypeAttr.get(comp.recExpr);
        if (contType == null) {
            throw new Report.Error("Type not set for container expression of component access");
        }
        
        while (contType instanceof TYP.NameType nt) {
            contType = nt.type();
        }
        if (!(contType instanceof TYP.RecType)) {
            throw new Report.Error("Component access on non-record type");
        }
        TYP.RecType recType = (TYP.RecType) contType;
        
       
       // Najdemo record v AST drevesu na katerega se tip nanaša
        AST.RecType astRec = TypeConstructor.astRecordMap.get(recType);
        if (astRec == null) {
            throw new Report.Error("Cannot find AST record type for " + recType);
        }
        
        //Najdemo komponento v record tipu ki jo iščemo
        AST.CompDefn compDef = null;
        for (AST.Node node : astRec.comps) {
            AST.CompDefn cd = (AST.CompDefn) node;
            if (cd.name.equals(comp.name)) {
                compDef = cd;
                break;
            }
        }
        if (compDef == null) {
            throw new Report.Error("Component '" + comp.name + "' not found in record type");
        }
        
        /**
         * Pridobimo način dostopa.
         * 
         *  Če dostop ni definiran se če:
         *      - union: je offset vedno 0
         *      - struct: se sprehodimo po komponentah dokler ne najdemo željene komponente ,
         *      medtem prištevamo offset vse velikosti tipo ki so pred iskano komponento.
         *      
         *      Na koncu izračunamo osnovni naslov + offset
         * 
         * Če je definiran dostop in je rel:
         *      Offset shranimo kot constanto in ga prištejemo  osnovnemu naslovu
         */
        MEM.Access access = Memory.accessAttr.get(compDef);
        if (access == null) {
            long offset = 0;
            // For unions, all members have offset 0; for structs, sum previous members' sizes
            if (!(recType instanceof TYP.UniType)) {
                for (int i  = 0; i < astRec.comps.size(); i++) {
                    AST.CompDefn cd = (AST.CompDefn) astRec.comps.get(i);
                    if (cd.name.equals(comp.name)) {
                        break;
                    }
                    TYP.Type ct = SemAn.ofTypeAttr.get(cd);
                    long sz = ct.accept(sizeEncoder, null);
                    offset += (sz % 8 == 0) ? sz : ((sz / 8) + 1) * 8;
                }
            }

            return new IMR.BINOP(IMR.BINOP.Oper.ADD, baseAddr, new IMR.CONST(offset));
        }

        if (!(access instanceof MEM.RelAccess rel)) {
            throw new Report.Error("Component offset not available");
        }
        IMR.Expr offset = new IMR.CONST(rel.offset);
        return new IMR.BINOP(IMR.BINOP.Oper.ADD, baseAddr, offset);
    }


}

