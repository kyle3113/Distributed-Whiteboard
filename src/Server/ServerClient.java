package Server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.json.JSONObject;

public class ServerClient {

    private String uid;
    private boolean isAdmin;
    private BufferedWriter toCli;
    private ServerClientListener srvCliListener;
    private Server srv;

    public ServerClient(Server srv, Socket cliSock) throws IOException {
        this.srvCliListener = new ServerClientListener(this, cliSock);
        this.toCli = new BufferedWriter(new OutputStreamWriter(cliSock.getOutputStream()));
        this.isAdmin = false;
        this.srv = srv;
        srvCliListener.start();
    }

    public void handleClientDisconnect() {
        srv.handleClientDisconnect(this);
    }

    public void handleClientUpdate(JSONObject cliCmd) {
        srv.handleClientUpdate(this, cliCmd);
    }

    public void sendToClient(JSONObject srvCmd) {
        try {
            toCli.write(srvCmd.toString());
            toCli.write("\n");
            toCli.flush();
        } catch (IOException e) {
            System.out.println("Failed send to client: " + srvCmd);
        }
    }

    public void disconnect() throws IOException {
        srvCliListener.disconnect();
    }

    public void setUID(String uid) {
        this.uid = uid;
    }

    public String getUID() {
        return uid;
    }

    public void grantAdmin() {
        isAdmin = true;
    }

    @Override
    public String toString() {
        return uid + " @ " + srvCliListener.getSocket();
    }

}
