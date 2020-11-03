import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class describes creator asm code
 */
public class ASM_Creator {
    /* main AST */
    private final AST ast;
    /* map with function AST, [] */
    private final HashMap<String, AST> defAST;
    /* variable map, [var name - place in stack] */
    private final HashMap<String, String> operationBlocks;
    /* result, asm code */
    private final String asmCode;
    /* pointer to new variable */
    private int varPointer;

    /* template for full asm file */
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

    /**
     * creator starter
     * @param ast - main AST
     * @param defAST - functions AST map
     * @throws CompilerException - threw this exception
     */
    public ASM_Creator(AST ast, HashMap<String, AST> defAST) throws CompilerException {
        /* initialise global variables */
        this.ast = ast;
        this.defAST = defAST;
        this.operationBlocks = new HashMap<>();
        this.varPointer = -1;

        /* fill map with asm fragments on every operation */
        loadOperationBlocks();

        /* start creating code */
        String[] functions = createFunctions();
        this.asmCode = String.format(masmTemplate, functions[0], mainCode(), functions[1]);

    }

    /**
     * fill map with code fragments
     */
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

        operationBlocks.put("PERCENT",  "\n\npop ebx\t; div\n" +
                                        "pop eax\n" +
                                        "cdq\n" +
                                        "idiv ebx\n" +
                                        "push edx");

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
                                    "\n%2$s\n" +
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
                                    "\n%2$s\n" +
                                    "\n\npop eax\n" +
                                    "cmp eax, 0\n" +
                                    "mov eax, 0\n" +
                                    "setne al\n" +
                                    "_end%1$d:\n" +
                                    "push eax");

        /* if operators */
        operationBlocks.put("TERNAR",   "\n\npop eax\t; ternar if\n" +      // before is condition (0)
                                        "cmp eax, 0\n" +
                                        "je _ternar_false_%1$d\n" +
                                        "\n%2$s\n" +                        // if true (1)
                                        "jmp _ternar_end_%1$d\n" +
                                        "_ternar_false_%1$d:\n" +
                                        "\n%3$s\n" +                        // if false (2)
                                        "_ternar_end_%1$d:");

        /* get vars/values */
        operationBlocks.put("INT", "\n\npush %s\t; int");
        operationBlocks.put("INT(CHAR)", "\n\npush %s\t; int(char)");
        operationBlocks.put("INT(FLOAT)", "\n\npush %s\t; int(float)");
        operationBlocks.put("INT(BINNUM)", "\n\npush %s\t; int(binnum)");
        operationBlocks.put("INT(OCTNUM)", "\n\npush %s\t; int(octnum)");
        operationBlocks.put("INT(HEXNUM)", "\n\npush %s\t; int(hexnum)");

        operationBlocks.put("ID",   "\n\nmov ebx, [ebp-%d]\t; get var: %s\n" +
                                    "push ebx");
    }

    /**
     * write asmCode to file
     * @param fileName - file name
     * @return - writing result
     */
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

    /**
     * create prototypes and procedure body code in array
     * @return - array with code
     * @throws CompilerException - if AST is incorrect, it throw this exception
     */
    private String[] createFunctions() throws CompilerException {
        /* 0 - PROTO, 1 - functions */
        String[] functions = {"" ,""};

        /* make asm code for all functions */
        for (String defName: defAST.keySet()) {
            /* make PROTO for {defName} */
            functions[0] += String.format("%s PROTO\n", defName);

            /* make {defName} prolog */
            StringBuilder funcTempl = new StringBuilder(String.format("%s PROC\n" +
                                                                    "push ebp\n" +
                                                                    "mov ebp, esp\n", defName));

            funcTempl.append(genBlockCode(defAST.get(defName).getRoot().getChildren(), new ArrayList<>()));

            /* make {defName} epilog */
            funcTempl.append(String.format( "\npop ebx\n" +
                                            "mov esp, ebp\n" +
                                            "pop ebp\n" +
                                            "ret\n" +
                                            "%s ENDP\n\n", defName));
            functions[1] += funcTempl;
        }

        return functions;
    }

    /**
     * make code fragment for block zone
     * @param block - block in list format
     * @param variableMap - list with stack of variable, current is the last in it
     * @return - code
     * @throws CompilerException - if AST is incorrect, it throw this exception
     */
    private String genBlockCode(ArrayList<Node_AST> block, ArrayList<HashMap<String, Integer>> variableMap)
            throws CompilerException {
        StringBuilder blockCode = new StringBuilder();
        boolean ifFlag = false;
        int memoryPointer = varPointer;
        /* hash code for make logic and conditional construction unique */
        String ifHashCode = "";
        variableMap.add(new HashMap<>());

        /* go by every statement in function body */
        for (Node_AST child: block) {

            /* append ending of IF construction using ifFlag */
            if (!child.getCurrent().getType().equals("IF") &&
                    !child.getCurrent().getType().equals("ELIF") &&
                    !child.getCurrent().getType().equals("ELSE") && ifFlag){
                blockCode.append(String.format("_if_end_%s:", ifHashCode));
                ifFlag = false;
            }

            String[] blockItems = genBlockItemCode(child, variableMap, ifHashCode, ifFlag);

            blockCode.append(blockItems[0]);
            ifHashCode = blockItems[1];
            ifFlag = blockItems[2].equals("1");
        }

        variableMap.remove(variableMap.size()-1);
        varPointer = memoryPointer;
        return blockCode.toString();
    }

    /**
     * make code for any statement
     * @param blockItem - statement (block-item type)
     * @param variableMap - list with stack of variable, current is the last in it
     * @param ifHashCode - hash code of IF Node_AST for generating conditional structures
     * @param ifFlag - flag for correct generating conditional structures
     * @return - block-item code
     * @throws CompilerException - if AST is incorrect, it throw this exception
     */
    private String[] genBlockItemCode(Node_AST blockItem, ArrayList<HashMap<String, Integer>> variableMap,
                                    String ifHashCode, boolean ifFlag)
            throws CompilerException {
        StringBuilder blockItemCode = new StringBuilder();

        switch (blockItem.getCurrent().getType()){

            /* RETURN statement, make retFlag true and break from function */
            case "RETURN":{
                blockItemCode.append(genExpCode(variableMap, blockItem.getChild(0)));
                blockItemCode.append("\npop ebx\n" +
                        "mov esp, ebp\n" +
                        "pop ebp\n" +
                        "ret\n");
                break;
            }

            /* ID statement */
            case "ID":{
                if (blockItem.getChildren().size() == 0){
                    throw new CompilerException("Variable referenced before assignment",
                            blockItem.getCurrent());
                }
                blockItemCode.append(genExpCode(variableMap, blockItem));
                break;
            }

            /* IF statement */
            case "IF" :{
                /* remember hashcode for jumping on special sign */
                ifHashCode = blockItem.hashCode()+"";
                ifFlag = true;

                /* initialise IF condition */
                blockItemCode.append(genExpCode(variableMap, blockItem.getChild(0)))
                        .append(   "\n\npop eax\t; if condition\n" + // before is condition IF <EXP> ":" (0)
                                "cmp eax, 0\n")
                        /* jump if <EXP> is false */
                        .append(String.format("je _if_false_%s", ifHashCode));

                if (blockItem.getChild(1).getChildren().size() == 0){
                    throw new CompilerException("This token need to have block body", blockItem.getCurrent());
                }

                /* create code for IF true part */
                blockItemCode.append(genBlockCode(blockItem.getChild(1).getChildren(), variableMap));
                /*for (Node_AST node: blockItem.getChild(1).getChildren()) {
                    blockItemCode.append(genExpCode(localVars, node));
                }*/

                /* if true, jump to end, next code will false */
                blockItemCode.append(String.format(   "\n\njmp _if_end_%1$s\n"+
                        "_if_false_%1$s:", ifHashCode));
                break;
            }

            /* ELIF statement */
            case "ELIF":{
                if (!ifFlag){
                    throw new CompilerException("IF token was missed", blockItem.getCurrent());
                }

                /* initialise ELIF condition */
                blockItemCode.append(genExpCode(variableMap, blockItem.getChild(0)))
                        .append(   "\n\npop eax\t; elif condition\n" +  // before is condition ELIF <EXP> ":" (0)
                                "cmp eax, 0\n")
                        /* jump elif <EXP> is false */
                        .append(String.format("je _elif_false_%d", blockItem.hashCode()));

                if (blockItem.getChild(1).getChildren().size() == 0){
                    throw new CompilerException("This token need to have block body", blockItem.getCurrent());
                }

                /* create code for ELIF true part */
                blockItemCode.append(genBlockCode(blockItem.getChild(1).getChildren(), variableMap));
                /*for (Node_AST node: blockItem.getChild(1).getChildren()) {
                    blockItemCode.append(genExpCode(localVars, node));
                }*/

                /* if true, jump to if end, next code will false */
                blockItemCode.append(String.format( "\n\njmp _if_end_%s\n"+
                        "_elif_false_%d:", ifHashCode, blockItem.hashCode()));
                break;
            }

            /* ELSE statement */
            case "ELSE":{
                if (!ifFlag){
                    throw new CompilerException("IF token was missed", blockItem.getCurrent());
                }
                if (blockItem.getChildren().size() == 0){
                    throw new CompilerException("This token need to have block body", blockItem.getCurrent());
                }
                /* ELSE has not true body */
                blockItemCode.append("\n\n\t\t; else");

                /* create code for ELSE part */
                blockItemCode.append(genBlockCode(blockItem.getChildren(), variableMap));
                /*for (Node_AST node: blockItem.getChildren()) {
                    blockItemCode.append(genExpCode(localVars, node));
                }*/
                break;
            }

            /* incorrect operation, throw exception */
            default:
                throw new CompilerException("Incorrect type of operation", blockItem.getCurrent());
        }

        return new String[]{blockItemCode.toString(), ifHashCode+"", ifFlag ? "1" : "0"};
    }

    /**
     * big generator part, connect operation template with nodes
     * @param variableMap - list with stack of variable, current is the last in it
     * @param current - current node to be used
     * @return - string for appending to code
     * @throws CompilerException - unknown operation throw this exception
     */
    private String genExpCode(ArrayList<HashMap<String, Integer>> variableMap, Node_AST current)
            throws CompilerException {
        switch (current.getCurrent().getType()){
            /* operations with one operand */
            case "UNAR_ADD":
            case "UNAR_SUB":
            case "NOT":{
                return genExpCode(variableMap, current.getChild(0))+
                        operationBlocks.get(current.getCurrent().getType());
            }
            /* operations with two operands */
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
            case "PERCENT":
            case "MUL":
            case "ADD":{
                return genExpCode(variableMap, current.getChild(0))+
                        genExpCode(variableMap, current.getChild(1))+
                    operationBlocks.get(current.getCurrent().getType());
            }
            /* operations with logic operands */
            case "OR":
            case "AND" :{
                return genExpCode(variableMap, current.getChild(0))+
                        String.format(operationBlocks.get(current.getCurrent().getType()),
                                        current.hashCode(),
                                        genExpCode(variableMap, current.getChild(1)));
            }
            /* ternary operand */
            case "TERNAR" :{
                return genExpCode(variableMap, current.getChild(0))+
                        String.format(operationBlocks.get(current.getCurrent().getType()),
                                        current.hashCode(),
                                        genExpCode(variableMap, current.getChild(1)),
                                        genExpCode(variableMap, current.getChild(2)));
            }
            /* value getter */
            case "INT(CHAR)":
            case "INT(BINNUM)":
            case "INT(HEXNUM)":
            case "INT(OCTNUM)":
            case "INT(FLOAT)":
            case "INT":{
                return String.format(operationBlocks.get(current.getCurrent().getType()),
                                                        current.getCurrent().getValue());
            }
            /* work with variables */
            case "ID": {
                // create variable
                if (current.getChildren().size() != 0){
                    /* make code for variable value */
                    String varExp = genExpCode(variableMap, current.getChild(0).getChild(0));

                    /* search and reinitialise variable */
                    for (int i = variableMap.size()-1; i >= 0; i--) {
                        if (variableMap.get(i).containsKey(current.getCurrent().getValue())) {
                            return String.format("%s\n" +
                                                "pop ebx\n" +
                                                "mov [ebp-%d], ebx\n",
                                    varExp, 4 * variableMap.get(i).get(current.getCurrent().getValue()));
                        }
                    }

                    /* create new variable */
                    variableMap.get(variableMap.size()-1).put(current.getCurrent().getValue(), ++varPointer);
                    return String.format("%s\n" +
                                        "pop ebx\n" +
                                        "mov [ebp-%d], ebx\n",
                            varExp, 4 * variableMap.get(variableMap.size()-1).get(current.getCurrent().getValue()));
                }

                // get variable
                /* search and get variable value */
                for (int i = variableMap.size()-1; i >= 0; i--) {
                    if (variableMap.get(i).containsKey(current.getCurrent().getValue())) {
                        return String.format(operationBlocks.get(current.getCurrent().getType()),
                                variableMap.get(i).get(current.getCurrent().getValue())*4,
                                current.getCurrent().getValue());
                    }
                }
                throw new CompilerException("Unknown variable", current.getCurrent());
            }
            /* error if operations is unknown */
            default:
                System.err.println(current.getParent().getParent().getCurrent().getType());
                throw new CompilerException("Unknown operation ", current.getCurrent());
        }
    }

    /**
     * code for _main procedure, generate other procedure calling
     * @return - _main code
     */
    private String mainCode(){
        StringBuilder code = new StringBuilder();

        for (Node_AST node: ast.getRoot().getChildren()) {
            if ("DEF_CALL".equals(node.getCurrent().getType())) {
                code.append(String.format("\tcall %s\n", node.getCurrent().getValue()));
            }
        }

        return code.toString();
    }

    /**
     * getter for asm code
     * @return - asm code
     */
    public String getAsmCode() {
        return asmCode;
    }
}
