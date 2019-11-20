package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginTUI extends Application {

    private static String uid;
    private static String srvIP;
    private static int srvPort;
    private static String srvPwd;
    private static WhiteBoardGUI gui;

    private void getParams() throws IOException {
        // Starting a client requires:
        // * Server IP address
        // * Server port
        // * Password (UNIX style prompt)
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter server IP: ");
        srvIP = br.readLine();
        System.out.print("Enter server port: ");
        srvPort = Integer.parseInt(br.readLine());
        // TODO: UNIX style invisible password entry
        System.out.print("Enter server password (guest if blank): ");
        srvPwd = br.readLine();
    }

    private void testParams() {
        uid = "mirrors_arent_real";
        srvIP = "localhost";
        srvPort = 8787;
        srvPwd = "failed_admin_breach";
    }

    @Override
    public void start(Stage primaryStage) {
        // TODO change to getParams() for production version
        testParams();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("WhiteBoard.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        boolean failed = true;
        gui = loader.getController();
        try {
            System.out.println("Attempting connection to " + srvIP + ":" + srvPort + "...");
            gui.initClient(srvIP, srvPort);
            failed = false;
        } catch (UnknownHostException e) {
            System.out.println("Cannot find host " + srvIP);
        } catch (IOException e) {
            System.out.println("Port " + srvPort + " closed");
        }
        if (failed) {
            System.out.println("Aborting program");
            System.exit(-1);
        } else {
            System.out.println("Successfully connected");
            gui.updateUID(uid);
            gui.updatePassword(srvPwd);
            System.out.println("Opening whiteboard...");
            primaryStage.setTitle("Distributed Whiteboard");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
            primaryStage.setOnCloseRequest((e) -> {
                gui.shutdown();
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
