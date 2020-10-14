import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

import java.io.*;

public class MainController {
    private String myName = "2-4-Java-IO-82-Verbovskyi";

    @FXML
    private Button btnOpenFile, btnSaveAs;

    @FXML
    private TextArea textInput, textFileName, textConsole, textASM;

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

    @FXML
    private void openFile(){
        FileChooser chooser = new FileChooser();
        String currentDir = System.getProperty("user.dir");
        File file = new File(currentDir);
        chooser.setInitialDirectory(file);
        chooser.setTitle("Open Input File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Python Files", "*.py"));

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

    @FXML
    private void save(){
        writeToFile(textFileName.getText(), textInput.getText());
    }

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
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Python Files", "*.py"));

        File selectedFile = chooser.showSaveDialog(btnSaveAs.getScene().getWindow());
        if (selectedFile != null) {
            writeToFile(selectedFile.getAbsolutePath(), textInput.getText());
        }
    }

    @FXML
    private boolean build(){
        save();

        Compiler compiler = new Compiler(String.format("%s.py", myName),
                                        String.format("%s.asm", myName));
        ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(consoleOutput);

        System.setOut(ps);
        System.setErr(ps);

        boolean compilationResult = compiler.compile();

        textConsole.setText(consoleOutput.toString());

        if (compilationResult)
            textASM.setText(readFromFile(String.format("%s.asm", myName)));

        return compilationResult;
    }

    @FXML
    private void run(){
        if (!build())
            return;

        String startBat =   "copy %1$s.asm %1$s\\masm32\\bin\\\n"+
                            "cd %1$s\\masm32\\bin\\\n"+
                            "ml /coff %1$s.asm -link /subsystem:console\n"+
                            "start cmd /c %1$s.exe ^& echo. ^& pause\n"+
                            "exit";
        writeToFile(String.format("%s-tmp.bat", myName), String.format(startBat, myName));

        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe",
                "/c",
                String.format("start /MIN %s-tmp.bat", myName));
        builder.redirectErrorStream(true);
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
