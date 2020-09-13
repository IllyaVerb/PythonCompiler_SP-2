public class CompilerException extends  Exception {
    public CompilerException(String message, int row, int column){
        super(String.format("%s\n\tat row=%d column=%d", message, (row+1), (column+1)));
    }
}
