package prev26lang.phase.imrlin;

import java.util.*;
import prev26lang.phase.*;
import prev26lang.phase.abstr.*;
import prev26lang.phase.memory.*;
import prev26lang.phase.imrgen.*;
import prev26lang.phase.imrlin.*;
//import prev26lang.common.logger.*;

public class ImrLin extends Phase {

    // All data chunks of the program

    private final static Vector<LIN.DataChunk> dataChunks = new Vector<LIN.DataChunk>();

    // all code chunks of the program
    private final static Vector<LIN.CodeChunk> codeChunks = new Vector<LIN.CodeChunk>();

    private final AST.Node root;

    public ImrLin(AST.Node root) {
        super("imrlin");
        this.root = root;
    }

    // Adds data chunk to a collection of data chunks.
    public static void addDataChunk(LIN.DataChunk dataChunk) {
        dataChunks.add(dataChunk);
    }

    // Adds code chunk to a collection of code chunks.
    public static void addCodeChunk(LIN.CodeChunk codeChunk) {
        codeChunks.add(codeChunk);
    }

    // Returns all data chunks of the program.
    public static Vector<LIN.DataChunk> getDataChunks() {
        return new Vector<LIN.DataChunk>(dataChunks);
    }

    // Returns all code chunks of the program.
    public static Vector<LIN.CodeChunk> getCodeChunks() {
        return new Vector<LIN.CodeChunk>(codeChunks);
    }

    public void run() {
        ChunkGenerator chunkGenerator = new ChunkGenerator();
        root.accept(chunkGenerator, null);

    }

    public void printChunks() {
        System.out.println("\n=== DATA CHUNKS ===");
        for (LIN.DataChunk dc : dataChunks) {
            System.out.printf("  %s: size=%d", dc.label.name, dc.size);
            if (dc.init != null) System.out.printf(", init=\"%s\"", escape(dc.init));
            System.out.println();
        }
        System.out.println("\n=== CODE CHUNKS ===");
        for (LIN.CodeChunk cc : codeChunks) {
            System.out.printf("  Function %s (entry=%s, exit=%s):\n",
                    cc.frame.label.name, cc.entryLabel.name, cc.exitLabel.name);
            for (IMR.Stmt stmt : cc.getStmts()) {
                System.out.print("    ");
                // Use a simple string representation of the statement
                System.out.println(stmtToString(stmt));
            }
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String stmtToString(IMR.Stmt stmt) {
        if (stmt instanceof IMR.LABEL) {
            return ((IMR.LABEL) stmt).label.name + ":";
        } else if (stmt instanceof IMR.MOVE) {
            IMR.MOVE move = (IMR.MOVE) stmt;
            return "MOVE " + exprToString(move.dst) + " := " + exprToString(move.src);
        } else if (stmt instanceof IMR.ESTMT) {
            IMR.ESTMT estmt = (IMR.ESTMT) stmt;
            return "ESTMT " + exprToString(estmt.expr);
        } else if (stmt instanceof IMR.CJUMP) {
            IMR.CJUMP cj = (IMR.CJUMP) stmt;
            return "CJUMP " + exprToString(cj.cond) + " ? " + labelName(cj.posAddr) + " : " + labelName(cj.negAddr);
        } else if (stmt instanceof IMR.JUMP) {
            IMR.JUMP jump = (IMR.JUMP) stmt;
            return "JUMP " + labelName(jump.addr);
        } else {
            return stmt.toString();
        }
    }

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

    private String labelName(IMR.Expr expr) {
        if (expr instanceof IMR.NAME) return ((IMR.NAME) expr).label.name;
        return expr.toString();
    }
/*
    public void log() {
        
        if (xmlLogger == null) return;
        for (LIN.DataChunk dataChunk : dataChunks) {
            xmlLogger.begElement("datachunk");
            xmlLogger.addAttribute("label", dataChunk.label.name);
            xmlLogger.addAttribute("size", Long.toString(dataChunk.size));
            if (dataChunk.init != null)
                xmlLogger.addAttribute("init", dataChunk.init);
            xmlLogger.endElement();
        }
        for (LIN.CodeChunk codeChunk : codeChunks) {
            xmlLogger.begElement("codechunk");
            xmlLogger.addAttribute("prologueLabel", codeChunk.frame.label.name);
            xmlLogger.addAttribute("entryLabel", codeChunk.entryLabel.name);
            xmlLogger.addAttribute("exitLabel", codeChunk.exitLabel.name);
            // log frame
            xmlLogger.begElement("frame");
            xmlLogger.addAttribute("label", codeChunk.frame.label.name);
            xmlLogger.addAttribute("depth", Long.toString(codeChunk.frame.depth));
            xmlLogger.addAttribute("locssize", Long.toString(codeChunk.frame.locsSize));
            xmlLogger.addAttribute("argsize", Long.toString(codeChunk.frame.argsSize));
            xmlLogger.addAttribute("size", Long.toString(codeChunk.frame.size));
            xmlLogger.addAttribute("FP", codeChunk.frame.FP.toString());
            xmlLogger.addAttribute("RV", codeChunk.frame.RV.toString());
            xmlLogger.endElement();
            for (IMR.Stmt stmt : codeChunk.getStmts()) {
                xmlLogger.begElement("stmt");
                stmt.log(xmlLogger);
                xmlLogger.endElement();
            }
            xmlLogger.endElement();
    
        }
    }
    */
    
}