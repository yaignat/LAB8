package ui;

import data.*;
import utils.LocaleManager;

import javax.swing.*;
import java.awt.*;

public class AddObjectDialog extends JDialog {
    private JTextField nameField, xField, yField, minPointField, pqMaxField, discNameField, lectureHoursField;
    private JComboBox<String> difficultyCombo;
    private LabWork result;
    private LabWork existingLabWork;

    public AddObjectDialog(JFrame parent, LabWork existing) {
        super(parent, existing == null ? LocaleManager.getString("dialog.add_title") : LocaleManager.getString("dialog.edit_title"), true);
        this.existingLabWork = existing;
        initUI(existing);
    }

    private void initUI(LabWork existing) {
        setLayout(new GridLayout(9, 2, 5, 5));
        setSize(450, 380); // Чуть увеличил размер для длинных текстов на других языках
        setLocationRelativeTo(null);

        add(new JLabel(LocaleManager.getString("field.name")));
        nameField = new JTextField(existing != null ? existing.getName() : "");
        add(nameField);

        add(new JLabel(LocaleManager.getString("field.coord_x")));
        xField = new JTextField(existing != null ? String.valueOf(existing.getCoordinates().getX()) : "");
        add(xField);

        add(new JLabel(LocaleManager.getString("field.coord_y")));
        yField = new JTextField(existing != null ? String.valueOf(existing.getCoordinates().getY()) : "");
        add(yField);

        add(new JLabel(LocaleManager.getString("field.min_point")));
        minPointField = new JTextField(existing != null ? String.valueOf(existing.getMinimalPoint()) : "");
        add(minPointField);

        add(new JLabel(LocaleManager.getString("field.pq_max")));
        pqMaxField = new JTextField(existing != null ? String.valueOf(existing.getPersonalQualitiesMaximum()) : "");
        add(pqMaxField);

        add(new JLabel(LocaleManager.getString("field.difficulty")));
        difficultyCombo = new JComboBox<>(new String[]{"EASY", "NORMAL", "HARD", "VERY_HARD", "HOPELESS"});
        if (existing != null) difficultyCombo.setSelectedItem(existing.getDifficulty().name());
        add(difficultyCombo);

        add(new JLabel(LocaleManager.getString("field.discipline")));
        discNameField = new JTextField(existing != null && existing.getDiscipline() != null ? existing.getDiscipline().getName() : "");
        add(discNameField);

        add(new JLabel(LocaleManager.getString("field.lecture_hours")));
        lectureHoursField = new JTextField(existing != null && existing.getDiscipline() != null ?
                String.valueOf(existing.getDiscipline().getLectureHours() != null ? existing.getDiscipline().getLectureHours() : 0) : "0");
        add(lectureHoursField);

        JPanel btnPanel = new JPanel();
        JButton okBtn = new JButton(LocaleManager.getString("btn.ok"));
        JButton cancelBtn = new JButton(LocaleManager.getString("btn.cancel"));
        btnPanel.add(okBtn);
        btnPanel.add(cancelBtn);
        add(btnPanel);

        okBtn.addActionListener(e -> onOk());
        cancelBtn.addActionListener(e -> dispose());
    }

    private void onOk() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.error_empty"), "Error", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return;
        }

        double x;
        try {
            x = Double.parseDouble(xField.getText().trim());
            if (x <= -279) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "X > -279", "Error", JOptionPane.ERROR_MESSAGE);
            xField.requestFocus();
            return;
        }

        long y;
        try {
            y = Long.parseLong(yField.getText().trim());
            if (y <= -240) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Y > -240", "Error", JOptionPane.ERROR_MESSAGE);
            yField.requestFocus();
            return;
        }

        float minP;
        try {
            minP = Float.parseFloat(minPointField.getText().trim());
            if (minP <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Min Point > 0", "Error", JOptionPane.ERROR_MESSAGE);
            minPointField.requestFocus();
            return;
        }

        double pqMax;
        try {
            pqMax = Double.parseDouble(pqMaxField.getText().trim());
            if (pqMax <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "PQ Max > 0", "Error", JOptionPane.ERROR_MESSAGE);
            pqMaxField.requestFocus();
            return;
        }

        String discName = discNameField.getText().trim();
        if (discName.isEmpty()) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("msg.error_empty"), "Error", JOptionPane.ERROR_MESSAGE);
            discNameField.requestFocus();
            return;
        }

        Integer lectureHours = 0;
        try {
            String lhText = lectureHoursField.getText().trim();
            if (!lhText.isEmpty()) {
                lectureHours = Integer.parseInt(lhText);
                if (lectureHours < 0) throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Lecture Hours >= 0", "Error", JOptionPane.ERROR_MESSAGE);
            lectureHoursField.requestFocus();
            return;
        }

        Difficulty diff = Difficulty.valueOf(difficultyCombo.getSelectedItem().toString());

        try {
            Coordinates coords = new Coordinates(x, y);
            Discipline disc = new Discipline(discName, lectureHours);
            result = new LabWork(name, coords, minP, pqMax, diff, disc);

            if (existingLabWork != null) {
                result.setId(existingLabWork.getId());
                result.setOwnerId(existingLabWork.getOwnerId());
                result.setCreationDate(existingLabWork.getCreationDate());
            }

            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public LabWork getResult() {
        return result;
    }
}