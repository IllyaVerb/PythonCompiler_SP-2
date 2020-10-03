import javafx.scene.Node;

import java.util.ArrayList;
import java.util.HashMap;

public class Parser {
    private final HashMap<String, AST> defAST;
    private final AST mainAST;
    private HashMap<String, String[]> templates;

    public Parser(ArrayList<Token> tokens) throws CompilerException {
        EnhancedIterator<Token> tokenEnhancedIterator = new EnhancedIterator<>(tokens);

        this.defAST = new HashMap<>();
        this.mainAST = new AST(tokenEnhancedIterator.next());
        this.templates = new HashMap<>();

        fillTemplates();
        parseProg("PROG", tokenEnhancedIterator);

        /*for (EnhancedIterator<Token> tokenIterator = new EnhancedIterator<>(tokens); tokenIterator.hasNext();) {

            parseTemplate("PROG", tokenIterator, null);
            mainAST.getRoot().getChild(0).appendChild(defAST.get("ret").getRoot());
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
        }*/
    }

    private void fillTemplates() {
        templates.put("PROG", new String[]{"FUNC", "CALL"});

        templates.put("CALL", new String[]{"WORD", "LBR", "RBR", "NEW_LINE"});
        templates.put("FUNC", new String[]{"DEF",  "WORD", "LBR", "RBR", "COLON", "NEW_LINE",
                                            "S", "STAT"});

        templates.put("STAT", new String[]{"RETURN", "EXP", "NEW_LINE"});
        templates.put("EXP", new String[]{"TERM", "BIN_1", "TERM"});
        templates.put("BIN_1", new String[]{"ADD|SUB"});
        //templates.put("EXP", new String[]{"_any", "TERM_BIN_EXP", "TERM", "_any", "_any"});

        //templates.put("TERM_BIN_EXP", new String[]{"TERM", "EXP"});

        templates.put("TERM", new String[]{"FACTOR", "BIN_2", "FACTOR"});
        templates.put("BIN_2", new String[]{"MUL|DIV"});
        //templates.put("TERM", new String[]{"_any", "TERM_BIN_TERM", "FACTOR", "_any", "_any"});

        //templates.put("TERM_BIN_TERM", new String[]{"TERM", "BIN_2", "TERM"});

        templates.put("FACTOR", new String[]{"BRACE_EXP", "UNAR_FACTOR",
                "INT|FLOAT|BINNUM|OCTNUM|HEXNUM|STRING", "_any"});
        templates.put("BRACE_EXP", new String[]{"LBR", "EXP", "RBR"});
        templates.put("UNAR_FACTOR", new String[]{"UNAR", "FACTOR"});
        //templates.put("RET_EXP", new String[]{"_any", "EXP", "_any", "INT|FLOAT|BINNUM|OCTNUM|HEXNUM|STRING", "_any"});
        templates.put("UNAR", new String[]{"SUB|NOT", "_any"});

        templates.put("S", new String[]{"TAB", "SPACE"});
    }

    private Node_AST parseTemplate(String key, EnhancedIterator<Token> tokenIterator, Token forAny) throws CompilerException {
        Node_AST returnNode = null;
        for (int i=0; i<templates.get(key).length; i++) {
            String part = templates.get(key)[i];
            if (part.equals("_any")){
                i++;
                while (!templates.get(key)[i].equals("_any")){
                    Node_AST tmp = parseTemplate(templates.get(key)[i], tokenIterator, forAny);
                    if (tmp != null){
                        return tmp;
                    }
                    i++;
                }
                i++;
                if (!templates.get(key)[i].equals("_any")) {
                    String[] any = templates.get(key)[i].split("\\|");
                    for (String oneOfAny : any) {
                        if (forAny.getType().equals(oneOfAny)) {
                            return new Node_AST(forAny);
                        }
                    }
                }
                return null;
            }
            if (part.equals(("_infZero"))){
                i++;
                Node_AST node_oper;
                if ((node_oper = parseTemplate(templates.get(key)[i], tokenIterator, tokenIterator.next())) != null){
                    //node_oper.
                }
            }
            if (templates.containsKey(part)) {
                switch (part) {
                    case "FUNC": {
                        AST tmp = new AST(parseTemplate("FUNC", tokenIterator, null));
                        defAST.put(tmp.getRoot().getCurrent().getValue(), tmp);
                        break;
                    }
                    case "STAT":{
                        Node_AST statement = parseTemplate("STAT", tokenIterator, null);
                        Node_AST def = new Node_AST(new Token("ret", "RETURN",
                                statement.getCurrent().getRow(), statement.getCurrent().getColumn()));
                        def.appendChild(statement);
                        statement.setParent(def);
                        returnNode = def;
                        break;
                    }
                    case "EXP":{
                        Node_AST exp = parseTemplate("EXP", tokenIterator, null);
                        Node_AST statement = new Node_AST(new Token(exp.getCurrent().getType(), "EXP",
                                exp.getCurrent().getRow(), exp.getCurrent().getColumn()));

                        statement.appendChild(exp);
                        exp.setParent(statement);
                        returnNode = statement;
                        break;
                    }
                    case "TERM":{
                        Node_AST factor = parseTemplate("TERM", tokenIterator,
                                forAny == null ? tokenIterator.next() : forAny);
                        if (factor == null)
                            return null;


                        break;
                    }
                    case "FACTOR":{
                        Node_AST factor = parseTemplate("FACTOR", tokenIterator,
                                forAny == null ? tokenIterator.next() : forAny);
                        if (factor == null)
                            return null;


                        break;
                    }
                    case "BRACE_EXP":{
                        if (tokenIterator.next().getType().equals("L_BRACE"))
                            return null;
                        Node_AST exp = parseTemplate("EXP", tokenIterator, null);
                        Token r_brace = tokenIterator.next();
                        if (r_brace.getType().equals("R_BRACE"))
                            fail(3, r_brace);

                        returnNode = exp;
                        break;
                    }
                    case "UNAR_FACTOR":{
                        Node_AST unar = parseTemplate("UNAR", tokenIterator,
                                forAny == null ? tokenIterator.next() : forAny);
                        if (unar == null)
                            return null;
                        Node_AST factor = parseTemplate("FACTOR", tokenIterator, tokenIterator.next());
                        if (factor == null)
                            fail(3, unar.getCurrent());

                        unar.appendChild(factor);
                        factor.setParent(unar);
                        returnNode = unar;
                        break;
                    }
                    case "UNAR":{
                        Node_AST unar = parseTemplate("UNAR", tokenIterator,
                                forAny == null ? tokenIterator.next() : forAny);
                        if (unar == null)
                            return null;
                        Node_AST child = parseTemplate("RET_EXP", tokenIterator, tokenIterator.next());
                        if (child == null)
                            fail(3, unar.getCurrent());

                        unar.appendChild(child);
                        child.setParent(unar);
                        returnNode = unar;
                        break;
                    }
                    case "CALL":{
                        parseTemplate("CALL", tokenIterator, null);
                        mainAST.getRoot().appendChild(new Node_AST(new Token("main", "DEF_CALL", 0, 0)));
                        break;
                    }
                }
            }
            else {
                Token token = tokenIterator.next();
                if (!token.getType().equals(part)){
                    fail(1, token);
                }
            }
        }
        return returnNode;
    }

    private void parseProg(String key, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        while (tokenEnhancedIterator.next().getType().equals("DEF")){
            tokenEnhancedIterator.previous();
            AST tmp = new AST(parseDef("FUNC", tokenEnhancedIterator));
            defAST.put(tmp.getRoot().getCurrent().getValue(), tmp);
        }
        tokenEnhancedIterator.previous();
        while (tokenEnhancedIterator.hasNext()){
            Node_AST call = parseDefCall("CALL", tokenEnhancedIterator);
            if (!defAST.containsKey(call.getCurrent().getValue()))
                fail(4, call.getCurrent());
            call.appendChild(defAST.get(call.getCurrent().getValue()).getRoot());
            mainAST.getRoot().appendChild(call);
        }
    }

    private Node_AST parseDef(String key, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        int[] currentSpaceTabCount = {0, 0};
        String defName = "";
        Token token;
        Node_AST def = null;

        for (String part : templates.get(key)) {
            token = tokenEnhancedIterator.next();
            switch (part){
                case "S":{
                    while (token.getType().matches("(TAB)|(SPACE)")) {
                        if (token.getType().equals("TAB")){
                            currentSpaceTabCount[1]++;
                        }
                        else {
                            currentSpaceTabCount[0]++;
                        }
                        token = tokenEnhancedIterator.next();
                    }
                    if (currentSpaceTabCount[0] + currentSpaceTabCount[1] == 0){
                        fail(0, token);
                    }
                    break;
                }
                case "STAT":{
                    tokenEnhancedIterator.previous();
                    Node_AST statement = parseStat("STAT", tokenEnhancedIterator);
                    def = new Node_AST(new Token(defName, "DEF_WORD",
                            token.getRow(), token.getColumn()));
                    def.appendChild(statement);
                    statement.setParent(def);
                    break;
                }
                case "WORD":{
                    defName = token.getValue();
                    break;
                }
                default:{
                    if (!token.getType().equals(part)){
                        fail(1, token);
                    }
                }
            }
        }

        return def;
    }

    private Node_AST parseStat(String key, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token;
        Node_AST statement = null;
        tokenEnhancedIterator.previous();
        for (String part : templates.get(key)) {
            token = tokenEnhancedIterator.next();
            if (part.equals("EXP")){
                tokenEnhancedIterator.previous();
                Node_AST exp = parseExp("EXP", tokenEnhancedIterator);
                statement = new Node_AST(new Token("return", "RETURN",
                        token.getRow(), token.getColumn()));
                statement.appendChild(exp);
                exp.setParent(statement);
            }
            else {
                if (!token.getType().equals(part)) {
                    fail(1, token);
                }
            }
        }
        return statement;
    }

    private Node_AST parseExp(String key, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Node_AST term = parseTerm("TERM", tokenEnhancedIterator),
                    oper = null;
        Token token = tokenEnhancedIterator.next();
        tokenEnhancedIterator.previous();
        while (token.getType().matches("(ADD)|(SUB)")){
            tokenEnhancedIterator.next();
            oper = new Node_AST(token);
            Node_AST nextTerm = parseTerm("TERM", tokenEnhancedIterator);
            oper.appendChild(term);
            oper.appendChild(nextTerm);
            term.setParent(oper);
            nextTerm.setParent(oper);
            token = tokenEnhancedIterator.next();
            tokenEnhancedIterator.previous();
        }

        return oper == null ? term : oper;
    }

    private Node_AST parseTerm(String key, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Node_AST factor = parseFactor("FACTOR", tokenEnhancedIterator),
                oper = null;
        Token token = tokenEnhancedIterator.next();
        tokenEnhancedIterator.previous();
        while (token.getType().matches("(MUL)|(DIV)|(INT_DIV)")){
            tokenEnhancedIterator.next();
            oper = new Node_AST(token);
            Node_AST nextTerm = parseFactor("FACTOR", tokenEnhancedIterator);
            oper.appendChild(factor);
            oper.appendChild(nextTerm);
            factor.setParent(oper);
            nextTerm.setParent(oper);
            token = tokenEnhancedIterator.next();
            tokenEnhancedIterator.previous();
        }

        return oper == null ? factor : oper;
    }

    private Node_AST parseFactor(String key, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token = tokenEnhancedIterator.next();

        Node_AST oper;
        if (token.getType().equals("LBR")){
            Node_AST exp = parseExp("EXP", tokenEnhancedIterator);
            if (!tokenEnhancedIterator.next().getType().equals("RBR")){
                fail(5, tokenEnhancedIterator.next());
            }
            return exp;
        }
        else {
            if (token.getType().matches("(ADD)|(SUB)|(NOT)")){
                oper = new Node_AST(new Token(token.getValue(), "UNAR_"+token.getType(),
                                                token.getRow(), token.getColumn()));
                Node_AST nextTerm = parseFactor("FACTOR", tokenEnhancedIterator);
                oper.appendChild(nextTerm);
                nextTerm.setParent(oper);
                return oper;
            }
            else {
                if (token.getType().matches("(INT)|(FLOAT)|(BINNUM)|(OCTNUM)|(HEXNUM)|(STRING)")){
                    return parseExpression(token);
                }
                else {
                    fail(3, token);
                }
            }
        }
        return null;
    }

    private Node_AST parseExpression(Token token) throws CompilerException {
        String value = token.getValue();

        switch (token.getType()){
            case "INT": {
                return new Node_AST(token);
            }
            case "FLOAT": {
                StringBuilder casted = new StringBuilder();
                for (char ch: value.toCharArray()) {
                    if (ch == '.'){
                        break;
                    }
                    casted.append(ch);
                }
                return new Node_AST(new Token(casted.toString(),
                        "INT(FLOAT)", token.getRow(), token.getColumn()));
            }
            case "HEXNUM": {
                return new Node_AST(new Token(Long.decode(value).toString(),
                        "INT(HEXNUM)", token.getRow(), token.getColumn()));
            }
            case "OCTNUM": {
                return new Node_AST(new Token(Integer.parseInt(value.substring(2), 8)+"",
                        "INT(OCTNUM)", token.getRow(), token.getColumn()));
            }
            case "BINNUM": {
                return new Node_AST(new Token(Integer.parseInt(value.substring(2), 2)+"",
                        "INT(BINNUM)", token.getRow(), token.getColumn()));
            }
            case "STRING": {
                if (value.length() == 3){
                    return new Node_AST(new Token((int)value.toCharArray()[1]+"",
                            "INT(CHAR)", token.getRow(), token.getColumn()));
                }
                else {
                    fail(2, token);
                }
                break;
            }
        }
        return null;
    }

    private Node_AST parseDefCall(String key, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token;
        Node_AST defCall = null;
        for (String part : templates.get(key)) {
            token = tokenEnhancedIterator.next();
            if (part.equals("WORD")){
                defCall = new Node_AST(new Token(token.getValue(), "DEF_CALL",
                        token.getRow(), token.getColumn()));
            }
            else {
                if (!token.getType().equals(part)) {
                    fail(1, token);
                }
            }
        }

        return defCall;
    }

    private void fail(int errId, Token token) throws CompilerException {
        String msg;

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
            case 5: {
                msg = "Expected ')'";
                break;
            }
            default: msg = "Unknown error";
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
