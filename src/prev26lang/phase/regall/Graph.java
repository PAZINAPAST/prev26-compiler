package prev26lang.phase.regall;

import java.util.*;
import prev26lang.*;
import prev26lang.phase.*;
import prev26lang.phase.asmgen.ASM;
import prev26lang.phase.memory.*;
import prev26lang.phase.livean.*;

/**
 * Graph
 * 
 * Graf ,ki je uporabljen za barvanje in posledično za dodeljevanje registrov
 */

public class Graph{

    /**
     * Osnovni elemnti:
     *  - nodeMap -> mapira register na vozlišče
     *  - nodes -> množica vozlišč
     *  - numOfColors -> število barv, število razpoložljivih registrov
     * 
     */
    public Map<ASM.Register, Node> nodeMap;
    public Set<Node> nodes;
    public int numOfColors;

    /**
     * Konstruktor grafa
     * 
     * Ustvari nove set in map in sprejme število registrov
     */
    public Graph(int k) {
        this.nodes = new HashSet<Node>();
        this.nodeMap = new HashMap<ASM.Register, Node>();
        this.numOfColors = k;
    }

    /**
     * Node getter
     */
    public Node getNode(ASM.Register reg) {
        return nodeMap.get(reg);
    }

    /**
     * Node adder
     */
    public Node addNode(ASM.Register reg) {
        Node checkNode = nodeMap.get(reg);
        if(checkNode == null) {
            checkNode = new Node(reg);
            int idx = ASM.COLORABLE.indexOf(reg);
            if(idx != -1) {
                checkNode.color = idx;
                checkNode.preColored = true;
            }
            nodeMap.put(reg, checkNode);
            nodes.add(checkNode);
        }

        return checkNode;
    }

    /**
     * Node remover
     */
    public void removeNode(ASM.Register reg) {
        Node node = nodeMap.get(reg);

        if(node != null) {
            nodes.remove(node);

            for(Node n : new ArrayList<>(node.neighbours)) {
                n.removeEdge(node);
            }
            node.neighbours.clear();
        }
    }

    /**
     * Edge adder
     */
    public void addEdge(ASM.Register reg1, ASM.Register reg2) {
        if(reg1 == reg2) return;

        addEdge(getNode(reg1), getNode(reg2));
    }

    public void addEdge(Node n1, Node n2) {
        n1.addEdge(n2);
        n2.addEdge(n1);
    }


    /**
     * Graph builder
     * 
     * Kot parameter dobi:
     *  - instructions -> seznam inštrukcij
     *  - liveOut -> map inštrukcije in seta ki so še šivi po izvedbi
     */
    public void build(List<ASM.Instr> instructions, Map<ASM.Instr,  Set<ASM.Register>> liveOut, LiveAn.Function fun) {

        /**
         * Gremo čez inštrukcije.
         * 
         * V graf dodamo registre iz množice use, def in liveOut
         */
        for(ASM.Instr instr : instructions) {
            for (ASM.Register reg : instr.use) {
                addNode(reg);
                //System.out.println("Instruction: " + instr + ", liveOut size: " + regs.size());
            }

            for(ASM.Register reg : instr.def) {
                addNode(reg);
            }

            for(ASM.Register reg : liveOut.get(instr)) {
                addNode(reg);
            }

            Set<ASM.Register> outSet = liveOut.get(instr);
            
            if(outSet != null) {
                for(ASM.Register reg : outSet) addNode(reg);
            }
        }

        /**
         * Označevanje FP in RV vozlišč kot precolored, saj drugače pri spillih lahko pride do napake,
         * in se njihova dejnska vrednost ne zapiše v  v spill slot
         */
        ASM.Register fpReg = new ASM.Register(fun.frame.FP.toString());
        ASM.Register rvReg = new ASM.Register(fun.frame.RV.toString());
        Node fpNode = getNode(fpReg);
        Node rvNode = getNode(rvReg);
        if(fpNode != null) fpNode.preColored = true;
        if(rvNode != null) rvNode.preColored = true;

        /**
         * Dodamo povezave med vozlišči/registri
         */
        for(ASM.Instr instr : instructions) {
            Set<ASM.Register> outSet = liveOut.get(instr);
            if(outSet == null) continue;

            for(ASM.Register d : instr.def) {
                for(ASM.Register t : outSet) {
                    if(!d.equals(t)) {
                        addEdge(d,t);
                    }
                }
            }

        }
    }

    /**
     * Nodes getter
     */
    public Set<Node> getnodes() {
        return nodes;
    }

    /**
     * Register to node map getter
     */
    public Map<ASM.Register, Node> getNodeMap() {
        return nodeMap;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Node n : nodes) {
            sb.append(n).append(" -> ").append(n.neighbours).append("\n");
        }

        return sb.toString();
    }
}