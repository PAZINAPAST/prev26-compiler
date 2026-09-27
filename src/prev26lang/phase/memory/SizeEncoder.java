package prev26lang.phase.memory;

import java.util.*;
import prev26lang.phase.seman.*;
import prev26lang.common.report.*;


public class SizeEncoder extends TYP.FullVisitor<Long, Object> {

    private final Set<TYP.Type> cyc = new HashSet<>();

    @Override
    public Long visit(TYP.VoidType type, Object arg) {
        return 0L;

    }

    @Override
    public Long visit(TYP.IntType type, Object arg) {
        return 8L;
    }

    @Override
    public Long visit(TYP.CharType type, Object arg) {
        return 1L;
    }

    @Override
    public Long visit(TYP.BoolType type, Object arg) {
        return 1L;
    }

    @Override
    public Long visit(TYP.ArrType type, Object arg) {
        return type.numElems * type.elemType.accept(this, null);
    }

    @Override
    public Long visit(TYP.PtrType type, Object arg) {
        return 8L;
    }

    @Override
    public Long visit(TYP.StrType type, Object arg) {
        if(cyc.contains(type)) {
            throw new Report.Error("Cyclic struct definition.");
        }
        cyc.add(type);
        Long size = 0L;
        for (final TYP.Type ct : type.compTypes) {
            final Long compSize = ct.accept(this, arg);
            size += (compSize / 8L) * 8L + (compSize % 8L == 0L ? 0L : 8L);
        }
        cyc.remove(type);
        return size;
    }

    @Override
    public Long visit(TYP.UniType type, Object arg) {
        if(cyc.contains(type)) {
            throw new Report.Error("Cyclic union definition.");
        }
        cyc.add(type);
        Long size = 0L;
        for (final TYP.Type ct : type.compTypes) {
            final Long compSize = ct.accept(this, arg);
            size = Long.max(size, (compSize / 8L) * 8L + (compSize % 8L == 0L ? 0L : 8L));
        }
        cyc.remove(type);
        return size;
    }

    @Override
    public Long visit(TYP.NameType type, Object arg) {
        return type.type().accept(this, arg);
    }

    @Override
    public Long visit(TYP.FunType type, Object arg) {
        return 8L;
    }

}