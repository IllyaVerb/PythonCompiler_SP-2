public class Compiler {
    private final String input;
    private final String output;

    public Compiler(String inputFilename, String outputFilename){
        this.input = inputFilename;
        this.output = outputFilename;
    }
    public boolean compile(){
        long time = System.nanoTime();

        Lexer lexer = new Lexer(input, true);

        lexer.printTokens();
        System.out.println("\n========================================\n");

        Parser parser;
        try {
            parser = new Parser(lexer.getTokens());
        } catch (CompilerException e) {
            System.err.println(e.getMessage());
            return false;
        }
        parser.getMainAST().printAST();


        System.out.println("\n========================================\n");

        ASM_Creator asm_creator = new ASM_Creator(parser.getMainAST(), parser.getDefAST());
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
    }
}
