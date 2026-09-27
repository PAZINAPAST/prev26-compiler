package prev26lang.phase.regall;

import java.util.*;
import prev26lang.*;
import prev26lang.phase.*;
import prev26lang.phase.asmgen.ASM;
import prev26lang.phase.livean.*;

/**
 * Node
 * 
 * Definira razred vozlišča v grafu ,ki ga bomo pobarvali
 */

public class Node {

    /**
     * Osnovni elementi:
     *  - register
     *  - color
     *  - potentialSpill
     *  - actualSpill
     *  - neighbours -> množica sosedov vozlišča
     * 
     */
    public ASM.Register register;
    public int color = -1;
    public boolean potentialSpill = false;
    public boolean actualSpill = false;
    public boolean preColored = false;
    public HashSet<Node> neighbours;


    /**
     * Konstruktor, potrebuje ASM.Register
     */
    public Node(ASM.Register temp) {
        this.register = temp;
        this.neighbours = new HashSet<Node>();

    }

    /**
     * Vrne stopnjo vozlišča, koliko ima sosedov
     */
    public int degree() {
        return neighbours.size();
    }

    /**
     * Edge adder
     */
    public void addEdge(Node node) {
        neighbours.add(node);
    }

    /**Edge remover */
    public void removeEdge(Node node) {
        neighbours.remove(node);
    }

    /**
     * Remove All edges
     */
    public void removeAllEdge() {
        neighbours.clear();
    }

    /**
     * Returns name of the register
     */
    public String registerName() {
        return register.toString();
    }

    @Override
    public String toString() {
        if(potentialSpill) {
            return "Potential spill: " + register.toString();
        } else if (actualSpill) {
            return "Actual spill: " + register.toString();
        } else {
            return register.toString();
        }
    }

}