package Server;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class ServerGUI extends Application {

	private static String wbName;
	private static int srvPort;
	private static String srvPwd;
	private static Server srv;

	@FXML
	public Button createBtn;
	public TextField wbnameInput;
	public TextField portInput;
	public TextField passwordInput;

	private void getParams() throws Exception {
		// Starting a server requires:
		// * white board name
		// * Port
		// * Password (UNIX style prompt)
		wbName = wbnameInput.getText();
		if (wbName.isEmpty()) {
			throw new Exception("Whiteboard name required");
		}

		String portStr = portInput.getText();
		if (portStr.isEmpty()) {
			throw new Exception("Port required");
		}
		try {
			srvPort = Integer.parseInt(portStr);
		} catch (NumberFormatException e) {
			throw new Exception("Integer port required");
		}
		srvPwd = passwordInput.getText();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("Server.fxml"));
		Parent root = null;
		try {
			root = loader.load();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		primaryStage.setTitle("Server");
		primaryStage.setScene(new Scene(root));
		primaryStage.show();

		// TODO check why these fields aren't initialised automatically
		wbnameInput = (TextField) root.getScene().lookup("#wbnameInput");
		portInput = (TextField) root.getScene().lookup("#portInput");
		passwordInput = (TextField) root.getScene().lookup("#passwordInput");
		createBtn = (Button) root.getScene().lookup("#createBtn");
		createBtn.setOnAction(event -> {
			try {
				getParams();
				// TODO FX buttons cannot change text?
				createBtn.setText("Connecting...");
			} catch (Exception e) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Information Dialog");
				alert.setHeaderText(null);
				alert.setContentText(e.getMessage());
				alert.showAndWait();
				return;
			}
			try {
	            srv = new Server(wbName, srvPort, srvPwd);
	            srv.start();
	        } catch (IOException e) {
	            System.out.println("Failed server start @ " + wbName + ":" + srvPort);
	        }
		});
		
	}

	public static void main(String[] args) {
		launch(args);
	}
}
