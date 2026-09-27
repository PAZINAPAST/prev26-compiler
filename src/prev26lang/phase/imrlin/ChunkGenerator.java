package prev26lang.phase.imrlin;

import java.util.*;
import prev26lang.phase.abstr.*;
import prev26lang.phase.memory.*;
import prev26lang.phase.imrgen.*;
import prev26lang.phase.seman.*;
import prev26lang.common.report.*;

/**
 * Chunk Generator
 * 
 * Generira data ali code chunke, ko se sprehaja po AST drevesu.
 * Ko generira code chunke vmeso kodo tudi linearizira.
 * Ko pride do spremenljivke ali 
 */

public class ChunkGenerator implements AST.FullVisitor<Void, Void> {

    /**
     * STRING
     * 
     * Ko pride do atomskega tipa preveri ali je string
     * 
     * Potem pridobimo dostop do stringa in ga nato ovijemo v dataChunk.
     * 
     * In dodamo v dataChunks list
     * 
     */
    @Override
    public Void visit(AST.AtomExpr atomExpr, Void arg) {
        if(atomExpr.type == AST.AtomExpr.Type.STR) {
            MEM.AbsAccess access = Memory.stringAttr.get(atomExpr);
            if(access != null) {
                ImrLin.addDataChunk(new LIN.DataChunk(access));
            }
        }

        return null;
    }

    /**
     * Definicija spremenljivk
     * 
     * Preverimo dostop do pomnilnika če je AbsAccess,
     * ga ovijemo v DataChunk in ga damo v list DataChunks
     */
    @Override
    public Void visit(AST.VarDefn var, Void arg) {
        MEM.Access access = Memory.accessAttr.get(var);
        if(access instanceof MEM.AbsAccess) {
            ImrLin.addDataChunk(new LIN.DataChunk((MEM.AbsAccess)access));
        }
        return null;
    }


    /**
     * Definicija funkcije
     * 
     * Na začetku pridobimo slednje ifromacije:
     *  Frame funkcije, vhodno labelo in izhodno labelo in drevo statementov kot telo.
     * 
     * Nato skonstruiramo nov Statement Evalvator in lineariziramo telo fonkcije.
     * 
     * Nato linearizerano kodo z frameom, vhodno in izhodno labelo zavijemo v codeChunk i dodamo v list code chunkov
     */
    @Override
    public Void visit(AST.DefFunDefn funDefn, Void arg) {
        //System.out.println("DEBUG: Processing function: " + funDefn.name);
        MEM.Frame frame = Memory.frameAttr.get(funDefn);
        MEM.Label entryLabel = ImrGen.bodyEntryLabelAttr.get(funDefn);
        MEM.Label exitLabel = ImrGen.bodyExitLabelAttr.get(funDefn);
        IMR.Stmt body = ImrGen.genStmtIMRAttr.get(funDefn);
        if (body == null) {
            throw new Report.Error("IMR code not generated for function: " + funDefn.name);
        }

        StatementEvaluator stmtEvaluator = new StatementEvaluator();
        Vector<IMR.Stmt> linearized = body.accept(stmtEvaluator, null);

        //linearized.add(0, new IMR.LABEL(entryLabel));
        //linearized.add(new IMR.JUMP (new IMR.NAME(exitLabel)));

        ImrLin.addCodeChunk(new LIN.CodeChunk(frame, linearized, entryLabel, exitLabel));
        funDefn.expr.accept(this, arg);
        return null;
    }

    /**
     * Expressions
     * 
     * Sprehodimo se po izrazih
     */
    @Override
    public Void visit(AST.Exprs exprs, Void arg) {
        for(AST.Expr expr : exprs.exprs) {
            expr.accept(this, arg);
        }
        return null;
    }

    /**
     * Let izraz
     * 
     * Sprehodimo se po definicijah in izrazih
     */
    @Override
    public Void visit(AST.LetExpr letExpr, Void arg) {
        //System.out.println("DEBUG: Processing let expression: " + letExpr.defns.size());
        for (AST.Defn defn : letExpr.defns) {
            //System.out.println("Definition: " + defn.getClass().getSimpleName() + " " + defn.name);
            defn.accept(this, arg);
        }
        letExpr.expr.accept(this, arg);
        return null;
    }
}