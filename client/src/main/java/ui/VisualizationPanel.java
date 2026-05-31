package ui;

import data.LabWork;
import utils.LocaleManager;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisualizationPanel extends JPanel {
    private List<LabWork> labWorks;
    private Map<Integer, Color> userColors;
    private Map<Long, Long> animationStartTimes;
    private LabWork selectedObject;
    private static final int ANIMATION_DURATION = 500;
    private double minX = -1000, maxX = 1000, minY = -1000, maxY = 1000;

    private MainWindow mainWindow;

    public VisualizationPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.userColors = new HashMap<>();
        this.animationStartTimes = new HashMap<>();
        setBackground(new Color(245, 245, 250));
        setPreferredSize(new Dimension(600, 400));
        updateBorder();

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick(e.getX(), e.getY());
                } else {
                    handleClick(e.getX(), e.getY());
                }
            }
        });

        Timer timer = new javax.swing.Timer(30, e -> repaint());
        timer.start();
    }

    private void updateBorder() {
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(LocaleManager.getString("label.visualization")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    public void updateTexts() {
        updateBorder();
        repaint();
    }

    public void updateData(List<LabWork> works) {
        this.labWorks = works;
        calculateBounds();
        assignColors();
        repaint();
    }

    public void triggerAnimation(long id) {
        if (id > 0) {
            animationStartTimes.put(id, System.currentTimeMillis());
            repaint();
        }
    }

    private void calculateBounds() {
        if (labWorks == null || labWorks.isEmpty()) {
            minX = minY = -1000;
            maxX = maxY = 1000;
            return;
        }
        minX = minY = Double.MAX_VALUE;
        maxX = maxY = -Double.MAX_VALUE;
        for (LabWork lw : labWorks) {
            double x = lw.getCoordinates().getX();
            double y = lw.getCoordinates().getY();
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        double paddingX = (maxX - minX) * 0.1;
        double paddingY = (maxY - minY) * 0.1;
        minX -= paddingX;
        maxX += paddingX;
        minY -= paddingY;
        maxY += paddingY;
        if (maxX == minX) { maxX += 100; minX -= 100; }
        if (maxY == minY) { maxY += 100; minY -= 100; }
    }

    private void assignColors() {
        if (labWorks == null) return;
        userColors.clear();
        for (LabWork lw : labWorks) {
            int ownerId = lw.getOwnerId();
            if (!userColors.containsKey(ownerId)) {
                float hue = (ownerId * 0.618033988749895f) % 1.0f;
                Color color = Color.getHSBColor(hue, 0.7f, 0.8f);
                userColors.put(ownerId, color);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawGrid(g2d);

        if (labWorks == null || labWorks.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("SansSerif", Font.ITALIC, 14));
            String msg = LocaleManager.getString("label.no_objects");
            if (msg.startsWith("!")) msg = "Нет объектов";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
            return;
        }
        for (LabWork lw : labWorks) drawObject(g2d, lw);
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(200, 200, 200, 50));
        int step = 100;
        for (int x = (int) minX; x <= maxX; x += step) {
            int screenX = mapX(x);
            g2d.drawLine(screenX, 0, screenX, getHeight());
        }
        for (int y = (int) minY; y <= maxY; y += step) {
            int screenY = mapY(y);
            g2d.drawLine(0, screenY, getWidth(), screenY);
        }
    }

    private void drawObject(Graphics2D g2d, LabWork lw) {
        if (lw == null || lw.getCoordinates() == null) return;
        Long id = lw.getId();
        if (id == null) id = 0L;

        int x = mapX(lw.getCoordinates().getX());
        int y = mapY(lw.getCoordinates().getY());
        int baseSize = 25 + (int) Math.min(lw.getMinimalPoint() * 3, 30);
        int size = getAnimatedSize(id, baseSize);

        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillOval(x - size / 2 + 3, y - size / 2 + 3, size, size);

        Color color = userColors.getOrDefault(lw.getOwnerId(), Color.GRAY);
        g2d.setColor(color);
        g2d.fillOval(x - size / 2, y - size / 2, size, size);

        GradientPaint gradient = new GradientPaint(x - size/2, y - size/2, color.brighter(), x + size/2, y + size/2, color.darker());
        g2d.setPaint(gradient);
        g2d.fillOval(x - size / 2, y - size / 2, size, size);

        g2d.setColor(lw.equals(selectedObject) ? Color.BLACK : color.darker());
        g2d.setStroke(new BasicStroke(lw.equals(selectedObject) ? 3 : 2));
        g2d.drawOval(x - size / 2, y - size / 2, size, size);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(lw.getName(), x - fm.stringWidth(lw.getName()) / 2, y - size / 2 - 5);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 9));
        String idLabel = "ID:" + id;
        g2d.drawString(idLabel, x - fm.stringWidth(idLabel) / 2, y + size / 2 + 12);
    }

    private int getAnimatedSize(long id, int finalSize) {
        if (!animationStartTimes.containsKey(id)) return finalSize;
        long elapsed = System.currentTimeMillis() - animationStartTimes.get(id);
        if (elapsed > ANIMATION_DURATION) {
            animationStartTimes.remove(id);
            return finalSize;
        }
        double progress = Math.min(1.0, (double) elapsed / ANIMATION_DURATION);
        return (int) (finalSize * (1 - Math.pow(1 - progress, 3)));
    }

    private int mapX(double x) {
        return (int) ((x - minX) / (maxX - minX) * (getWidth() - 40) + 20);
    }

    private int mapY(double y) {
        return getHeight() - (int) ((y - minY) / (maxY - minY) * (getHeight() - 40) + 20);
    }

    private void handleDoubleClick(int mx, int my) {
        if (labWorks == null) return;
        for (LabWork lw : labWorks) {
            int ox = mapX(lw.getCoordinates().getX());
            int oy = mapY(lw.getCoordinates().getY());
            if (Math.hypot(mx - ox, my - oy) < 30) {
                mainWindow.openEditFromGraph(lw);
                return;
            }
        }
    }

    private void handleClick(int mx, int my) {
        if (labWorks == null) return;
        for (LabWork lw : labWorks) {
            int ox = mapX(lw.getCoordinates().getX());
            int oy = mapY(lw.getCoordinates().getY());
            if (Math.hypot(mx - ox, my - oy) < 30) {
                selectedObject = lw;
                showInfo(lw);
                repaint();
                return;
            }
        }
        selectedObject = null;
        repaint();
    }

    private void showInfo(LabWork lw) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><b>").append(lw.getName()).append("</b><br>");
        sb.append("ID: ").append(lw.getId()).append("<br>");

        NumberFormat nf = LocaleManager.getNumberFormat();
        DateFormat df = LocaleManager.getDateFormat();

        sb.append("X: ").append(nf.format(lw.getCoordinates().getX())).append("<br>");
        sb.append("Y: ").append(lw.getCoordinates().getY()).append("<br>");
        sb.append("Min Point: ").append(nf.format(lw.getMinimalPoint())).append("<br>");
        sb.append("Created: ").append(df.format(lw.getCreationDate())).append("<br>");
        sb.append("Owner ID: ").append(lw.getOwnerId()).append("</html>");

        JOptionPane.showMessageDialog(this, sb.toString(),
                LocaleManager.getString("dialog.object_info"), JOptionPane.INFORMATION_MESSAGE);
    }
}