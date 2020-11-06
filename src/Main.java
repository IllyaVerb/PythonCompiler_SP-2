import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.FileReader;
import java.io.IOException;

/**
 * main class for work with compiler
 */
public class Main extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        /* connect interface file */
        Parent root = FXMLLoader.load(getClass().getResource("Style.fxml"));
        Scene scene = new Scene(root);

        stage.setScene(scene);
        //stage.setResizable(false);

        stage.setTitle("PythonCompiler_IllyaVerb");
        TextArea textFilename = (TextArea) scene.lookup("#text_input_file"),
                    textInput = (TextArea) scene.lookup("#text_input_print");

        /* name for all files */
        String myName = "5-4-Java-IO-82-Verbovskyi";

        /* set default open filename as %myName%.py */
        textFilename.setText(String.format("%s\\%s.py", System.getProperty("user.dir"), myName));

        /* open default file and show it in first window */
        StringBuilder code = new StringBuilder();
        try (FileReader reader = new FileReader(textFilename.getText())) {
            int symb;
            while ((symb = reader.read()) != -1) {
                code.append((char)symb);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        textInput.setText(code.toString());

        stage.show();
    }
}
