import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class ASM_Creator {
    private static final int MAX_LOCAL_VARS = 25;

    private int clauseNum;
    private final AST ast;
    private final HashMap<String, AST> defAST;
    private final HashMap<String, String> operationBlocks;
    private final String asmCode;

    private String masmTemplate = ".386\n" +
            ".model flat,stdcall\n" +
            "option casemap:none\n\n" +
            "include     ..\\include\\masm32rt.inc\n\n" +
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

    public ASM_Creator(AST ast, HashMap<String, AST> defAST) throws CompilerException {
        this.ast = ast;
        this.defAST = defAST;
        this.operationBlocks = new HashMap<>();
        this.clauseNum = 0;
        loadOperationBlocks();

        String[] functions = createFunctions();
        this.asmCode = String.format(masmTemplate, functions[0], mainCode(), functions[1]);

    }

    private void loadOperationBlocks() {
        /* Operations for 1 args */
        operationBlocks.put("NOT",  "\n\npop ebx\t; not\n" +
                                    "xor eax, eax\n" +
                                    "cmp eax, ebx\n" +
                                    "sete al\n" +
                                    "push eax");

        operationBlocks.put("UNAR_ADD", "\n\n\t\t; unar add\n");

        operationBlocks.put("UNAR_SUB", "\n\npop ebx\t; unar sub\n" +
                                        "neg ebx\n" +
                                        "push ebx");

        /* Operations for 2 args */
        operationBlocks.put("ADD",  "\n\npop ebx\t; add\n" +
                                    "pop eax\n" +
                                    "add ebx, eax\n" +
                                    "push ebx");

        operationBlocks.put("SUB",  "\n\npop ebx\t; sub\n" +
                                    "pop eax\n" +
                                    "sub eax, ebx\n" +
                                    "push eax");

        operationBlocks.put("MUL",  "\n\npop ebx\t; mul\n" +
                                    "pop eax\n" +
                                    "imul ebx, eax\n" +
                                    "push ebx");

        operationBlocks.put("DIV",  "\n\npop ebx\t; div\n" +
                                    "pop eax\n" +
                                    "cdq\n" +
                                    "idiv ebx\n" +
                                    "push eax");

        operationBlocks.put("L_SHIFT",  "\n\npop ebx\t; left shift\n" +
                                        "pop eax\n" +
                                        "sal eax, ebx\n" +
                                        "push eax");

        operationBlocks.put("R_SHIFT",  "\n\npop ebx\t; right shift\n" +
                                        "pop eax\n" +
                                        "sar eax, ebx\n" +
                                        "push eax");

        operationBlocks.put("EQ",   "\n\npop ebx\t; equal\n" +
                                    "pop eax\n" +
                                    "cmp eax, ebx\n" +
                                    "mov eax, 0\n" +
                                    "sete al\n" +
                                    "push eax");

        operationBlocks.put("NE",   "\n\npop ebx\t; not equal\n" +
                                    "pop eax\n" +
                                    "cmp eax, ebx\n" +
                                    "mov eax, 0\n" +
                                    "setne al\n" +
                                    "push eax");

        operationBlocks.put("GE",   "\n\npop ebx\t; great or equal\n" +
                                    "pop eax\n" +
                                    "cmp eax, ebx\n" +
                                    "mov eax, 0\n" +
                                    "setge al\n" +
                                    "push eax");

        operationBlocks.put("LE",   "\n\npop ebx\t; less or equal\n" +
                                    "pop eax\n" +
                                    "cmp eax, ebx\n" +
                                    "mov eax, 0\n" +
                                    "setle al\n" +
                                    "push eax");

        operationBlocks.put("GT",   "\n\npop ebx\t; great then\n" +
                                    "pop eax\n" +
                                    "cmp eax, ebx\n" +
                                    "mov eax, 0\n" +
                                    "setg al\n" +
                                    "push eax");

        operationBlocks.put("LT",   "\n\npop ebx\t; less then\n" +
                                    "pop eax\n" +
                                    "cmp eax, ebx\n" +
                                    "mov eax, 0\n" +
                                    "setl al\n" +
                                    "push eax");

        operationBlocks.put("AND",  "\n\npop eax\t; and\n" +
                                    "cmp eax, 0\n" +
                                    "jne _clause%1$d\n" +
                                    "jmp _end%1$d\n" +
                                    "_clause%1$d:\n" +
                                    "\n%2$s" +
                                    "\n\npop eax\n" +
                                    "cmp eax, 0\n" +
                                    "mov eax, 0\n" +
                                    "setne al\n" +
                                    "_end%1$d:\n" +
                                    "push eax");

        operationBlocks.put("OR",  "\n\npop eax\t; or\n" +
                                    "cmp eax, 0\n" +
                                    "je _clause%1$d\n" +
                                    "mov eax, 1\n" +
                                    "jmp _end%1$d\n" +
                                    "_clause%1$d:\n" +
                                    "\n%2$s" +
                                    "\n\npop eax\n" +
                                    "cmp eax, 0\n" +
                                    "mov eax, 0\n" +
                                    "setne al\n" +
                                    "_end%1$d:\n" +
                                    "push eax");

        operationBlocks.put("INT", "\n\npush %s\t; int");
        operationBlocks.put("INT(CHAR)", "\n\npush %s\t; int(char)");
        operationBlocks.put("INT(FLOAT)", "\n\npush %s\t; int(float)");
        operationBlocks.put("INT(BINNUM)", "\n\npush %s\t; int(binnum)");
        operationBlocks.put("INT(OCTNUM)", "\n\npush %s\t; int(octnum)");
        operationBlocks.put("INT(HEXNUM)", "\n\npush %s\t; int(hexnum)");

        operationBlocks.put("ID",   "\n\nmov ebx, [%d+ebp+4]\t; get var: %s\n" +
                                    "push ebx");
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

    private String[] createFunctions() throws CompilerException {
        /* 0 - PROTO, 1 - functions */
        String[] functions = {"" ,""};

        for (String defName: defAST.keySet()) {
            /* make PROTO for {defName} */
            functions[0] += String.format("%s PROTO\n", defName);

            /* make {defName} prolog */
            String funcTempl = String.format(   "%s PROC\n" +
                                                "mov ebp, esp\n" +
                                                "add esp, %d\n", defName, MAX_LOCAL_VARS*4);
            boolean retFlag = false;
            HashMap<String, Integer> localVars = new HashMap<>();
            int vars = 0;

            for (Node_AST child: defAST.get(defName).getRoot().getChildren()) {
                switch (child.getCurrent().getType()){
                    case "RETURN":{
                        String retVar = genExpCode(localVars, child.getChild(0));
                        funcTempl += retVar;
                        retFlag = true;
                        break;
                    }
                    case "ID":{
                        if (child.getChildren().size() == 0){
                            throw new CompilerException("Variable referenced before assignment",
                                    child.getCurrent().getRow(), child.getCurrent().getColumn());
                        }
                        String varExp = genExpCode(localVars, child.getChild(0).getChild(0));
                        if (!localVars.containsKey(child.getCurrent().getValue())){
                            localVars.put(child.getCurrent().getValue(), ++vars);
                        }
                        funcTempl += String.format( "%s\n" +
                                                    "pop ebx\n" +
                                                    "mov [%d+ebp+4], ebx\n",
                                varExp, 4*localVars.get(child.getCurrent().getValue()));
                        break;
                    }
                }
                if (vars >= MAX_LOCAL_VARS)
                    throw new CompilerException("Too many local variables",
                            child.getCurrent().getRow(), child.getCurrent().getColumn());
                if (retFlag)
                    break;
            }

            /* make {defName} epilog */
            funcTempl += String.format( "\npop ebx\n" +
                                        "sub esp, %d\n" +
                                        "ret\n" +
                                        "%s ENDP\n\n", MAX_LOCAL_VARS*4, defName);
            functions[1] += funcTempl;
        }

        return functions;
    }

    private String genExpCode(HashMap<String, Integer> localVars, Node_AST current) throws CompilerException {
        switch (current.getCurrent().getType()){
            case "UNAR_ADD":
            case "UNAR_SUB":
            case "NOT":{
                return genExpCode(localVars, current.getChild(0))+
                        operationBlocks.get(current.getCurrent().getType());
            }
            case "L_SHIFT":
            case "R_SHIFT":
            case "EQ":
            case "NE":
            case "GT":
            case "LT":
            case "GE":
            case "LE":
            case "SUB":
            case "DIV":
            case "MUL":
            case "ADD":{
                return genExpCode(localVars, current.getChild(0))+
                        genExpCode(localVars, current.getChild(1))+
                    operationBlocks.get(current.getCurrent().getType());
            }
            case "OR":
            case "AND" :{
                return genExpCode(localVars, current.getChild(0))+
                        String.format(operationBlocks.get(current.getCurrent().getType()),
                                ++clauseNum, genExpCode(localVars, current.getChild(1)));
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
            case "ID": {
                if (!localVars.containsKey(current.getCurrent().getValue())){
                    throw new CompilerException("Unknown variable",
                            current.getCurrent().getRow(), current.getCurrent().getColumn());
                }

                return String.format(operationBlocks.get(current.getCurrent().getType()),
                        localVars.get(current.getCurrent().getValue())*4, current.getCurrent().getValue());
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
