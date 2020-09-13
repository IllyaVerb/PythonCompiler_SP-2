import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Parser {
    private ArrayList<Token> tokens;
    private HashMap<String, AST> defAST;
    private AST mainAST;

    public Parser(ArrayList<Token> tokens) throws CompilerException {
        this.tokens = tokens;
        this.defAST = new HashMap<>();
        this.mainAST = new AST(new Token(null, "START", 0, 0));

        for (Iterator<Token> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
            Token token = tokenIterator.next();
            switch (token.getType()){
                case "DEF": {
                    AST tmp = new AST(parseDef(token, tokenIterator));
                    defAST.put(tmp.getRoot().getCurrent().getValue(), tmp);
                    break;
                }
                case "WORD": {
                    mainAST.getRoot().appendChild(parseWord(token, tokenIterator));
                    break;
                }
                default:
                    System.out.println(token.getValue() + " " + token.getType());
            }
        }
    }

    private Node_AST parseDef(Token prev, Iterator<Token> tokenIterator) throws CompilerException {
        int[] currentSpaceTabCount = {0, 0};
        String defName;

        Token token = tokenIterator.next();
        /*while (tokens.get(index).getType().matches("(TAB)|(SPACE)")) {
            if (tokens.get(index).getType().equals("TAB")){
                currentSpaceTabCount[1]++;
            }
            else {
                currentSpaceTabCount[0]++;
            }
            index++;
        }*/
        if (!token.getType().equals("WORD")){
            fail(1, token);
        }
        defName = token.getValue();
        token = tokenIterator.next();
        if (!token.getType().equals("LBR")){
            fail(1, token);
        }
        token = tokenIterator.next();
        if (!token.getType().equals("RBR")){
            fail(1, token);
        }
        token = tokenIterator.next();
        if (!token.getType().equals("COLON")){
            fail(1, token);
        }
        token = tokenIterator.next();
        if (!token.getType().equals("NEW_LINE")){
            fail(1, token);
        }

        Node_AST statement = parseStatement(token, tokenIterator, currentSpaceTabCount);
        Node_AST def = new Node_AST(new Token(defName, "DEF_WORD", prev.getRow(), prev.getColumn()));
        def.appendChild(statement);
        statement.setParent(def);

        return def;
    }

    private Node_AST parseStatement(Token prev, Iterator<Token> tokenIterator, int[] spaceTabCount)
            throws CompilerException {
        int[] currentSpaceTabCount = {0, 0};
        Token token = tokenIterator.next();

        while (token.getType().matches("(TAB)|(SPACE)")) {
            if (token.getType().equals("TAB")){
                currentSpaceTabCount[1]++;
            }
            else {
                currentSpaceTabCount[0]++;
            }
            token = tokenIterator.next();
        }
        if (currentSpaceTabCount[1] - spaceTabCount[1] != 1){
            fail(0, token);
        }
        if (!token.getType().equals("RETURN")){
            fail(1, token);
        }
        token = tokenIterator.next();
        if (!token.getType().equals("INT") && !token.getType().equals("FLOAT") &&
                !token.getType().equals("HEXNUM") && !token.getType().equals("OCTNUM") &&
                !token.getType().equals("BINNUM") && !token.getType().equals("STRING")){
            fail(2, token);
        }
        Node_AST exp = parseExpression(token);
        Node_AST statement = new Node_AST(new Token("return", "RETURN",
                                                        prev.getRow(), prev.getColumn()));
        statement.appendChild(exp);
        exp.setParent(statement);

        token = tokenIterator.next();
        if (!token.getType().equals("NEW_LINE")){
            fail(1, token);
        }

        return statement;
    }

    private Node_AST parseExpression(Token token) throws CompilerException {
        Node_AST intVar = new Node_AST(null, null);
        String value = token.getValue();

        switch (token.getType()){
            case "INT": {
                intVar = new Node_AST(token);
                break;
            }
            case "FLOAT": {
                StringBuilder casted = new StringBuilder();
                for (char ch: value.toCharArray()) {
                    if (ch == '.'){
                        break;
                    }
                    casted.append(ch);
                }
                intVar = new Node_AST(new Token(casted.toString(),
                        "INT(FLOAT)", token.getRow(), token.getColumn()));
                break;
            }
            case "HEXNUM": {
                intVar = new Node_AST(new Token(Long.decode(value).toString(),
                        "INT(HEXNUM)", token.getRow(), token.getColumn()));
                break;
            }
            case "OCTNUM": {
                intVar = new Node_AST(new Token(Integer.parseInt(value.substring(2), 8)+"",
                        "INT(OCTNUM)", token.getRow(), token.getColumn()));
                break;
            }
            case "BINNUM": {
                intVar = new Node_AST(new Token(Integer.parseInt(value.substring(2), 2)+"",
                        "INT(BINNUM)", token.getRow(), token.getColumn()));
                break;
            }
            case "STRING": {
                if (value.length() == 1){
                    intVar = new Node_AST(new Token(Character.getNumericValue(value.toCharArray()[0])+"",
                            "INT(CHAR)", token.getRow(), token.getColumn()));
                }
                else {
                    fail(2, token);
                }
                break;
            }
        }
        return intVar;
    }

    private Node_AST parseWord(Token prev, Iterator<Token> tokenIterator) throws CompilerException {
        Token token = tokenIterator.next();
        Node_AST word = new Node_AST(null, null);

        switch (token.getType()){
            case "LBR" : {
                word = parseDefCall(prev, tokenIterator);
                break;
            }
            default: {
                fail(3, token);
            }
        }

        return word;
    }

    private Node_AST parseDefCall(Token prev, Iterator<Token> tokenIterator) throws CompilerException {
        Token token = tokenIterator.next();

        if (!token.getType().equals("RBR")){
            fail(1, token);
        }
        token = tokenIterator.next();
        if (!token.getType().equals("NEW_LINE")){
            fail(1, token);
        }
        Node_AST defCall = new Node_AST(new Token(prev.getValue(), "DEF_CALL",
                prev.getRow(), prev.getColumn()));
        if (!defAST.containsKey(prev.getValue())){
            fail(4, prev);
        }
        else {
            defCall.appendChild(defAST.get(prev.getValue()).getRoot());
        }

        return defCall;
    }

    private void fail(int errId, Token token) throws CompilerException {
        String msg = "";

        switch (errId){
            case 0: {
                msg = "Incorrect tab count";
                break;
            }
            case 1: {
                msg = "Incorrect type";
                break;
            }
            case 2: {
                msg = "Cannot cast to INT";
                break;
            }
            case 3: {
                msg = "Unexpected token";
                break;
            }
            case 4: {
                msg = "Unknown method call";
                break;
            }
        }
        throw new CompilerException(msg, token.getRow(), token.getColumn());
    }

    public HashMap<String, AST> getDefAST() {
        return defAST;
    }

    public AST getMainAST() {
        return mainAST;
    }
}
