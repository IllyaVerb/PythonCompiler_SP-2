public class Token {
    private final String value;
    private final String type;
    private final int row;
    private final int column;

    public Token(String value, String type, int row, int column){
        this.value = value;
        this.type = type;
        this.row = row;
        this.column = column;
    }

    public String getValue() {
        return value;
    }

    public String getRawValue() {
        switch (type){
            case "NEW_LINE": return "\\n";
            case "TAB": return "\\t";
            case "SPACE": return "\\s";
            default: return value;
        }
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
