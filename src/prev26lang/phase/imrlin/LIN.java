package prev26lang.phase.imrlin;

import java.util.*;
import prev26lang.phase.memory.*;
import prev26lang.phase.imrgen.*;

public class LIN {

    public static abstract class Chunk {}


    public static class DataChunk extends Chunk {

        // The Label where data is placed.
        public final MEM.Label label;

        //size of the data
        public final long size;

        // initital value
        public final String init;

        public DataChunk(MEM.AbsAccess access) {
            this.label = access.label;
            this.size = access.size;
            this.init = access.init;
        }
    }


    public static class CodeChunk extends Chunk {

        //function frame
        public final MEM.Frame frame;

        // statements of the function
        private final Vector<IMR.Stmt> stmts;

        //The function's body entry label, i.e., the label the prologue jumps to.
        public final MEM.Label entryLabel;

        //The function's body exit label, i.e., the label at which the epilogue starts.
        public final MEM.Label exitLabel;


        public CodeChunk(MEM.Frame frame, Vector<IMR.Stmt> stmts, MEM.Label entryLabel, MEM.Label exitLabel) {
            this.frame = frame;
            this.stmts = new Vector<IMR.Stmt>(stmts);
            this.entryLabel = entryLabel;
            this.exitLabel = exitLabel;
        }

        public Vector<IMR.Stmt> getStmts() {
            return new Vector<IMR.Stmt>(stmts);
        }
    }
}