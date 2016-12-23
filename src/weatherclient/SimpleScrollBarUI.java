package weatherclient;

import com.sun.java.swing.plaf.motif.MotifScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Egyszerű megjelenéső görgetősáv osztálya.
 *
 * @author imruf84
 */
public class SimpleScrollBarUI extends MotifScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new SimpleScrollBarUI();
    }

    @Override
    protected JButton createIncreaseButton(int i) {
        JButton b = new JButton();
        b.setVisible(false);
        b.setPreferredSize(new Dimension());
        return b;
    }

    @Override
    protected JButton createDecreaseButton(int i) {
        JButton b = new JButton();
        b.setVisible(false);
        b.setPreferredSize(new Dimension());
        return b;
    }

    @Override
    public void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        // Ha le van toltva, akkor nem teszünk semmit.
        if (!c.isEnabled()) {
            return;
        }

        // Egyébként kirajzoljuk.
        g.translate(thumbBounds.x, thumbBounds.y);

        int x = 0;
        int y = 0;
        int w = thumbBounds.width - 1;
        int h = thumbBounds.height - 1;

        g.setColor(Color.white);
        g.drawLine(x, y, x + w, y);
        g.drawLine(x, y, x, y + h);
        g.setColor(this.thumbDarkShadowColor);
        g.drawLine(x + w, y, x + w, y + h);
        g.drawLine(x, y + h, x + w, y + h);

        g.translate(-thumbBounds.x, -thumbBounds.y);
    }
}
