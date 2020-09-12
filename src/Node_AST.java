import java.util.ArrayList;

public class Node_AST {
    private Token current;
    private Node_AST parent;
    private ArrayList<Node_AST> children;

    public Node_AST(Token token){
        this.current = token;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    public Node_AST(Token token, Node_AST parent){
        this.current = token;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public Node_AST(Token token, Node_AST parent, ArrayList<Node_AST> children){
        this.current = token;
        this.parent = parent;
        this.children = children;
    }

    public Node_AST getParent() {
        return parent;
    }

    public Token getCurrent() {
        return current;
    }

    public ArrayList<Node_AST> getChildren() {
        return children;
    }

    public Node_AST getChild(int id){
        return children.get(id);
    }

    public void setParent(Node_AST parent) {
        this.parent = parent;
    }

    public void insertChild(int position, Node_AST child) {
        children.add(position, child);
    }

    public void appendChild(Node_AST child){
        children.add(child);
    }
}
