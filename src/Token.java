public class Token {
    private String value;
    private String type;
    private int row;
    private int column;

    public Token(String value, String type, int row, int column){
        this.value = value;
        this.type = type;
        this.row = row;
        this.column = column;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}
