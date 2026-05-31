import ui.LoginDialog;
import ui.MainWindow;
import network.ClientNetworkService;
import javax.swing.*;
import java.io.IOException;

public class ClientApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame tempFrame = new JFrame();
            tempFrame.setUndecorated(true);

            LoginDialog loginDialog = new LoginDialog(tempFrame);
            loginDialog.setVisible(true);

            if (loginDialog.success) {
                try {
                    ClientNetworkService network = new ClientNetworkService(
                            "localhost", 5001, loginDialog.login, loginDialog.passwordHash);

                    MainWindow mainWindow = new MainWindow(
                            loginDialog.login, loginDialog.passwordHash, network);
                    mainWindow.setVisible(true);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(tempFrame,
                            "Server connection error: " + e.getMessage());
                }
            }
        });
    }
}