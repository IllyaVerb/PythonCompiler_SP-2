public class Main {
    public static void main(String[] args) {
        String parseFile = "def main():\t\t#Python lang\n\treturn 0x0102\n\nmain()";

        //Lexer lexer = new Lexer("C:\\Users\\illya\\Desktop\\minesweeper.py", true);
        Lexer lexer = new Lexer(parseFile, false);
        System.out.println("========================================");
        System.out.println(parseFile);
        System.out.println("========================================");

        lexer.makeTokens();
        lexer.printTokens();

        System.out.println();

        Parser parser = new Parser(lexer.getTokens());
        for (String ast: parser.getDefAST().keySet()) {
            parser.getDefAST().get(ast).printAST();
        }
    }
}
