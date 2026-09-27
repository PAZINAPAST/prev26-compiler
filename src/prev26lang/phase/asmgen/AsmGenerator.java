package prev26lang.phase.asmgen;

import java.util.*;
import prev26lang.phase.imrgen.*;
import prev26lang.phase.imrlin.*;
import prev26lang.common.report.*;

/**
 * Assembly Generator.
 * 
 * Je generatorn ki sprejme data chunks in Code chunks ,ki so napisani v intermediate kodi,
 * in zgenerira iz te vmesne kode strojno kodo.
 */

public class AsmGenerator {

    private final Vector<LIN.DataChunk> dataChunks;
    private final Vector<LIN.CodeChunk> codeChunks;

    //Preslikava tepov v registre
    private final HashMap<IMR.TEMP, ASM.Register> tempsRegs = new HashMap<>();
    //privtae final HashMap<String, MEM.Frame> stringToFrame = new HashMap<>();
    private int nextTempId = 0;


    /**
     * Kosntruktor generatorja strojne kode
     */
    public AsmGenerator(Vector<LIN.DataChunk> dataChunks, Vector<LIN.CodeChunk> codeChunks) {
        this.dataChunks = dataChunks;
        this.codeChunks = codeChunks;
    }

    /**
     * Funkcija ki zgenerira strojno kodo
     */
    public ASM.ASMChunk generate() {

        // ustvari nov ASMChunk
        ASM.ASMChunk asm = new ASM.ASMChunk();

        //Definira podatkovni del programa
        asm.addLine(new ASM.DataLine());
        for(LIN.DataChunk dc : dataChunks) {
            if(dc.init != null) {
                asm.addLine(new ASM.AsciiZ(dc.label.name, dc.init));
            } else {
                asm.addLine(new ASM.ZeroData(dc.label.name, dc.size));
            }
        }

        // Definira polje programske kode/executable kode
        asm.addLine(new ASM.TextLine());
        //asm.addLine(new ASM.GloblLine("main"));
        for(LIN.CodeChunk cc : codeChunks) {
            generateFunction(cc, asm);
        }
        /*
        System.out.println("Temp to register maping: \n");
        for(Map.Entry<IMR.TEMP, ASM.Register> entry : tempsRegs.entrySet()) {
            IMR.TEMP tem = entry.getKey();
            ASM.Register reg = entry.getValue();

            System.out.println(tem.toString() + " = " + reg.toString());
        }*/


        return asm;
    }

    /**
     * Funkcija generira strojno kodo za funkcijo napisano v IMR kodi
     */
    public void generateFunction(LIN.CodeChunk cc, ASM.ASMChunk asm) {
        //Počistimo vse tempe, ki so bili morda generirani pri prejšnjem generiranju funkcije
        tempsRegs.clear();
        nextTempId = 0;

        //asm.addLine(new ASM.Label(new IMR.LABEL(cc.entryLabel)));
        
        //Sprehodimo se po statementih funkcije in vsakgea posebaj prevedemo
        for(IMR.Stmt stmt : cc.getStmts()) {
            tranlsateStamenets(stmt, asm);
        }
        
        //Mapiramo ime funkcije z njenim frameom v code ckuhk asm
        asm.addMapStringToFrame(cc.frame.label.name, cc.frame);
        
        // na koncu dodamo še return
        asm.addInstr(new ASM.RetInstr());

    }


    /**
     * Funkija ki prevede IMR stavke v strojno kodo.
     */
    public void tranlsateStamenets(IMR.Stmt stmt, ASM.ASMChunk asm) {

        /**
         * IMR.LABEL -> ASM.LABEL
         */
        if(stmt instanceof IMR.LABEL l) {
            asm.addLine(new ASM.Label(l));
        } 
        /**
         * IMR.CJUMP -> 
         *  - najprej prevedemo pogojni izraz -> ASM.Register cond
         *  - zgeneriramo labelo za naslov začetka kode za primer ko je pogoj negativen -> ASM.Label neg
         *  - ustvarimo ASM.BInstr
         */
        else if (stmt instanceof IMR.CJUMP cjump) {
            ASM.Register cond = translateExpr(cjump.cond, asm);
            ASM.Label neg = new ASM.Label(new IMR.LABEL(((IMR.NAME) cjump.negAddr).label));
            asm.addInstr(new ASM.BInstr("beq", cond, ASM.ZERO, neg));

        } 
        /**
         * IMR.ESTMT -> translateEpxr(estmt.expr)
         */
        else if(stmt instanceof IMR.ESTMT estmt) {
            translateExpr(estmt.expr, asm);
        }
        /**
         * IMR.JUMP ->
         *  - ASM.Label l = naslov kamor skoči jump
         *  - ASM.Jinstr 
         */ 
        else if (stmt instanceof IMR.JUMP j) {
            ASM.Label l = new ASM.Label( new IMR.LABEL(((IMR.NAME) j.addr).label));
            asm.addInstr(new ASM.JInstr("j", null, l));
        }
        /**
         * IMR.MOVE ->
         *  - evalviramo src expression
         *  - nato pa če move.dst == :
         *      - IMR.TEMP - > dst = getReg(tem); ASM.RInstr
         *      - IMR.MEM1 -> dst = tranlsateExpr(mem.addr); ASM.StoreInstr(opcode = sb)   
         *      - IMR.MEM8 -> dst = tranlsateExpr(mem.addr); ASM.StoreInstr(opcode = sd)   
         * 
         *  - drugače error
         */ 
        else if (stmt instanceof IMR.MOVE move) {
            ASM.Register src = translateExpr(move.src, asm);

            if(move.dst instanceof IMR.TEMP tem) {
                ASM.Register dst = getReg(tem);
                asm.addInstr(new ASM.RInstr("add", dst, src, ASM.ZERO));
            } else if(move.dst instanceof IMR.MEM1 mem) {
                ASM.Register dst = translateExpr(mem.addr, asm);
                asm.addInstr(new ASM.StoreInstr("sb", src, dst, 0));
            } else if(move.dst instanceof IMR.MEM8 mem) {
                ASM.Register dst = translateExpr(mem.addr, asm);
                asm.addInstr(new ASM.StoreInstr("sd", src, dst, 0));
            } else {
                throw new Report.Error("Unsupported Move destination.");
            }
        } else {
            throw new Report.Error("Ne more obstajat drugacne vrste statement.");
        }
    }

    /**
     * Funkcija ,ki prevede IMR izraze v strojno kod in vrača registre
     */
    public ASM.Register translateExpr(IMR.Expr expr, ASM.ASMChunk asm) {

        /**
         * IMR.BINOP -> 
         *  - fst, snd = translateExpr()
         *  - dst = newTempReg()
         *  - generateBinop()
         *  return dst
         */

        if(expr instanceof IMR.BINOP binop) {
            ASM.Register fst = translateExpr(binop.fstExpr, asm);
            ASM.Register snd = translateExpr(binop.sndExpr, asm);
            ASM.Register dst = newTempReg();
            generateBinop(binop.oper, dst, fst, snd, asm);
            return dst;   
        }
        /**
         *  IMR.CALL ->
         *      - shrani argumente na stack nad SP(konec klicnega zapisa trenutne funkcije)
         *      - naredi jump instruction na začetek klicane funkcije in ustavri nov temp kamor se rezultat klicane funkcije shrani
         */ 
        else if (expr instanceof IMR.CALL call) {

            //DEefinicija caller-saved registrov 
            ASM.Register[] argRegs = new ASM.Register[] {ASM.A0, ASM.A1, ASM.A2, ASM.A3, ASM.A4, ASM.A5, ASM.A6, ASM.A7};
            for(int i = 0; i < call.args.size(); i++) {
                IMR.Expr arg = call.args.get(i);
                long offset = call.offs.get(i);

                //Zapiše inštrukcije za shranjevanje argumentov na stack 
                ASM.Register argReg = translateExpr(arg, asm);
                asm.addInstr(new ASM.StoreInstr("sd", argReg, ASM.SP, offset));

                // Prvih 8 argumentov se zapiše tudi v a0-a7 (potrebno za ExtFunDefn klice po RISC-V ABI;
                // pri DefFunDefn klicih je to odvečno, ker parametre beremo s sklada, ne iz teh registrov)
                if(i < argRegs.length) {
                    asm.addInstr(new ASM.RInstr("add", argRegs[i], argReg, ASM.ZERO));
                }

            }

            //Naredi jump inštrukcijo na začetek klicane funkcije
            ASM.Instr jalInstr;
            if(call.addr instanceof IMR.NAME name) {
                jalInstr = new ASM.JInstr("jal", ASM.RA, new ASM.Label(new IMR.LABEL(name.label)));

            } else {
                ASM.Register addr = translateExpr(call.addr, asm);
                jalInstr = new ASM.IInstr("jalr", ASM.RA, addr, 0);
            }

            //Povemo da smo sedaj v drugi funkciji in niso stari registri več aktualni
            jalInstr.addDef(ASM.A0);
            jalInstr.addDef(ASM.A1);
            jalInstr.addDef(ASM.A2);
            jalInstr.addDef(ASM.A3);
            jalInstr.addDef(ASM.A4);
            jalInstr.addDef(ASM.A5);
            jalInstr.addDef(ASM.A6);
            jalInstr.addDef(ASM.A7);
            jalInstr.addDef(new ASM.Register("t0"));
            jalInstr.addDef(new ASM.Register("t1"));
            jalInstr.addDef(new ASM.Register("t2"));
            jalInstr.addDef(new ASM.Register("t3"));
            jalInstr.addDef(new ASM.Register("t4"));
            jalInstr.addDef(new ASM.Register("t5"));
            jalInstr.addDef(new ASM.Register("t6"));
                        
            asm.addInstr(jalInstr);

            //Ustvari register kamor se rezultat klicane funkcije zapiše/shrani
            ASM.Register ret = newTempReg();
            asm.addInstr(new ASM.RInstr("add", ret, ASM.A0, ASM.ZERO));
            return ret;
        } 
        /**
         * IMR.CONST -> ASM.IInstr (shrni vrednost v register)
         */
        else if(expr instanceof IMR.CONST con) {
            ASM.Register tmp = newTempReg();
            asm.addInstr(new ASM.IInstr("addi", tmp, ASM.ZERO, con.value));
            return tmp;
        } 
        /**
         * IMR.MEM1 -> 
         *  - translate mem.addr
         *  - ustvari nov temp in vanj naloži byte
         */
        else if(expr instanceof IMR.MEM1 mem) {
            ASM.Register addr = translateExpr(mem.addr, asm);
            ASM.Register tmp = newTempReg();
            asm.addInstr(new ASM.LoadInstr("lb", tmp, addr, 0));
            return tmp;
        } 
        /**
         * IMR.MEM1 -> 
         *  - translate mem.addr
         *  - ustvari nov temp in vanj naloži 8 bytov
         */
        else if (expr instanceof IMR.MEM8 mem) {
            ASM.Register addr = translateExpr(mem.addr, asm);
            ASM.Register tmp = newTempReg();
            asm.addInstr(new ASM.LoadInstr("ld", tmp, addr, 0));
            return tmp;
        }
        /**
         * IMR.NAME -> ASM.LAInstr (nalozi v tmp spremenljivko označeno z imenon)
         */
        else if( expr instanceof IMR.NAME name) {
            ASM.Register tmp = newTempReg();
            asm.addInstr(new ASM.LAInstr(tmp, new ASM.Label(new IMR.LABEL(name.label))));
            return tmp;
        }
        /**
         * IMR.SEXPR -> translate statemente in vrne razrešeni expression
         */
        else if(expr instanceof IMR.SEXPR st) {
            tranlsateStamenets(st.stmt, asm);
            return translateExpr(st.expr, asm);
        } 
        /**
         * IMR.TEMP -> ustvari nov temp
         */
        else if(expr instanceof IMR.TEMP temp) {
            return getReg(temp);
        } 
        /**
         * IMR.UNOP -> 
         *  - translate subExpr
         *  - ustvari nov temp za dst
         *  - Če je operator:
         *      NOT: shiftamo immediate za 1 in shranimo v dst
         *      NEG: odštejemo 0 da dobimo negativno vrednost
         *      ADD: ne naredimo nič
         */
        else if(expr instanceof IMR.UNOP unop) {
            ASM.Register sub = translateExpr(unop.subExpr, asm);
            ASM.Register dst = newTempReg();

            if(unop.oper == IMR.UNOP.Oper.NOT) {
                asm.addInstr(new ASM.IInstr("sltiu", dst, sub, 1));
            } else if(unop.oper == IMR.UNOP.Oper.NEG) {
                asm.addInstr(new ASM.RInstr("sub", dst, ASM.ZERO, sub));
            }

            return dst;
        } else {
            throw new Report.Error("This type of expression doesnt exist.");
        }
    }


    /**
     * Funkcija za prevajanje binarnih operacij v strojno kodo
     */
    public void generateBinop(IMR.BINOP.Oper oper, ASM.Register  dst, ASM.Register fst, ASM.Register snd, ASM.ASMChunk asm) {
        switch(oper) {
            case ADD -> asm.addInstr(new ASM.RInstr("add", dst, fst, snd));
            case SUB -> asm.addInstr(new ASM.RInstr("sub", dst, fst, snd));
            case MUL -> asm.addInstr(new ASM.RInstr("mul", dst, fst,snd));
            case DIV -> asm.addInstr(new ASM.RInstr("div", dst, fst,snd));
            case MOD -> asm.addInstr(new ASM.RInstr("rem", dst, fst, snd));
            case AND -> asm.addInstr(new ASM.RInstr("and", dst, fst, snd));
            case OR -> asm.addInstr(new ASM.RInstr("or", dst, fst, snd));
            case EQU -> {
                asm.addInstr(new ASM.RInstr("xor", dst, fst, snd));
                asm.addInstr(new ASM.IInstr("sltiu", dst, dst, 1));
            }
            case NEQ -> {
                asm.addInstr(new ASM.RInstr("xor", dst, fst, snd));
                asm.addInstr(new ASM.RInstr("sltu", dst, ASM.ZERO, dst));
            }
            case LTH -> asm.addInstr(new ASM.RInstr("slt", dst, fst, snd));
            case GTH -> asm.addInstr(new ASM.RInstr("slt", dst, snd, fst));
            case LEQ -> {
                asm.addInstr(new ASM.RInstr("slt", dst, snd, fst));
                asm.addInstr(new ASM.IInstr("xori", dst, dst, 1));
            }
            case GEQ -> {
                asm.addInstr(new ASM.RInstr("slt", dst, fst, snd));
                asm.addInstr(new ASM.IInstr("xori", dst, dst, 1));
            }
        }
    }

    /**
     * Pridobi iskani register TEMP, če ne obstaja naredi novega
     */
    private ASM.Register getReg(IMR.TEMP temp) {
        return tempsRegs.computeIfAbsent(temp, t -> new ASM.Register(temp));
    }

    /**
     * Ustvari nov temp
     */
    private ASM.Register newTempReg() {
        return new ASM.Register("tmp" + (nextTempId++));
    }

}