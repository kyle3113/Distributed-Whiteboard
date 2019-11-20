package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerTUI {

    private static String srvName;
    private static int srvPort;
    private static String srvPwd;

    private static Server srv;

    private static void getParams() throws NumberFormatException, IOException {
        // Starting a server requires:
        // * Whiteboard name
        // * Port
        // * Password (UNIX style prompt)
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter whiteboard name: ");
        srvName = br.readLine();
        System.out.print("Enter port: ");
        srvPort = Integer.parseInt(br.readLine());
        // TODO: UNIX style invisible password entry
        System.out.print("Enter server password: ");
        srvPwd = br.readLine();
    }

    private static void testParams() {
        srvName = "ATesterWBoard";
        srvPort = 8787;
        srvPwd = "true";
    }

    public static void main(String[] args) {
        // TODO change to getParams() for production version
        testParams();
        try {
            srv = new Server(srvName, srvPort, srvPwd);
            srv.start();
        } catch (IOException e) {
            System.out.println("Failed server start @ " + srvName + ":" + srvPort);
        }
    }

}
