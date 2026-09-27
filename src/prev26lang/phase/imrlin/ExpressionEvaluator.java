package prev26lang.phase.imrlin;

import java.util.*;
import prev26lang.phase.memory.*;
import prev26lang.phase.imrgen.*;
import prev26lang.common.report.*;

public class ExpressionEvaluator implements IMR.Visitor<IMR.Expr, Vector<IMR.Stmt>> {

    @Override
    public IMR.Expr visit(IMR.CONST constant, Vector<IMR.Stmt> stmts) {
        return constant;
    }

    @Override
    public IMR.Expr visit(IMR.NAME name, Vector<IMR.Stmt> stmts) {
        return name;
    }

    @Override
    public IMR.Expr visit(IMR.TEMP temp, Vector<IMR.Stmt> stmts) {
        return temp;
    }

    @Override
    public IMR.Expr visit(IMR.MEM1 mem, Vector<IMR.Stmt> stmts) {
        IMR.Expr addr = mem.addr.accept(this, stmts);
        return new IMR.MEM1(addr);
    }

    @Override
    public IMR.Expr visit(IMR.MEM8 mem, Vector<IMR.Stmt> stmts) {
        IMR.Expr addr = mem.addr.accept(this, stmts);
        return new IMR.MEM8(addr);
    }

    @Override
    public IMR.Expr visit(IMR.BINOP binop, Vector<IMR.Stmt> stmts) {
        IMR.Expr left = binop.fstExpr.accept(this, stmts);
        IMR.Expr right = binop.sndExpr.accept(this, stmts);
        return new IMR.BINOP(binop.oper, left, right);
    }

    @Override
    public IMR.Expr visit(IMR.UNOP un, Vector<IMR.Stmt> stmts) {
        IMR.Expr expr = un.subExpr.accept(this, stmts);
        return new IMR.UNOP(un.oper, expr);
    }

    @Override
    public IMR.Expr visit(IMR.CALL call, Vector<IMR.Stmt> stmts) {
        MEM.Temp temp = new MEM.Temp();
        IMR.Expr addr = call.addr.accept(this, stmts);
        Vector<IMR.Expr> args = new Vector<>();

        for(IMR.Expr arg : call.args) {
            args.add(arg.accept(this, stmts));
        }

        IMR.CALL newCall = new IMR.CALL(addr, call.offs, args);
        stmts.add(new IMR.MOVE(new IMR.TEMP(temp), newCall));
        return new IMR.TEMP(temp);

    }

    @Override
    public IMR.Expr visit(IMR.SEXPR sexpr, Vector<IMR.Stmt> stmts) {

        StatementEvaluator stmtEvaluator = new StatementEvaluator();
        stmts.addAll(sexpr.stmt.accept(stmtEvaluator, null));

        return sexpr.expr.accept(this, stmts);
    }
}