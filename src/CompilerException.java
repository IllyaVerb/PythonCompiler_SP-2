public class CompilerException extends Exception {
    public CompilerException(String message, Token token){
        super(String.format("%s\n\tat Token='%s' [row=%d column=%d]",
                message, token.getRawValue(), (token.getRow()+1), (token.getColumn()+1)));
    }
}
