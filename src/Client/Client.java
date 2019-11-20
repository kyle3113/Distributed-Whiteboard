package Client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONObject;

public class Client {

    private Socket srvSock;
    private BufferedWriter toSrv;
    private ClientListener cliListen;
    private WhiteBoardGUI gui;

    public Client(String srvIP, int srvPort) throws UnknownHostException, IOException {
        srvSock = new Socket(srvIP, srvPort);
        toSrv = new BufferedWriter(new OutputStreamWriter(srvSock.getOutputStream()));
        cliListen = new ClientListener(this, srvSock);
        cliListen.start();
    }

    public void shutdown() throws IOException {
        srvSock.close();
    }

    public void handleFailedConnection() {
        gui.handleFailedConnection();
    }

    public void setGUI(WhiteBoardGUI gui) {
        this.gui = gui;
    }

    public void updateFromServer(JSONObject srvCmd) {
        gui.updateFromServer(srvCmd);
    }

    public void sendToServer(JSONObject cliCmd) {
        try {
            toSrv.write(cliCmd.toString());
            toSrv.write("\n");
            toSrv.flush();
        } catch (IOException e) {
            System.out.println("Cannot write to server: " + cliCmd);
            gui.handleFailedConnection();
        }
    }

}
