/**
 * Class for any compiler exception
 */
public class CompilerException extends Exception {
    public CompilerException(String message, Token token){
        /* print message with data of incorrect token */
        super(String.format("%s\n\tat Token='%s' [row=%d column=%d]",
                message, token.getRawValue(), (token.getRow()+1), (token.getColumn()+1)));
    }
}
