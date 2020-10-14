import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Parser {
    private final HashMap<String, AST> defAST;
    private final AST mainAST;
    private final HashMap<String, String[]> templates;
    private final HashMap<Integer, String> priority;

    public Parser(ArrayList<Token> tokens) throws CompilerException {
        EnhancedIterator<Token> tokenEnhancedIterator = new EnhancedIterator<>(tokens);

        this.defAST = new HashMap<>();
        this.mainAST = new AST(tokenEnhancedIterator.next());
        this.templates = new HashMap<>();
        this.priority = new HashMap<>();

        fillTemplates();
        initPriority();
        parseProg(tokenEnhancedIterator);
    }

    private void initPriority() {
        //priority.put(4, "POW");
        //priority.put(5, "(ADD)|(SUB)|(BIT_NOT)");
        priority.put(6, "(MUL)|(DIV)|(INT_DIV)|(PERCENT)");
        priority.put(7, "(ADD)|(SUB)");
        priority.put(8, "(L_SHIFT)|(R_SHIFT)");
        priority.put(9, "BIT_AND");
        priority.put(10, "BIT_XOR");
        priority.put(11, "BIT_OR");
        priority.put(12, "(IN)|(IS)|(LT)|(GT)|(LE)|(GE)|(NE)|(EQ)");
        priority.put(13, "NOT");
        priority.put(14, "AND");
        priority.put(15, "OR");
    }

    private void fillTemplates() {
        templates.put("PROG", new String[]{"FUNC", "CALL"});

        templates.put("CALL", new String[]{"WORD", "LBR", "RBR", "NEW_LINE"});
        templates.put("FUNC", new String[]{"DEF",  "WORD", "LBR", "RBR", "COLON", "NEW_LINE",
                                            "S", "STAT"});

        templates.put("STAT", new String[]{"RETURN", "EXP", "NEW_LINE"});
        templates.put("EXP", new String[]{"WORD", "ASSIGN", "EXP|ADDD"});
        templates.put("ADDD", new String[]{"TERM", "BIN_1", "TERM"});
        templates.put("BIN_1", new String[]{"ADD|SUB"});

        templates.put("TERM", new String[]{"FACTOR", "BIN_2", "FACTOR"});
        templates.put("BIN_2", new String[]{"MUL|DIV"});

        templates.put("FACTOR", new String[]{"BRACE_EXP", "UNAR_FACTOR",
                "INT|FLOAT|BINNUM|OCTNUM|HEXNUM|STRING"});
        templates.put("BRACE_EXP", new String[]{"LBR", "EXP", "RBR"});
        templates.put("UNAR_FACTOR", new String[]{"UNAR", "FACTOR"});
        templates.put("UNAR", new String[]{"SUB|NOT"});

        templates.put("S", new String[]{"TAB", "SPACE"});
    }

    private void parseProg(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        while (tokenEnhancedIterator.next().getType().equals("DEF")){
            tokenEnhancedIterator.previous();
            AST tmp = new AST(parseFunc(tokenEnhancedIterator));
            defAST.put(tmp.getRoot().getCurrent().getValue(), tmp);
        }
        tokenEnhancedIterator.previous();
        while (tokenEnhancedIterator.hasNext()){
            Node_AST call = parseDefCall("CALL", tokenEnhancedIterator);
            if (!defAST.containsKey(call.getCurrent().getValue()))
                fail(4, call.getCurrent());
            call.appendChildren(defAST.get(call.getCurrent().getValue()).getRoot().getChildren());
            mainAST.getRoot().appendChild(call);
        }
    }

    private Node_AST parseFunc(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        int currentDefSpaceTabCount = -1;
        Token defName;
        Token token;
        ArrayList<Node_AST> statements;

        token = tokenEnhancedIterator.next();
        if (!token.getType().equals("DEF")){
            fail(1, token);
        }

        token = tokenEnhancedIterator.next();
        if (!token.getType().equals("WORD")){
            fail(1, token);
        }
        defName = token;

        token = tokenEnhancedIterator.next();
        if (!token.getType().equals("LBR")){
            fail(1, token);
        }

        token = tokenEnhancedIterator.next();
        if (!token.getType().equals("RBR")){
            fail(1, token);
        }

        token = tokenEnhancedIterator.next();
        if (!token.getType().equals("COLON")){
            fail(1, token);
        }

        token = tokenEnhancedIterator.next();
        if (!token.getType().equals("NEW_LINE")){
            fail(1, token);
        }

        token = tokenEnhancedIterator.next();
        if (!token.getType().equals("TAB") && !token.getType().equals("SPACE")){
            fail(1, token);
        }

        tokenEnhancedIterator.previous();
        statements = new ArrayList<>();
        while(token.getType().equals("TAB") || token.getType().equals("SPACE")){
            int tmpSpaceTabCount = 0;
            tokenEnhancedIterator.next();

            while (token.getType().matches("(TAB)|(SPACE)")) {
                if (token.getType().equals("TAB"))
                    tmpSpaceTabCount += 8;
                else
                    tmpSpaceTabCount++;
                token = tokenEnhancedIterator.next();
            }

            if (currentDefSpaceTabCount == -1)
                currentDefSpaceTabCount = tmpSpaceTabCount;
            if (tmpSpaceTabCount != currentDefSpaceTabCount){
                fail(0, token);
            }
            tokenEnhancedIterator.previous();

            statements.add(parseStat(tokenEnhancedIterator));

            token = tokenEnhancedIterator.next();
            tokenEnhancedIterator.previous();
        }
        Node_AST def = new Node_AST(new Token(defName.getValue(), "DEF_WORD",
                defName.getRow(), defName.getColumn()));

        def.appendChildren(statements);
        for (Node_AST child: statements) {
            child.setParent(def);
        }

        return def;
    }

    private Node_AST parseStat(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token;

        token = tokenEnhancedIterator.next();
        tokenEnhancedIterator.previous();
        if (token.getType().equals("RETURN")){
            tokenEnhancedIterator.next();
            Node_AST returnNode = new Node_AST(new Token("return", "RETURN",
                    token.getRow(), token.getColumn())),
                    retExp = parseExp(tokenEnhancedIterator);

            returnNode.appendChild(retExp);
            retExp.setParent(returnNode);

            token = tokenEnhancedIterator.next();
            if (!token.getType().equals("NEW_LINE")){
                fail(1, token);
            }

            return returnNode;
        }
        else {
            Node_AST exp = parseExp(tokenEnhancedIterator);

            token = tokenEnhancedIterator.next();
            if (!token.getType().equals("NEW_LINE")){
                fail(1, token);
            }

            return exp;
        }
    }

    private Node_AST parseExp(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token = tokenEnhancedIterator.next(),
                token2 = tokenEnhancedIterator.next();

        tokenEnhancedIterator.previous();
        tokenEnhancedIterator.previous();
        if (token.getType().equals("WORD") && token2.getType().equals("ASSIGN")){
            tokenEnhancedIterator.next();
            tokenEnhancedIterator.next();
            Node_AST id = new Node_AST(new Token(token.getValue(), "ID", token.getRow(), token.getColumn()));

            Node_AST assign = new Node_AST(token2),
                    exp = parseExp(tokenEnhancedIterator);

            assign.appendChild(exp);
            exp.setParent(assign);

            id.appendChild(assign);
            assign.setParent(id);

            return id;
        }
        else {
            return parsePriority(15, tokenEnhancedIterator);
        }
    }

    private Node_AST parsePriority(int prior, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Node_AST topOperSign = null, left;
        if (prior <= 6)
            left = parseFactor(tokenEnhancedIterator);
        else
            left = parsePriority(prior-1, tokenEnhancedIterator);

        ArrayList<Node_AST> nodeQueue = new ArrayList<>(),
                            operQueue = new ArrayList<>();
        nodeQueue.add(left);

        Token token = tokenEnhancedIterator.next();
        tokenEnhancedIterator.previous();

        if (token.getType().matches(priority.get(prior))) {
            while (token.getType().matches(priority.get(prior))) {
                tokenEnhancedIterator.next();

                operQueue.add(new Node_AST(token));

                Node_AST node;
                if (prior <= 6)
                    node = parseFactor(tokenEnhancedIterator);
                else
                    node = parsePriority(prior-1, tokenEnhancedIterator);
                nodeQueue.add(node);

                token = tokenEnhancedIterator.next();
                tokenEnhancedIterator.previous();
            }
            Collections.reverse(operQueue);
            Collections.reverse(nodeQueue);

            EnhancedIterator<Node_AST> enhancedIteratorOper = new EnhancedIterator<>(operQueue);
            EnhancedIterator<Node_AST> enhancedIteratorNode = new EnhancedIterator<>(nodeQueue);
            topOperSign = enhancedIteratorOper.next();
            topOperSign.appendChild(enhancedIteratorNode.next());

            while (enhancedIteratorOper.hasNext()) {
                Node_AST deepest = topOperSign.getDeepestLeft(),
                        tmpOper = enhancedIteratorOper.next(),
                        tmpNode = enhancedIteratorNode.next();

                tmpOper.appendChild(tmpNode);
                tmpNode.setParent(tmpOper);

                deepest.setFirstChild(tmpOper);
                tmpOper.setParent(deepest);
            }
            topOperSign.getDeepestLeft().setFirstChild(enhancedIteratorNode.next());
        }
        return topOperSign == null ? left : topOperSign;
    }

    private Node_AST parseFactor(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token = tokenEnhancedIterator.next();

        Node_AST oper;
        if (token.getType().equals("LBR")){
            Node_AST exp = parseExp(tokenEnhancedIterator);
            if (!tokenEnhancedIterator.next().getType().equals("RBR")){
                fail(5, tokenEnhancedIterator.next());
            }
            return exp;
        }
        else {
            if (token.getType().matches("(ADD)|(SUB)|(NOT)")){
                oper = new Node_AST(new Token(token.getValue(), "UNAR_"+token.getType(),
                                                token.getRow(), token.getColumn()));
                Node_AST nextFactor = parseFactor(tokenEnhancedIterator);
                oper.appendChild(nextFactor);
                nextFactor.setParent(oper);
                return oper;
            }
            else {
                if (token.getType().equals("WORD")){
                    return new Node_AST(new Token(token.getValue(), "ID", token.getRow(), token.getColumn()));
                }
                else {
                    if (token.getType().matches("(INT)|(FLOAT)|(BINNUM)|(OCTNUM)|(HEXNUM)|(STRING)")) {
                        return parseExpression(token);
                    } else {
                        fail(3, token);
                    }
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
