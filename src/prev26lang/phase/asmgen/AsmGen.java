package prev26lang.phase.asmgen;

import java.util.*;

import prev26lang.*;
import prev26lang.phase.*;
import prev26lang.phase.abstr.AST;
import prev26lang.phase.asmgen.ASM;
import prev26lang.phase.imrgen.IMR;
import prev26lang.phase.imrlin.*;
import prev26lang.phase.memory.*;

/**
 * AsmGen
 * 
 * Definira fazo generiranje strojne kode v prevajalniku prev26.
 */

public class AsmGen extends Phase {

    /**
     * Vektor kjer je shranjen programi/programi
     */
    private final static Vector<ASM.ASMChunk> asmChunks = new Vector<ASM.ASMChunk>();

    AsmGenerator asmGen;
    private final Vector<LIN.DataChunk> dc;
    private final Vector<LIN.CodeChunk> cc;

    /**
     * Konstruktor faze
     * 
     * Ustvari:
     *  fazo
     *  dc - podatkovna koda
     *  cc - program
     *  asmGen - generator strojne kode
     *  doda program v vektor asmChunks
     */
    public AsmGen(Vector<LIN.DataChunk> dc, Vector<LIN.CodeChunk> cc) {
        super("asmgen");
        this.dc = dc;
        this.cc = cc;
        asmGen = new AsmGenerator(dc, cc);
        ASM.ASMChunk ac = asmGen.generate();
        addAsmChunk(ac);
    }

    // doda na novo generirano strojno kodo v vektor ,ki shranjuje vse porgrame
    public static void addAsmChunk(ASM.ASMChunk asmch) {
        asmChunks.add(asmch);
    }

    // vrne kodo ,ki definir podatke
    public static List<ASM.Line> getDataCode() {
        List<ASM.Line> dataCode = new ArrayList<>();

        for(ASM.ASMChunk asmChunk : asmChunks) {
            for(ASM.Line line : asmChunk.lines) {
                if(line instanceof ASM.DataLine || line instanceof ASM.AsciiZ || line instanceof ASM.ZeroData) {
                    dataCode.add(line);
                } 
            }
        }

        return dataCode;
    }

    // vrne programe napisane v strojni kodi iz vektorja
    public static Vector<ASM.ASMChunk> getAsmChunks() {
        return new Vector<ASM.ASMChunk>(asmChunks);
    }

    // izpiše ves program
    public void log() {
        for(ASM.ASMChunk ac : asmChunks) {
            System.out.println("Assembly Chunk:");
            for(ASM.Line line : ac.lines) {
                System.out.println(line.toString());
                //System.out.println();
            }
        }
    }
}