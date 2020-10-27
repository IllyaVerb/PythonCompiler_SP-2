/**
 * Class describe Asynchronous Syntax Tree
 */
public class AST {
    /* root node of AST*/
    private final Node_AST root;

    /**
     * create AST
     * @param root - root node
     */
    public AST(Node_AST root){
        this.root = root;
    }

    /**
     * create AST with new Node_AST from token
     * @param token - root token
     */
    public AST(Token token){
        this.root = new Node_AST(token, null);
    }

    /**
     * start recursive printing AST
     */
    public void printAST(){
        recursivePrintChildren(root, 0);
    }

    /**
     * second print part
     * @param child - current child which data will be printed
     * @param depth - current depth for pretty print AST
     */
    private void recursivePrintChildren(Node_AST child, int depth){
        StringBuilder prev = new StringBuilder();
        if (depth > 1){
            prev = new StringBuilder();
            prev.append("|\t".repeat(depth - 1));
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

    /**
     * getter for root node
     * @return - root node
     */
    public Node_AST getRoot() {
        return root;
    }
}
