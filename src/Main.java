public class Main {
    public static void main(String[] args) {
        String parseFile = "def main():\t\t#Python lang\n\treturn 0xff\n\nmain()";

        Lexer lexer = new Lexer();
        //lexer.parseFile("C:\\Users\\illya\\Desktop\\minesweeper.py", true);
        lexer.parseFile(parseFile, false);
        System.out.println("========================================");
        System.out.println(parseFile);
        System.out.println("========================================");

        lexer.makeTokens();
        lexer.printTokens();
    }
}
