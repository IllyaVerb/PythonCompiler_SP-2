public class Main {
    public static void main(String[] args) {
        String parseFile = "def main():\t\t#Python lang\n"+
                            "\treturn 0x0102\n"+
                            "\n"+
                            "def second():\n"+
                            "\treturn 0o66\n"+
                            "second()\n"+
                            "main()";

        long time = System.nanoTime();

        //Lexer lexer = new Lexer("C:\\Users\\illya\\Desktop\\minesweeper.py", true);
        Lexer lexer = new Lexer(parseFile, false);
        System.out.println("\n========================================\n");
        System.out.println(parseFile);
        System.out.println("\n========================================\n");

        lexer.printTokens();

        System.out.println("\n========================================\n");

        Parser parser = null;
        try {
            parser = new Parser(lexer.getTokens());
        } catch (CompilerException e) {
            System.err.println(e.getMessage());
            return;
        }
        parser.getMainAST().printAST();

        System.out.println("\n========================================\n");

        ASM_Creator asm_creator = new ASM_Creator(parser.getMainAST(), parser.getDefAST());
        String filePath = "C:\\Users\\illya\\Desktop\\code.asm";
        boolean success = asm_creator.createFile(filePath);

        time = System.nanoTime() - time;
        if (success){
            System.out.println("Compilation was successful,\n\t'code.asm' is located in " + filePath);
            System.out.println(String.format("\tElapsed %,9.3f ms\n", time/1_000_000.0));
        }
        else {
            System.err.println("Compilation was failed");
        }

    }
}
