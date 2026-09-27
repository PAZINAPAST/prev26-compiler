parser grammar Prev26Parser;

@header {

    package prev26lang.phase.synan;
    
    import java.util.*;
    import prev26lang.common.report.*;
    import prev26lang.phase.lexan.*;
    import prev26lang.phase.abstr.*;

}

@members {

    private Location loc(final Token tok) { return new Location((LexAn.LocLogToken)tok); }
    private Location loc(final Locatable loc) { return new Location(loc); }
    private Location loc(final Token tok1, final Token tok2) { return new Location((LexAn.LocLogToken)tok1, (LexAn.LocLogToken)tok2); }
    private Location loc(final Token tok1, final Locatable loc2) { if (loc2 == null) return null; return new Location((LexAn.LocLogToken)tok1, loc2); }
    private Location loc(final Locatable loc1, final Token tok2) { if (loc1 == null) return null; return new Location(loc1, (LexAn.LocLogToken)tok2); }
    private Location loc(final Locatable loc1, final Locatable loc2) { if ((loc1 == null) || (loc2 == null)) return null; return new Location(loc1, loc2); }

}

options{
    tokenVocab=Prev26Lexer;
}

//-----------------------------------------------------------------------------------
// Končna preslikava drevesa programa v source drevo
//-----------------------------------------------------------------------------------
source returns [AST.Nodes<AST.FullDefn> ast]
    : /*** ??? TODO ??? ***/
        prog {$ast = $prog.ast; prev26lang.phase.abstr.Abstr.tree = $ast;}  
    ;

//----------------------------------------------------------------------------------------
// Program: je sestavljen iz nepraznega zaporedja definicij
//  ->  D1, D2, ...., Dn ; D - Definicije
//  Izhod: AST.Node<AST.FullDefn>
//  D = AST.FullDefn
//-----------------------------------------------------------------------------------------
prog returns [AST.Nodes<AST.FullDefn> ast]
    : defn EOF  
        { List<AST.FullDefn> list = new ArrayList<>();
            list.add($defn.ast);
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($defn.ast, $EOF));   }       //syn:1
    | defn rest=prog 
        { List<AST.FullDefn> list = new ArrayList<>();
            list.add($defn.ast);
            for(AST.FullDefn node : $rest.ast) {
                list.add(node);
            }
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($defn.ast, $rest.ast));
            }        //syn:1
    ;

//--------------------------------------------------------------------------------------------------------------------------
// Definicije D: 
// Izhod : AST.FullDefn
// Definicija tipa -> typ id = T; T je tip | AST.TypDefn(String name, Type type)
// Definicija spremenljivke -> var id : T; T je tip | AST.VarDefn(String name, Type type)
// Definicija zunanje funkcije(Sistemske funkcije) -> fun id (id1 : T1, id2 : T2, ....., idn : Tn) : T  | AST.ExtFunDefn(String name, Nodes<ParDefn> pars, Type type) 
// Definicija definirane funckije -> fun id (id1 : T1, id2 : T2, ....., idn : Tn) : T = E1, E2, .... Em| AST.DefFunDefn(String name, Nodes<ParDefn> pars, Type type, Expr expr)
//------------------------------------------------------------------------------------------------------------------------
defn returns [AST.FullDefn ast]
    : TYP NAME ASS type     { $ast = new AST.TypDefn($NAME.getText(), $type.ast); Abstr.locAttr.put($ast, loc($TYP, $type.ast));}                            //syn:2
    | VAR NAME COLON type   { $ast = new AST.VarDefn($NAME.getText(), $type.ast); Abstr.locAttr.put($ast, loc($VAR, $type.ast));}                            //syn:3
    | FUN NAME LP defcn RP COLON type   { $ast = new AST.ExtFunDefn($NAME.getText(), $defcn.ast, $type.ast); Abstr.locAttr.put($ast, loc($FUN, $type.ast));}               //syn:4
    | FUN n=NAME LP p=defcn RP COLON t=type ASS body=exs   
    { 
        AST.Exprs bodyExprs = new AST.Exprs($body.ast);
        Abstr.locAttr.put(bodyExprs, loc($body.start, $body.stop));
        $ast = new AST.DefFunDefn($n.getText(), $p.ast, $t.ast, bodyExprs); 
        Abstr.locAttr.put($ast, loc($FUN, $body.stop));}       //syn:5
    ;

//------------------------------------------------------------------------------------------------
// Zaporedje expressionov/izrazov -> E1, E2, ...., En | Zaporedje je lahko prazno
// Izhod: AST.Nodes<AST.Expr>
// E -> AST.Expr()
//------------------------------------------------------------------------------------------------
exs returns [AST.Nodes<AST.Expr> ast]                             //n > 0
    : e 
        { List<AST.Expr> list = new ArrayList<>();
            list.add($e.ast);
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($e.ast, $e.ast));
        }
    | e COMMA rest=exs
        { List<AST.Expr> list = new ArrayList<>();
            list.add($e.ast);
            for(AST.Expr node : $rest.ast) {
                list.add(node);
            }
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($e.ast, $rest.ast));
            }
    ;

exsn returns [AST.Nodes<AST.Expr> ast]
    : exs {$ast = $exs.ast;}
    |   { $ast = new AST.Nodes<>(new ArrayList<>());Location lo = new Location(0,0); Abstr.locAttr.put($ast, lo);}
    ;

//------------------------------------------------------------------------------------------------------------------------------
// Parametri oblike -> id1 : T1 , id2 : T2,  .... , idn : Tn | Parametrov lahko tudi ni
// Izhod: AST.Nodes<AST.ParDefn>
// id1 : T1 = AST.ParDefn(String name, Type type)
//------------------------------------------------------------------------------------------------------------------------------
defcn returns [AST.Nodes<AST.ParDefn> ast]
    : defc { $ast = $defc.ast;}                   //izpis tipov definiciji s n >= 0
    |   { $ast = new AST.Nodes<>(new ArrayList<>());Location lo = new Location(0,0); Abstr.locAttr.put($ast, lo);}
    ;

defc returns [AST.Nodes<AST.ParDefn> ast]                                    //izpis tiopov definicij n > 0
    : n=NAME COLON t=type
        {
            AST.ParDefn par = new AST.ParDefn($n.getText(), $t.ast);
            Abstr.locAttr.put(par, loc($n, $t.ast));
            List<AST.ParDefn> list = new ArrayList<>();
            list.add(par);
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc(par, par));
        }
    | n=NAME COLON t=type COMMA rest=defc
        {
            AST.ParDefn par = new AST.ParDefn($n.getText(), $t.ast);
            Abstr.locAttr.put(par, loc($n, $t.ast));
            List<AST.ParDefn> list = new ArrayList<>();
            list.add(par);
            for (AST.ParDefn node : $rest.ast) {
                list.add(node);
            }
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc(par, $rest.ast));
        }
    ;

//--------------------------------------------------------------------------------------------------------------
// Definicija / zaporedje component v strukturi/uniji/funkciji -> id1 : T1, id2 : T2, .... , idn : Tn
// Izhod: AST.Nodes<AST.CompDefn>
// id1 : T1 -> AST.CompDefn(Strin name, Type type)
//--------------------------------------------------------------------------------------------------------------
compsn returns [AST.Nodes<AST.CompDefn> ast]
    : comps { $ast = $comps.ast;}                   //izpis tipov definiciji s n >= 0
    |   { $ast = new AST.Nodes<>(new ArrayList<>());Location lo = new Location(0,0); Abstr.locAttr.put($ast, lo);}
    ;

comps returns [AST.Nodes<AST.CompDefn> ast]                                    //izpis tiopov definicij n > 0
    : n=NAME COLON t=type
        {
            AST.CompDefn comp = new AST.CompDefn($n.getText(), $t.ast);
            Abstr.locAttr.put(comp, loc($n, $t.ast));
            List<AST.CompDefn> list = new ArrayList<>();
            list.add(comp);
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc(comp, comp));
        }
    | n=NAME COLON t=type COMMA rest=comps
        {
            AST.CompDefn comp = new AST.CompDefn($n.getText(), $t.ast);
            Abstr.locAttr.put(comp, loc($n, $t.ast));
            List<AST.CompDefn> list = new ArrayList<>();
            list.add(comp);
            for (AST.CompDefn node : $rest.ast) {
                list.add(node);
            }
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc(comp, $rest.ast));
        }
    ;


//--------------------------------------------------------------------------------------------------------------
// Dela enako kot prog, le da so defs uporabljeni v let expressionih
//--------------------------------------------------------------------------------------------------------------
defs returns [AST.Nodes<AST.FullDefn> ast]                    //izpis vecih definicij
    : d=defn
        {
            List<AST.FullDefn> list = new ArrayList<>();
            list.add($d.ast);
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($d.ast, $d.ast));
        }
    | d=defn rest=defs
        {
            List<AST.FullDefn> list = new ArrayList<>();
            list.add($d.ast);
            for(AST.FullDefn node : $rest.ast) {
                list.add(node);
            }
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($d.ast, $rest.ast));
        }
    ;

//------------------------------------------------------------------------------------------------------------------------------------------------------------
// Tipi T:
// Izhod: AST.Type
// Atomski tipi -> int/bool/char/void (rezervirane besede) | AST.AtomType(Type type)
// Imenski tip -> id (ponavadi na novo ustvarjen tip iz večih struktur) | AST.NameType(String name)
// Array tip -> [int] T ; int - celoštevilska konstanta | AST.ArrType(Type elemType, String numElems) 
// Pointer tip -> ^T | AST.PtrType(Type baseType) 
// Struct tip -> (id1 : T1, id2 : T2, ... , idn : Tn) | AST.StrType(Nodes<CompDefn> comps)
// Struct tip -> {id1 : T1, id2 : T2, ... , idn : Tn} | AST.UniType(Nodes<CompDefn> comps)
// Funkcijski tip -> (: T1, T2,  ... , Tn : T) | AST.FunType(Nodes<Type> parType, Type type)
// Castanje tipov -> (T) 
//------------------------------------------------------------------------------------------------------------------------------------------------------------
type returns [AST.Type ast]
    : INT   { $ast = new AST.AtomType(AST.AtomType.Type.INT); Abstr.locAttr.put($ast, loc($INT));}                           //syn:6
    | BOOL  { $ast = new AST.AtomType(AST.AtomType.Type.BOOL); Abstr.locAttr.put($ast, loc($BOOL));}                          //syn:6
    | CHAR  { $ast = new AST.AtomType(AST.AtomType.Type.CHAR); Abstr.locAttr.put($ast, loc($CHAR));}                          //syn:6
    | VOID  { $ast = new AST.AtomType(AST.AtomType.Type.VOID); Abstr.locAttr.put($ast, loc($VOID));}                          //syn:6
    | NAME   { $ast = new AST.NameType($NAME.getText()); Abstr.locAttr.put($ast, loc($NAME));}                       //syn:7
    | LBRCK i=int RBRCK t=type  { $ast = new AST.ArrType($t.ast, $i.ast); Abstr.locAttr.put($ast, loc($LBRCK, $t.ast));}          //syn:8
    | CARET t=type  { $ast = new AST.PtrType($t.ast); Abstr.locAttr.put($ast, loc($CARET, $t.ast));}                    //syn:9
    | LP cs=comps RP    { $ast = new AST.StrType($cs.ast); Abstr.locAttr.put($ast, loc($LP,$RP));}                   //syn:10
    | LBRCE cu=comps RBRCE   { $ast = new AST.UniType($cu.ast); Abstr.locAttr.put($ast, loc($LBRCE,$RBRCE));}              //syn:11
    | LP COLON par=typesn COLON t=type RP     { $ast = new AST.FunType($par.ast, $t.ast); Abstr.locAttr.put($ast, loc($LP, $RP));} //syn:12
    | LP t=type RP    { $ast = $t.ast; Abstr.locAttr.put($ast, loc($LP, $RP));}                    //syn:13
    ;

//------------------------------------------------------------------------------------------
// Pomožni gramatika za celoštevilske konstante
// izhod : String 
//------------------------------------------------------------------------------------------
int returns [ String ast]
    : sign=( PLUS | MINUS)? NUM
        {
            $ast = "";
            if($sign != null) $ast += $sign.getText();
            $ast += $NUM.getText();
            //System.out.println($ast);
        }
    ;  

//----------------------------------------------------------------------------------------------------------------------------------------
// Zaporedje tipov -> T1, T2,  ...., Tn
// Izhod: AST.Node<AST.Type>
//----------------------------------------------------------------------------------------------------------------------------------------
types returns [AST.Nodes<AST.Type> ast]
    : t=type
        {
            List<AST.Type> list = new ArrayList<>();
            list.add($t.ast);
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($t.ast, $t.ast));
        }
    | t=type COMMA rest=types
        {
            List<AST.Type> list = new ArrayList<>();
            list.add($t.ast);
            for(AST.Type node : $rest.ast) {
                list.add(node);
            }
            $ast = new AST.Nodes<>(list);
            Abstr.locAttr.put($ast, loc($t.ast, $rest.ast));
        }
    ;


typesn returns [AST.Nodes<AST.Type> ast]
    : t=types { $ast = $t.ast;}
    |  {$ast = new AST.Nodes<>(new ArrayList<>());Location lo = new Location(0,0); Abstr.locAttr.put($ast, lo);}
    ;

//----------------------------------------------------------------------------------------------------------------------------------------------------
// Izrazi hierarhija (Prioriteta je enaka poziciji najvišje opercaije imajo najvišjo Prioriteto):
// postfiksni operatorji (·,·, . . . ,·) [·] ^ . |
// prefiksni operatorji not + - ^
// multiplikativni operatorji * / %
// aditivni operatorji + -
// primerjalni operatorji == != < > <= >=
// logicni operatorji ˇ and
// logicni operatorji ˇ or
// pretvorba tipa as
// prirejanje =
// Izhod: AST.Expr
//----------------------------------------------------------------------------------------------------------------------------------------------------

e returns [AST.Expr ast]
    : expr  { $ast = $expr.ast;}                      //syn:16,17,18,19,20,21,23
    ;

expr returns [ AST.Expr ast]
    : assexpr { $ast = $assexpr.ast;}
    ;

assexpr returns [AST.Expr ast]
    : l=asexpr (ass=ASS r=asexpr)?
        {
            if ($ass == null) {
                $ast = $l.ast;
            } else {
                $ast = new AST.AsgnExpr($l.ast, $r.ast);
                Abstr.locAttr.put($ast, loc($l.ast, $r.ast));
            }
        }
    ;

asexpr returns [AST.Expr ast]
    : l=orexpr { $ast = $l.ast;} (AS t=type { AST.Expr expr = $ast;$ast = new AST.CastExpr($t.ast, expr); Abstr.locAttr.put($ast, loc(expr, $t.ast));})*
    ;

orexpr returns [AST.Expr ast]
    : l=andexpr { $ast = $l.ast;} (OR r=andexpr { AST.Expr l = $ast; $ast = new AST.BinExpr(AST.BinExpr.Oper.OR, l, $r.ast); Abstr.locAttr.put($ast, loc(l, $r.ast));})*
    ;

andexpr returns[AST.Expr ast]
    : l=compexpr { $ast = $l.ast;} (AND r=compexpr { AST.Expr l = $ast; $ast = new AST.BinExpr(AST.BinExpr.Oper.AND, l, $r.ast); Abstr.locAttr.put($ast, loc(l, $r.ast));})*
    ;

compexpr returns [AST.Expr ast]
    : l=addexpr (op=(EQ | NEQ | LEQ | GEQ | LT | GT) r=addexpr)?
        {
            if ($op == null) {
                $ast = $l.ast;
            } else {
                AST.BinExpr.Oper oper;

                switch($op.getType()) {
                    case Prev26Lexer.EQ: oper = AST.BinExpr.Oper.EQU; break;
                    case Prev26Lexer.NEQ: oper = AST.BinExpr.Oper.NEQ; break;
                    case Prev26Lexer.LEQ: oper = AST.BinExpr.Oper.LEQ; break;
                    case Prev26Lexer.GEQ: oper = AST.BinExpr.Oper.GEQ; break;
                    case Prev26Lexer.LT: oper = AST.BinExpr.Oper.LTH; break;
                    case Prev26Lexer.GT: oper = AST.BinExpr.Oper.GTH; break;
                    default: throw new Report.InternalError();
                }

                $ast = new AST.BinExpr(oper, $l.ast, $r.ast);
                Abstr.locAttr.put($ast, loc($l.ast, $r.ast));

            }
        }
    ;

addexpr returns [AST.Expr ast]
    : l=mulexpr {$ast = $l.ast;} (op=(PLUS | MINUS) r=mulexpr
        {
            AST.Expr l = $ast;
            AST.BinExpr.Oper oper;

            switch($op.getType()) {
                case Prev26Lexer.PLUS: oper = AST.BinExpr.Oper.ADD; break;
                case Prev26Lexer.MINUS: oper = AST.BinExpr.Oper.SUB; break;
                default: throw new Report.InternalError();
            }

            $ast = new AST.BinExpr(oper, l, $r.ast);
            Abstr.locAttr.put($ast, loc(l, $r.ast));
            
        }
        )*
    ;

mulexpr returns [AST.Expr ast]
    : l=prefixexpr { $ast = $l.ast;} (op=(MUL | DIV | MOD) r=prefixexpr
        {
            AST.Expr l = $ast;
            AST.BinExpr.Oper oper;

            switch($op.getType()) {
                case Prev26Lexer.MUL: oper = AST.BinExpr.Oper.MUL; break;
                case Prev26Lexer.DIV: oper = AST.BinExpr.Oper.DIV; break;
                case Prev26Lexer.MOD: oper = AST.BinExpr.Oper.MOD; break;
                default: throw new Report.InternalError();
            }

            $ast = new AST.BinExpr(oper, l, $r.ast);
            Abstr.locAttr.put($ast, loc(l, $r.ast));
            
        }
    )*
    ;

prefixexpr returns [AST.Expr ast]
    : NOT ex=prefixexpr  { $ast = new AST.PfxExpr(AST.PfxExpr.Oper.NOT, $ex.ast); Abstr.locAttr.put($ast, loc($NOT, $ex.ast));}
    | PLUS ex=prefixexpr { $ast = new AST.PfxExpr(AST.PfxExpr.Oper.ADD, $ex.ast); Abstr.locAttr.put($ast, loc($PLUS, $ex.ast));}
    | MINUS ex=prefixexpr    { $ast = new AST.PfxExpr(AST.PfxExpr.Oper.SUB, $ex.ast); Abstr.locAttr.put($ast, loc($MINUS, $ex.ast));}
    | CARET ex=prefixexpr    { $ast = new AST.PfxExpr(AST.PfxExpr.Oper.PTR, $ex.ast); Abstr.locAttr.put($ast, loc($CARET, $ex.ast));}
    | postfixexpr { $ast = $postfixexpr.ast;}
    ;

postfixexpr returns [AST.Expr ast]
    : prim  { $ast = $prim.ast;}
    | p=postfixexpr LP args=exsn RP { $ast = new AST.CallExpr($p.ast, $args.ast); Abstr.locAttr.put($ast, loc($p.ast, $RP));}
    | p=postfixexpr LBRCK idxExpr=e RBRCK    { $ast = new AST.ArrExpr($p.ast, $idxExpr.ast); Abstr.locAttr.put($ast, loc($p.ast,$RBRCK));}
    | p=postfixexpr CARET   { $ast = new AST.SfxExpr(AST.SfxExpr.Oper.PTR, $p.ast); Abstr.locAttr.put($ast, loc($p.ast, $CARET));} 
    | p=postfixexpr DOT n=NAME  { $ast = new AST.CompExpr($p.ast, $n.getText()); Abstr.locAttr.put($ast, loc($p.ast, $n));}
    ;

prim returns [AST.Expr ast]                //syn:14,15
    : NUM   { $ast = new AST.AtomExpr(AST.AtomExpr.Type.INT, $NUM.getText()); Abstr.locAttr.put($ast, loc($NUM));}
    | CHARCONST { $ast = new AST.AtomExpr(AST.AtomExpr.Type.CHAR, $CHARCONST.getText()); Abstr.locAttr.put($ast, loc($CHARCONST));}
    | STRING    { $ast = new AST.AtomExpr(AST.AtomExpr.Type.STR, $STRING.getText()); Abstr.locAttr.put($ast, loc($STRING));}
    | TRUE  { $ast = new AST.AtomExpr(AST.AtomExpr.Type.BOOL, $TRUE.getText()); Abstr.locAttr.put($ast, loc($TRUE));}
    | FALSE { $ast = new AST.AtomExpr(AST.AtomExpr.Type.BOOL, $FALSE.getText()); Abstr.locAttr.put($ast, loc($FALSE));}
    | NIL   { $ast = new AST.AtomExpr(AST.AtomExpr.Type.PTR, $NIL.getText()); Abstr.locAttr.put($ast, loc($NIL));}
    | NONE  { $ast = new AST.AtomExpr(AST.AtomExpr.Type.VOID, $NONE.getText()); Abstr.locAttr.put($ast, loc($NONE));}
    | NAME  { $ast = new AST.NameExpr($NAME.getText()); Abstr.locAttr.put($ast, loc($NAME));}
    | LP list=exs RP { $ast = new AST.Exprs($list.ast); Abstr.locAttr.put($ast, loc($LP, $RP));}
    | SIZEOF t=type   { $ast = new AST.SizeExpr($t.ast); Abstr.locAttr.put($ast, loc($SIZEOF, $t.ast));}           //syn:22
    | IF cond=e THEN tb=exs END     //syn:24
        {
            AST.Exprs thenBlock = new AST.Exprs($tb.ast);
            Abstr.locAttr.put(thenBlock, loc($tb.start, $tb.stop));
            $ast = new AST.IfThenExpr($cond.ast, thenBlock);
            Abstr.locAttr.put($ast, loc($IF, $END));
        }
    | IF cond=e THEN tb=exs ELSE el=exs END    //syn:25
        {
            AST.Exprs thenBlock = new AST.Exprs($tb.ast);
            Abstr.locAttr.put(thenBlock, loc($tb.start, $tb.stop));
            AST.Exprs elseBlock = new AST.Exprs($el.ast);
            Abstr.locAttr.put(elseBlock, loc($el.start, $el.stop));
            $ast = new AST.IfThenElseExpr($cond.ast, thenBlock, elseBlock);
            Abstr.locAttr.put($ast, loc($IF, $END));
        }
    | WHILE cond=e DO body=exs END        //syn:26
        {
            AST.Exprs bodyBlock = new AST.Exprs($body.ast);
            Abstr.locAttr.put(bodyBlock, loc($body.start, $body.stop));
            $ast = new AST.WhileExpr($cond.ast, bodyBlock);
            Abstr.locAttr.put($ast, loc($WHILE, $END));
        }
    | LET d=defs IN body=exs END       //syn:27
        {
            AST.Exprs bodyBlock = new AST.Exprs($body.ast);
            Abstr.locAttr.put(bodyBlock, loc($body.start, $body.stop));
            $ast = new AST.LetExpr($d.ast, bodyBlock);
            Abstr.locAttr.put($ast, loc($LET, $END));
        }
    ;
