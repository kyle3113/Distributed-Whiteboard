package Client;

import java.awt.Checkbox;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class WhiteBoardGUI extends Application implements Initializable {

    public VBox vbox;
    public ColorPicker cpick;
    public Slider wslide;
    public CheckBox filled;
    public Canvas whiteboard;
    public Canvas tmpboard;
    public ListView<String> chatboard;
    public ListView<String> peerlist;
    public TextField chatinput;
    public MenuItem requestAdminItem;
    public Menu adminMenu;
    private Stage stage;
    private GraphicsContext gc;
    private GraphicsContext tmp;

    private ShapeType shape;
    private List<Double> coords;
    private double x1, y1, x2, y2;
    private boolean isAdmin;
    private String uid;
    private String srvPwd;
    private String text;

    private Client cli;

    private ObservableList<String> chatMessages;
    private ObservableList<String> peers;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
    }

    public void initClient(String srvIP, int srvPort) throws IOException {
        cli = new Client(srvIP, srvPort);
        cli.setGUI(this);
    }

    public void updateUID(String uid) {
        this.uid = uid;
        requestUID();
    }

    public void updatePassword(String srvPwd) {
        this.srvPwd = srvPwd;
        requestAdmin();
    }

    private void dialogAskPassword() {
        TextInputDialog in = new TextInputDialog();
        in.setContentText("Enter password:");
        in.setHeaderText(null);
        in.showAndWait();
        String newPwd = in.getResult();
        if (newPwd != null) {
            updatePassword(newPwd);
        }
    }

    public void shutdown() {
        try {
            System.out.println("Shutting down client @ " + cli + "...");
            cli.shutdown();
        } catch (IOException e) {
            System.out.println("Failed client shutdown, running in background");
        }
        System.out.println("Shutting down GUI...");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        requestAdminItem.setOnAction(e -> {
            dialogAskPassword();
        });

        chatinput.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                String msg = chatinput.getText();
                JSONObject j = new JSONObject();
                j.put("cmd", "chat");
                j.put("msg", msg);
                cli.sendToServer(j);
                chatinput.clear();
            }
        });

        cpick.setValue(Color.BLACK);
        cpick.setOnAction(e -> {
            Color c = cpick.getValue();
            tmp.setStroke(c);
            tmp.setFill(c);
            gc.setStroke(c);
            gc.setFill(c);
        });

        filled.setOnAction(e -> {
            boolean fill = filled.isSelected();
            switch (shape) {
                case CIRCLE:
                case FILL_CIRCLE:
                    shape = fill ? ShapeType.FILL_CIRCLE : ShapeType.CIRCLE;
                    break;
                case RECT:
                case FILL_RECT:
                    shape = fill ? ShapeType.FILL_RECT : ShapeType.RECT;
                    break;
                case OVAL:
                case FILL_OVAL:
                    shape = fill ? ShapeType.FILL_OVAL : ShapeType.OVAL;
                    break;
                default:
                    break;
            }
        });

        wslide.valueProperty().addListener(e -> {
            double w = wslide.getValue();
            tmp.setLineWidth(w);
            gc.setLineWidth(w);
        });

        shape = ShapeType.LINE;

        gc = whiteboard.getGraphicsContext2D();
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.BLACK);
        gc.setLineWidth(wslide.getValue());

        tmp = tmpboard.getGraphicsContext2D();
        tmp.setStroke(Color.BLACK);
        gc.setFill(Color.BLACK);
        tmp.setLineWidth(wslide.getValue());

        tmpboard.setOnMousePressed(e -> {
            onMousePress(e);
        });

        tmpboard.setOnMouseDragged(e -> {
            onMouseDrag(e);
        });

        tmpboard.setOnMouseReleased(e -> {
            onMouseRelease(e);
        });

        text = "DefaultText";
        disableAdmin();
        resetCursorHistory();
        eraseBoard();
        clearTmp();

        chatMessages = FXCollections.observableArrayList();
        chatboard.setItems(chatMessages);

        peers = FXCollections.observableArrayList();
        peerlist.setItems(peers);

    }

    private String makeTimeString() {
        Date d = new Date();
        String date = "";
        date += String.format("%02d", d.getHours());
        date += ":" + String.format("%02d", d.getMinutes());
        date += ":" + String.format("%02d", d.getSeconds());
        return date;
    }

    public void handleFailedConnection() {
        Platform.runLater(() -> {
            peers.clear();
            chatMessages.add(makeTimeString() + "\n" + "Reconnection required");
        });
    }

    private void elevateIfOnlyUser() {
        if (peers.size() == 1) {
            enableAdmin();
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("Only user, elevating to admin");
                alert.showAndWait();
            });
        }
    }

    public synchronized void updateFromServer(JSONObject srvCmd) {
        System.out.println("Received from SRV: " + srvCmd);
        String cmd = srvCmd.getString("cmd");
        if (cmd.equals("auth")) {
            String pwd = srvCmd.getString("pwd");
            if (pwd.isEmpty()) {
                return;
            }
            boolean accepted = srvCmd.getBoolean("accepted");
            if (accepted) {
                enableAdmin();
            }
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("Password " + (accepted ? "accepted" : "rejected"));
                alert.showAndWait();
            });
        } else if (cmd.equals("draw")) {
            serverDraw(srvCmd);
        } else if (cmd.equals("chat")) {
            String uid = srvCmd.getString("uid");
            String msg = srvCmd.getString("msg");
            String chat = makeTimeString() + " - " + uid + ": " + "\n" + msg;
            Platform.runLater(() -> {
                chatMessages.add(chat);
            });
        } else if (cmd.equals("uid")) {
            boolean success = srvCmd.getBoolean("success");
            String oldUID = srvCmd.getString("oldUID");
            String newUID = srvCmd.getString("newUID");
            if (uid.equals(oldUID)) {
                uid = newUID;
                String update = (success ? "Updated uid: " : "Failed uid: ") + "\n" + newUID;
                Platform.runLater(() -> {
                    chatMessages.add(update);
                });
            } else if (success) {
                Platform.runLater(() -> {
                    peers.remove(oldUID);
                    peers.add(newUID);
                });
            }
        } else if (cmd.equals("peers")) {
            ObservableList<String> newPeers = FXCollections.observableArrayList();
            JSONArray givenPeers = srvCmd.getJSONArray("peers");
            for (Object p : givenPeers) {
                newPeers.add((String) p);
            }
            Platform.runLater(() -> {
                peers = newPeers;
                peerlist.setItems(peers);
                elevateIfOnlyUser();
            });
        } else if (cmd.equals("peerUpdate")) {
            String uid = srvCmd.getString("uid");
            boolean active = srvCmd.getBoolean("active");
            Platform.runLater(() -> {
                if (active) {
                    peers.add(uid);
                } else {
                    peers.remove(uid);
                    elevateIfOnlyUser();
                }
            });
        } else if (cmd.equals("new")) {
            eraseBoard();
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("Admin switched to new board");
                alert.showAndWait();
            });
        } else if (cmd.equals("save")) {
            String fname = srvCmd.getString("fname");
            boolean success = srvCmd.getBoolean("success");
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText((success ? "Successfully saved " : "Failed to save ") + fname);
                alert.showAndWait();
            });
        } else if (cmd.equals("boardHist")) {
            JSONArray boardHist = srvCmd.getJSONArray("boardHist");
            for (Object h : boardHist) {
                JSONObject drawCmd = (JSONObject) h;
                serverDraw(drawCmd);
            }
        } else if (cmd.equals("kickUser")) {
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText("You have been kicked");
                alert.showAndWait();
            });
        } else if (cmd.equals("kickedUser")) {
            Platform.runLater(() -> {
                peers.remove(srvCmd.getString("kickUID"));
                elevateIfOnlyUser();
            });
        }
    }

    @FXML
    public void requestAdmin() {
        if (srvPwd.isEmpty()) {
            return;
        }
        JSONObject j = new JSONObject();
        j.put("cmd", "auth");
        j.put("pwd", srvPwd);
        cli.sendToServer(j);
    }

    public void requestServerShutdown() {
        JSONObject j = new JSONObject();
        j.put("cmd", "shutdown");
        cli.sendToServer(j);
    }

    public void requestUID() {
        JSONObject j = new JSONObject();
        j.put("cmd", "uid");
        j.put("uid", uid);
        cli.sendToServer(j);
    }

    public void requestSave() {
        TextInputDialog in = new TextInputDialog();
        in.setContentText("Enter savefile:");
        in.setHeaderText(null);
        in.showAndWait();
        String fname = in.getResult();
        if (fname == null) {
            return;
        }
        if (!fname.endsWith(".wboard")) {
            fname += ".wboard";
        }
        JSONObject j = new JSONObject();
        j.put("cmd", "save");
        j.put("fname", fname);
        cli.sendToServer(j);
    }

    @FXML
    private void requestKickPeer() {
        TextInputDialog in = new TextInputDialog();
        in.setContentText("Enter user to kick:");
        in.setHeaderText(null);
        in.showAndWait();
        String kickUID = in.getResult();
        if (kickUID == null) {
            return;
        }
        JSONObject svrCmd = new JSONObject();
        svrCmd.put("cmd", "kick");
        svrCmd.put("kickUID", kickUID);
        cli.sendToServer(svrCmd);
    }

    private void disableAdmin() {
        for (MenuItem i : adminMenu.getItems()) {
            i.setDisable(true);
        }
        requestAdminItem.setDisable(false);
        isAdmin = false;
    }

    private void enableAdmin() {
        for (MenuItem i : adminMenu.getItems()) {
            i.setDisable(false);
        }
        isAdmin = true;
    }

    @FXML
    private void requestNewWhiteboard() {
        // Modify spec - only 1 Whiteboard per Server, modular and tolerates failures
        // Just wipe board instead, propagate to others
        JSONObject j = new JSONObject();
        j.put("cmd", "new");
        cli.sendToServer(j);
    }

    @FXML
    private void close() {
        shutdown();
    }

    @FXML
    private void openImage() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        File f = fc.showOpenDialog(stage);
        if (f == null) {
            return;
        }
        System.out.println("Open: " + f);
        requestNewWhiteboard();
        try {
            String fname = f.getName();
            if (fname.endsWith(".wboard")) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                while ((line = br.readLine()) != null) {
                    JSONObject drawCmd = new JSONObject(line);
                    cli.sendToServer(drawCmd);
                }
                br.close();
            } else {
                FileInputStream is = new FileInputStream(f);
                Image img = new Image(is);
                gc.drawImage(img, 0, 0);
                is.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @FXML
    private void saveAsImage() {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        File f = fc.showSaveDialog(stage);
        if (f == null) {
            return;
        }
        System.out.println("SaveAs: " + f.getPath());
        WritableImage wim = new WritableImage((int) whiteboard.getWidth(), (int) whiteboard.getHeight());
        WritableImage snap = whiteboard.snapshot(new SnapshotParameters(), wim);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(snap, null), "png", f);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void clearTmp() {
        Canvas cnv = tmp.getCanvas();
        tmp.clearRect(0, 0, cnv.getWidth(), cnv.getHeight());
    }

    private void eraseBoard() {
        Canvas cnv = gc.getCanvas();
        Color oldCol = cpick.getValue();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, cnv.getWidth(), cnv.getHeight());
        gc.setFill(oldCol);
    }

    private void eraseBoard(double x, double y, double w, double h) {
        Color oldCol = cpick.getValue();
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, w, h);
        gc.setFill(oldCol);
    }

    @FXML
    private void chooseLine() {
        shape = ShapeType.LINE;
    }

    @FXML
    private void chooseCircle() {
        shape = filled.isSelected() ? ShapeType.FILL_CIRCLE : ShapeType.CIRCLE;
    }

    @FXML
    private void chooseRect() {
        shape = filled.isSelected() ? ShapeType.FILL_RECT : ShapeType.RECT;
    }

    @FXML
    private void chooseOval() {
        shape = filled.isSelected() ? ShapeType.FILL_OVAL : ShapeType.OVAL;
    }

    @FXML
    private void chooseFreehand() {
        shape = ShapeType.FREEHAND;
    }

    @FXML
    private void chooseEraser() {
        shape = ShapeType.ERASER;
    }

    @FXML
    private void chooseText() {
        shape = ShapeType.TEXT;
    }

    private void appendCursorHistory(double x, double y) {
        coords.add(x);
        coords.add(y);
    }

    private void resetCursorHistory() {
        coords = new ArrayList<Double>();
    }

    private void onMousePress(MouseEvent e) {
        resetCursorHistory();
        x1 = e.getX();
        y1 = e.getY();
        x2 = x1;
        y2 = y1;
        appendCursorHistory(x1, y1);
        if (shape.equals(ShapeType.TEXT)) {
            userDraw();
        }
    }

    private void onMouseDrag(MouseEvent e) {
        x2 = e.getX();
        y2 = e.getY();
        if (shape.equals(ShapeType.FREEHAND)) {
            appendCursorHistory(x2, y2);
        } else {
            clearTmp();
        }
        userDraw();
    }

    private void onMouseRelease(MouseEvent e) {
        x2 = e.getX();
        y2 = e.getY();
        appendCursorHistory(x2, y2);
        if (shape.equals(ShapeType.TEXT)) {
            TextInputDialog in = new TextInputDialog();
            in.showAndWait();
            text = in.getResult();
            if (text == null) {
                clearTmp();
                return;
            }
        }
        JSONObject cliCmd = makeDrawCommand();
        cli.sendToServer(cliCmd);
        clearTmp();
    }

    private JSONObject makeDrawCommand() {
        JSONObject j = new JSONObject();
        j.put("cmd", "draw");
        // len(coords) >= 4, so num(coordpairs) >= 2
        // coords generalises to all shapes!
        j.put("shape", shape);
        j.put("colour", cpick.getValue());
        j.put("width", wslide.getValue());
        j.put("coords", coords);
        if (shape.equals(ShapeType.TEXT)) {
            j.put("text", text);
        }
        return j;
    }

    private void userDraw() {
        double xs = Math.min(x1, x2);
        double ys = Math.min(y1, y2);
        double xe = Math.max(x1, x2);
        double ye = Math.max(y1, y2);
        double w = xe - xs;
        double h = ye - ys;
        switch (shape) {
            case LINE:
                tmp.strokeLine(x1, y1, x2, y2);
                break;
            case CIRCLE:
                tmp.strokeOval(xs, ys, w, w);
                break;
            case FILL_CIRCLE:
                tmp.fillOval(xs, ys, w, w);
                break;
            case RECT:
                tmp.strokeRect(xs, ys, w, h);
                break;
            case FILL_RECT:
                tmp.fillRect(xs, ys, w, h);
                break;
            case OVAL:
                tmp.strokeOval(xs, ys, w, h);
                break;
            case FILL_OVAL:
                tmp.fillOval(xs, ys, w, h);
                break;
            case FREEHAND:
                int len = coords.size();
                double xPrev = coords.get(len - 4);
                double yPrev = coords.get(len - 3);
                tmp.strokeLine(xPrev, yPrev, x2, y2);
                break;
            case ERASER:
                tmp.strokeRect(xs, ys, w, h);
                break;
            case TEXT:
                tmp.strokeText("DefaultText", x2, y2);
                break;
            default:
                break;
        }
    }

    private void serverDraw(JSONObject srvCmd) {
        ShapeType shape = ShapeType.fromString(srvCmd.getString("shape"));
        Color col = Color.web(srvCmd.getString("colour"));
        Color oldCol = cpick.getValue();
        double width = srvCmd.getDouble("width");
        double oldWidth = wslide.getValue();
        JSONArray tmpArr = srvCmd.getJSONArray("coords");
        int len = tmpArr.length();
        Double[] coords = new Double[len];
        for (int i = 0; i < len; i++) {
            coords[i] = tmpArr.getDouble(i);
        }
        double x1 = coords[0];
        double y1 = coords[1];
        double x2 = coords[len - 2];
        double y2 = coords[len - 1];
        double xs = Math.min(x1, x2);
        double ys = Math.min(y1, y2);
        double xe = Math.max(x1, x2);
        double ye = Math.max(y1, y2);
        double w = xe - xs;
        double h = ye - ys;
        gc.setFill(col);
        gc.setStroke(col);
        gc.setLineWidth(width);
        switch (shape) {
            case LINE:
                gc.strokeLine(x1, y1, x2, y2);
                break;
            case CIRCLE:
                gc.strokeOval(xs, ys, w, w);
                break;
            case FILL_CIRCLE:
                gc.fillOval(xs, ys, w, w);
                break;
            case RECT:
                gc.strokeRect(xs, ys, w, h);
                break;
            case FILL_RECT:
                gc.fillRect(xs, ys, w, h);
                break;
            case OVAL:
                gc.strokeOval(xs, ys, w, h);
                break;
            case FILL_OVAL:
                gc.fillOval(xs, ys, w, h);
                break;
            case FREEHAND:
                for (int i = 0; i < len - 2; i += 2) {
                    x1 = coords[i];
                    y1 = coords[i + 1];
                    x2 = coords[i + 2];
                    y2 = coords[i + 3];
                    gc.strokeLine(x1, y1, x2, y2);
                }
                break;
            case ERASER:
                eraseBoard(xs, ys, w, h);
                break;
            case TEXT:
                String text = srvCmd.getString("text");
                gc.strokeText(text, x2, y2);
                break;
            default:
                break;
        }
        gc.setFill(oldCol);
        gc.setStroke(oldCol);
        gc.setLineWidth(oldWidth);
    }

}
