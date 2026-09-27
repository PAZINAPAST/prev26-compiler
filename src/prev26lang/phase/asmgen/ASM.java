package prev26lang.phase.asmgen;

import java.util.*;
import prev26lang.common.logger.*;
import prev26lang.common.report.Report;
import prev26lang.phase.imrgen.IMR;
import prev26lang.phase.memory.MEM;

/**
 * Definiran je razred za lažje generiranje strojne RISC-V kode iz vmesne kode.
 * 
 * Definirane so vse inštrukcije ki so uporabne za programski jezik prev26.
 */

public class ASM {


    /**
     * Abstraktni razred uporabljen za poenotenje kode in lažje opreiranje z le to.
     */
    public static abstract class Line {
    }

    /**
     * Register
     * 
     * Regitser lahko definiramo kot dejanski register RSIC-V (npr: a0, ... ,a7, sp, fp, itd.)
     * Lahko pa definiramo kot temporary.
     * 
     */
    public static class Register {
        public final String name;
        public final IMR.TEMP temp;

        public Register(String name) {
            this.name = name;
            this.temp = null;
        }

        public Register(IMR.TEMP temp) {
            this.name = temp.temp.toString();
            this.temp = temp;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * equals vrne true če sta registra enaka in false če nista
         */
        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;

            if(obj == null || getClass() != obj.getClass()) {
                return false;
            }

            Register reg = (Register) obj;

            return Objects.equals(name, reg.name);
        }


        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }
    }

    /**
     * Labela
     */
    public static class Label extends Line {
        public final IMR.LABEL label;

        public Label(IMR.LABEL label) {
            this.label = label;
        }

        public String getName() {
            return label.label.name;
        }

        @Override
        public String toString() {
            return label.label.name + ":";
        }
    }

    /**
     * Abstraktni razred, ki nam definira okvir za vse RISC-V inštrukcije
     * 
     */
    public static abstract class Instr extends Line{
        /**
         * Vsaka inštrukcija ima svoj opcode.
         * 
         * vektorja def in use bosta prišla do izraza šele v liveness analizi.
         * 
         * Kar pa rabimo vedeti da:
         *  def - vsebuje registre/ dele inštrukcij ,ki jih inštrukcija definira
         *  use - vsebuje registre/ dele inštrukcij, ki jih inštrukcija uporabi
         */

        public final String opcode;
        public final Vector<Register> use = new Vector<>();
        public final Vector<Register> def = new Vector<>();

        public Instr(String opcode) {
            this.opcode = opcode;
        }

        protected void addUse(Register reg) {
            if(reg != null) {
                use.add(reg);
            }
        }

        protected void addDef(Register reg) {
            if(reg != null) {
                def.add(reg);
            }
        }
    }

    /**
     * Register instructions
     * 
     *  Insštrukcije, ki operirajo z vrednostmi v registrih rs1 in rs2,
     *  rezuktat operacije pa se zapiše v rd.
     */
    public static class RInstr extends Instr {
        public final Register rd;
        public final Register rs1;
        public final Register rs2;

        public RInstr(String opcode, Register rd, Register rs1, Register rs2) {
            super(opcode);
            this.rd = rd;
            this.rs1 = rs1;
            this.rs2 = rs2;
            addDef(rd);
            addUse(rs1);
            addUse(rs2);
        }

        @Override
        public String toString() {
            return String.format("%s %s, %s, %s", opcode, rd, rs1, rs2);
        }
    }

    /**
     * Immediate instructions
     * 
     *  Ukazi ki operirajo z enim registrom in eno long vrednostjo.
     *  Rezultat se zapiše v rd.
     */
    public static class IInstr extends Instr {
        public final Register rd;
        public final Register rs1;
        public final long imm;

        public IInstr(String opcode, Register rd, Register rs1, long imm) {
            super(opcode);
            this.rd = rd;
            this.rs1 = rs1;
            this.imm = imm;
            addDef(rd);
            addUse(rs1);
        }

        @Override
        public String toString() {
            return String.format("%s %s, %s, %d", opcode, rd, rs1, imm);
        }


    }

    /**
     * Load intruction
     * 
     * Ukaz ,ki registru rs1 doda vrednost imm in vrednost iz izračunanega naslova naloži v register rd.
     */
    public static class LoadInstr extends Instr {
        public final Register rd;
        public final Register rs1;
        public final long imm;

        public LoadInstr(String opcode, Register rd, Register rs1, long imm) {
            super(opcode);
            this.rd = rd;
            this.rs1 = rs1;
            this.imm = imm;
            addDef(rd);
            addUse(rs1);
        }

        @Override
        public String toString() {
            return String.format("%s %s, %d(%s)", opcode, rd, imm, rs1);
        }
    }

    /**
     * Store instruction
     * 
     * izračuna naslov z vrednostjo imm + vrednost v rs1 in shrani,
     *  vrednost ,ki je shranjena v rs2 na izračunani naslov
     */
    public static class StoreInstr extends Instr {
        public final Register rs2;
        public final Register rs1;
        public final long imm;

        public StoreInstr(String opcode, Register rs2, Register rs1, long imm) {
            super(opcode);
            this.rs2 = rs2;
            this.rs1 = rs1;
            this.imm = imm;
            addUse(rs2);
            addUse(rs1);
        }

        @Override
        public String toString() {
            return String.format("%s %s, %d(%s)", opcode, rs2, imm, rs1);
        }
    }

    /**
     * Branch instruction
     * 
     * Primerja rs1 in rs2 glede na pogoj, in če je pogoj izpolnjen skooči na labelo target,
     * drugače nadaljuje z naslednjo inštrukcijo (fall-through)
     */
    public static class BInstr extends Instr {
        public final Register rs1;
        public final Register rs2;
        public final Label target;

        public BInstr(String opcode, Register rs1, Register rs2, Label target) {
            super(opcode);
            this.rs1 = rs1;
            this.rs2 = rs2;
            this.target = target;
            addUse(rs1);
            addUse(rs2);
        }

        @Override
        public String toString() {
            return String.format("%s %s, %s, %s", opcode, rs1, rs2, target.label.label.name);
        }
    }

    /**
     * Jump instruction
     * 
     * Skoči na labelo target. Če je podan register rd, se vanj pred skokom shrani 
     * povratni naslov (naslednja inštrukcija) - to je uporabljeno pri klicu funkcij (jal).
     * Če je rd == null, gre za brezpogojni skok brez shranjevanja povratnega naslova (j).
     */
    public static class JInstr extends Instr {
        public final Label target;
        public final Register rd;

        public JInstr(String opcode, Register rd, Label target) {
            super(opcode);
            this.rd = rd;
            this.target = target;
            if(rd != null) {
                addDef(rd);
            }
        }


        @Override
        public String toString() {
            if(rd == null) {
                if(target == null) return opcode;
                return String.format("%s %s", opcode, target.label.label.name);
            } else {
                if(target == null) return opcode;
                return String.format("%s %s, %s", opcode, rd, target.label.label.name);
            }
        }
    }

    /**
     * Load Address instruction
     * 
     * V register rd naloži naslov na katerega kaže labela
     */
    public static class LAInstr extends Instr {
        public final Label label;
        public final Register rd;

        public LAInstr(Register rd, Label label) {
            super("la");
            this.rd = rd;
            this.label = label;
            addDef(rd);
        }

        @Override
        public String toString() {
            return String.format("la %s, %s", rd, label.label.label.name);
        }
    }

    /**
     * Return instruction
     * 
     * Skoči nazaj na naslov shranjen v ra
     */
    public static class RetInstr extends Instr {
        public RetInstr() {
            super("ret");
        }
        @Override
        public String toString() {
            return "ret";
        }
    }

    /**
     * Data line
     * 
     * je assembler directive, ki  pove assemblerju, da so od direktive naprej podatki,
     * ki imajo začetno vrednost in naj bodo shranjeni v podatkovnem delu programa.
     * 
     */
    public static class DataLine extends Line {

        public DataLine() {}

        @Override
        public String toString() {
            return ".data";
        }
    }

    /**
     * Direktiva ASCIIZ
     * 
     * je direktiva ,ki pomeni null-terminated string
     */
    public static class AsciiZ extends Line {
        public final String label;
        public final String str;

        public AsciiZ(String label, String str) {
            this.label = label;
            this.str = str;

        }

        @Override
        public String toString() {
            return label + ": .asciz \"" + str.replace("\"", "\\\"") + "\"";
        }
    }

    /**
     * Word direktiva
     * 
     * v pomnilnik zapiše eno ali več besed (words)
     */
    public static class Word extends Line {
        public final String label;
        public final long val;

        public Word(String label, long val) {
            this.label = label;
            this.val = val;

        }

        @Override
        public String toString() {
            return label + ": .word " + val;
        }
    }

    /**
     * Zero direktiva
     * 
     * rezervira oziroma napolni size bajtov z ničlami
     */
    public static class ZeroData extends Line {
        public final String label;
        public final long size;

        public ZeroData(String label, long size) {
            this.label = label;
            this.size = size;

        }

        @Override
        public String toString() {
            return label + ": .zero " + size;
        }
    }

    /**
     * Text direktiva
     * 
     * pravi, da se od tukaj naprej nahaja programska koda/inštrukcije
     */
    public static class TextLine extends Line {
        @Override
        public String toString() { return ".text"; }
    }


    /**
     * Globl direktiva
     * 
     * Pove assemblerju ,da je neka oznaka/labela globalno vidna,
     *  oziroma jo lahko uporabljajo tudi drugi objekti, moduli.
     */
    public static class GloblLine extends Line {
        public final String name;
        public GloblLine(String name) { this.name = name; }
        @Override
        public String toString() { return ".globl " + name; }
    }

    /**
     * ASMChunk
     * 
     * Razred ,ki prestavlja celotno kodo programa.
     */
    public static class ASMChunk {
        /**
         * Vsebuje vso strojno kodo in mapirane ima vsako ime funkcije z njeni frame-om
         */
        public final Vector<Line> lines = new Vector<>();
        public final Map<String, MEM.Frame> stringToFrame = new HashMap<>();

        public ASMChunk() {}
        
        public void addLine(Line line) {
            lines.add(line);
        }

        public void addInstr(Instr instr) {
            lines.add(instr);
        }

        public void addMapStringToFrame(String s, MEM.Frame f) {
            stringToFrame.put(s, f);
        }

    }

    /**
     * Raw je pa samo razred,ki vsebuje dejanski string.
     * Brez nikakeršnih modifikacij.
     */
    public static class Raw extends Line {
        private String text;

        public Raw(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    //Seznam ,ki nam bo pomagal pri reševanju problema shranjevanja registrov za klic funkcije
    public static final List<Register> COLORABLE = List.of(
        new Register("t0"), new Register("t1"), new Register("t2"), new Register("t3"),
        new Register("t4"), new Register("t5"), new Register("t6"),
        new Register("s1"), new Register("s2"), new Register("s3"), new Register("s4"),
        new Register("s5"), new Register("s6"), new Register("s7"), new Register("s8"),
        new Register("s9"), new Register("s10"), new Register("s11")
    );

    /*
    public static class LabelToFrame {
        public final Map<Label, MEM.Frame> ltf;

        public LabelToFrame() {
            
        }
    }*/

    public static final Register ZERO = new Register("zero");
    public static final Register RA = new Register("ra");
    public static final Register SP = new Register("sp");
    public static final Register GP = new Register("gp");
    public static final Register TP = new Register("tp");
    public static final Register FP = new Register("s0");
    public static final Register A0 = new Register("a0");
    public static final Register A1 = new Register("a1");
    public static final Register A2 = new Register("a2");
    public static final Register A3 = new Register("a3");
    public static final Register A4 = new Register("a4");
    public static final Register A5 = new Register("a5");
    public static final Register A6 = new Register("a6");
    public static final Register A7 = new Register("a7");

}