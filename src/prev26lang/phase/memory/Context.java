package prev26lang.phase.memory;

/**
 * Context
 * 
 * Pomožni razred za lažje sledenje in shranjevanje konteksta pri dodeljevanju frame-ov funkcijam,
 * predvsem vgnezdenim funkcijam.
 * 
 * Kontest vsebuje:
 *  offset - za določanje premika za doseg željene spremenljivke
 *  depth - globina funkcije/kolikokrat je funkcija vgnezdena
 */

public class Context {
    long offset;
    long depth;
    boolean isParams;
    boolean isLoc;
    boolean isStruct;
    boolean isUnion;
    boolean isCall;
    long argsSize;
    long locSize;


    Context() {
        offset = 0;
        depth = -1;
        isParams = false;
        isLoc = false;
        isStruct = false;
        isUnion = false;
        isCall = false;
        argsSize = 0;
        locSize = 0;
    } 

    Context(Context ctx) {
        offset = ctx.offset;
        depth = ctx.depth;
        isParams = ctx.isParams;
        isLoc = ctx.isLoc;
        isStruct = ctx.isStruct;
        isUnion = ctx.isUnion;
        isCall = ctx.isCall;
        argsSize = ctx.argsSize;
        locSize = ctx.locSize;
    }

    public String toString() {
        return "offset: " + offset + ", depth: " + depth + ", isParams: " + isParams + ", isLoc: "+ isLoc + ", argsSize: " + argsSize + ", locSize: " + locSize;
    }
}