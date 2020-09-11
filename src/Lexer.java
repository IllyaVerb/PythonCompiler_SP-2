import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private String parseText;

    private Map<String, String> keywords = new HashMap<>();
    private Map<String, String> symbols = new HashMap<>();
    private Map<String, String> whitespace = new HashMap<>();

    private ArrayList<Token> tokens = new ArrayList<>();

    public Lexer(){
        parseText = "";
        fillMaps();
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
        symbols.put("?", "QUESTION_MARK");
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

    public void makeTokens(){
        String[] parseLines = parseText.split("\n");

        for (String line: parseLines) {
            if (parseLine(line, makeRegexTokenPattern())){
                tokens.add(new Token("\n", whitespace.get("\n")));
            }
        }
    }

    private boolean parseLine(String line, String pattern){
        // remove comment part
        String noCommentLine = line.split("\\#")[0];

        // check if it is clear line
        Pattern p = Pattern.compile("^\\s*$");
        Matcher m = p.matcher(noCommentLine);
        if(m.matches()){
            return false;
        }

        Pattern tokenPattern = Pattern.compile(pattern);
        Matcher tokenMatch = tokenPattern.matcher(noCommentLine);

        while(tokenMatch.find()) {
            String currentToken = tokenMatch.group();
            if (keywords.containsKey(currentToken)){
                tokens.add(new Token(currentToken, keywords.get(currentToken)));
            }
            else {
                if (symbols.containsKey(currentToken)) {
                    tokens.add(new Token(currentToken, symbols.get(currentToken)));
                }
                else {
                    if (whitespace.containsKey(currentToken)) {
                        tokens.add(new Token(currentToken, whitespace.get(currentToken)));
                    }
                    else {
                        if (Pattern.compile("\\w+").matcher(currentToken).matches()) {
                            tokens.add(new Token(currentToken, "WORD"));
                        }
                        else {
                            if (Pattern.compile("0x[\\da-f]{1,6}").matcher(currentToken).matches()) {
                                tokens.add(new Token(currentToken, "HEXNUM"));
                            }
                            else {
                                if (Pattern.compile("0o[0-7]{1,6}").matcher(currentToken).matches()) {
                                    tokens.add(new Token(currentToken, "OCTNUM"));
                                }
                                else {
                                    if (Pattern.compile("0b[01]{1,6}").matcher(currentToken).matches()) {
                                        tokens.add(new Token(currentToken, "BINNUM"));
                                    }
                                    else {
                                        if (Pattern.compile("\\d+\\.\\d*").matcher(currentToken).matches()) {
                                            tokens.add(new Token(currentToken, "FLOATNUM"));
                                        }
                                        else {
                                            if (Pattern.compile("\\d+").matcher(currentToken).matches()) {
                                                tokens.add(new Token(currentToken, "NUMBER"));
                                            }
                                            else {

                                            }
                                        }
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

    private String makeRegexTokenPattern(){
        Set<String> symbolsList = symbols.keySet();
        String res = "(\\t)|( )|(0x[\\da-f]{1,6})|(0o[0-7]{1,6})|(0b[01]{1,6})"+
                        "|(\\d+\\.?\\d*)|(\\w+)";

        for (String symbol: symbolsList) {
            String newSymbol;
            if(symbol.length() == 1){
                newSymbol = symbol;
            }
            else{
                newSymbol = new StringBuilder(symbol).insert(1, '\\').toString();
            }
            res += "|(\\" + newSymbol + ")";
        }

        return res;
    }

    private  String makeRegexKeywordPattern(){
        Set<String> keywordsList = keywords.keySet();
        String res = "";

        for (String key: keywordsList) {
            res += "|(\\" + key + ")";
        }

        return res;
    }

    public void parseFile(String nameFile, boolean isFile){
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
            switch (token.getValue()){
                case "\n": val = "\\n"; break;
                case "\t": val = "\\t"; break;
                case " ": val = "\\s"; break;
                default: val = token.getValue();
            }
            System.out.println("[\t"+val+"\t\t\t\t"+token.getType()+"\t]");
        }
    }
}
