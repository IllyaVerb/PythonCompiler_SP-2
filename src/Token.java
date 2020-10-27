/**
 * Class describe token
 */
public class Token {
    /* value of token */
    private final String value;
    /* type of token */
    private final String type;
    /* row of token in input file */
    private final int row;
    /* column of token in input file */
    private final int column;

    /**
     * create new token
     * @param value - token value
     * @param type - token type
     * @param row - row of token in input file
     * @param column - column of token in input file
     */
    public Token(String value, String type, int row, int column){
        this.value = value;
        this.type = type;
        this.row = row;
        this.column = column;
    }

    /**
     * getter for value
     * @return - value
     */
    public String getValue() {
        return value;
    }

    /**
     * getter for raw value
     * if token is special symbol, return raw string with this symbol
     * @return - raw value
     */
    public String getRawValue() {
        switch (type){
            case "NEW_LINE": return "\\n";
            case "TAB": return "\\t";
            case "SPACE": return "\\s";
            default: return value;
        }
    }

    /**
     * getter for type
     * @return - type
     */
    public String getType() {
        return type;
    }

    /**
     * getter for row
     * @return - row
     */
    public int getRow() {
        return row;
    }

    /**
     * getter for column
     * @return - column
     */
    public int getColumn() {
        return column;
    }
}
