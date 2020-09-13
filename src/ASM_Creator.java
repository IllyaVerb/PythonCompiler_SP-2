import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ASM_Creator {
    private AST ast;
    private HashMap<String, AST> defAST;
    private String asmCode;

    private String masmTemplate = ".386\n" +
            ".model flat,stdcall\n" +
            "option casemap:none\n\n" +
            "include     E:\\masm32\\include\\windows.inc\n" +
            "include     E:\\masm32\\include\\kernel32.inc\n" +
            "include     E:\\masm32\\include\\masm32.inc\n" +
            "includelib  E:\\masm32\\lib\\kernel32.lib\n" +
            "includelib  E:\\masm32\\lib\\masm32.lib\n\n" +
            "_NumbToStr   PROTO :DWORD,:DWORD\n" +
            "_main        PROTO\n\n" +
            "%s\n" + // insert prototype of functions
            ".data\n" +
            "buff        db 11 dup(?)\n\n" +
            ".code\n" +
            "_start:\n" +
            "\tinvoke  _main\n" +
            "\tinvoke  _NumbToStr, ebx, ADDR buff\n" +
            "\tinvoke  StdOut,eax\n" +
            "\tinvoke  ExitProcess,0\n\n" +
            "_main PROC\n\n" +
            "%s" + // insert code
            "\n\tret\n\n" +
            "_main ENDP\n\n" +
            "%s" + // insert functions
            "\n_NumbToStr PROC uses ebx x:DWORD,buffer:DWORD\n\n" +
            "\tmov     ecx,buffer\n" +
            "\tmov     eax,x\n" +
            "\tmov     ebx,10\n" +
            "\tadd     ecx,ebx\n" +
            "@@:\n" +
            "\txor     edx,edx\n" +
            "\tdiv     ebx\n" +
            "\tadd     edx,48\n" +
            "\tmov     BYTE PTR [ecx],dl\n" +
            "\tdec     ecx\n" +
            "\ttest    eax,eax\n" +
            "\tjnz     @b\n" +
            "\tinc     ecx\n" +
            "\tmov     eax,ecx\n" +
            "\tret\n\n" +
            "_NumbToStr ENDP\n\n" +
            "END _start\n";

    public ASM_Creator(AST ast, HashMap<String, AST> defAST){
        this.ast = ast;
        this.defAST = defAST;

        String[] functions = createFunctions();
        this.asmCode = String.format(masmTemplate, functions[0], mainCode(), functions[1]);
    }

    public boolean createFile(String fileName){
        try(FileWriter writer = new FileWriter(fileName, false))
        {
            writer.write(asmCode);
            writer.flush();
            return true;
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
            return false;
        }
    }

    private String[] createFunctions(){
        String[] functions = {"" ,""};

        for (String defName: defAST.keySet()) {
            functions[0] += String.format("%s\tPROTO\n", defName);
            String funcTempl = String.format("%s PROC\n", defName);
            for (Node_AST child: defAST.get(defName).getRoot().getChildren()) {
                // do smth what is function body
                if (child.getCurrent().getType().equals("RETURN")){
                    String retVar = child.getChild(0).getCurrent().getValue();
                    funcTempl += String.format("mov ebx, %s\nret\n", retVar);
                    break;
                }
            }
            funcTempl += String.format("%s ENDP\n", defName);
            functions[1] += funcTempl;
        }

        return functions;
    }

    private String mainCode(){
        String code = "";

        for (Node_AST node: ast.getRoot().getChildren()) {
            switch (node.getCurrent().getType()){
                case "DEF_CALL": {
                    code += String.format("\tcall %s\n", node.getCurrent().getValue());
                }
            }
        }

        return code;
    }

    public String getAsmCode() {
        return asmCode;
    }
}
