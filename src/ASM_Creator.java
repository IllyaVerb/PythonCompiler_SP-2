import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ASM_Creator {
    private final AST ast;
    private final HashMap<String, AST> defAST;
    private final HashMap<String, String> operationBlocks;
    private final String asmCode;

    private String masmTemplate = ".386\n" +
            ".model flat,stdcall\n" +
            "option casemap:none\n\n" +
            "include     ..\\include\\windows.inc\n" +
            "include     ..\\include\\kernel32.inc\n" +
            "include     ..\\include\\masm32.inc\n" +
            "includelib  ..\\lib\\kernel32.lib\n" +
            "includelib  ..\\lib\\masm32.lib\n\n" +
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
        this.operationBlocks = new HashMap<>();
        loadOperationBlocks();

        String[] functions = createFunctions();
        this.asmCode = String.format(masmTemplate, functions[0], mainCode(), functions[1]);

    }

    private void loadOperationBlocks() {
        operationBlocks.put("NOT",  "\n\npop ebx\t; not\n" +
                                    "xor eax, eax\n" +
                                    "cmp eax, ebx\n" +
                                    "sete al\n" +
                                    "push eax");

        operationBlocks.put("ADD",  "\n\npop ebx\t; add\n" +
                                    "pop eax\n" +
                                    "add ebx, eax\n" +
                                    "push ebx");

        operationBlocks.put("UNAR_ADD", "\n\n\t\t; unar add\n");

        operationBlocks.put("SUB", "\n\npop ebx\t; sub\n" +
                                    "pop eax\n" +
                                    "sub eax, ebx\n" +
                                    "push eax");

        operationBlocks.put("UNAR_SUB", "\n\npop ebx\t; unar sub\n" +
                                        "neg ebx\n" +
                                        "push ebx");

        operationBlocks.put("MUL",  "\n\npop ebx\t; mul\n" +
                                    "pop eax\n" +
                                    "imul ebx, eax\n" +
                                    "push ebx");

        operationBlocks.put("DIV",  "\n\npop ebx\t; div\n" +
                                    "pop eax\n" +
                                    "cdq\n" +
                                    "idiv ebx\n" +
                                    "push eax");

        operationBlocks.put("INT", "\n\npush %s\t; int\n");
        operationBlocks.put("INT(CHAR)", "\n\npush %s\t; int(char)\n");
        operationBlocks.put("INT(FLOAT)", "\n\npush %s\t; int(float)\n");
        operationBlocks.put("INT(BINNUM)", "\n\npush %s\t; int(binnum)\n");
        operationBlocks.put("INT(OCTNUM)", "\n\npush %s\t; int(octnum)\n");
        operationBlocks.put("INT(HEXNUM)", "\n\npush %s\t; int(hexnum)\n");
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
                    String retVar = genExpCode(child.getChild(0));
                    funcTempl += String.format("%s\n\npop ebx\nret\n", retVar);
                    break;
                }
            }
            funcTempl += String.format("%s ENDP\n", defName);
            functions[1] += funcTempl;
        }

        return functions;
    }

    private String genExpCode(Node_AST current) {
        switch (current.getCurrent().getType()){
            case "UNAR_ADD":
            case "UNAR_SUB":
            case "NOT":{
                return genExpCode(current.getChild(0))+
                        operationBlocks.get(current.getCurrent().getType());
            }
            case "SUB":
            case "DIV":
            case "MUL":
            case "ADD":{
                return genExpCode(current.getChild(0))+
                        genExpCode(current.getChild(1))+
                    operationBlocks.get(current.getCurrent().getType());
            }
            case "INT(CHAR)":
            case "INT(BINNUM)":
            case "INT(HEXNUM)":
            case "INT(OCTNUM)":
            case "INT(FLOAT)":
            case "INT":{
                return String.format(operationBlocks.get(current.getCurrent().getType()),
                                                        current.getCurrent().getValue());
            }
            default:
                return "Unknown operation "+current.getCurrent().getType();
        }
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
