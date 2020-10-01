import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Lexer {
    private String parseText;

    private Map<String, String> keywords;
    private Map<String, String> symbols;
    private Map<String, String> whitespace;

    private ArrayList<Token> tokens = new ArrayList<>();

    public Lexer(String nameFile, boolean isFile){
        keywords = new HashMap<>();
        symbols = new HashMap<>();
        whitespace = new HashMap<>();
        fillMaps();

        if(isFile) {
            try (FileReader reader = new FileReader(nameFile)) {
                int symb;
                while ((symb = reader.read()) != -1) {
                    parseText += (char) symb;
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        else{
            parseText = nameFile;
        }

        makeTokens();
    }

    private void fillMaps(){
        keywords.put("and", "AND");
        keywords.put("as", "AS");
        keywords.put("assert", "ASSERT");
        keywords.put("break", "BREAK");
        keywords.put("class", "CLASS");
        keywords.put("continue", "CONTINUE");
        keywords.put("def", "DEF");
        keywords.put("del", "DEL");
        keywords.put("elif", "ELIF");
        keywords.put("else", "ELSE");
        keywords.put("except", "EXCEPT");
        keywords.put("False", "FALSE");
        keywords.put("finally", "FINALLY");
        keywords.put("for", "FOR");
        keywords.put("from", "FROM");
        keywords.put("global", "GLOBAL");
        keywords.put("if", "IF");
        keywords.put("import", "IMPORT");
        keywords.put("in", "IN");
        keywords.put("is", "IS");
        keywords.put("lambda", "LAMBDA");
        keywords.put("None", "NONE");
        keywords.put("nonlocal", "NONLOCAL");
        keywords.put("not", "NOT");
        keywords.put("or", "OR");
        keywords.put("pass", "PASS");
        keywords.put("raise", "RAISE");
        keywords.put("return", "RETURN");
        keywords.put("True", "TRUE");
        keywords.put("try", "TRY");
        keywords.put("while", "WHILE");
        keywords.put("with", "WITH");
        keywords.put("yield", "YIELD");

        symbols.put(".", "DOT");
        symbols.put(",", "COMMA");
        symbols.put("=", "ASSIGNMENT");
        symbols.put(";", "SEMICOLON");
        symbols.put("'", "QUOTE");
        symbols.put("\"", "DBQUOTE");
        symbols.put("(", "LBR");
        symbols.put(")", "RBR");
        symbols.put("+", "ADD");
        symbols.put("-", "SUB");
        symbols.put("*", "MUL");
        symbols.put("**", "POW");
        symbols.put("/", "DIV");
        symbols.put("//", "INT_DIV");
        symbols.put("?", "QUESTION");
        symbols.put(":", "COLON");
        symbols.put("<", "LT");
        symbols.put(">", "GT");
        symbols.put("!", "NOT");
        symbols.put("<=", "LE");
        symbols.put(">=", "GE");
        symbols.put("==", "EQ");
        symbols.put("!=", "NE");
        symbols.put("[", "LSBR");
        symbols.put("]", "RSBR");
        symbols.put("{", "LBRC");
        symbols.put("}", "RBRC");
        symbols.put("&", "LOGIC_AND");
        symbols.put("|", "LOGIC_OR");
        symbols.put("^", "LOGIC_XOR");

        whitespace.put(" ", "SPACE");
        whitespace.put("\t", "TAB");
        whitespace.put("\n", "NEW_LINE");
    }

    private void makeTokens(){
        if (parseText.substring(0, 4).equals("null")){
            parseText = parseText.substring(4);
        }
        String[] parseLines = parseText.split("[\n\r]");

        for (int i=0; i < parseLines.length; i++) {
            if (parseLine(parseLines[i], i)){
                tokens.add(new Token("\n", whitespace.get("\n"), i, parseLines.length));
            }
        }
    }

    private boolean parseLine(String line, int row){
        // remove comment part
        String noCommentLine = line.split("#")[0];
        if (noCommentLine.matches("^\\s*$")){
            return false;
        }

        String[] symbLine = noCommentLine.split("");
        boolean spaceTabPart = true;

        for (int i=0; i < symbLine.length; i++) {
            if (i+1 != symbLine.length && symbols.containsKey(symbLine[i] + symbLine[i+1])){
                tokens.add(new Token(symbLine[i] + symbLine[i+1],
                        symbols.get(symbLine[i] + symbLine[i+1]), row, i));
                i++;
            }
            else {
                if (symbols.containsKey(symbLine[i])){
                    if (i+1 != symbLine.length && (symbLine[i].matches("[\"']"))){
                        StringBuilder num = new StringBuilder(symbLine[i]);
                        short j = 1;
                        while (i+j < symbLine.length){
                            num.append(symbLine[i + j]);
                            j++;
                            if (symbLine[i+j-1].equals(symbLine[i])){
                                break;
                            }
                        }

                        tokens.add(new Token(num.toString(), "STRING", row, i));
                        i += num.length()-1;
                    }
                    else {
                        tokens.add(new Token(symbLine[i], symbols.get(symbLine[i]), row, i));
                    }
                }
                else {
                    if (whitespace.containsKey(symbLine[i])){
                        if (spaceTabPart) {
                            tokens.add(new Token(symbLine[i], whitespace.get(symbLine[i]), row, i));
                        }
                    }
                    else {
                        if (i+1 != symbLine.length && symbLine[i].equals("0") &&
                                symbLine[i+1].matches("[xob]")){
                            StringBuilder num = new StringBuilder("0" + symbLine[i + 1]);
                            short j = 2;
                            while ((i+j < symbLine.length) &&
                                    isOthNum(symbLine[i+j], symbLine[i+1]) && j < 8){
                                num.append(symbLine[i + j]);
                                j++;
                            }

                            switch (symbLine[i+1]){
                                case "x": tokens.add(new Token(num.toString(), "HEXNUM", row, i)); break;
                                case "o": tokens.add(new Token(num.toString(), "OCTNUM", row, i)); break;
                                case "b": tokens.add(new Token(num.toString(), "BINNUM", row, i)); break;
                            }

                            i += num.length()-1;
                        }
                        else {
                            if (symbLine[i].matches("\\d")){
                                StringBuilder num = new StringBuilder();
                                boolean isFloat = false;
                                short j = 0;
                                while (i+j < symbLine.length &&
                                        symbLine[i+j].matches("[\\d.]")){
                                    if (symbLine[i+j].equals(".")){
                                        isFloat = true;
                                    }
                                    num.append(symbLine[i + j]);
                                    j++;
                                }

                                if (isFloat){
                                    tokens.add(new Token(num.toString(), "FLOAT", row, i));
                                }
                                else {
                                    tokens.add(new Token(num.toString(), "INT", row, i));
                                }
                                i += num.length()-1;
                            }
                            else {
                                if (symbLine[i].matches("[a-zA-Z]")){
                                    StringBuilder num = new StringBuilder();
                                    short j = 0;
                                    while (i+j < symbLine.length &&
                                            symbLine[i+j].matches("\\w")){
                                        num.append(symbLine[i + j]);
                                        j++;
                                    }

                                    tokens.add(new Token(num.toString(),
                                            keywords.getOrDefault(num.toString(), "WORD"), row, i));
                                    spaceTabPart = false;
                                    i += num.length()-1;
                                }
                                else {
                                    tokens.add(new Token(symbLine[i], "UNDEF", row, i));
                                    try {
                                        throw new CompilerException("Undefined symbol", row, i);
                                    } catch (CompilerException e) {
                                        System.err.println(e.getMessage());
                                        System.err.println((int) symbLine[i].toCharArray()[0]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isOthNum(String s, String system){
        switch (system){
            case "x": return s.matches("[\\da-fA-F]");
            case "o": return s.matches("[0-7]");
            case "b": return s.equals("1") || s.equals("0");
            default: return false;
        }
    }

    public String getParseText() {
        return parseText;
    }

    public ArrayList<Token> getTokens() {
        return tokens;
    }

    public void printTokens() {
        for (Token token: tokens) {
            String val;
            switch (token.getType()){
                case "NEW_LINE": val = "\\n"; break;
                case "TAB": val = "\\t"; break;
                case "SPACE": val = "\\s"; break;
                default: val = token.getValue();
            }
            System.out.println(String.format("[ %10s <==> %-10s ]", val, token.getType()));
        }
    }
}
