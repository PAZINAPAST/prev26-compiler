package prev26lang.phase.imrlin;

import java.util.*;
import prev26lang.phase.memory.*;
import prev26lang.phase.imrgen.*;
import prev26lang.common.report.*;

/**
 * Statement Evaluator
 * 
 * Sprehodi se bo drevesu in linearizira statemente in vrne linearizirano kodo
 */

public class StatementEvaluator implements IMR.Visitor<Vector<IMR.Stmt>, Object> {

    /**
     * IMR.STMTS
     * 
     * Ustvari nov vektor in se sprehodi po statementih in jih vse doda v lineariziran vektor statementov.
     * Vse statemente iterativno obišče,
     * vrne linearizirane statemente
     */
    @Override
    public Vector<IMR.Stmt> visit(IMR.STMTS seq , Object arg) {
        Vector<IMR.Stmt> stmts = new Vector<>();
        for(IMR.Stmt stmt : seq.stmts) {
            stmts.addAll(stmt.accept(this, null));
        }
        return stmts;
    }

    /**
     * IMR.MOVE
     * 
     * Ustvari nov stmts vektor,
     * 
     * Konstruira novv Expression evaluator in linearizira izraza dst in src.
     * Nato doda nov IMR.Move z lineariziranima izrazoma v vektor stmts
     */
    @Override
    public Vector<IMR.Stmt> visit(IMR.MOVE move, Object arg) {
        Vector<IMR.Stmt> stmts = new Vector<>();

        ExpressionEvaluator exprEvaluator = new ExpressionEvaluator();
        IMR.Expr dst = move.dst.accept(exprEvaluator, stmts);
        IMR.Expr src = move.src.accept(exprEvaluator, stmts);
        

        stmts.add(new IMR.MOVE(dst, src));
        return stmts;

    }

    /**
     * IMR.ESTMT
     * 
     * Ustvari no stmts vektor.
     * 
     * Konstruira nov ExpressionEvaluatro in linearizira izraze
     * 
     * Nato doda linearizirane izraze v nov IMR.ESTMT in vrne stmts
     */
    @Override
    public Vector<IMR.Stmt> visit(IMR.ESTMT estmt, Object arg) {
        Vector<IMR.Stmt> stmts = new Vector<>();

        ExpressionEvaluator exprEvaluator = new ExpressionEvaluator();
        IMR.Expr expr = estmt.expr.accept(exprEvaluator, stmts);

        stmts.add(new IMR.ESTMT(expr));
        return stmts;
    }

    /**
     * IMR.JUMP
     * 
     * Ustvari nov vector stmts
     * 
     * doda IMR.JUMP v stmts in ga vrne
     */
    @Override
    public Vector<IMR.Stmt> visit(IMR.JUMP jump, Object arg) {
        Vector<IMR.Stmt> stmts = new Vector<>();
        stmts.add(jump);
        return stmts;
    }

    /**
     * IMR.LABEL
     * 
     * ustvari nov vetor stmts in doda IMR.LABEL in vrne stmts
     */
    @Override
    public Vector<IMR.Stmt> visit(IMR.LABEL label, Object arg) {
        Vector<IMR.Stmt> stmts = new Vector<>();
        stmts.add(label);
        return stmts;
    }

    /**
     * IMR.CJUMP
     * 
     * Ustvari nov vektor stmts
     * 
     * Konstruira Expression Evaluator in linearizira pogoj, nato doda nov IMR.CJUMP,
     * z lineariziranim pogojem
     */
    @Override
    public Vector<IMR.Stmt> visit(IMR.CJUMP cjump, Object arg) {
        Vector<IMR.Stmt> stmts = new Vector<>();

        ExpressionEvaluator exprEvaluator = new ExpressionEvaluator();
        IMR.Expr cond = cjump.cond.accept(exprEvaluator, stmts);

        stmts.add(new IMR.CJUMP(cond, cjump.posAddr, cjump.negAddr));
        return stmts;
    }

    /**
     * NE morejo biti izrazi
     */
    @Override
    public Vector<IMR.Stmt> visit(IMR.BINOP binop, Object arg) {
        throw new Report.Error("BINOP cannot be a statement");
    }

    @Override
    public Vector<IMR.Stmt> visit(IMR.CALL binop, Object arg) {
        throw new Report.Error("CALL cannot be a statement");
    }
    @Override
    public Vector<IMR.Stmt> visit(IMR.CONST binop, Object arg) {
        throw new Report.Error("CONST cannot be a statement");
    }
    @Override
    public Vector<IMR.Stmt> visit(IMR.MEM1 binop, Object arg) {
        throw new Report.Error("MEM1 cannot be a statement");
    }
    @Override
    public Vector<IMR.Stmt> visit(IMR.MEM8 binop, Object arg) {
        throw new Report.Error("MEM8 cannot be a statement");
    }
    @Override
    public Vector<IMR.Stmt> visit(IMR.NAME binop, Object arg) {
        throw new Report.Error("NAME cannot be a statement");
    }
    @Override
    public Vector<IMR.Stmt> visit(IMR.SEXPR binop, Object arg) {
        throw new Report.Error("SEXPR cannot be a statement");
    }
    @Override
    public Vector<IMR.Stmt> visit(IMR.TEMP binop, Object arg) {
        throw new Report.Error("TEMP cannot be a statement");
    }
    @Override
    public Vector<IMR.Stmt> visit(IMR.UNOP binop, Object arg) {
        throw new Report.Error("UNOP cannot be a statement");
    }
}