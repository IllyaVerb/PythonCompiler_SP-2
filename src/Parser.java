import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Parser {
    private ArrayList<Token> tokens;
    private HashMap<String, AST> defAST;
    private AST mainAST;

    public Parser(ArrayList<Token> tokens){
        this.tokens = tokens;
        this.defAST = new HashMap<>();
        this.mainAST = new AST(new Token(null, "START"));

        for (Iterator<Token> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
            Token token = tokenIterator.next();
            switch (token.getType()){
                case "DEF": {
                    AST tmp = new AST(parseDef(tokenIterator));
                    defAST.put(tmp.getRoot().getCurrent().getValue(), tmp);
                    break;
                }
                case "WORD": {

                    break;
                }
                default:
                    System.out.println(token.getValue() + " " + token.getType());
            }
        }
    }

    private Node_AST parseDef(Iterator<Token> tokenIterator){
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

        Node_AST statement = parseStatement(tokenIterator, currentSpaceTabCount);
        Node_AST def = new Node_AST(new Token(defName, "DEF_WORD"));
        def.appendChild(statement);
        statement.setParent(def);

        return def;
    }

    private Node_AST parseStatement(Iterator<Token> tokenIterator, int[] spaceTabCount){
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
        Node_AST statement = new Node_AST(new Token("return", "RETURN"));
        statement.appendChild(exp);
        exp.setParent(statement);

        token = tokenIterator.next();
        if (!token.getType().equals("NEW_LINE")){
            fail(1, token);
        }

        return statement;
    }

    private Node_AST parseExpression(Token token){
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
                intVar = new Node_AST(new Token(casted.toString(), "INT(FLOAT)"));
                break;
            }
            case "HEXNUM": {
                intVar = new Node_AST(new Token(Long.decode(value).toString(), "INT(HEXNUM)"));
                break;
            }
            case "OCTNUM": {
                intVar = new Node_AST(new Token(Integer.parseInt(value.substring(2), 8)+"",
                        "INT(OCTNUM)"));
                break;
            }
            case "BINNUM": {
                intVar = new Node_AST(new Token(Integer.parseInt(value.substring(2), 2)+"",
                        "INT(BINNUM)"));
                break;
            }
            case "STRING": {
                if (value.length() == 1){
                    intVar = new Node_AST(new Token(Character.getNumericValue(value.toCharArray()[0])+"",
                            "INT(CHAR)"));
                }
                else {
                    fail(2, token);
                }
                break;
            }
        }
        return intVar;
    }



    private void fail(int errId, Token token){
        String msg = "";

        switch (errId){
            case 0: {
                msg = "Incorrect tab count in (" + token.getValue() + ") token";
                break;
            }
            case 1: {
                msg = "Incorrect type in (" + token.getValue() + ") token";
                break;
            }
            case 2: {
                msg = "Incorrect return type in (" + token.getValue() + ") token";
                break;
            }
        }
        System.out.println(msg);
    }

    public HashMap<String, AST> getDefAST() {
        return defAST;
    }
}
