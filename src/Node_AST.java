import java.util.ArrayList;

/**
 * Class describe each node in asynchronous syntax tree
 */
public class Node_AST {
    /* base token for node */
    private final Token current;
    /* parent node */
    private Node_AST parent;
    /* list of children nodes */
    private final ArrayList<Node_AST> children;

    /**
     * create node on some token
     * parent and children will be null
     * @param token - base token for node
     */
    public Node_AST(Token token){
        this.current = token;
        this.parent = null;
        this.children = new ArrayList<>();
    }

    /**
     * create node with some token and with parent
     * @param token - base token
     * @param parent - parent node
     */
    public Node_AST(Token token, Node_AST parent){
        this.current = token;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    /**
     * create node with some token, parent and children
     * @param token - base token
     * @param parent - parent node
     * @param children - children nodes list
     */
    public Node_AST(Token token, Node_AST parent, ArrayList<Node_AST> children){
        this.current = token;
        this.parent = parent;
        this.children = children;
    }

    /**
     * getter for parent
     * @return - parent node
     */
    public Node_AST getParent() {
        return parent;
    }

    /**
     * getter for base token
     * @return - base token
     */
    public Token getCurrent() {
        return current;
    }

    /**
     * getter for children
     * @return - children nodes list
     */
    public ArrayList<Node_AST> getChildren() {
        return children;
    }

    /**
     * getter for one child by id
     * @param id - id in list of children
     * @return - child node from list
     */
    public Node_AST getChild(int id){
        return children.get(id);
    }

    /**
     * use recurse for getting the deepest left element in children tree
     * @return - the deepest left node
     */
    public Node_AST getDeepestLeft(){
        return goDeeperLeft(this);
    }

    /**
     * second part for getDeepestLeft() method
     * go deeper by searching left node
     * @param start - node for searching
     * @return - first (left) child of start node
     */
    private Node_AST goDeeperLeft(Node_AST start){
        if (start.getChildren().size() > 1)
            return goDeeperLeft(start.getChild(0));
        else
            return start;
    }

    /**
     * use recurse for getting the deepest right element in children tree
     * @return - the deepest right node
     */
    public Node_AST getDeepestRight(){
        return goDeeperRight(this);
    }

    /**
     * second part for getDeepestRight() method
     * go deeper by searching right node
     * @param start - node for searching
     * @return - last (right) child of start node
     */
    private Node_AST goDeeperRight(Node_AST start){
        if (start.getChildren().size() > 1)
            return goDeeperRight(start.getChild(1));
        else
            return start;
    }

    /**
     * use this method for getting the deepest element in children tree
     * if every node have only one child
     * @return - the deepest node
     */
    public Node_AST getDeepestSolo(){
        return goDeeperSolo(this);
    }

    /**
     * second part for getDeepestSolo() method
     * go deeper by searching the deepest node
     * @param start - node for searching
     * @return - one child of start node
     */
    private Node_AST goDeeperSolo(Node_AST start){
        if (start.getChildren().size() > 0)
            return goDeeperSolo(start.getChild(0));
        else
            return start;
    }

    /**
     * setter for parent
     * @param parent - node is need to be set as parent for current node
     */
    public void setParent(Node_AST parent) {
        this.parent = parent;
    }

    /**
     * insert node in children list
     * @param position - id where need to insert
     * @param child - node which will be insert
     */
    public void insertChild(int position, Node_AST child) {
        children.add(position, child);
    }

    /**
     * add node to the end of children list
     * @param child - node which will be added
     */
    public void appendChild(Node_AST child){
        children.add(child);
    }

    /**
     * add list of nodes to current children list
     * @param children - list which will be added
     */
    public void appendChildren(ArrayList<Node_AST> children){ this.children.addAll(children); }

    /**
     * insert node in children list as first element
     * @param child - node which will be insert
     */
    public void setFirstChild(Node_AST child){
        insertChild(0, child);
    }
}
