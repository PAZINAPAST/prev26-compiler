package prev26lang.phase.imrlin;

import java.util.*;
import prev26lang.phase.memory.*;
import prev26lang.phase.imrgen.*;
import prev26lang.common.report.*;

/**
 * Interpreter for linearized IMR code (for testing purposes only).
 */
public class Interpreter {

    private Scanner scanner = new Scanner(System.in);
    private boolean debug = false;
    private Random random = new Random();

    // Memory simulation (byte‑addressable)
    private HashMap<Long, Byte> memory = new HashMap<>();
    // Temporary variables (registers)
    private HashMap<MEM.Temp, Long> temps = new HashMap<>();
    // Data chunk labels -> start address
    private HashMap<MEM.Label, Long> dataMemLabels = new HashMap<>();
    // Code labels -> statement index (for jumps)
    private HashMap<MEM.Label, Integer> jumpMemLabels = new HashMap<>();
    // Function entry labels -> code chunk
    private HashMap<MEM.Label, LIN.CodeChunk> callMemLabels = new HashMap<>();

    // Special temporary registers
    private MEM.Temp SP = new MEM.Temp();
    private MEM.Temp FP = new MEM.Temp();
    private MEM.Temp RV = new MEM.Temp();
    private MEM.Temp HP = new MEM.Temp();

    public Interpreter(Vector<LIN.DataChunk> dataChunks, Vector<LIN.CodeChunk> codeChunks) {
        // Initialise stack and heap pointers
        tempST(SP, 0x7FFFFFFFFFFFFFF8L);
        tempST(HP, 0x2000000000000000L);

        // Allocate data chunks (global variables and string constants)
        for (LIN.DataChunk dataChunk : dataChunks) {
            long addr = tempLD(HP);
            dataMemLabels.put(dataChunk.label, addr);
            if (dataChunk.init != null) {
                for (int i = 0; i < dataChunk.init.length(); i++) {
                    memST(addr + i, (long) dataChunk.init.charAt(i));
                }
            }
            tempST(HP, addr + dataChunk.size);
        }

        // Build jump label map and function map
        for (LIN.CodeChunk codeChunk : codeChunks) {
            callMemLabels.put(codeChunk.frame.label, codeChunk);
            Vector<IMR.Stmt> stmts = codeChunk.getStmts();
            for (int i = 0; i < stmts.size(); i++) {
                if (stmts.get(i) instanceof IMR.LABEL) {
                    jumpMemLabels.put(((IMR.LABEL) stmts.get(i)).label, i);
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // Memory access (8 bytes, little‑endian)
    // ----------------------------------------------------------------------
    private void memST(Long address, Long value) { memST(address, value, debug); }
    private void memST(Long address, Long value, boolean dbg) {
        if (dbg) System.out.printf("### [%d] <- %d\n", address, value);
        for (int b = 0; b < 8; b++) {
            memory.put(address + b, (byte) (value & 0xFF));
            value >>= 8;
        }
    }

    private Long memLD(Long address) { return memLD(address, debug); }
    private Long memLD(Long address, boolean dbg) {
        long val = 0;
        for (int b = 7; b >= 0; b--) {
            Byte bval = memory.get(address + b);
            if (bval == null) bval = (byte) (random.nextLong() & 0xFF);
            val = (val << 8) | (bval & 0xFFL);
        }
        if (dbg) System.out.printf("### %d <- [%d]\n", val, address);
        return val;
    }

    // ----------------------------------------------------------------------
    // Temporary variables (registers)
    // ----------------------------------------------------------------------
    private void tempST(MEM.Temp temp, Long value) { tempST(temp, value, debug); }
    private void tempST(MEM.Temp temp, Long value, boolean dbg) {
        temps.put(temp, value);
        if (dbg) {
            if (temp == SP) System.out.printf("### SP <- %d\n", value);
            else if (temp == FP) System.out.printf("### FP <- %d\n", value);
            else if (temp == RV) System.out.printf("### RV <- %d\n", value);
            else if (temp == HP) System.out.printf("### HP <- %d\n", value);
            else System.out.printf("### T%d <- %d\n", temp.temp, value);
        }
    }

    private Long tempLD(MEM.Temp temp) { return tempLD(temp, debug); }
    private Long tempLD(MEM.Temp temp, boolean dbg) {
        Long val = temps.get(temp);
        if (val == null) throw new Report.Error("Uninitialized temporary " + temp);
        if (dbg) {
            if (temp == SP) System.out.printf("### %d <- SP\n", val);
            else if (temp == FP) System.out.printf("### %d <- FP\n", val);
            else if (temp == RV) System.out.printf("### %d <- RV\n", val);
            else if (temp == HP) System.out.printf("### %d <- HP\n", val);
            else System.out.printf("### %d <- T%d\n", val, temp.temp);
        }
        return val;
    }

    // ----------------------------------------------------------------------
    // Expression evaluator
    // ----------------------------------------------------------------------
    private class ExprInterpreter implements IMR.Visitor<Long, Object> {
        @Override
        public Long visit(IMR.BINOP bin, Object arg) {
            Long left = bin.fstExpr.accept(this, null);
            Long right = bin.sndExpr.accept(this, null);
            switch (bin.oper) {
                case OR:  return (left != 0 || right != 0) ? 1L : 0L;
                case AND: return (left != 0 && right != 0) ? 1L : 0L;
                case EQU: return left.equals(right) ? 1L : 0L;
                case NEQ: return !left.equals(right) ? 1L : 0L;
                case LTH: return left < right ? 1L : 0L;
                case GTH: return left > right ? 1L : 0L;
                case LEQ: return left <= right ? 1L : 0L;
                case GEQ: return left >= right ? 1L : 0L;
                case ADD: return left + right;
                case SUB: return left - right;
                case MUL: return left * right;
                case DIV: return left / right;
                case MOD: return left % right;
                default: throw new Report.InternalError();
            }
        }
        @Override
        public Long visit(IMR.CALL call, Object arg) {
            throw new Report.Error("CALL should not appear inside expressions");
        }
        @Override
        public Long visit(IMR.CONST cnst, Object arg) { return cnst.value; }
        @Override
        public Long visit(IMR.MEM1 mem, Object arg) {
            long addr = mem.addr.accept(this, null);
            return memLD(addr) & 0xFFL;
        }
        @Override
        public Long visit(IMR.MEM8 mem, Object arg) {
            long addr = mem.addr.accept(this, null);
            return memLD(addr);
        }
        @Override
        public Long visit(IMR.NAME name, Object arg) {
            Long addr = dataMemLabels.get(name.label);
            if (addr == null) throw new Report.Error("Undefined data label: " + name.label.name);
            return addr;
        }
        @Override
        public Long visit(IMR.SEXPR sexpr, Object arg) {
            // Execute statement, then evaluate expression
            sexpr.stmt.accept(new StmtInterpreter(), null);
            return sexpr.expr.accept(this, null);
        }
        @Override
        public Long visit(IMR.TEMP temp, Object arg) { return tempLD(temp.temp); }
        @Override
        public Long visit(IMR.UNOP unop, Object arg) {
            Long sub = unop.subExpr.accept(this, null);
            switch (unop.oper) {
                case NOT: return (sub == 0) ? 1L : 0L;
                case NEG: return -sub;
                default: throw new Report.InternalError();
            }
        }
    }

    // ----------------------------------------------------------------------
    // Statement interpreter
    // ----------------------------------------------------------------------
    private class StmtInterpreter implements IMR.Visitor<MEM.Label, Object> {
        @Override
        public MEM.Label visit(IMR.CJUMP cjump, Object arg) {
            Long cond = cjump.cond.accept(new ExprInterpreter(), null);
            IMR.Expr target = (cond != 0) ? cjump.posAddr : cjump.negAddr;
            return ((IMR.NAME) target).label;
        }
        @Override
        public MEM.Label visit(IMR.ESTMT estmt, Object arg) {
            if (estmt.expr instanceof IMR.CALL) {
                call((IMR.CALL) estmt.expr);
            } else {
                estmt.expr.accept(new ExprInterpreter(), null);
            }
            return null;
        }
        @Override
        public MEM.Label visit(IMR.JUMP jump, Object arg) {
            return ((IMR.NAME) jump.addr).label;
        }
        @Override
        public MEM.Label visit(IMR.LABEL label, Object arg) { return null; }
        @Override
        public MEM.Label visit(IMR.MOVE move, Object arg) {
            Long srcVal;
            if (move.src instanceof IMR.CALL) {
                call((IMR.CALL) move.src);
                srcVal = tempLD(RV);
            } else {
                srcVal = move.src.accept(new ExprInterpreter(), null);
            }
            if (move.dst instanceof IMR.MEM1) {
                Long addr = ((IMR.MEM1) move.dst).addr.accept(new ExprInterpreter(), null);
                memST(addr, srcVal & 0xFFL);
            } else if (move.dst instanceof IMR.MEM8) {
                Long addr = ((IMR.MEM8) move.dst).addr.accept(new ExprInterpreter(), null);
                memST(addr, srcVal);
            } else if (move.dst instanceof IMR.TEMP) {
                tempST(((IMR.TEMP) move.dst).temp, srcVal);
            } else {
                throw new Report.Error("Invalid MOVE destination");
            }
            return null;
        }
        @Override
        public MEM.Label visit(IMR.STMTS stmts, Object arg) {
            throw new Report.Error("STMTS should have been flattened");
        }
    }

    // ----------------------------------------------------------------------
    // Function call handling (including built‑ins)
    // ----------------------------------------------------------------------
    private void call(IMR.CALL call) {
		// Push arguments onto the stack (outgoing area)
		long offset = 0;
		for (IMR.Expr arg : call.args) {
			Long argVal = arg.accept(new ExprInterpreter(), null);
			memST(tempLD(SP) + offset, argVal);
			offset += 8;
		}
		// Check for built‑in functions
		String name = ((IMR.NAME) call.addr).label.name;
		switch (name) {
			case "_new":
				// size is the first argument (at offset 0)
				Long size = memLD(tempLD(SP) + 0);
				Long addr = tempLD(HP);
				tempST(HP, addr + size);
				tempST(RV, addr);          // result in RV
				return;
			case "_del":
				tempST(RV, 0L);
				return;
			case "_exit":
				System.exit(0);
			case "_putint":
				Long val = memLD(tempLD(SP) + 0);
				System.out.print(val);
				tempST(RV, 0L);
				return;
			case "_getint":
				Long input = scanner.nextLong();
				tempST(RV, input);
				return;
			case "_putchar":
				Long ch = memLD(tempLD(SP) + 0);
				System.out.print((char) (ch & 0xFF));
				tempST(RV, 0L);
				return;
			case "_getchar":
				try {
					int c = System.in.read();
					tempST(RV, (long) c);
				} catch (Exception e) {
					tempST(RV, -1L);
				}
				return;
			default:
				// User function call
				funCall(call.addr);
		}
	}

    // Execute a user function
    private void funCall(IMR.Expr funAddr) {
        MEM.Label entryLabel = ((IMR.NAME) funAddr).label;
        LIN.CodeChunk chunk = callMemLabels.get(entryLabel);
        if (chunk == null) throw new Report.Error("Function " + entryLabel.name + " not found");
        MEM.Frame frame = chunk.frame;
        Vector<IMR.Stmt> stmts = chunk.getStmts();

        // Prologue: save state, create new frame
        HashMap<MEM.Temp, Long> savedTemps = temps;
        temps = new HashMap<>(temps);
        MEM.Temp savedFP = FP;
        MEM.Temp savedRV = RV;
        FP = frame.FP;
        RV = frame.RV;
        tempST(frame.FP, tempLD(SP));
        tempST(SP, tempLD(SP) - frame.size);

        int stmtOffset = jumpMemLabels.get(chunk.entryLabel);
        MEM.Label currentLabel = null;

        // Execute body
        while (currentLabel != chunk.exitLabel) {
            if (currentLabel != null) {
                Integer off = jumpMemLabels.get(currentLabel);
                if (off == null) throw new Report.Error("Label " + currentLabel.name + " not found");
                stmtOffset = off;
            }
            IMR.Stmt stmt = stmts.get(stmtOffset);
            currentLabel = stmt.accept(new StmtInterpreter(), null);
            stmtOffset++;
            if (stmtOffset > 1000000) throw new Report.Error("Possible infinite loop");
        }

        // Epilogue: restore state
        Long result = tempLD(frame.RV);
        memST(tempLD(frame.FP), result);   // store result in caller's frame (optional)
        tempST(SP, tempLD(SP) + frame.size);
        FP = savedFP;
        RV = savedRV;
        temps = savedTemps;
        // Put result in RV for caller
        tempST(RV, result);
    }

    // ----------------------------------------------------------------------
    // Public entry point
    // ----------------------------------------------------------------------
    public long run(String entryLabelName) {
        for (MEM.Label label : callMemLabels.keySet()) {
            if (label.name.equals(entryLabelName)) {
                funCall(new IMR.NAME(label));
                return tempLD(RV);
            }
        }
        throw new Report.Error("No entry function " + entryLabelName);
    }
}