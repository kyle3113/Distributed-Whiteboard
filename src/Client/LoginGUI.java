package Client;

import java.io.IOException;
import java.net.UnknownHostException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginGUI extends Application {

    private static String uid;
    private static String srvIP;
    private static int srvPort;
    private static String srvPwd;
    private static WhiteBoardGUI gui;

    @FXML
    public Button joinBtn;
    public TextField UsernameInput;
    public TextField IPInput;
    public TextField PortInput;
    public TextField PasswordInput;

    private void testParams() {
        uid = "mirrors_arent_real";
        srvIP = "localhost";
        srvPort = 8787;
        srvPwd = "failed_admin_breach";
    }

    private void getParams() throws Exception {
        // Starting a client requires:
        // * Username (uid)
        // * Server IP address
        // * Server port
        // * Password (UNIX style prompt)
        uid = UsernameInput.getText();
        if (uid.isEmpty()) {
            throw new Exception("Username required");
        }
        srvIP = IPInput.getText();
        if (srvIP.isEmpty()) {
            throw new Exception("Server IP required");
        }
        String portStr = PortInput.getText();
        if (portStr.isEmpty()) {
            throw new Exception("Port required");
        }
        try {
            srvPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new Exception("Integer port required");
        }
        srvPwd = PasswordInput.getText();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = null;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
        try {
            root = loader.load();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        primaryStage.setTitle("Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // TODO check why these fields aren't initialised automatically
        UsernameInput = (TextField) root.getScene().lookup("#UsernameInput");
        IPInput = (TextField) root.getScene().lookup("#IPInput");
        PortInput = (TextField) root.getScene().lookup("#PortInput");
        PasswordInput = (TextField) root.getScene().lookup("#PasswordInput");
        joinBtn = (Button) root.getScene().lookup("#joinBtn");
        joinBtn.setOnAction(event -> {
            try {
                getParams();
                // TODO FX buttons cannot change text?
                joinBtn.setText("Connecting...");
            } catch (Exception e) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                return;
            }
            FXMLLoader wbLoader = new FXMLLoader(getClass().getResource("WhiteBoard.fxml"));
            Parent wbRoot = null;
            try {
                wbRoot = wbLoader.load();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            gui = wbLoader.getController();
            try {
                System.out.println("Attempting connection to " + srvIP + ":" + srvPort + "...");
                gui.initClient(srvIP, srvPort);
            } catch (UnknownHostException e) {
                System.out.println("Cannot find host " + srvIP);
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Information Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("Cannot find host");
                    alert.showAndWait();
                });
                return;
            } catch (IOException e) {
                System.out.println("Port " + srvPort + " closed");
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Information Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("Port closed");
                    alert.showAndWait();
                });
                return;
            } finally {
                joinBtn.setText("Join");
            }
            System.out.println("Successfully connected");
            gui.updateUID(uid);
            gui.updatePassword(srvPwd);
            System.out.println("Opening whiteboard...");
            primaryStage.setTitle("Distributed Whiteboard");
            primaryStage.setScene(new Scene(wbRoot));
            primaryStage.show();
            primaryStage.setOnCloseRequest((e) -> {
                gui.shutdown();
            });
        });

    }

    public static void main(String[] args) {
        launch(args);
    }

}
