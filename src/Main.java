import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.FileReader;
import java.io.IOException;

public class Main extends Application {
    public static void main(String[] args) {
        Application.launch(args);

    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("Style.fxml"));
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setResizable(false);

        stage.setTitle("PythonCompiler_IllyaVerb");
        TextArea textFilename = (TextArea) scene.lookup("#text_input_file"),
                    textInput = (TextArea) scene.lookup("#text_input_print");
        textFilename.setText(System.getProperty("user.dir")+"\\2-4-Java-IO-82-Verbovskyi.py");

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
