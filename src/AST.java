public class AST {
    private Node_AST root;

    public AST(Node_AST root){
        this.root = root;
    }

    public AST(Token token){
        this.root = new Node_AST(token, null);
    }

    public void printAST(){
        recursivePrintChildren(root, 0);
    }

    private void recursivePrintChildren(Node_AST child, int depth){
        String prev = "";
        if (depth > 1){
            prev = "|";
            for (int i = 0; i < depth-1; i++) {
                prev += "\t\t";
            }
            prev += "+---";
        }
        else {
            if (depth > 0){
                prev = "+---";
            }
        }
        System.out.println(String.format("%s{%1s - %-1s}", prev, child.getCurrent().getValue(), child.getCurrent().getType()));
        for (Node_AST node: child.getChildren()) {
            recursivePrintChildren(node, depth+1);
        }
    }

    /*
    public void printAST(Lexem root){
        recursivePrintChildren(root, 0);
    }

    private void recursivePrintChildren(Lexem child, int depth){
        String prev = "";
        if (depth > 1){
            for (int i = 0; i < depth-1; i++) {
                prev += "|\t\t";
            }
            prev += "@---";
        }
        else {
            if (depth > 0){
                prev = "@---";
            }
        }
        System.out.println(String.format("%s{%1s - %-1s}", prev, child.getV(), child.getT()));
        if (!child.isNot_childR()){
            recursivePrintChildren(child.getChildR(), depth+1);
        }
        if (!child.isNot_childL()){
            recursivePrintChildren(child.getChildL(), depth+1);
        }
    }

     */

    public Node_AST getRoot() {
        return root;
    }
}
