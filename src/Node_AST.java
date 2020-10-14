import java.util.ArrayList;

public class Node_AST {
    private final Token current;
    private Node_AST parent;
    private final ArrayList<Node_AST> children;

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

    public Node_AST getDeepestLeft(){
        return goDeeperLeft(this);
    }

    private Node_AST goDeeperLeft(Node_AST start){
        if (start.getChildren().size() > 1)
            return goDeeperLeft(start.getChild(0));
        else
            return start;
    }

    public Node_AST getDeepestRight(){
        return goDeeperRight(this);
    }

    private Node_AST goDeeperRight(Node_AST start){
        if (start.getChildren().size() > 1)
            return goDeeperRight(start.getChild(1));
        else
            return start;
    }

    public Node_AST getDeepestSolo(){
        return goDeeperSolo(this);
    }

    private Node_AST goDeeperSolo(Node_AST start){
        if (start.getChildren().size() > 0)
            return goDeeperSolo(start.getChild(0));
        else
            return start;
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

    public void appendChildren(ArrayList<Node_AST> children){ this.children.addAll(children); }

    public void setFirstChild(Node_AST child){
        insertChild(0, child);
    }
}
