package ui;

import commands.*;
import data.*;
import network.ClientNetworkService;
import utils.LocaleManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainWindow extends JFrame {
    private final String login;
    private final String passwordHash;
    private final ClientNetworkService network;

    private JTable table;
    private DefaultTableModel tableModel;
    private VisualizationPanel visPanel;
    private List<LabWork> currentData;

    private JLabel userLabel;
    private JLabel filterLabel;
    private JLabel statusLabel;
    private JButton btnAdd, btnEdit, btnDelete, btnRefresh;
    private JButton btnClear, btnSum, btnPrintDiff, btnFilterDisc;
    private JComboBox<String> langCombo;

    private int currentUserId = -1;

    public MainWindow(String login, String passwordHash, ClientNetworkService network) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.network = network;
        this.currentData = new ArrayList<>();
        initUI();
        fetchCurrentUserId();
    }

    private void initUI() {
        setTitle(LocaleManager.getString("app.title") + " - " + login);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        List<Locale> supportedLocales = LocaleManager.getSupportedLocales();
        String[] langNames = supportedLocales.stream()
                .map(l -> LocaleManager.getString("lang." + l.getLanguage()))
                .toArray(String[]::new);

        langCombo = new JComboBox<>(langNames);
        for (int i = 0; i < supportedLocales.size(); i++) {
            if (supportedLocales.get(i).equals(LocaleManager.getCurrentLocale())) {
                langCombo.setSelectedIndex(i);
                break;
            }
        }
        langCombo.addActionListener(e -> changeLanguage());

        topPanel.add(new JLabel("Lang:"));
        topPanel.add(langCombo);
        userLabel = new JLabel(LocaleManager.getString("label.user") + login);
        topPanel.add(userLabel);
        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] cols = {
                LocaleManager.getString("col.id"),
                LocaleManager.getString("col.name"),
                LocaleManager.getString("col.x"),
                LocaleManager.getString("col.y"),
                LocaleManager.getString("col.min_point"),
                LocaleManager.getString("col.creation_date"),
                LocaleManager.getString("col.owner")
        };

        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                sortByColumn(col);
            }
        });

        // Filter
        JTextField filterField = new JTextField(15);
        filterField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                filterTable(filterField.getText());
            }
        });
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterLabel = new JLabel(LocaleManager.getString("label.filter"));
        filterPanel.add(filterLabel);
        filterPanel.add(filterField);

        // Visualization
        visPanel = new VisualizationPanel(this);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), visPanel);
        split.setDividerLocation(450);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(filterPanel, BorderLayout.NORTH);
        centerPanel.add(split, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel
        JPanel southPanel = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel();
        JPanel extraBtnPanel = new JPanel();

        btnAdd = new JButton(LocaleManager.getString("btn.add"));
        btnEdit = new JButton(LocaleManager.getString("btn.edit"));
        btnDelete = new JButton(LocaleManager.getString("btn.delete"));
        btnRefresh = new JButton(LocaleManager.getString("btn.refresh"));

        btnAdd.addActionListener(e -> openAddDialog());
        btnEdit.addActionListener(e -> openEditDialog());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e -> loadData());

        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        btnClear = new JButton(LocaleManager.getString("btn.clear"));
        btnSum = new JButton(LocaleManager.getString("btn.sum"));
        btnPrintDiff = new JButton(LocaleManager.getString("btn.print_diff"));
        btnFilterDisc = new JButton(LocaleManager.getString("btn.filter_disc"));

        btnClear.addActionListener(e -> clearCollection());
        btnSum.addActionListener(e -> sumMinimalPoint());
        btnPrintDiff.addActionListener(e -> printFieldDescendingDifficulty());
        btnFilterDisc.addActionListener(e -> filterLessThanDiscipline());

        extraBtnPanel.add(btnClear);
        extraBtnPanel.add(btnSum);
        extraBtnPanel.add(btnPrintDiff);
        extraBtnPanel.add(btnFilterDisc);

        statusLabel = new JLabel(LocaleManager.getString("label.objects_count") + "0");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        southPanel.add(btnPanel, BorderLayout.CENTER);
        southPanel.add(extraBtnPanel, BorderLayout.EAST);
        southPanel.add(statusLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void changeLanguage() {
        int index = langCombo.getSelectedIndex();
        List<Locale> supportedLocales = LocaleManager.getSupportedLocales();
        if (index >= 0 && index < supportedLocales.size()) {
            Locale newLocale = supportedLocales.get(index);
            LocaleManager.setLocale(newLocale.getLanguage(), newLocale.getCountry());
            updateUITexts();
        }
    }

    private void updateUITexts() {
        setTitle(LocaleManager.getString("app.title") + " - " + login);
        userLabel.setText(LocaleManager.getString("label.user") + login);
        filterLabel.setText(LocaleManager.getString("label.filter"));

        btnAdd.setText(LocaleManager.getString("btn.add"));
        btnEdit.setText(LocaleManager.getString("btn.edit"));
        btnDelete.setText(LocaleManager.getString("btn.delete"));
        btnRefresh.setText(LocaleManager.getString("btn.refresh"));

        btnClear.setText(LocaleManager.getString("btn.clear"));
        btnSum.setText(LocaleManager.getString("btn.sum"));
        btnPrintDiff.setText(LocaleManager.getString("btn.print_diff"));
        btnFilterDisc.setText(LocaleManager.getString("btn.filter_disc"));

        String[] cols = {
                LocaleManager.getString("col.id"),
                LocaleManager.getString("col.name"),
                LocaleManager.getString("col.x"),
                LocaleManager.getString("col.y"),
                LocaleManager.getString("col.min_point"),
                LocaleManager.getString("col.creation_date"),
                LocaleManager.getString("col.owner")
        };
        tableModel.setColumnIdentifiers(cols);
        statusLabel.setText(LocaleManager.getString("label.objects_count") + currentData.size());
        visPanel.updateTexts();
        pack();
    }

    private void fetchCurrentUserId() {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Определение пользователя..."));
                String response = network.sendCommand(new InfoCommand());

                if (response.contains("ID=")) {
                    int start = response.indexOf("ID=") + 3;
                    int end = response.indexOf(")", start);
                    if (end > start) {
                        currentUserId = Integer.parseInt(response.substring(start, end).trim());
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText(LocaleManager.getString("label.objects_count") + "0 | User ID: " + currentUserId);
                            loadData();
                        });
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Ошибка подключения", "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Загрузка..."));
                String response = network.sendCommand(new ShowCommand());
                List<LabWork> works = LabWorkReader.parseServerResponse(response);
                SwingUtilities.invokeLater(() -> {
                    updateUI(works);
                    statusLabel.setText(LocaleManager.getString("label.objects_count") + works.size() + " | User ID: " + currentUserId);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Ошибка сети"));
            }
        }).start();
    }

    private void updateUI(List<LabWork> works) {
        this.currentData = works;
        tableModel.setRowCount(0);
        NumberFormat nf = LocaleManager.getNumberFormat();
        DateFormat df = LocaleManager.getDateFormat();

        for (LabWork lw : works) {
            tableModel.addRow(new Object[]{
                    lw.getId(),
                    lw.getName(),
                    nf.format(lw.getCoordinates().getX()),
                    lw.getCoordinates().getY(),
                    nf.format(lw.getMinimalPoint()),
                    df.format(lw.getCreationDate()),
                    lw.getOwnerId()
            });
        }
        visPanel.updateData(works);
    }

    private void sortByColumn(int colIndex) {
        List<LabWork> sorted = currentData.stream()
                .sorted((a, b) -> {
                    switch (colIndex) {
                        case 0: return Long.compare(a.getId(), b.getId());
                        case 1: return a.getName().compareTo(b.getName());
                        case 2: return Double.compare(a.getCoordinates().getX(), b.getCoordinates().getX());
                        case 3: return Long.compare(a.getCoordinates().getY(), b.getCoordinates().getY());
                        case 4: return Float.compare(a.getMinimalPoint(), b.getMinimalPoint());
                        case 5: return a.getCreationDate().compareTo(b.getCreationDate());
                        case 6: return Integer.compare(a.getOwnerId(), b.getOwnerId());
                        default: return 0;
                    }
                })
                .collect(Collectors.toList());
        updateUI(sorted);
    }

    private void filterTable(String text) {
        if (text == null || text.isEmpty()) {
            updateUI(currentData);
            return;
        }
        String lower = text.toLowerCase();
        List<LabWork> filtered = currentData.stream()
                .filter(lw -> lw.getName().toLowerCase().contains(lower) ||
                        String.valueOf(lw.getId()).contains(text) ||
                        String.valueOf(lw.getOwnerId()).contains(text))
                .collect(Collectors.toList());
        updateUI(filtered);
    }

    private void clearCollection() {
        int confirm = JOptionPane.showConfirmDialog(this,
                LocaleManager.getString("msg.confirm_clear"),
                LocaleManager.getString("dialog.confirm_clear"),
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    String response = network.sendCommand(new ClearCommand());
                    SwingUtilities.invokeLater(() -> {
                        loadData();
                        JOptionPane.showMessageDialog(this, response);
                    });
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
                }
            }).start();
        }
    }

    private void sumMinimalPoint() {
        new Thread(() -> {
            try {
                String response = network.sendCommand(new SumOfMinimalPointCommand());
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, response, LocaleManager.getString("btn.sum"), JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }).start();
    }

    private void printFieldDescendingDifficulty() {
        new Thread(() -> {
            try {
                String response = network.sendCommand(new PrintFieldDescendingDifficultyCommand());
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, response, LocaleManager.getString("btn.print_diff"), JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }).start();
    }

    private void filterLessThanDiscipline() {
        String discName = JOptionPane.showInputDialog(this, LocaleManager.getString("msg.enter_disc_name"));
        if (discName != null && !discName.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    String response = network.sendCommand(new FilterLessThanDisciplineCommand(discName.trim()));
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this, response, LocaleManager.getString("btn.filter_disc"), JOptionPane.INFORMATION_MESSAGE));
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
                }
            }).start();
        }
    }

    private void openAddDialog() {
        AddObjectDialog dialog = new AddObjectDialog(this, null);
        dialog.setVisible(true);
        if (dialog.getResult() != null) {
            processCommand(new AddCommand(dialog.getResult()), LocaleManager.getString("msg.success_add"));
        }
    }

    private void openEditDialog() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.select_row"));
            return;
        }
        LabWork selected = currentData.get(row);
        if (selected.getOwnerId() != currentUserId) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.no_permission"), "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AddObjectDialog dialog = new AddObjectDialog(this, selected);
        dialog.setVisible(true);
        if (dialog.getResult() != null) {
            processCommand(new UpdateCommand(selected.getId(), dialog.getResult()), LocaleManager.getString("msg.success_update"));
        }
    }

    public void openEditFromGraph(LabWork lw) {
        if (lw.getOwnerId() != currentUserId) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.no_permission"), "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AddObjectDialog dialog = new AddObjectDialog(this, lw);
        dialog.setVisible(true);
        if (dialog.getResult() != null) {
            processCommand(new UpdateCommand(lw.getId(), dialog.getResult()), LocaleManager.getString("msg.success_update"));
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.select_row"));
            return;
        }
        LabWork selected = currentData.get(row);
        if (selected.getOwnerId() != currentUserId) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.no_permission"), "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String confirmMessage = LocaleManager.getString("msg.confirm_delete", selected.getId());
        Object[] options = {LocaleManager.getString("btn.yes"), LocaleManager.getString("btn.no")};
        int confirm = JOptionPane.showOptionDialog(this, confirmMessage,
                LocaleManager.getString("dialog.confirm_delete"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (confirm == JOptionPane.YES_OPTION) {
            processCommand(new RemoveByIdCommand(selected.getId()), LocaleManager.getString("msg.success_delete"));
        }
    }

    private void processCommand(Command cmd, String successMsg) {
        new Thread(() -> {
            try {
                String response = network.sendCommand(cmd);
                SwingUtilities.invokeLater(() -> {
                    if (response.contains("успешно") || response.contains("добавлен") || response.contains("удалён") || response.contains("обновлён")) {
                        loadData();
                        JOptionPane.showMessageDialog(this, successMsg);
                    } else {
                        JOptionPane.showMessageDialog(this, "Ошибка: " + response);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Ошибка сети: " + e.getMessage()));
            }
        }).start();
    }
}