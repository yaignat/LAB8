package ui;

import security.PasswordUtils;
import utils.LocaleManager;
import network.ClientNetworkService;
import commands.InfoCommand;
import commands.RegisterCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginDialog extends JDialog {
    private JTextField loginField;
    private JPasswordField passwordField;

    public String login;
    public String passwordHash;
    public boolean success = false;

    public LoginDialog(JFrame parent) {
        super(parent, LocaleManager.getString("login.title"), true);
        initUI();
    }

    private void initUI() {
        setLayout(new GridLayout(3, 2, 10, 10));
        setSize(350, 180);
        setLocationRelativeTo(null);

        add(new JLabel(LocaleManager.getString("label.login")));
        loginField = new JTextField();
        add(loginField);

        add(new JLabel(LocaleManager.getString("label.password")));
        passwordField = new JPasswordField();
        add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton btnLogin = new JButton(LocaleManager.getString("btn.login"));
        JButton btnRegister = new JButton(LocaleManager.getString("btn.register"));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);
        add(buttonPanel);

        btnLogin.addActionListener(this::onLogin);
        btnRegister.addActionListener(this::onRegister);
    }

    private void onLogin(ActionEvent e) {
        String enteredLogin = loginField.getText().trim();
        String enteredPass = new String(passwordField.getPassword());

        if (enteredLogin.isEmpty() || enteredPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.error_empty"));
            return;
        }

        try {
            String hash = PasswordUtils.hashPassword(enteredPass);
            ClientNetworkService tempNetwork = new ClientNetworkService("localhost", 5001, enteredLogin, hash);
            String response = tempNetwork.sendCommand(new InfoCommand());
            tempNetwork.close();

            if (response.contains("Ошибка аутентификации") ||
                    response.contains("Неверный логин") ||
                    response.contains("Неверный пароль")) {
                JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.error_auth"));
                return;
            }

            this.login = enteredLogin;
            this.passwordHash = hash;
            this.success = true;
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка подключения к серверу:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void onRegister(ActionEvent e) {
        String enteredLogin = loginField.getText().trim();
        String enteredPass = new String(passwordField.getPassword());

        if (enteredLogin.isEmpty() || enteredPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.error_empty"));
            return;
        }

        if (enteredLogin.length() < 3 || enteredLogin.length() > 20) {
            JOptionPane.showMessageDialog(this, "Логин должен быть от 3 до 20 символов!");
            return;
        }

        if (enteredPass.length() < 6) {
            JOptionPane.showMessageDialog(this, "Пароль должен быть минимум 6 символов!");
            return;
        }

        try {
            String hash = PasswordUtils.hashPassword(enteredPass);
            ClientNetworkService tempNetwork = new ClientNetworkService("localhost", 5001, enteredLogin, hash);
            String response = tempNetwork.sendCommand(new RegisterCommand(enteredLogin, hash));
            tempNetwork.close();

            if (response.contains("успешно") || response.contains("зарегистрирован")) {
                JOptionPane.showMessageDialog(this,
                        "Пользователь '" + enteredLogin + "' успешно зарегистрирован!\nТеперь вы можете войти.",
                        "Успех",
                        JOptionPane.INFORMATION_MESSAGE);

                this.login = enteredLogin;
                this.passwordHash = hash;
                this.success = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Ошибка регистрации:\n" + response,
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка подключения к серверу:\n" + ex.getMessage(),
                    "Ошибка сети",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}