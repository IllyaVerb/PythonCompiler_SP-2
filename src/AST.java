public class AST {
    private Node_AST root;

    public AST(Node_AST root){
        this.root = root;
    }

    public AST(Token token){
        this.root = new Node_AST(token, null);
    }

    public void printAST(){
        recursivePrintChildren(root);
    }

    private void recursivePrintChildren(Node_AST child){
        System.out.println("\n" + child.getCurrent().getValue() + " - " + child.getCurrent().getType());
        for (Node_AST node: child.getChildren()) {
            recursivePrintChildren(node);
        }
    }

    public Node_AST getRoot() {
        return root;
    }
}
