import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.*;

public class MainController {
    @FXML
    private Button btnOpenFile, btnSave, btnSaveAs, btnBuild, btnRun;

    @FXML
    private TextArea textInput, textFileName, textConsole, textASM;

    @FXML
    private void openFile(MouseEvent mouseEvent){
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
    private void save(MouseEvent mouseEvent){
        writeToFile(textFileName.getText(), textInput.getText());
    }

    @FXML
    private void saveAs(MouseEvent mouseEvent){
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
    private boolean build(MouseEvent mouseEvent){
        save(mouseEvent);

        Compiler compiler = new Compiler("2-4-Java-IO-82-Verbovskyi.py",
                                        "2-4-Java-IO-82-Verbovskyi.asm");
        ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(consoleOutput);

        System.setOut(ps);
        System.setErr(ps);

        boolean compilationResult = compiler.compile();

        textConsole.setText(consoleOutput.toString());
        if (compilationResult)
            textASM.setText(readFromFile("2-4-Java-IO-82-Verbovskyi.asm"));

        return compilationResult;
    }

    @FXML
    private void run(MouseEvent mouseEvent){
        if (!build(mouseEvent))
            return;

        String startBat =   "copy 2-4-Java-IO-82-Verbovskyi.asm masm32\\bin\\\n"+
                            "cd masm32\\bin\\\n"+
                            "ml /coff 2-4-Java-IO-82-Verbovskyi.asm -link /subsystem:console\n"+
                            "start cmd /c 2-4-Java-IO-82-Verbovskyi.exe ^& echo. ^& pause\n"+
                            "exit";
        writeToFile("2-4-Java-IO-82-Verbovskyi-run.bat", startBat);

        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe",
                "/c",
                "start /MIN 2-4-Java-IO-82-Verbovskyi-run.bat");
        builder.redirectErrorStream(true);
        try {
            Process p = builder.start();
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
