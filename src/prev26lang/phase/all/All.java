package prev26lang.phase.all;

import java.io.*;
import java.util.*;
import prev26lang.*;
import prev26lang.common.report.*;
import prev26lang.phase.*;
import prev26lang.phase.asmgen.ASM;
import prev26lang.phase.memory.*;
import prev26lang.phase.livean.*;

public class All extends Phase{

    private final List<ASM.Line> dataCode;
    private final List<ASM.Line> functionCode;

    public All(List<ASM.Line> dataCode, List<ASM.Line> functionCode) {
        super("all");
        this. dataCode = dataCode;
        this.functionCode = functionCode;
    }


    public void writteAssembley() {
        String fileName = Compiler.cmdLineOptValue("--dst-file-name");

        if(fileName.equals("")) {
            fileName = "out.asm";
        }

        try( PrintWriter out = new PrintWriter(new FileWriter(fileName))) {

            for(ASM.Line line : dataCode) {
                out.println(line.toString());
            }


            //out.println(".data");
            out.println("_heap_start: .zero 1048576");
            out.println("_heap_ptr: .dword _heap_start");
            //out.println("_heap_start: .space 1048576");

            out.println(".bss");
            out.println(".align 8");
            out.println("_stack_bottom:");
            out.println(".zero 4096");
            out.println("_stack_top:");

            out.println(".text");
            out.println(".globl _start");
            out.println("_start:");
            out.println("la sp, _stack_top");
            out.println("call _main");
            out.println("mv a0, a0");
            out.println("j _exit");

            for(ASM.Line line : functionCode) {
                out.println(line.toString());
            } 


            out.println("_putint:");
            out.println("    ld a0, 0(sp)");
            out.println("    li a7, 1");
            out.println("    ecall");
            out.println("    ret");
            out.println("_putchar:");
            out.println("    ld a0, 0(sp)");
            out.println("    li a7, 11");
            out.println("    ecall");
            out.println("    ret");
            out.println("_getint:");
            out.println("    li a7, 5");
            out.println("    ecall");
            out.println("    ret");
            out.println("_getchar:");
            out.println("    li a7, 12");
            out.println("    ecall");
            out.println("    ret");
            out.println("_exit:");
            out.println("    li a7, 93");
            out.println("    ecall");
            out.println("    ret");
            out.println("_new:");
            out.println("    mv t0, a0");
            out.println("    la t1, _heap_ptr");
            out.println("    ld t2, 0(t1)");
            out.println("    mv a0, t2");
            out.println("    add t2, t2, t0");
            out.println("    sd t2, 0(t1)");
            out.println("    ret");
            out.println("_del:");
            out.println("    ret");

            out.flush();
        } catch (IOException e) {
            throw new Report.Error("Cannot write to file name: " + fileName);
        }
    }



}