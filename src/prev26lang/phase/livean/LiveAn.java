package prev26lang.phase.livean;

import java.util.*;

import prev26lang.*;
import prev26lang.phase.*;
import prev26lang.phase.asmgen.ASM;
import prev26lang.phase.memory.*;

/**
 * LiveAn
 * 
 * Definira liveness analysis fazo v prevajalniku prev26.
 * 
 * Torej analizira koliko spremenljivk je živih v programu v nekem trenutku.
 */

public class LiveAn extends Phase {

    //Koda prideobljena iz asmgen faze
    private static Vector<ASM.ASMChunk> code;

    //Koda z funkcijami in njenimi množicami aktivnih spremenljivk pri določeni inštrukciji
    private static List<LivenessResult> res;

    /**
     * Konstruktor Liveness analysis faze
     */
    public LiveAn(Vector<ASM.ASMChunk> code) {
        super("livean");
        this.code = code;

    }


    /**
     * Liveness Result
     * 
     * Razred, ki vsebuje:
     *  Function - razred funkcija, ki opisuje funkcijo,
     *  Instruction - inštrukcije te funkcije,
     *  LiveOut - map, ki mapira inštrukcije s množico aktivnih tempov pri tej inštrukciji  
     */
    public static class LivenessResult {
        private final Function function;
        private final List<ASM.Instr> instructions;
        private final Map<ASM.Instr, Set<ASM.Register>> liveOut;

        /**
         * Konstruktor
         */
        public LivenessResult(Function fun, List<ASM.Instr> instructions, Map<ASM.Instr, Set<ASM.Register>> liveOut) {
            this.function = fun;
            this.instructions = instructions;
            this.liveOut = liveOut;
        }


        public Function getFun() {
            return function;
        }

        public List<ASM.Instr> getInstructions() {
            return instructions;
        }

        public Map<ASM.Instr, Set<ASM.Register>> getLiveOut() {
            return liveOut;
        }
    }

    /**
     * Vrne rezultat faze
     */
    public static List<LivenessResult> getLiveness() {
        return res;
    }

    /**
     * Razred Function
     * 
     * Atributi:
     *  - name -> ime funkcije
     *  - lines -> inštrukcije te funkcije
     *  - frame -> pripadajoči frame funkcije
     * 
     * Ti dva atributa sta potrebna pri dodeljevanju registrov
     *  - spilledSize -> koliko spremenljivk ne moramo dati v registre
     *  - numOfUsedRegisters -> število uporabljenih registrov
     */
    public static class Function {
        public final String name;
        public final List<ASM.Line> lines;
        public final MEM.Frame frame;
        public long spilledSize = 0;
        public long numberOfusedRegisters = 0;
        public List<ASM.Register> usedRegs = new ArrayList<>();
        //public final Map<String, ASM.Instr> labelToInstr;

        public Function(String name, List<ASM.Line> lines, MEM.Frame frame) {
            this.name = name;
            this.lines = lines;
            this.frame = frame;
            //labelToInstr = null;
        }

        @Override
        public String toString() {
            return String.format("%s" + "\nMemory frame:\n" + frame.toString() + "SpilledSize: %d\n" + "UsedRegisters: %d\n",name, spilledSize , numberOfusedRegisters);
        }
        
    }


    /**
     * Analiza življenske dobe spremenljivk
     */
    public static void livenessAnalaysis() {

        //Ustvari nov seznam Rezultatov funkcij
        List<LivenessResult> results = new ArrayList<>();
        List<Function> functions;

        //Sprehod po kodi
        for(ASM.ASMChunk ac : code) {
            //Ločimo posebaj vsako funkcijo in jih ločene vrnemo
            functions = separateFunctions(ac);
            

            //Za vsako funkcijo posebaj izračunaj liveness tempov in dodaj v seznam rešitev
            for(Function fun : functions) {
                
                LivenessResult r = chunkLiveness(fun);
                results.add(r);
                
            }
            //chunkLiveness(ac);
        }

        res = new ArrayList<>(results);

    }

    
    /**
     * Funkcija za ločevanje funkcij iz enega samega lista inštrukcij celotnega programa
     * 
     * Vrne seznam vseh funkcij ,ki so same po sebi ločene
     */
    public static List<Function> separateFunctions(ASM.ASMChunk chunk) {
        
        /**
         * Definiramo nov seznam funkcij,
         * Spremnljivko v kateri hranimo trenutno inštrukcijo
         * Ime funkcije
         */
        List<Function> functions = new ArrayList<>();
        List<ASM.Line> currLines = null;
        String funName = null;

        /**
         * Sprehajamo se čez program in ko pridemo do začetne labele
         *  funkcije začnem shranjevati ,v nov seznam instrukcij za to funkcijo,
         * inštrukcije. Ko pridemo do konca ret se shrani funkcija v seznam in ponastavi spremenljivki currLine in funName
         */
        for(ASM.Line line : chunk.lines) {

            if(line instanceof ASM.Label label) {
                if(chunk.stringToFrame.containsKey(label.getName()) ) {


                    funName = label.getName();
                    //MEM.Frame frame = chunk.stringToFrame.get(funName);
                    currLines = new ArrayList<ASM.Line>();
                }


            }

            if(currLines != null) {
                //System.out.println(line.toString());
                currLines.add(line);
            }

            if(line instanceof ASM.RetInstr) {
                //System.out.println("Ime funkcije: " + funName);
                MEM.Frame fr = chunk.stringToFrame.get(funName);
                Function fun = new Function(funName, currLines, fr);
                functions.add(fun);
                funName = null;
                currLines = null;
            }
        }

        return functions;
    }
    
    /**
     * Glavna funkcija za določanje livenessa spremenljivk
     */
    public static LivenessResult chunkLiveness(Function fun) {
        /**
         * Definicija uporabnih struktur:
         *  - in -> map ,ki mapira inštrukcijo z množico registrov ki gredo in
         *  - put -> map, ki mapira inštrukcijo z množico registrov ki gredo out
         * - labelToInst -> mapira labelo z inštrukcijo za skoke in branche
         *  - instructions -> inštrukcije, brez label
         */

        Map<ASM.Instr, Set<ASM.Register>> in = new HashMap<>();
        Map<ASM.Instr, Set<ASM.Register>> out = new HashMap<>();
        Map<String, ASM.Instr> labelToInstr = new HashMap<>();
        List<ASM.Instr> instructions = new ArrayList<>();

        
        /**
         * Ta for loop zapolni tabelo inštrukcij in labele odstrani in jih shrani v map labelToInstr,
         * Poleg tega pa ustvari za map in , out kluče oziroma točno te inštrukcije 
         */
        for(int i = 0; i < fun.lines.size(); i++) {
            ASM.Line line = fun.lines.get(i);

            if(line instanceof ASM.Label label) {

                for(int j = i + 1; j < fun.lines.size(); j++) {
                    if (fun.lines.get(j) instanceof ASM.Instr instr) {
                        labelToInstr.put(label.getName(), instr);
                        break;
                    }
                }
            } else if(line instanceof ASM.Instr instr) {
                instructions.add(instr);
                in.put(instr, new HashSet<>());
                out.put(instr, new HashSet<>());
            }
        }
        
        
        /**
         * Jedro algoritma (Fixed point iteracija)
         */
        boolean changed;

        do {
            //nastavi da ni bilo še nobenih sprememb
            changed = false;

            /**
             * For loop ,ki se pomika iz zadnje inštrukcije do prve v funkciji
             */
            for(int i = instructions.size() - 1; i >= 0; i--) {

                //Pridobimo inštrukcijo
                ASM.Instr instr = instructions.get(i);

                /**
                 * Ustvarimo tri nove hash sete:
                 *  inOld in outOld sta seta od inštrukcije,
                 *  
                 * newOut je novi hash set kateri nam bo povedal kaj je še živo takoj po tej inštrukciji
                 */
                Set<ASM.Register> inOld = new HashSet<>(in.get(instr));
                Set<ASM.Register> outOld = new HashSet<>(out.get(instr));
                Set<ASM.Register> newOut = new HashSet<>();

                //Vendar kdo je naslednik je odvisno tudi od tipa inštrukcije

                //Če je JINSTR
                if(instr instanceof ASM.JInstr jump) {
                    
                    //naslednik je fizično naslednja inštrukcija (fall-thorugh)
                    if(jump.rd != null) {
                      
                      if(i + 1 < instructions.size())  {
                        newOut.addAll(in.get(instructions.get(i+1)));
                      }

                    //edini naslednik je cilj skoka
                    } else if(jump.target != null) {
                        ASM.Instr target = labelToInstr.get(jump.target.getName());
                        
                        if(target != null) {
                            newOut.addAll(in.get(target));
                        }
                    }

                //Če je BInstr
                }else if(instr instanceof ASM.BInstr branch) {

                    /**
                     * Možna sta dva naslednika.
                     * 
                     * Če je  pogoj branch.taget izpolnjen je cilj brancha ali pa 
                     * fizično naslednja inštrukcija fall-though.
                     * Zato newOut dobi oba
                     */
                    ASM.Instr trueTarget = labelToInstr.get(branch.target.getName());
                    if(trueTarget != null) {
                        newOut.addAll(in.get(trueTarget));



                    }

                    if(i + 1 < instructions.size()) {
                        newOut.addAll(in.get(instructions.get(i+1)));
                    }

                //Drugače
                } else {
                    // Je le en sam naslednik, fizično naslednja inštrukcija
                    if(i +1 < instructions.size()) {
                        newOut.addAll(in.get(instructions.get(i+1)));
                    }
                }

                // Ustvarimo množico, ki nam pove kaj je živo pred to inštrukcijo
                Set<ASM.Register> newIn = new HashSet<>();

                //V množico dodamo vse registre v newIn iz use množice trenutne inštrukcije, razen ASM.ZERO
                for(ASM.Register reg : instr.use) {
                    if(reg != ASM.ZERO) {
                        newIn.add(reg);
                    }
                }

                /**
                 * Ustvari nov tempOut set, ki se napolni z newOut tempi
                 * 
                 * Odstranimo če je register ASM.ZERO
                 * odstranimo vse iz množice def, te inštrukcije
                 * in dodamo vse preostale v množico newInt
                 */
                Set<ASM.Register> tempOut = new HashSet<>(newOut);
                tempOut.removeIf(reg -> reg ==ASM.ZERO);
                tempOut.removeAll(instr.def);
                newIn.addAll(tempOut);

                // Množici in in out trenutne inštrukcije posodobimo z novima in and out množicama
                in.put(instr, newIn);
                out.put(instr, newOut);

                // Preverimo če je prišlo do spremembe v množicah
                if(!newIn.equals(inOld) || !newOut.equals(outOld)) {
                    changed = true;
                }

            }

            //To ponavljamo dokler ni nobenih sprememb
        } while(changed);

        /*
        for(ASM.Instr instr : instructions) {
            System.out.println(instr);
            System.out.println(" IN: " + in.get(instr));
            System.out.println(" OUT: " + out.get(instr));
        }*/

        return new LivenessResult(fun, instructions, out);
        
    }
}