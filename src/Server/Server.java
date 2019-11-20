package Server;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Server {

    private String srvName;
    private int srvPort;
    private String srvPwd;
    private List<JSONObject> drawCommands;

    private ServerSocket srvSock;
    private List<ServerClient> clients;

    private static int MAX_USERS = 1000;
    private List<Integer> randIDs;

    public Server(String srvName, int srvPort, String srvPwd) throws IOException {
        this.srvSock = new ServerSocket(srvPort);
        this.srvName = srvName;
        this.srvPwd = srvPwd;
        this.drawCommands = new ArrayList<JSONObject>();
        this.clients = new ArrayList<ServerClient>();
        this.randIDs = makeRandomUIDs();
    }

    public void start() {
        while (true) {
            System.out.println("Awaiting request...");
            Socket cliSock = null;
            try {
                cliSock = srvSock.accept();
            } catch (IOException e) {
                System.out.println("IOException on accepting client");
            }
            ServerClient sc = null;
            try {
                sc = new ServerClient(this, cliSock);
            } catch (IOException e) {
                System.out.println("Failed to create ServerClient for: " + cliSock);
            }
            sc.setUID(getRandomUID());
            clients.add(sc);
            System.out.println("Accepted client: " + cliSock);
            sendPeers(sc);
            sendPeerUpdate(sc, true);
            sendBoardHist(sc);
        }
    }

    public void shutdown() {
        boolean allDisconnected = true;
        for (ServerClient sc : clients) {
            try {
                sc.disconnect();
                System.out.println("Disconnected: " + sc);
            } catch (IOException e) {
                allDisconnected = false;
                System.out.println("Failed to disconnect: " + sc);
            }
        }
        System.out.println("Shutting down server...");
        System.exit(allDisconnected ? 0 : -1);
    }

    public void handleClientDisconnect(ServerClient sc) {
        System.out.println("Disconnected: " + sc);
        clients.remove(sc);
        sendPeerUpdate(sc, false);
    }

    public synchronized void handleClientUpdate(ServerClient sc, JSONObject cliCmd) {
        String cmd = cliCmd.getString("cmd");
        if (cmd.equals("auth")) {
            boolean accepted = cliCmd.getString("pwd").equals(srvPwd);
            if (accepted) {
                sc.grantAdmin();
            }
            cliCmd.put("accepted", accepted);
            sc.sendToClient(cliCmd);
        } else if (cmd.equals("new")) {
            drawCommands = new ArrayList<JSONObject>();
            for (ServerClient c : clients) {
                c.sendToClient(cliCmd);
            }
        } else if (cmd.equals("save")) {
            String fname = cliCmd.getString("fname");
            BufferedWriter bw = null;
            boolean success = false;
            try {
                bw = new BufferedWriter(new FileWriter(fname));
                for (JSONObject j : drawCommands) {
                    bw.write(j + "\n");
                }
                success = true;
            } catch (IOException e) {
                System.out.println("Error writing to " + fname);
            }
            try {
                bw.close();
            } catch (IOException e) {
                System.out.println("Could not close " + fname);
            }
            cliCmd.put("success", success);
            sc.sendToClient(cliCmd);
        } else if (cmd.equals("uid")) {
            String oldUID = sc.getUID();
            String newUID = cliCmd.getString("uid");
            boolean success = true;
            for (ServerClient c : clients) {
                if (c.getUID().equals(newUID)) {
                    success = false;
                    break;
                }
            }
            if (success) {
                sc.setUID(newUID);
                JSONObject j = new JSONObject();
                j.put("cmd", "uid");
                j.put("oldUID", oldUID);
                j.put("newUID", newUID);
                j.put("success", success);
                for (ServerClient c : clients) {
                    c.sendToClient(j);
                }
            }
        } else if (cmd.equals("shutdown")) {
            shutdown();
        } else if (cmd.equals("draw")) {
            drawCommands.add(cliCmd);
            for (ServerClient c : clients) {
                c.sendToClient(cliCmd);
            }
        } else if (cmd.equals("kick")) {
            String kickUID = cliCmd.getString("kickUID");
            JSONObject kickCmd = new JSONObject();
            kickCmd.put("cmd", "kickUser");
            for (ServerClient c : clients) {
                if (c.getUID().equals(kickUID)) {
                    c.sendToClient(kickCmd);
                    try {
                        c.disconnect();
                    } catch (IOException e) {
                        System.out.println("Failed to kick " + c);
                    } finally {
                        break;
                    }
                }
            }
            JSONObject notifyUser = new JSONObject();
            notifyUser.put("cmd", "userKicked");
            notifyUser.put("kickUID", kickUID);
            for (ServerClient c : clients) {
                c.sendToClient(notifyUser);
            }
        } else {
            cliCmd.put("uid", sc.getUID());
            for (ServerClient c : clients) {
                c.sendToClient(cliCmd);
            }
        }
    }

    private void sendBoardHist(ServerClient sc) {
        JSONObject j = new JSONObject();
        JSONArray board = new JSONArray(drawCommands);
        j.put("cmd", "boardHist");
        j.put("boardHist", board);
        sc.sendToClient(j);
    }

    private void sendPeers(ServerClient sc) {
        JSONObject j = new JSONObject();
        JSONArray peers = new JSONArray();
        for (ServerClient c : clients) {
            peers.put(c.getUID());
        }
        j.put("cmd", "peers");
        j.put("peers", peers);
        sc.sendToClient(j);
    }

    private void sendPeerUpdate(ServerClient sc, boolean active) {
        JSONObject j = new JSONObject();
        j.put("cmd", "peerUpdate");
        j.put("uid", sc.getUID());
        j.put("active", active);
        for (ServerClient c : clients) {
            if (c != sc) {
                c.sendToClient(j);
            }
        }
    }

    private List<Integer> makeRandomUIDs() {
        List<Integer> randIDs = new ArrayList<Integer>();
        for (int i = 0; i < MAX_USERS; i++) {
            randIDs.add(i);
        }
        Collections.shuffle(randIDs);
        return randIDs;
    }

    private String getRandomUID() {
        int r = randIDs.remove(0);
        String uid = "guest_" + Integer.toString(r);
        return uid;
    }

}
