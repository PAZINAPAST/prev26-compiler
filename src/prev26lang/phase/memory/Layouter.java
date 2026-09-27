package prev26lang.phase.memory;

import java.util.*;

import prev26lang.phase.abstr.*;
import prev26lang.phase.seman.*;
import prev26lang.phase.memory.*;
import prev26lang.common.report.*;
import prev26lang.phase.memory.Context;
import prev26lang.phase.memory.SizeEncoder;
import prev26lang.phase.abstr.AST;
import prev26lang.phase.memory.MEM.*;

/**
 * Layouter
 * 
 * Sprehodi se po AST drevesu in za vsako definicijo tipa/spremenljivke
 *  izraćuna njen dostop:
 *      -AbsAccess za globalne spremenljivke in konstantne nize
 *      - RelAccess za lokalne spremenljivke, parametre in komponente struktur/unij
 * 
 *  Za vsako funkcijo pa zgradi okostje klicnega zapisa: frame
 * 
 *  Velikost tipov pridobimo iz SizeEncoderja.
 *  Konteksten razred (Context) prenaša trenutno stanje(offset, globino, ali smo v parametrih, ali smo v argumentih, itd.)
 */

public class Layouter implements AST.FullVisitor<Object, Context> {
    private static final SizeEncoder sizeEncoder = new SizeEncoder();


    /**
     * Osnoven prehod ustvari se nov kontekst
     */
    public TYP.Type visit(AST.Nodes<? extends AST.Node> nodes, Context context) {
        if(context == null) {
            context = new Context();
        }

        for(final AST.Node node : nodes) {
            node.accept(this, context);
        }

        return null;
    }

    /**
     * definirana spremenljivka
     */
    @Override
    public Object visit(AST.VarDefn var, Context context) {

        //Pridobimo tip spremenljivke in dodelimo velikost in jo nato še paddamo
        TYP.Type varType = SemAn.ofTypeAttr.get(var);
        Long varSize = varType.accept(sizeEncoder, null);
        Long padding = (varSize % 8 == 0) ? varSize : (varSize / 8 ) * 8 + 8;

        /**
         * Če je globina contextu:
         * - < 0 : Globalna spremenljivka
         * - isParams : spremenljivka definirana v parametru, realtivni dostop
         * - drugače je spremenljivka lokalno definirrana ponovno RelAccess
         */
        if(context.depth < 0) {
            AbsAccess access = new AbsAccess(varSize, new Label(var.name));
            Memory.accessAttr.put(var, access);
        } else if(context.isParams) {
            //context.offset += padding;
            RelAccess access = new RelAccess(varSize, context.offset, context.depth);
            Memory.accessAttr.put(var, access);
            context.offset += padding;
            //context.argsSize += padding;
        } else {
            //context.offset -= padding;
            if(context.isLoc) {
                context.locSize += padding;
                context.offset -= padding;
            } else {
                //context.argsSize += padding;
            }
            RelAccess access = new RelAccess(varSize, context.offset, context.depth);
            Memory.accessAttr.put(var, access);
        }

        return null;
    }

    /**
     * Definicija tipa
     */
    @Override
    public Object visit(AST.TypDefn type, Context context) {
        type.type.accept(this, context);
        return null;
    }

    /**
     * Dostop do komponent (a.id)
     */
    @Override
    public Object visit(AST.CompDefn comp, Context context) {

        //Pridobitev tipa komponente in izračun velikost
        TYP.Type compType = SemAn.ofTypeAttr.get(comp);
        Long compSize = compType.accept(sizeEncoder, null);
        Long padding = (compSize % 8 == 0) ? compSize : (compSize / 8 ) * 8 + 8;

        /**
         * Če je componenta od structa dodamo nov relAccess z novim offsetom,
         * Če je pa union pa dodamo offset 0 in dodamo da je offset v kontekstu enak padding če je padding večji od offseta
         *
         */
        if(context.isStruct) {
            RelAccess access = new RelAccess(compSize, context.offset, -1);
            Memory.accessAttr.put(comp, access);
            context.offset += padding;
        } else if(context.isUnion) {
            RelAccess access = new RelAccess(compSize, 0, -1);
            Memory.accessAttr.put(comp, access);
            if(padding > context.offset) {
                context.offset = padding;
            }
        } else {
            throw new Report.Error("Component definition outside of struct or union");
        }

        return null;
    }
    
    /**
     * Struktura
     */
    @Override
    public Object visit(AST.StrType struct, Context context) {
        // V kontesktu nastavimo isStruct in ohranimo offset
        long of = context.offset;
        context.isStruct = true;
        struct.comps.accept(this, context);
        context.isStruct = false;
        context.offset = of;
        return null;
    }

    /**
     * Unija
     */
    @Override
    public Object visit(AST.UniType union, Context context) {
        // v kontekstu nastavimo da je unija in preden gremos shranimo offset in nastavimo offset konteksta na 0

        long of = context.offset;
        context.offset = 0;
        context.isUnion = true;
        for(final AST.CompDefn comp : union.comps) {
            comp.accept(this, context);
            context.offset = 0;
        }
        context.isUnion = false;
        context.offset = of;
        return null;
    }

    /**
     * Definirana funkcija
     */
    @Override
    public Object visit(AST.DefFunDefn fun, Context context) {

        //Shranimo stari kotekst
        //Funkcija si ustvari svoj nov kontekst
        Context oldContext = new Context(context);
        Label funLabel;

        if (context.depth > 0) {
            funLabel = new Label();
        } else {
            funLabel = new Label(fun.name);
        }

        
        //Nastavino globino funkcije
        long funDepth = (context.depth < 0) ? 0 : context.depth + 1;
        context.depth = funDepth;
        //System.out.println("LAYOUT: function " + fun.name + " depth = " + funDepth);


        //paramtetri
        context.isParams = true;
        // če je globina večja od nič se rezervira prostor za static link
        context.offset = (context.depth > 0) ? 8 : 0;
        fun.pars.accept(this, context);
        

        //epxr
        context.isParams = false;
        context.offset = 0;
        context.locSize = 0;
        context.argsSize = 0;
        fun.expr.accept(this, context);

        // paddanje locSize in argsSize
        long locSize = context.locSize;
        if(locSize % 8 != 0) {
            locSize = (locSize / 8) * 8 + 8;
        }
        long argsSize = context.argsSize;
        if(argsSize % 8 != 0) {
            argsSize = (argsSize / 8) * 8 + 8;
        }

        // Frame size je enak vsoti max številu argumentov, stevilu lokalnih spremneljivk in frame pointer in return value
        long frameSize = argsSize + locSize + 16;
        Frame frame = new Frame(funLabel, funDepth, locSize, argsSize, frameSize);

        Memory.frameAttr.put(fun, frame);

        //POnastavi stari kontekst
        context.offset = oldContext.offset;
        context.depth = oldContext.depth;
        context.isParams = oldContext.isParams;
        context.argsSize = oldContext.argsSize;
        context.locSize = oldContext.locSize;

        return null;
    }

    /**
     * Zunanja funkcija
     */
    @Override
    public Object visit(AST.ExtFunDefn fun, Context context) {
        return null;
    }

    /**
     * Definicije parametrov
     */
    @Override
    public Object visit(AST.ParDefn par, Context context) {
        //Pridobimo tip parametra
        TYP.Type parType = SemAn.ofTypeAttr.get(par);

        //Pri imenskih tipih pridobimo pravi tip
        while (parType instanceof TYP.NameType) {
            parType = ((TYP.NameType) parType).type();
        }

        //Določimo velikost parametra
        Long parSize;
        if(parType instanceof TYP.FunType || parType instanceof TYP.ArrType || parType instanceof TYP.StrType || parType instanceof TYP.UniType) {
            parSize = 8L;
        } else {
            parSize = parType.accept(sizeEncoder, null);
        }
       
        Long padding = (parSize % 8 == 0) ? parSize : (parSize / 8 ) * 8 + 8;
        //context.offset += padding;
        //context.locSize += padding;

        RelAccess access = new RelAccess(parSize, context.offset, context.depth);
        Memory.accessAttr.put(par, access);

        context.offset += padding;

        return null;

    }

    /**
     * Let izraz
     */
    @Override
    public Object visit(AST.LetExpr let, Context context) {
        //Najprej shranimo stari kontekst
        Context oldContext = new Context(context);
        
        // Nastavimo da se spuščamo gledati definicije lokalnih spremenljivk
        context.isLoc = true;
        let.defns.accept(this, context);

        // Sedaj se spuščamo v izraze let izraza
        context.isLoc = false;

        let.expr.accept(this, context);

        return null;
    }

    /**
     * Atomski izrazi
     */
    @Override
    public Object visit(AST.AtomExpr atom, Context context) {

        //Če je string se shrani kot globalna spremenljivka in se shrani kot stringAttr
        if(atom.type == AST.AtomExpr.Type.STR) {
            String r = atom.value;
            String s = r.substring(1, r.length() - 1);
            long size = s.length();

            Label label = new Label();
            AbsAccess access = new AbsAccess(size, label, s);
            Memory.stringAttr.put(atom, access);
        }

        return null;
    }

    /**
     * Klic funkcije
     */
    @Override
    public Object visit(AST.CallExpr call, Context context) {
        // V kontekstu nastavimo da je klic na funkcijo
        context.isCall = true;

        //Izračun velikosti argumentov
        long callArgsSize = 0;
        for(AST.Expr arg : call.argExprs) {
            
            //Pridobimo tip argumenta
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
            //System.out.println("arg type: " + argType);
            //if(argType instanceof TYP.FunType) {
            //    argSize += 8;
            //}
            long padding = (argSize % 8 == 0) ? argSize : (argSize / 8 ) * 8 + 8;
            callArgsSize += padding;
        }

        callArgsSize += 8;

        //Če je ta klic funkcije vsebuje več argumentov kot prejšnji nastavimo velikost argumentov v frame-u na treutno velikost
        if(callArgsSize > context.argsSize) {
            context.argsSize = callArgsSize;
        }

        
        for(AST.Expr arg : call.argExprs) {
            arg.accept(this, context);
        }

        context.isCall = false;
        return null;    
    }

    
}