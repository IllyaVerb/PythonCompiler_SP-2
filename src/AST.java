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
        StringBuilder prev = new StringBuilder();
        if (depth > 1){
            prev = new StringBuilder();
            for (int i = 0; i < depth-1; i++) {
                prev.append("|\t");
            }
            prev.append("|-----");
        }
        else {
            if (depth > 0){
                prev = new StringBuilder("|-----");
            }
        }
            System.out.printf("%s{%1s <-> '%-1s'}%n", prev.toString(),
                    child.getCurrent().getType(), child.getCurrent().getValue());
            for (Node_AST node : child.getChildren()) {
                recursivePrintChildren(node, depth + 1);
            }
    }

    public Node_AST getRoot() {
        return root;
    }
}
