package prev26lang.phase.regall;

import java.util.*;
import prev26lang.*;
import prev26lang.phase.*;
import prev26lang.phase.asmgen.ASM;
import prev26lang.phase.memory.*;
import prev26lang.phase.livean.*;

/**
 * RegAll
 * 
 * Definira fazo alokacije registrov. Barvanje grafa in dodeljevanje registrov.
 */

public class RegAll extends Phase {

    /**
     * Definirani elemnti:
     * 
     *  - K -> število možnih pobarvanih registrov
     *  - containsSpils -> boolean vrednost, ki pove ali je prišlo do prelivanja
     *  - tempIx -> osnovni indeks za dodajanje tempov
     *  - fnctionCode -> koda za funkcijo
     */
    private final int K;
    public boolean containsSpills = false;
    private int tempIx = 1000;
    public static List<ASM.Line> functionCode = new ArrayList<>();



    /**
     * Konstruktor,
     *  sprejme število barv in jih tudi nastavi
     */
    public RegAll(int K) {
        super("regall");
        this.K = K;
    }

    /**
     * Vrne kodo funkcij
     */
    public static List<ASM.Line> getFunctionCode() {
        return new ArrayList<>(functionCode);
    }

    /**
     * Funkcija ,ki prevede  ukaz, ki ima spilled vozlišče, v ukaze, ki popravijo ta preliv
     */
    public List<ASM.Line> translateInstr(ASM.Instr instr, Map<ASM.Register, Long> spillSlots) {

        //Nov seznam za novo kodo
        List<ASM.Line> lines = new ArrayList<>();

        //Slovar, ki bo mapiral registre in njihove zamenjave
        Map<ASM.Register, ASM.Register> useReplacement = new HashMap<>();

        /**
         * Sprehodimo se čez množico use v inštrukciji in če je spilled vozlišče v tej inštrukciji,
         * dodamo novo inštrukcijo in naloži vrednost iz temp polja v klicnem zapisu
         * in shrani v slovar vozlišče in njegov nov temporary
         */
        for(ASM.Register reg : instr.use) {
            if(spillSlots.containsKey(reg)) {
                ASM.Register tmp = newTempReg();
                useReplacement.put(reg, tmp);
                long off = spillSlots.get(reg);
                lines.add(new ASM.LoadInstr("ld", tmp, ASM.FP, off));
            }
        }

        //Slovar, ki mapira registre in njihove zamenjave v def
        Map<ASM.Register, ASM.Register> defReplacement = new HashMap<>();

        /**
         * Sprehodimo se čez množico def v inštrukciji in če je spilled vozlišče v tej inštrukciji,
         * naredi nov temp in ga shrani v slovar z vozliščem
         */
        for(ASM.Register reg : instr.def) {
            if(spillSlots.containsKey(reg)) {
                ASM.Register tmp = newTempReg();
                defReplacement.put(reg, tmp);
            }
        }

        //Zamenjamo registre v inštrukcijah
        ASM.Instr newInstr = replaceRegistersInInstr(instr, useReplacement, defReplacement);
        lines.add(newInstr);

        /**
         * Sprehodimo se po novih temp registrih in za vsak def replacement ustvarimo store ukaz,
         * za ta temp
         */
        for(Map.Entry<ASM.Register, ASM.Register> e : defReplacement.entrySet()) {
            ASM.Register tmp = e.getValue();
            long off = spillSlots.get(e.getKey());
            lines.add(new ASM.StoreInstr("sd", tmp, ASM.FP, off));

        }

        return lines;
    }

    /**
     * Register getter
     */
    public ASM.Register getReg(ASM.Register reg, Map<ASM.Register, ASM.Register> useMap, Map<ASM.Register, ASM.Register> defMap) {
        if(useMap.containsKey(reg)) {
            return useMap.get(reg);

        }

        if(defMap.containsKey(reg)) {
            return defMap.get(reg);
        }

        return reg;
    }

    /**
     * Funkcija zamenja registre v inštrukcijah  in vrne nove inštrukcije
     */
    public ASM.Instr replaceRegistersInInstr(ASM.Instr instr, Map<ASM.Register, ASM.Register> u, Map<ASM.Register, ASM.Register> d) {

        if(instr instanceof ASM.RInstr ri) {
            return new ASM.RInstr(ri.opcode, getReg(ri.rd, u, d), getReg(ri.rs1, u, d), getReg(ri.rs2, u,d));
        } else if(instr instanceof ASM.IInstr ii) {
            return new ASM.IInstr(ii.opcode, getReg(ii.rd, u, d), getReg(ii.rs1, u, d), ii.imm);
        } else if(instr instanceof ASM.LoadInstr li) {
            return new ASM.LoadInstr(li.opcode, getReg(li.rd, u, d), getReg(li.rs1, u, d), li.imm);
        } else if(instr instanceof ASM.StoreInstr si) {
            return new ASM.StoreInstr(si.opcode, getReg(si.rs2, u, d), getReg(si.rs1, u, d), si.imm);
        } else if(instr instanceof ASM.BInstr bi) {
            return new ASM.BInstr(bi.opcode, getReg(bi.rs1, u, d), getReg(bi.rs2, u, d), bi.target);
        } else if(instr instanceof ASM.JInstr ji) {
            return new ASM.JInstr(ji.opcode, ji.rd != null ? getReg(ji.rd, u ,d) : null, ji.target);
        } else if(instr instanceof ASM.LAInstr la) {
            return new ASM.LAInstr(getReg(la.rd,u ,d ), la.label);
        } else if(instr instanceof ASM.RetInstr) {
            return new ASM.RetInstr();
        } else {
            throw new RuntimeException("Nevem kje si si izmislil nov asm ukaz.");
        }

    }

    /**
     * Ustvari nov temp Register
     */
    private ASM.Register newTempReg() {
        return new ASM.Register("tmp"+ (tempIx++));
    }

    /**
     * Če pride do spillov, moramo funkcijo ponovno napisati, da se 
     * znebimo tega spilla.
     * 
     * Kot parameter dobimo:
     *  - results -> funkcije iz liveness faze
     *  - spillsed -> seznam vozlišč ki so bili preliti
     */
    public void rewriteFunctions(LiveAn.LivenessResult result, List<Node> spillsed) {
        /**
         * spillSlots -> map, ki mapira registre z njihovo velikostjo
         * fun -> funkcija, ki jo bomo prepisali
         */
        Map<ASM.Register, Long> spillSlots = new HashMap<>();
        LiveAn.Function fun = result.getFun();

        /**
         * Nastavimo:
         *  prevSpillSize ->če je že prišlo do spillov, kako velik je
         *  offset -> offset kamor lahko dodelimo še začasne spremenljivke v frame
         */
        long prevSpillSize = fun.spilledSize;
        long offset = -(fun.frame.locsSize + 16 + prevSpillSize);

        // Dodamo spilled vozlišča in njihove offsete v spilSlots map
        for(Node node : spillsed) {
            offset -= 8;
            spillSlots.put(node.register, offset);
        }

        /**
         * Ustvarimo nov seznam za novo kodo
         */
        List<ASM.Line> oldLines = fun.lines;
        List<ASM.Line> newLines = new ArrayList<>();

        /**
         * Funkciji ponastavimo velikost temporaries polja v klicnem zapisu
         */
        fun.spilledSize = prevSpillSize + (spillsed.size() * 8L);
        //spilledSize = fun.frame.locsSize + spillSize + fun.frame.argsSize + 16;


        /**
         * Sprehodimo se po stari kodi in ponovno napišemo kodo
         */
        for(ASM.Line line : oldLines) {
            if(line instanceof ASM.Instr instr) {
                List<ASM.Line> nl = translateInstr(instr, spillSlots);
                newLines.addAll(nl);
            } else {
                newLines.add(line);
            }
        }

        // Zamnejamo staro kodo za novo napisano kodo
        fun.lines.clear();
        fun.lines.addAll(newLines);


    }

    /**
     * Barvanje grafa
     * 
     * Kot parameter dobi:
     *  - graf
     *  - spillsOut -> seznam vozlišč ,ki se prelijejo
     */
    public boolean colorGraph(Graph graph, List<Node> spillsOut) {
        
        /**
         * Remaining -> množica preostalihvozlišč
         * Degree -> map, ki mapira vozlišča z številom njihovih sosedov
         */
        Set<Node> remaining = new HashSet<>(graph.nodes);
        Map<Node, Integer> degree = new HashMap<>();

        //gre čez remaining vozlišča in jih shrani v degree map
        for(Node node : remaining) {
            degree.put(node, node.degree());
        }

        // Ustvarimo stack
        Deque<Node> stack = new ArrayDeque<>();

        //POnavljamo dokler ni množica vozlišč povsem prazna
        while(!remaining.isEmpty()) {

            Node low = null;

            //Najdemo vozlišče z najnižjo stopnjo
            for(Node n : remaining) {
                if(degree.get(n) < K && !isFixedReg(n.register) && !n.preColored) {
                    low = n;
                    break;
                }
            }

            /**
             * Če smo najšli vozlišče z najmanjšo stopnjo:
             *  - odstranimo vozlišče iz množice remaining
             *  - vozlišče pushnemo na stack
             *  - vozlišče odstranimo iz grafa in iz vseh povezav od sosedov
             * 
             * Če nismo našli najmanjše stopnje:
             *  - definiramo vozlišče spill
             *  - poiščemo vozlišče z največjo stopnjo
             *  - vozlišče označimo kot potencialni preliv in ga odstranimo iz množice remaining in iz grafa
             *  - potisnemo ga na skladd
             */
            if( low != null) {
                remaining.remove(low);
                stack.push(low);

                for (Node neigh : low.neighbours) {
                    if(remaining.contains(neigh)) {
                        degree.put(neigh, degree.get(neigh) - 1);
                    }
                }
            } else {
                Node spill = null;
                int maxDeg = -1;

                for (Node n : remaining) {
                    int d = degree.get(n);

                    if(!isFixedReg(n.register) && !n.preColored &&d > maxDeg) {
                        maxDeg = d;
                        spill = n;
                    }
                }

                if(spill == null) break;

                spill.potentialSpill = true;
                remaining.remove(spill);
                stack.push(spill);

                for (Node neigh : spill.neighbours) {
                    if(remaining.contains(neigh)) {
                        degree.put(neigh, degree.get(neigh) - 1);
                    }
                }
            }



        }

        // Ponavljamo dokler stack ni prazen
        while(!stack.isEmpty()) {
            // Popnemo iz stacka vozlišče
            Node node = stack.pop();
            // tavela uporabljenih barv
            boolean[] used =  new boolean[Math.max(K, ASM.COLORABLE.size())];

            /**
             * Sprehodimo se čez sosede in če je sosed že pobarvan,
             * v tabeli used nastavimo true
             */
            for(Node neigh : node.neighbours) {
                if( neigh.color != -1) {
                    used[neigh.color] = true;
                }

            }
            
            /**
             * Nato se sprehodimo čez barve in tista barva,ki še ni uporabljena je,
             * sedaj uporabljena za barvanje trnutnega vozlišča
             */
            for(int c = 0; c < K ;c ++) {
                if(!used[c]) {
                    node.color = c;
                    break;
                }
            }

            /**
             * Če vozlišča ni bilo mogoče pobarvati in ni fiksen register,
             * vozlišče označimo kot dejanski preliv in ga dodamo v seznam sillsOut 
             */
            if(node.color == -1 && !(isFixedReg(node.register))) {
                node.actualSpill = true;
                spillsOut.add(node);

            }
        }

            /*
            System.out.println("Spilled registers:");
                for(Node n : spills) {
                    System.out.println(n.toString());
                }
            */

        //Vrnemo boolean ali je prišlo do preliva ali ne
        return !spillsOut.isEmpty();
        
    }

    /**
     * Ko smo uspešno pobarvali ali prišli do 1000 iteracij zamenjamo Tempe za fizične registre
     */
    private void replaceWithPysical(LiveAn.Function fun, Graph graph) {
        /**
         * Registers -> seznam registrov
         * usedregister -> množica že uporabljenih registrov
         * fp -> frame pointer
         * rv -> return addres
         */
        //List<ASM.Register> registers = new ArrayList<>();
        Set<ASM.Register> usedregister = new HashSet<>();
        MEM.Temp fp = fun.frame.FP;
        MEM.Temp rv = fun.frame.RV;

        //Ustvarimo map ,za barvanje
        Map<ASM.Register, ASM.Register> colorMap = new HashMap<>();

        // Če je register v seznamu ga shranimo v colorMap
        for (ASM.Register physReg : List.of(
                ASM.A0, ASM.A1, ASM.A2, ASM.A3, ASM.A4, ASM.A5, ASM.A6, ASM.A7,
                ASM.RA, ASM.SP, ASM.ZERO)) {
            colorMap.put(physReg, physReg);
        }

        
        // Sprehodimo se po vozliščih grafa
        for(Node node : graph.nodes) {
            
            
            // Če je registru ime fp -> ga shranimo kot FP
            if(node.register.name.equals(fp.toString()) /*|| node.register.name.equals("T2")*/) {
                colorMap.put(node.register, ASM.FP);
                continue;
            }

            // Če je registru ime rv -> ga shranimo kot A=
            if(node.register.name.equals(rv.toString())) {
                colorMap.put(node.register, ASM.A0);
                continue;
            }

            if(node.preColored) continue;

            // Če je vozlišče pobarvano in ni fixed register,
            // ga mapirav nov neuporabljen rei+gister in shrani ko uporabljen register
            if(node.color != -1 && !isFixedReg(node.register)) {
                ASM.Register physReg = ASM.COLORABLE.get(node.color % ASM.COLORABLE.size());;
                colorMap.put(node.register, physReg);
                usedregister.add(physReg);
                //System.out.println("Added: " + physReg.toString());

                // shranimo uporaben register v klicni zapis funkcije
                if(physReg.name.startsWith("s") && !fun.usedRegs.contains(physReg)) {
                    fun.usedRegs.add(physReg);
                }
            }
        }


        /**
         * Sprehodimo se po programu in določimo registre inštrukcija,
         * ter jih dodamo v seznam newLines
         */
        List<ASM.Line> newLines = new ArrayList<>();
        for(ASM.Line line : fun.lines) {
            if (line instanceof ASM.Instr instr) {
                newLines.add(replaceRegistersInInstr(instr, colorMap, colorMap));
            } else {
                newLines.add(line);
            }
        }

        /**
         * V funkcijo shranimo število uporabljenih registrov in dodamo inštrukcije z fizičnimi registri
         */
        fun.numberOfusedRegisters = usedregister.size() * 8;
        fun.lines.clear();
        fun.lines.addAll(newLines);

    }

    /**
     * Pobarva graf za vsako funkcijo in ji dodeli registre in doda prolog in epilog
     */
    public void allocate(List<LiveAn.LivenessResult> results) {

        //Loop čez vse funkcije
        for(int i = 0; i < results.size(); i++) {

            //Pridobimo funkcij
            LiveAn.LivenessResult res = results.get(i);

            boolean changed;
            int iterations = 0;
            //long spilledSize = 0;

            //barvanje grafa
            do {
                
                /**
                 * Skonstruira nov graf in ga zgradi
                 */
                Graph graph = new Graph(K);
                graph.build(res.getInstructions(), res.getLiveOut(), res.getFun());

                /**
                 * Ustvari nov list, kamor skranjuje potencialna vozlišča ki se prelijejo.
                 * Pobarva graf in sporoči ali je prišlo do preliva
                 */
                List<Node> spillsed = new ArrayList<>();
                boolean spilled = colorGraph(graph, spillsed);
                
                
                //System.out.println("Spilled registers:");
                //for(Node n : spillsed) {
                    //System.out.println(n.toString());
                //}

                /**
                 * Če je bilo poizkusov barvanja grafov enako 1000:
                 *  - zamenja tempe v funkciji z fizičnimi registri
                 * 
                 * Če ni preliva:
                 *  - zamenja tempe z fizičnimi registri in nastavi  false za spremenjen
                 * 
                 * Če pride do preliva in ni še 1000 iteracija:
                 *  - ponovno napišemo funkcijo in jo ponovno poženemo skozi liveness analysis,
                 *  nastavi nov rezultat na tem indeksu in nastavi da je prišlo do spremembe
                 */
                if(iterations >= 1000) {
                   
                    replaceWithPysical(res.getFun(), graph);
                    break;
                }

                if(!spilled) {
                    replaceWithPysical(res.getFun(), graph);
                    changed = false;
                } else {
                    
                    rewriteFunctions(res, spillsed);

                    res = LiveAn.chunkLiveness(res.getFun());

                    results.set(i, res);
                    changed = true;
                }

                iterations++;

            // Ponavlja se dokler ne pride do 1000 iteracije ali pa ni več sprememb
            } while(changed);

            
            /**
             * Pridobimo funkcijo z alociranimi registri ter dodamo funkicji še prolog in epilog
             * 
             * Končano funkcijo dodamo v seznam function code
             */
            LiveAn.Function fun = res.getFun();
            addPrologueAndEpilogue(fun, fun.frame);
            //System.out.println("Register allocation:");
            for(ASM.Line line : fun.lines) {
                //System.out.println(line.toString());
                functionCode.add(line);
            }
            //System.out.println(fun.toString());

            
            
        }


    }
    /*
    public void printOutCallingConvention(LiveAn.Function function) {
        MEM.Frame frame = function.frame;
        
        if(frame.depth > 0)
    }*/

    private void addPrologueAndEpilogue(LiveAn.Function fun, MEM.Frame frame) {
        List<ASM.Line> oldLines = fun.lines;
        List<ASM.Line> newLines = new ArrayList<>();
        boolean labela = true;
        long totalFrameSize = frame.size + fun.spilledSize + fun.numberOfusedRegisters;
        
        //System.out.println("Spilled frame size : " + totalFrameSize );

        for(ASM.Line line : oldLines) {
            if(line instanceof ASM.RetInstr) {

                for(int i = 0; i < fun.usedRegs.size(); i++) {
                    ASM.Register reg = fun.usedRegs.get(i);
                    long off = frame.argsSize + fun.spilledSize + 8 * i;
                    newLines.add(new ASM.LoadInstr("ld", reg, ASM.SP, off));
                }

                newLines.add(new ASM.LoadInstr("ld", ASM.RA, ASM.SP,  (frame.argsSize + fun.spilledSize + fun.numberOfusedRegisters ) + 8));
                newLines.add(new ASM.LoadInstr("ld", ASM.FP, ASM.SP, (frame.argsSize + fun.spilledSize + fun.numberOfusedRegisters )));
                newLines.add(new ASM.IInstr("addi", ASM.SP, ASM.SP, totalFrameSize));
                newLines.add(line);
            } else if(labela) {
                newLines.add(line);
                newLines.add(new ASM.IInstr("addi", ASM.SP, ASM.SP, -totalFrameSize));
                newLines.add(new ASM.StoreInstr("sd", ASM.RA, ASM.SP, (frame.argsSize + fun.spilledSize + fun.numberOfusedRegisters ) + 8));
                newLines.add(new ASM.StoreInstr("sd", ASM.FP, ASM.SP, (frame.argsSize + fun.spilledSize + fun.numberOfusedRegisters ) ));
                newLines.add(new ASM.IInstr("addi", ASM.FP, ASM.SP, totalFrameSize));
                for(int i = 0; i < fun.usedRegs.size(); i++) {
                    ASM.Register reg = fun.usedRegs.get(i);
                    long off = frame.argsSize + fun.spilledSize + 8 * i;
                    newLines.add(new ASM.StoreInstr("sd", reg, ASM.SP, off));
                }
                labela = false;

            } else {
                newLines.add(line);
            }
        }

        fun.lines.clear();

    
        fun.lines.addAll(newLines);
    }


    private boolean isFixedReg(ASM.Register reg) {
        String name = reg.name;

        return name.equals("zero") || name.equals("ra") || name.equals("sp") || name.equals("gp") || name.equals("tp") || name.startsWith("a") || name.startsWith("s") ;
    }
}