public class Main {
    public static void main(String[] args) {
        long time = System.nanoTime();

        Lexer lexer = new Lexer("1-4-Java-IO-82-Verbovskyi.py", true);

        lexer.printTokens();

        System.out.println("\n========================================\n");

        Parser parser;
        try {
            parser = new Parser(lexer.getTokens());
        } catch (CompilerException e) {
            System.err.println(e.getMessage());
            return;
        }
        parser.getMainAST().printAST();

        System.out.println("\n========================================\n");

        ASM_Creator asm_creator = new ASM_Creator(parser.getMainAST(), parser.getDefAST());
        String filePath = "1-4-Java-IO-82-Verbovskyi.asm";
        boolean success = asm_creator.createFile(filePath);

        time = System.nanoTime() - time;
        if (success){
            System.out.println("Compilation was successful,\n\t'code.asm' is located in " +
                    System.getProperty("user.dir") + "\\" + filePath);
            System.out.println(String.format("\tElapsed %,9.3f ms\n", time/1000000.0));
        }
        else {
            System.err.println("Compilation was failed");
        }

    }
}
