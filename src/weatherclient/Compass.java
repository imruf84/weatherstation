package weatherclient;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Virtuális iránytű.
 *
 * @author imruf84
 */
public class Compass extends JPanel {

    private WeatherListData data = null;
    private final JLabel infoLabel;

    public Compass() {
        setLayout(new BorderLayout());
        infoLabel = new JLabel("-");
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        add(infoLabel, BorderLayout.NORTH);
    }

    public void setWeatherData(WeatherListData wd) {
        data = wd;
        update();
    }

    private void update() {
        updateInfoLabel();
        repaint();
    }

    @Override
    public void paint(Graphics pg) {
        super.paint(pg);
        Graphics2D g = (Graphics2D) pg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.translate(getWidth() / 2, getHeight() / 2);
        double scaleFactor = Math.min((double) getWidth() / 100d, (double) getHeight() / 100d);
        g.scale(scaleFactor, scaleFactor);
        g.rotate(Math.PI);

        // Elforgatjuk a megfelelő irányba.
        g.rotate(Math.toRadians(null == data ? 0 : data.getDirection()));

        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(-15, 1, 15, 1);

        // Irány mutató.
        g.setColor(Color.red);
        g.fillPolygon(new int[]{-3, 3, 0}, new int[]{0, 0, 40}, 3);

        // Sebesség mutató.
        g.setColor(Color.green);
        int lSpeed = (null == data ? 0 : -Math.round(data.getSpeed()));
        g.fillPolygon(new int[]{-2, 2, 2, -2}, new int[]{0, 0, lSpeed, lSpeed}, 4);

        // Sebesség skála.
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(.7f));
        for (int r = 10; r <= Math.abs(lSpeed) + 10; r += 10) {
            g.drawLine(-3, -r, 3, -r);
        }

        g.setStroke(new BasicStroke(.3f));
        g.drawLine(-2, lSpeed, 2, lSpeed);

    }

    private void updateInfoLabel() {
        String dir = "-";
        String speed = "-";
        if (null != data) {
            dir = data.getDirection() + "";
            speed = data.getSpeed() + "";
        }

        infoLabel.setText("Irány: " + dir + "°" + " Sebesség: " + speed + "km/h");
    }
}
