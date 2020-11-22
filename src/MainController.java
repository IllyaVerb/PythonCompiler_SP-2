import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

import java.io.*;

/**
 * controller for Style.fxml file
 */
public class MainController {

    @FXML
    private Button btnOpenFile, btnSaveAs;

    @FXML
    private TextArea textInput, textFileName, textConsole, textASM;

    /**
     * event for key bindings
     * F9               - Build button
     * F10              - Run button
     * Ctrl+S           - Save button
     * Ctrl+Shift+S     - Save as... button
     * @param keyEvent  - event with keys
     */
    @FXML
    private void keyReleased(KeyEvent keyEvent){
        KeyCombination save = new KeyCodeCombination(KeyCode.S,
                KeyCombination.CONTROL_DOWN);
        KeyCombination saveAs = new KeyCodeCombination(KeyCode.S,
                KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);

        switch (keyEvent.getCode()){
            case F9: {
                build();
                break;
            }
            case F10: {
                run();
                break;
            }
            default:{
                if (save.match(keyEvent)){
                    save();
                    break;
                }
                if (saveAs.match(keyEvent)){
                    saveAs();
                    break;
                }
            }
        }
    }

    /**
     * start open file dialog and read chosen file
     */
    @FXML
    private void openFile(){
        FileChooser chooser = new FileChooser();
        String currentDir = System.getProperty("user.dir");
        File file = new File(currentDir);
        chooser.setInitialDirectory(file);
        chooser.setTitle("Open Input File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Python Files", "*.py"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File selectedFile = chooser.showOpenDialog(btnOpenFile.getScene().getWindow());
        if (selectedFile != null) {
            textFileName.setText(selectedFile.getAbsolutePath());

            StringBuilder code = new StringBuilder();
            try (FileReader reader = new FileReader(selectedFile)) {
                int symb;
                while ((symb = reader.read()) != -1) {
                    code.append((char)symb);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            textInput.setText(code.toString());
        }
    }

    /**
     * save source code to current file
     */
    @FXML
    private void save(){
        writeToFile(textFileName.getText(), textInput.getText());
    }

    /**
     * save source code to file chosen by save file dialog
     */
    @FXML
    private void saveAs(){
        FileChooser chooser = new FileChooser();
        File file = new File(textFileName.getText());
        chooser.setInitialFileName(file.getName());
        if(file.exists()) {
            chooser.setInitialDirectory(new File(file.getParent()));
        }
        else {
            file = new File(System.getProperty("user.dir"));
            chooser.setInitialDirectory(file);
        }
        chooser.setTitle("Save File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Python Files", "*.py"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File selectedFile = chooser.showSaveDialog(btnSaveAs.getScene().getWindow());
        if (selectedFile != null) {
            writeToFile(selectedFile.getAbsolutePath(), textInput.getText());
        }
    }

    /**
     * launch compiler
     * redirect out and err stream to interface console field
     * print created .asm file to interface ASM result
     * @return - result of building project (true/false)
     */
    @FXML
    private boolean build(){
        save();

        /* name for outer files */
        StringBuilder myName = new StringBuilder();
        String[] namePartially = (new File(textFileName.getText())).getName().split("\\.");
        for (int i = 0; i < namePartially.length-1; i++) {
            myName.append(namePartially[i]);
            if (i != namePartially.length -2)
                myName.append('.');
        }

        Compiler compiler = new Compiler(String.format("%s.py", myName.toString()),
                                        String.format("%s.asm", myName.toString()));
        ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(consoleOutput);

        System.setOut(ps);
        System.setErr(ps);

        boolean compilationResult = compiler.compile();

        textConsole.setText(consoleOutput.toString());

        if (compilationResult)
            textASM.setText(readFromFile(String.format("%s.asm", myName.toString())));

        return compilationResult;
    }

    /**
     * compile and run built .asm file in console using masm32
     */
    @FXML
    private void run(){
        if (!build())
            return;

        /* name for outer files */
        StringBuilder myName = new StringBuilder();
        String[] namePartially = (new File(textFileName.getText())).getName().split("\\.");
        for (int i = 0; i < namePartially.length-1; i++) {
            myName.append(namePartially[i]);
            if (i != namePartially.length -2)
                myName.append('.');
        }

        String startBat =   "copy %1$s.asm %1$s\\masm32\\bin\\\n"+
                            "cd %1$s\\masm32\\bin\\\n"+
                            "ml /coff %1$s.asm -link /subsystem:console\n"+
                            "start \"%1$s.asm\" cmd /c %1$s.exe ^& echo. ^& pause\n"+
                            "exit";
        writeToFile(String.format("%s-tmp.bat", myName.toString()), String.format(startBat, myName.toString()));

        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe",
                            "/c",
                String.format("start /MIN %s-tmp.bat", myName.toString()));
        builder.redirectErrorStream(true);
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write text to file
     * @param fileName - name of file for writing
     * @param text - text, that will be written
     */
    private void writeToFile(String fileName, String text){
        try(FileWriter writer = new FileWriter(fileName, false))
        {
            writer.write(text);
            writer.flush();
        }
        catch(IOException ex){
            System.err.println(ex.getMessage());
        }
    }

    /**
     * read content of file
     * @param nameFile - file to be read
     * @return - xontent of file in type String
     */
    private String readFromFile(String nameFile) {
        StringBuilder result = new StringBuilder();
        try (FileReader reader = new FileReader(nameFile)) {
            int symb;
            while ((symb = reader.read()) != -1) {
                result.append((char) symb);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return result.toString();
    }
}
