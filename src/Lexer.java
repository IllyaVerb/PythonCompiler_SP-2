import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Lexer {
    /* start text, which will be parsed */
    private String parseText;

    /* maps with different tokens */
    private final Map<String, String> keywords;
    private final Map<String, String> symbols;
    private final Map<String, String> whitespace;

    /* list of future tokens */
    private final ArrayList<Token> tokens = new ArrayList<>();

    /**
     * create lexer object
     * @param nameFile - get python code from this file
     * @param isFile - is nameFile represent file name or string with code
     */
    public Lexer(String nameFile, boolean isFile){
        /* initialise maps */
        keywords = new HashMap<>();
        symbols = new HashMap<>();
        whitespace = new HashMap<>();

        /* StringBuilder for reading data from file */
        StringBuilder parseBuilder = new StringBuilder();

        /* fill maps by common tokens */
        fillMaps();

        /* add root token */
        tokens.add(new Token("null", "START", -1, -1));

        if(isFile) {
            /* read code from file */
            try (FileReader reader = new FileReader(nameFile)) {
                int symb;
                while ((symb = reader.read()) != -1) {
                    parseBuilder.append((char) symb);
                }
                this.parseText = parseBuilder.toString();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        else{
            /* use name as string with code */
            this.parseText = nameFile;
        }

        /* start lexer parsing */
        makeTokens();
    }

    /**
     * fill maps with common tokens
     */
    private void fillMaps(){
        /* fill word tokens */
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

        /* fill symbol tokens */
        symbols.put(".", "DOT");
        symbols.put(",", "COMMA");
        symbols.put("=", "ASSIGN");
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
        symbols.put("%", "PERCENT");
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
        symbols.put("<<", "L_SHIFT");
        symbols.put(">>", "R_SHIFT");
        symbols.put("&", "BIT_AND");
        symbols.put("|", "BIT_OR");
        symbols.put("^", "BIT_XOR");
        symbols.put("~", "BIT_NOT");
        symbols.put("+=", "ADD_ASSIGN");
        symbols.put("-=", "SUB_ASSIGN");
        symbols.put("/=", "DIV_ASSIGN");
        symbols.put("*=", "MUL_ASSIGN");
        symbols.put("%=", "PERCENT_ASSIGN");
        symbols.put("<<=", "L_SHIFT_ASSIGN");
        symbols.put(">>=", "R_SHIFT_ASSIGN");
        symbols.put("&=", "BIT_AND_ASSIGN");
        symbols.put("|=", "BIT_OR_ASSIGN");
        symbols.put("^=", "BIT_XOR_ASSIGN");

        /* fill delimiter tokens */
        whitespace.put(" ", "SPACE");
        whitespace.put("\t", "TAB");
        whitespace.put("\n", "NEW_LINE");
    }

    /**
     * start lexer analysis
     */
    private void makeTokens(){
        /*
        * .py files have 'null' fragment before code
        * remove this part
        */
        if (parseText.startsWith("null")){
            parseText = parseText.substring(4);
        }

        /* split python code into array with code lines */
        String[] parseLines = parseText.split("[\n\r]");

        /* go by every line and parse it */
        for (int i=0; i < parseLines.length; i++) {
            /* parse and if result is true, add token(\n) after */
            if (parseLine(parseLines[i], i)){
                tokens.add(new Token("\n", whitespace.get("\n"), i, parseLines.length));
            }
        }
    }

    /**
     * parse line of tokens
     * @param line - line for parsing
     * @param row - number of row for exception message
     * @return - result of line parsing
     */
    private boolean parseLine(String line, int row){
        /* end if started comment part */
        String noCommentLine = line.split("#")[0];

        /* end if line only with whitespaces and/or delimiters */
        if (noCommentLine.matches("^\\s*$")){
            return false;
        }

        /* create array of symbols */
        String[] symbLine = noCommentLine.split("");

        /* flag for adding whitespace tokens only in star of line */
        boolean spaceTabPart = true;

        /* go by every symbol and choosing correct token */
        for (int i=0; i < symbLine.length; i++) {
            /* symbol tokens with tree characters */
            if (i+2 < symbLine.length && symbols.containsKey(symbLine[i] +
                                                                symbLine[i+1] +
                                                                symbLine[i+2])){
                tokens.add(new Token(symbLine[i] + symbLine[i+1] + symbLine[i+2],
                        symbols.get(symbLine[i] + symbLine[i+1] + symbLine[i+2]), row, i));
                i+=2;
            }
            else {
                /* symbol tokens with two characters */
                if (i + 1 < symbLine.length && symbols.containsKey(symbLine[i] + symbLine[i + 1])) {
                    tokens.add(new Token(symbLine[i] + symbLine[i + 1],
                            symbols.get(symbLine[i] + symbLine[i + 1]), row, i));
                    i++;
                } else {
                    /* one character symbol tokens and string var */
                    if (symbols.containsKey(symbLine[i])) {
                        /* string var choose if start with <'> or <"> */
                        if (i + 1 != symbLine.length && (symbLine[i].matches("[\"']"))) {
                            StringBuilder stringToken = new StringBuilder(symbLine[i]);
                            short j = 1;
                            /* add characters while line not ended or symbol is <'> or <"> */
                            while (i + j < symbLine.length) {
                                stringToken.append(symbLine[i + j]);
                                j++;
                                if (symbLine[i + j - 1].equals(symbLine[i])) {
                                    break;
                                }
                            }

                            tokens.add(new Token(stringToken.toString(), "STRING", row, i));

                            /* jump to end of string */
                            i += stringToken.length() - 1;
                        } else {
                            /* add symbol token */
                            tokens.add(new Token(symbLine[i], symbols.get(symbLine[i]), row, i));
                        }
                    } else {
                        if (whitespace.containsKey(symbLine[i])) {
                            /* add whitespace token if it on start of line */
                            if (spaceTabPart) {
                                tokens.add(new Token(symbLine[i], whitespace.get(symbLine[i]), row, i));
                            }
                        } else {
                            /* add bin/oct/hex number token if first is <0> and second is <x|o|b> */
                            if (i + 1 != symbLine.length && symbLine[i].equals("0") &&
                                    symbLine[i + 1].matches("[xob]")) {
                                /* using the same algorithm as string var */
                                StringBuilder num = new StringBuilder("0" + symbLine[i + 1]);
                                short j = 2;
                                while ((i + j < symbLine.length) &&
                                        isOthNum(symbLine[i + j], symbLine[i + 1]) && j < 8) {
                                    num.append(symbLine[i + j]);
                                    j++;
                                }

                                switch (symbLine[i + 1]) {
                                    case "x":
                                        tokens.add(new Token(num.toString(), "HEXNUM", row, i));
                                        break;
                                    case "o":
                                        tokens.add(new Token(num.toString(), "OCTNUM", row, i));
                                        break;
                                    case "b":
                                        tokens.add(new Token(num.toString(), "BINNUM", row, i));
                                        break;
                                }

                                i += num.length() - 1;
                            } else {
                                /* add float or int number tokens */
                                if (symbLine[i].matches("\\d")) {
                                    /* using the same algorithm as string var */
                                    StringBuilder num = new StringBuilder();
                                    /* float flag, true if found <.> */
                                    boolean isFloat = false;
                                    short j = 0;
                                    while (i + j < symbLine.length &&
                                            symbLine[i + j].matches("[\\d.]")) {
                                        if (symbLine[i + j].equals(".")) {
                                            isFloat = true;
                                        }
                                        num.append(symbLine[i + j]);
                                        j++;
                                    }

                                    if (isFloat) {
                                        tokens.add(new Token(num.toString(), "FLOAT", row, i));
                                    } else {
                                        tokens.add(new Token(num.toString(), "INT", row, i));
                                    }
                                    i += num.length() - 1;
                                } else {
                                    /* add word tokens */
                                    if (symbLine[i].matches("[a-zA-Z]")) {
                                        /* using the same algorithm as string var */
                                        StringBuilder wordToken = new StringBuilder();
                                        short j = 0;
                                        while (i + j < symbLine.length &&
                                                symbLine[i + j].matches("\\w")) {
                                            wordToken.append(symbLine[i + j]);
                                            j++;
                                        }

                                        tokens.add(new Token(wordToken.toString(),
                                                keywords.getOrDefault(wordToken.toString(), "WORD"), row, i));
                                        spaceTabPart = false;
                                        i += wordToken.length() - 1;
                                    } else {
                                        /* if token not chosen before it, add undefined token */
                                        tokens.add(new Token(symbLine[i], "UNDEF", row, i));
                                        try {
                                            throw new CompilerException("Undefined symbol", tokens.get(tokens.size() - 1));
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
        }
        return true;
    }

    /**
     * test if character is other number (HEX/BIN/OCT)
     * @param s - character
     * @param system - x(HEX), o(OCT), b(BIN)
     * @return is char represent element of this system
     */
    private boolean isOthNum(String s, String system){
        switch (system){
            case "x": return s.matches("[\\da-fA-F]");
            case "o": return s.matches("[0-7]");
            case "b": return s.equals("1") || s.equals("0");
            default: return false;
        }
    }

    /**
     * getter for read text form file
     * @return - text
     */
    public String getParseText() {
        return parseText;
    }

    /**
     * getter for token list
     * @return - list of parsed tokens
     */
    public ArrayList<Token> getTokens() {
        return tokens;
    }

    /**
     * print table of tokens(lexems)
     */
    public void printTokens() {
        for (Token token: tokens) {
            System.out.printf("[ %10s <==> %-10s ]%n", token.getRawValue(), token.getType());
        }
    }
}
