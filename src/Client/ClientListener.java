package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.json.JSONObject;

public class ClientListener extends Thread {

    private Client cli;
    private BufferedReader fromSrv;

    public ClientListener(Client cli, Socket srvSock) throws IOException {
        this.cli = cli;
        this.fromSrv = new BufferedReader(new InputStreamReader(srvSock.getInputStream()));
    }

    public void run() {
        JSONObject srvCmd = null;
        while (true) {
            String raw = null;
            try {
                raw = fromSrv.readLine();
            } catch (IOException e) {
                cli.handleFailedConnection();
                break;
            }
            if (raw == null) {
                cli.handleFailedConnection();
                break;
            }
            srvCmd = new JSONObject(raw);
            cli.updateFromServer(srvCmd);
        }
    }

}