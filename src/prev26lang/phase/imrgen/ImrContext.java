package prev26lang.phase.imrgen;

import prev26lang.phase.memory.MEM;

public class ImrContext {
    public final MEM.Frame frame;
    public final Long depth;
    public IMR.Expr lastExpr;

    ImrContext(MEM.Frame frame, Long depth) {
        this.frame = frame;
        this.depth = depth;
    }
}