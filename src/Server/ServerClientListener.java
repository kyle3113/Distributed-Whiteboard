package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.json.JSONObject;

public class ServerClientListener extends Thread {

    private ServerClient sc;
    private Socket cliSock;
    private BufferedReader fromCli;

    public ServerClientListener(ServerClient sc, Socket cliSock) throws IOException {
        this.sc = sc;
        this.cliSock = cliSock;
        this.fromCli = new BufferedReader(new InputStreamReader(cliSock.getInputStream()));
    }

    public void run() {
        JSONObject cliCmd = null;
        String raw = null;
        while (true) {
            try {
                raw = fromCli.readLine();
            } catch (IOException e) {
                sc.handleClientDisconnect();
                break;
            }
            if (raw == null) {
                sc.handleClientDisconnect();
                break;
            }
            cliCmd = new JSONObject(raw);
            sc.handleClientUpdate(cliCmd);
        }
    }

    public void disconnect() throws IOException {
        cliSock.close();
    }

    public Socket getSocket() {
        return cliSock;
    }

}
