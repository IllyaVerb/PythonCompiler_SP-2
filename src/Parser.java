import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Parser {
    private final AST mainAST;
    /* AST for all functions */
    private final HashMap<String, AST> defAST;
    /* map for all templates */
    private final HashMap<String, String[]> templates;
    /* map for operation tokens with different priority */
    private final HashMap<Integer, String> priority;

    public Parser(ArrayList<Token> tokens) throws CompilerException {
        EnhancedIterator<Token> tokenEnhancedIterator = new EnhancedIterator<>(tokens);

        /* initialise all global variables */
        this.defAST = new HashMap<>();
        this.mainAST = new AST(tokenEnhancedIterator.next());
        this.templates = new HashMap<>();
        this.priority = new HashMap<>();

        /* use methods that fill maps */
        fillTemplates();
        initPriority();

        /* start parsing */
        parseProg(tokenEnhancedIterator);
    }

    /**
     * fill priority map by token types
     */
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

    /**
     * fill templates for definition high-level tokens
     * (deprecated)
     */
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

    /**
     * test is next token has correct type
     * @param enhancedIterator - iterator
     * @param type - type to be needed
     * @param errId - error id when exception
     * @return - this next token, if it need to be used
     * @throws CompilerException - fail will produce this exception
     */
    private Token isLikeTemplate(EnhancedIterator<Token> enhancedIterator, String type, int errId)
            throws CompilerException {
        Token token = enhancedIterator.next();
        if (!token.getType().equals(type)){
            fail(errId, token);
        }
        return token;
    }

    /**
     * parse Program type
     * @param tokenEnhancedIterator - iterator
     * @throws CompilerException - fail will produce this exception
     */
    private void parseProg(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        /* parsing program like a block */
        while (tokenEnhancedIterator.hasNext()){
            Node_AST node = parseStat(0, tokenEnhancedIterator);

            switch (node.getCurrent().getType()){
                case "DEF_WORD": {

                    /* create function AST */
                    AST tmp = new AST(node);

                    /* add AST to map */
                    if (defAST.containsKey(node.getCurrent().getValue()))
                        defAST.replace(tmp.getRoot().getCurrent().getValue(), tmp);
                    else
                        defAST.put(tmp.getRoot().getCurrent().getValue(), tmp);

                    break;
                }
                default:{
                    node.setParent(mainAST.getRoot());
                    mainAST.getRoot().appendChild(node);
                }
            }
        }
    }

    /**
     * parse Block, as '{ TAB | SPACE } STAT'
     * @param prevSpaceTabCount - count of tabs and spaces common for previous structure
     * @param tokenEnhancedIterator - iterator
     * @return - list of statements
     * @throws CompilerException - fail will produce this exception
     */
    private ArrayList<Node_AST> parseBlock(int prevSpaceTabCount, EnhancedIterator<Token> tokenEnhancedIterator)
            throws CompilerException {
        ArrayList<Node_AST> statements = new ArrayList<>();
        int currentSpaceTabCount = -1;

        Token token = tokenEnhancedIterator.peek();
        do {
            int tmpSpaceTabCount = 0, spaceTokenCount = 0;
            tokenEnhancedIterator.next();

            /* count all tabs and spaces */
            while (token.getType().matches("(TAB)|(SPACE)")) {
                spaceTokenCount++;
                if (token.getType().equals("TAB"))
                    tmpSpaceTabCount += 8;
                else
                    tmpSpaceTabCount++;
                token = tokenEnhancedIterator.next();
            }

            /* initialise space count for first statement */
            if (currentSpaceTabCount == -1)
                currentSpaceTabCount = tmpSpaceTabCount-prevSpaceTabCount;

            /* go back before spaces */
            if (tmpSpaceTabCount <= prevSpaceTabCount){
                for (int i = 0; i < spaceTokenCount; i++) {
                    tokenEnhancedIterator.previous();
                }
                break;
            }

            /* error if count of spaces is incorrect */
            if (tmpSpaceTabCount - prevSpaceTabCount != currentSpaceTabCount){
                System.err.printf("Expected %d spaces, but found %d!\n",
                        currentSpaceTabCount+prevSpaceTabCount, tmpSpaceTabCount);
                fail(0, token);
            }
            tokenEnhancedIterator.previous();

            statements.add(parseStat(tmpSpaceTabCount, tokenEnhancedIterator));

            if (!tokenEnhancedIterator.hasNext())
                return statements;

            token = tokenEnhancedIterator.peek();
        }while(true);

        tokenEnhancedIterator.previous();

        return statements;
    }

    /**
     * parse DEF | RETURN | IF | ELIF | ELSE, or expression
     * @param currentSpaceCount - spaces count in current statement
     * @param tokenEnhancedIterator - iterator
     * @return - statement node
     * @throws CompilerException - fail will produce this exception
     */
    private Node_AST parseStat(int currentSpaceCount, EnhancedIterator<Token> tokenEnhancedIterator)
            throws CompilerException {
        Token token;

        token = tokenEnhancedIterator.peek();
        switch (token.getType()){

            /* create function statement: 'DEF WORD "(" ")" ":" NEW_LINE { STAT }' */
            case "DEF":{
                tokenEnhancedIterator.next();

                /* test if tokens is correctly placed for function */
                Token defToken = isLikeTemplate(tokenEnhancedIterator, "WORD", 1);
                isLikeTemplate(tokenEnhancedIterator, "LBR", 1);

                /* parsing method parameters */
                Node_AST params = parseParams(tokenEnhancedIterator, true);

                isLikeTemplate(tokenEnhancedIterator, "RBR", 1);
                isLikeTemplate(tokenEnhancedIterator, "COLON", 1);
                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                Node_AST def = new Node_AST(new Token(defToken.getValue(), "DEF_WORD",
                        defToken.getRow(), defToken.getColumn()));

                params.setParent(def);
                def.appendChild(params);

                /* parse function block */
                ArrayList<Node_AST> statements = parseBlock(currentSpaceCount, tokenEnhancedIterator);

                def.appendChildren(statements);
                for (Node_AST child: statements) {
                    child.setParent(def);
                }

                return def;
            }

            /* return statement: 'RETURN <EXP> NEW_LINE' */
            case "RETURN":{
                tokenEnhancedIterator.next();
                Node_AST returnNode = new Node_AST(token),
                        /* parse exp in return statement */
                        retExp = parseExp(tokenEnhancedIterator);

                /* set family relations */
                returnNode.appendChild(retExp);
                retExp.setParent(returnNode);

                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                return returnNode;
            }

            case "BREAK":
            case "CONTINUE": {
                tokenEnhancedIterator.next();
                Node_AST oneWordNode = new Node_AST(token);

                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                return oneWordNode;
            }

            /* if statement: 'IF <EXP> ":" NEW_LINE { <STAT> }' */
            case "IF":{
                tokenEnhancedIterator.next();
                Node_AST ifNode = new Node_AST(token);

                /* parse exp in if statement */
                Node_AST    ifExp = parseExp(tokenEnhancedIterator),
                            ifTrue = new Node_AST(new Token("iftrue", "IF_TRUE",
                        tokenEnhancedIterator.current().getRow(), tokenEnhancedIterator.current().getColumn()));

                isLikeTemplate(tokenEnhancedIterator, "COLON", 1);
                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                /* parse block after if */
                ArrayList<Node_AST> ifBlock = parseBlock(currentSpaceCount, tokenEnhancedIterator);

                /* set family relations */
                ifTrue.appendChildren(ifBlock);
                for (Node_AST child: ifBlock) {
                    child.setParent(ifTrue);
                }

                ifNode.appendChild(ifExp);
                ifNode.appendChild(ifTrue);
                ifExp.setParent(ifNode);
                ifTrue.setParent(ifNode);

                return ifNode;
            }

            /* elif statement: 'ELIF <EXP> ":" NEW_LINE { <STAT> }' */
            case "ELIF":{
                tokenEnhancedIterator.next();

                Node_AST elifNode = new Node_AST((token));

                /* parse exp in elif statement */
                Node_AST    elifExp = parseExp(tokenEnhancedIterator),
                            elifTrue = new Node_AST(new Token("eliftrue", "ELIF_TRUE",
                        tokenEnhancedIterator.current().getRow(), tokenEnhancedIterator.current().getColumn()));

                isLikeTemplate(tokenEnhancedIterator, "COLON", 1);
                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                /* parse block after elif */
                ArrayList<Node_AST> elifBlock = parseBlock(currentSpaceCount, tokenEnhancedIterator);

                /* set family relations */
                elifTrue.appendChildren(elifBlock);
                for (Node_AST child: elifBlock) {
                    child.setParent(elifTrue);
                }

                elifNode.appendChild(elifExp);
                elifNode.appendChild(elifTrue);
                elifExp.setParent(elifNode);
                elifTrue.setParent(elifNode);

                return elifNode;
            }

            /* elif statement: 'ELSE ":" NEW_LINE { <STAT> }' */
            case "ELSE":{
                tokenEnhancedIterator.next();

                Node_AST elseNode = new Node_AST((token));

                isLikeTemplate(tokenEnhancedIterator, "COLON", 1);
                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                ArrayList <Node_AST> elseBlock = parseBlock(currentSpaceCount, tokenEnhancedIterator);

                /* set family relations */
                elseNode.appendChildren(elseBlock);
                for (Node_AST child: elseBlock) {
                    child.setParent(elseNode);
                }

                return elseNode;
            }

            /* for statement: 'FOR WORD IN RANGE "(" <EXP> [ "," <EXP> [ "," <EXP> ] ] ")" ":" NEW_LINE { <STAT> }' */
            case "FOR": {
                tokenEnhancedIterator.next();
                Token token_tmp = tokenEnhancedIterator.next();

                Node_AST forNode = new Node_AST((token)),
                        forVar = null,
                        forAssign = new Node_AST(new Token("=", "ASSIGN",
                            token_tmp.getRow(), token_tmp.getColumn())),
                        forAssignThird = new Node_AST(new Token("=", "ASSIGN",
                            token_tmp.getRow(), token_tmp.getColumn())),
                        forLessThan = new Node_AST(new Token("<", "LT",
                            token_tmp.getRow(), token_tmp.getColumn())),
                        forAdd = new Node_AST(new Token("+", "ADD",
                            token_tmp.getRow(), token_tmp.getColumn()));

                if (token_tmp.getType().equals("WORD")){
                    forVar = new Node_AST(new Token(token_tmp.getValue(), "ID",
                            token_tmp.getRow(), token_tmp.getColumn()));
                }
                else {
                    if (token_tmp.getType().equals("DOWN_LINE")){
                        forVar = new Node_AST(new Token("_tmp_for_var", "ID",
                                token_tmp.getRow(), token_tmp.getColumn()));
                    }
                    else {
                        fail(1, token_tmp);
                    }
                }

                isLikeTemplate(tokenEnhancedIterator, "IN", 1);
                isLikeTemplate(tokenEnhancedIterator, "RANGE", 1);
                isLikeTemplate(tokenEnhancedIterator, "LBR", 7);

                Node_AST fExp = parseExp(tokenEnhancedIterator), sExp, tExp;

                if (tokenEnhancedIterator.peek().getType().equals("COMMA")){
                    tokenEnhancedIterator.next();
                    sExp = parseExp(tokenEnhancedIterator);
                    if (tokenEnhancedIterator.peek().getType().equals("COMMA")){
                        tokenEnhancedIterator.next();
                        tExp = parseExp(tokenEnhancedIterator);
                    }
                    else {
                        tExp = new Node_AST(new Token("1", "INT",
                                tokenEnhancedIterator.current().getRow(),
                                tokenEnhancedIterator.current().getColumn()));
                    }

                    /* set relationship for first data and ASSIGN */
                    fExp.setParent(forAssign);
                    forAssign.appendChild(fExp);
                    /* set relationship for ASSIGN and cycle variable */
                    forAssign.setParent(forVar);
                    forVar.appendChild(forAssign);
                    /* set relationship for cycle variable and FOR node */
                    forVar.setParent(forNode);
                    forNode.appendChild(forVar);

                    /* create copy of variable */
                    Node_AST forVarSec = new Node_AST(forVar.getCurrent());
                    /* set relationship for cycle variable and LT */
                    forVarSec.setParent(forLessThan);
                    forLessThan.appendChild(forVarSec);
                    /* set relationship for second data and LT */
                    sExp.setParent(forLessThan);
                    forLessThan.appendChild(sExp);
                }
                else {
                    sExp = new Node_AST(new Token("0", "INT",
                            tokenEnhancedIterator.current().getRow(),
                            tokenEnhancedIterator.current().getColumn()));
                    tExp = new Node_AST(new Token("1", "INT",
                            tokenEnhancedIterator.current().getRow(),
                            tokenEnhancedIterator.current().getColumn()));

                    /* set relationship for first data and ASSIGN */
                    sExp.setParent(forAssign);
                    forAssign.appendChild(sExp);
                    /* set relationship for ASSIGN and cycle variable */
                    forAssign.setParent(forVar);
                    forVar.appendChild(forAssign);
                    /* set relationship for cycle variable and FOR node */
                    forVar.setParent(forNode);
                    forNode.appendChild(forVar);

                    /* create copy of variable */
                    Node_AST forVarSec = new Node_AST(forVar.getCurrent());
                    /* set relationship for cycle variable and LT */
                    forVarSec.setParent(forLessThan);
                    forLessThan.appendChild(forVarSec);
                    /* set relationship for second data and LT */
                    fExp.setParent(forLessThan);
                    forLessThan.appendChild(fExp);
                }

                /* set relationship for LT and FOR node */
                forLessThan.setParent(forNode);
                forNode.appendChild(forLessThan);

                isLikeTemplate(tokenEnhancedIterator, "RBR", 5);

                /* create copy of variable for ADD and for ASSIGN */
                Node_AST    forVarThird = new Node_AST(forVar.getCurrent()),
                            forVarInit = new Node_AST(forVar.getCurrent());
                /* set relationship for cycle variable and ADD */
                forVarThird.setParent(forAdd);
                forAdd.appendChild(forVarThird);
                /* set relationship for third data and ADD */
                tExp.setParent(forAdd);
                forAdd.appendChild(tExp);
                /* set relationship for LT and ASSIGN node */
                forAdd.setParent(forAssignThird);
                forAssignThird.appendChild(forAdd);
                /* set relationship for ASSIGN and VAR node */
                forAssignThird.setParent(forVarInit);
                forVarInit.appendChild(forAssignThird);
                /* set relationship for VAR and FOR node */
                forVarInit.setParent(forNode);
                forNode.appendChild(forVarInit);

                isLikeTemplate(tokenEnhancedIterator, "COLON", 1);
                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                Node_AST forBody = new Node_AST(new Token("for_body", "FOR_BODY",
                        token_tmp.getRow(), token_tmp.getColumn()));

                ArrayList <Node_AST> forBlock = parseBlock(currentSpaceCount, tokenEnhancedIterator);

                /* set family relations */
                forBody.appendChildren(forBlock);
                for (Node_AST child: forBlock) {
                    child.setParent(forBody);
                }

                forBody.setParent(forNode);
                forNode.appendChild(forBody);

                return forNode;
            }

            /* while statement: 'WHILE "(" <EXP> ")" ":" NEW_LINE' */
            case "WHILE": {
                tokenEnhancedIterator.next();

                Node_AST whileNode = new Node_AST(token), whileExp = parseExp(tokenEnhancedIterator);

                whileExp.setParent(whileNode);
                whileNode.appendChild(whileExp);

                isLikeTemplate(tokenEnhancedIterator, "COLON", 1);
                Token body = isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                Node_AST whileBody = new Node_AST(new Token("while_body", "WHILE_BODY",
                        body.getRow(), body.getColumn()));

                ArrayList <Node_AST> whileBlock = parseBlock(currentSpaceCount, tokenEnhancedIterator);

                /* set family relations */
                whileBody.appendChildren(whileBlock);
                for (Node_AST child: whileBlock) {
                    child.setParent(whileBody);
                }

                whileBody.setParent(whileNode);
                whileNode.appendChild(whileBody);

                return whileNode;
            }

            /* else variant such as create var, or do something: <EXP> */
            default: {
                Node_AST exp = parseExp(tokenEnhancedIterator);

                isLikeTemplate(tokenEnhancedIterator, "NEW_LINE", 1);

                return exp;
            }
        }
    }

    /**
     * parsing parameters of functions for defining and calling
     * @param tokenEnhancedIterator - iterator
     * @param isDef - flag showing is this parameters in defining, or in calling
     * @return - parameters node
     * @throws CompilerException - fail will produce this exception
     */
    private Node_AST parseParams(EnhancedIterator<Token> tokenEnhancedIterator, boolean isDef)
            throws CompilerException {
        Node_AST params = new Node_AST(new Token("params", "PARAMS",
                tokenEnhancedIterator.peek().getRow(), tokenEnhancedIterator.peek().getColumn()));
        Token token = tokenEnhancedIterator.peek();

        while (!token.getType().equals("RBR")){
            if (isDef) {
                tokenEnhancedIterator.next();
                if (token.getType().equals("WORD")) {
                    if (tokenEnhancedIterator.peek().getType().equals("LBR")) {
                        isLikeTemplate(tokenEnhancedIterator, "LBR", 5);
                        isLikeTemplate(tokenEnhancedIterator, "RBR", 7);

                        params.appendChild(new Node_AST(new Token(token.getValue(), "DEF_CALL",
                                token.getRow(), token.getColumn()), null,
                                defAST.get(token.getValue()).getRoot().getChildren()));
                    } else {
                        params.appendChild(new Node_AST(new Token(token.getValue(), "ID",
                                token.getRow(), token.getColumn())));
                    }
                }
            }
            else {
                params.appendChild(parseTernar(tokenEnhancedIterator));
            }

            if (tokenEnhancedIterator.peek().getType().equals("COMMA"))
                tokenEnhancedIterator.next();
            else {
                if (!tokenEnhancedIterator.peek().getType().equals("RBR"))
                    fail(5, tokenEnhancedIterator.peek());
            }

            token = tokenEnhancedIterator.peek();
        }

        return params;
    }

    /**
     * parse assignment or start parsing ternary
     * @param tokenEnhancedIterator - iterator
     * @return - expression node
     * @throws CompilerException - fail will produce this exception
     */
    private Node_AST parseExp(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token = tokenEnhancedIterator.next(),
                token2 = tokenEnhancedIterator.next();

        tokenEnhancedIterator.previous();
        tokenEnhancedIterator.previous();
        /* parse variable assignment: '<WORD> [("+"|"-"|"/"|"*"|"&"|"|"|"^"|"<<"|">>")]"=" <EXP>' */
        if (token.getType().equals("WORD") && (token2.getType().equals("ASSIGN") ||
                                                token2.getType().equals("ADD_ASSIGN") ||
                                                token2.getType().equals("SUB_ASSIGN") ||
                                                token2.getType().equals("DIV_ASSIGN") ||
                                                token2.getType().equals("MUL_ASSIGN") ||
                                                token2.getType().equals("PERCENT_ASSIGN") ||
                                                token2.getType().equals("L_SHIFT_ASSIGN") ||
                                                token2.getType().equals("R_SHIFT_ASSIGN") ||
                                                token2.getType().equals("BIT_AND_ASSIGN") ||
                                                token2.getType().equals("BIT_OR_ASSIGN") ||
                                                token2.getType().equals("BIT_XOR_ASSIGN"))){
            tokenEnhancedIterator.next();
            tokenEnhancedIterator.next();
            Node_AST id = new Node_AST(new Token(token.getValue(), "ID", token.getRow(), token.getColumn())),
                    assign = new Node_AST(new Token("=", "ASSIGN", token2.getRow(), token2.getColumn())),
                    exp = parseExp(tokenEnhancedIterator);

            /* do this for any TYPE_ASSIGN */
            if (!token2.getType().equals("ASSIGN")){
                Node_AST operation = new Node_AST(new Token(
                        /* make + from += */
                        token2.getValue().substring(0, token2.getValue().length()-1),
                        /* make ADD from ADD_ASSIGN */
                        token2.getType().substring(0, token2.getType().length()-7),
                        token2.getRow(), token2.getColumn())),
                        id2 = new Node_AST(id.getCurrent());

                operation.appendChild(id2);
                operation.appendChild(exp);

                id2.setParent(operation);
                exp.setParent(operation);

                assign.appendChild(operation);
                operation.setParent(assign);
            }
            else {
                assign.appendChild(exp);
                exp.setParent(assign);
            }

            id.appendChild(assign);
            assign.setParent(id);

            return id;
        }
        /* parse ternary operator, or other priority actions */
        else {
            return parseTernar(tokenEnhancedIterator);
        }
    }

    /**
     * parse Ternary operator, or priority actions
     * @param tokenEnhancedIterator - iterator
     * @return - node which is useful for expression
     * @throws CompilerException - fail will produce this exception
     */
    private Node_AST parseTernar(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        /* parse true condition: <EXP>, or priority, if this is not ternary */
        Node_AST trueCon = parsePriority(15, tokenEnhancedIterator);

        /* parse ternary */
        if (tokenEnhancedIterator.peek().getType().equals("IF")){
            tokenEnhancedIterator.next();

            /* expression for if: IF <EXP> */
            Node_AST ifExp = parsePriority(15, tokenEnhancedIterator);

            isLikeTemplate(tokenEnhancedIterator, "ELSE", 6);

            /* parse ternary: ELSE <TERNAR> */
            Node_AST elseCon = parseTernar(tokenEnhancedIterator),
                        ternarNode = new Node_AST(new Token("ternar", "TERNAR",
                                ifExp.getCurrent().getRow(), ifExp.getCurrent().getColumn()));

            /* set family relations */
            ifExp.setParent(ternarNode);
            trueCon.setParent(ternarNode);
            elseCon.setParent(ternarNode);
            ternarNode.appendChild(ifExp);
            ternarNode.appendChild(trueCon);
            ternarNode.appendChild(elseCon);

            return ternarNode;
        }

        return trueCon;
    }

    /**
     * parse all arithmetic/logical/bitwise operators by their priority level
     * use recursive for going deeper in priority and check equals
     * @param prior - current level priority
     * @param tokenEnhancedIterator - iterator
     * @return - node
     * @throws CompilerException - fail will produce this exception
     */
    private Node_AST parsePriority(int prior, EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Node_AST topOperSign = null, left;

        /* exit from recursion, go to parseFactor (top priority method) */
        if (prior <= 6)
            left = parseFactor(tokenEnhancedIterator);
        /* go deeper */
        else
            left = parsePriority(prior-1, tokenEnhancedIterator);

        ArrayList<Node_AST> nodeQueue = new ArrayList<>(),
                            operQueue = new ArrayList<>();
        nodeQueue.add(left);

        Token token = tokenEnhancedIterator.peek();

        /* if equals operator: <OPER> <EXP> */
        if (token.getType().matches(priority.get(prior))) {
            /* check for repeating operators with one priority: { <OPER> <EXP> } */
            while (token.getType().matches(priority.get(prior))) {
                tokenEnhancedIterator.next();

                /* add operator node to list */
                operQueue.add(new Node_AST(token));

                Node_AST node;
                /* exit from recursion, go to parseFactor (top priority method) */
                if (prior <= 6)
                    node = parseFactor(tokenEnhancedIterator);
                /* go deeper */
                else
                    node = parsePriority(prior-1, tokenEnhancedIterator);

                /* add right node to list */
                nodeQueue.add(node);

                token = tokenEnhancedIterator.peek();
            }

            /*
            * reverse lists for going back because
            * first operator in row of one level operators
            * must be calculated first
            */
            Collections.reverse(operQueue);
            Collections.reverse(nodeQueue);

            EnhancedIterator<Node_AST> enhancedIteratorOper = new EnhancedIterator<>(operQueue);
            EnhancedIterator<Node_AST> enhancedIteratorNode = new EnhancedIterator<>(nodeQueue);
            topOperSign = enhancedIteratorOper.next();
            topOperSign.appendChild(enhancedIteratorNode.next());

            /* create relations in backwards order */
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

        /* return call stack, or only left, if one element */
        return topOperSign == null ? left : topOperSign;
    }

    /**
     * parse Factor, top level priority operators
     * @param tokenEnhancedIterator - iterator
     * @return - node
     * @throws CompilerException - fail will produce this exception
     */
    private Node_AST parseFactor(EnhancedIterator<Token> tokenEnhancedIterator) throws CompilerException {
        Token token = tokenEnhancedIterator.next();

        Node_AST oper;

        /* parse ' "(" <EXP> ")" ' */
        if (token.getType().equals("LBR")){
            Node_AST exp = parseTernar(tokenEnhancedIterator);
            isLikeTemplate(tokenEnhancedIterator, "RBR", 5);

            return exp;
        }
        /* parse ' <UNAR> <EXP> ' */
        else {
            if (token.getType().matches("(ADD)|(SUB)|(NOT)")){
                oper = new Node_AST(new Token(token.getValue(), "UNAR_"+token.getType(),
                                                token.getRow(), token.getColumn()));
                Node_AST nextFactor = parseFactor(tokenEnhancedIterator);
                oper.appendChild(nextFactor);
                nextFactor.setParent(oper);
                return oper;
            }
            /* parse getter from var: ' <WORD> ' */
            else {
                if (token.getType().equals("WORD")){
                    if (tokenEnhancedIterator.peek().getType().equals("LBR")){
                        isLikeTemplate(tokenEnhancedIterator, "LBR", 5);

                        /* parsing method parameters */
                        Node_AST params = parseParams(tokenEnhancedIterator, false);

                        isLikeTemplate(tokenEnhancedIterator, "RBR", 7);

                        Node_AST def_call = new Node_AST(new Token(token.getValue(), "DEF_CALL",
                                token.getRow(), token.getColumn()));

                        params.setParent(def_call);
                        def_call.appendChild(params);

                        return def_call;
                    }
                    else {
                        return new Node_AST(new Token(token.getValue(), "ID", token.getRow(), token.getColumn()));
                    }
                }
                /* parse number: ' <NUM> ' */
                else {
                    if (token.getType().matches("(INT)|(FLOAT)|(BINNUM)|(OCTNUM)|(HEXNUM)|(STRING)")) {
                        /* parse <NUM> to <INT> */
                        return parseExpression(token);
                    } else {
                        fail(3, token);
                    }
                }
            }
        }
        return null;
    }

    /**
     * convert number from any system, or float to decimal int
     * @param token - token that will be converted
     * @return - int node
     * @throws CompilerException - fail will produce this exception
     */
    private Node_AST parseExpression(Token token) throws CompilerException {
        String value = token.getValue();

        switch (token.getType()){
            case "INT": {
                return new Node_AST(token);
            }
            case "FLOAT": {
                StringBuilder casted = new StringBuilder();
                for (char ch: value.toCharArray()) {
                    /* stop on <.> */
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

    /**
     * throw CompilerException with error message and token, which call an exception
     * @param errId - massage error id
     * @param token - token, which call an exception
     * @throws CompilerException - threw for stop program working
     */
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
            case 6: {
                msg = "Expect ELSE token";
                break;
            }
            case 7: {
                msg = "Expected '('";
                break;
            }
            default: msg = "Unknown error";
        }
        throw new CompilerException(msg, token);
    }

    /**
     * getter for map with functions AST
     * @return - map
     */
    public HashMap<String, AST> getDefAST() {
        return defAST;
    }

    /**
     * getter for main AST
     * @return - main AST
     */
    public AST getMainAST() {
        return mainAST;
    }
}
