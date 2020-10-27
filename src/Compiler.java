/**
 * general compiler which contain lexer, parser, asm creator
 */
public class Compiler {
    /* names for input(.py) and output(.asm) files */
    private final String input;
    private final String output;

    /**
     * create compiler object
     * @param inputFilename - input python file
     * @param outputFilename - output assembler file
     */
    public Compiler(String inputFilename, String outputFilename){
        this.input = inputFilename;
        this.output = outputFilename;
    }

    /**
     * compile python code
     * @return - result of compilation
     */
    public boolean compile(){
        /* time before compilation */
        long time = System.nanoTime();

        /* start lexer */
        Lexer lexer = new Lexer(input, true);

        /* print lexing table */
        lexer.printTokens();
        System.out.println("\n========================================\n");

        /* start parser */
        Parser parser;
        try {
            parser = new Parser(lexer.getTokens());
        } catch (CompilerException e) {
            /* if result of parsing is incorrect return false and print error message */
            System.err.println(e.getMessage());
            return false;
        }
        /* print AST (result of parsing) */
        parser.getMainAST().printAST();
        System.out.println("\n========================================\n");

        /* start ASM creator */
        ASM_Creator asm_creator;
        try {
            asm_creator = new ASM_Creator(parser.getMainAST(), parser.getDefAST());
        } catch (CompilerException e) {
            /* if result of creating asm is incorrect return false and print error message */
            System.err.println(e.getMessage());
            return false;
        }
        boolean success = asm_creator.createFile(output);

        time = System.nanoTime() - time;

        if (success){
            System.out.println("Compilation was successful,\n\toutput ASM file is located in " +
                    System.getProperty("user.dir") + "\\" + output);
            System.out.printf("\tElapsed %,9.3f ms\n%n", time/1000000.0);
            return true;
        }
        else {
            System.err.println("Compilation was failed");
            return false;
        }
        //return true;
    }
}
