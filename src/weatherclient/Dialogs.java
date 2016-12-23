package weatherclient;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * Dialógusok gyűjteménye.
 *
 * @author imruf84
 */

public class Dialogs {

    /**
     * Hibaüzenet megjelenítése.
     *
     * @param c szülő
     * @param msg szöveg
     */
    public static void showErrorMessageDialog(Component c, String msg) {
        JOptionPane.showMessageDialog(
                c,
                msg,
                "Hiba",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Információs dialógus megjelenítése.
     *
     * @param c szülő
     * @param msg szöveg
     */
    public static void showInfoMessageDialog(Component c, String msg) {
        JOptionPane.showMessageDialog(
                c,
                msg,
                "Információ",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Fájl megnyitása sikertelen hibaüzenet megjelenítése.
     *
     * @param c szülő
     * @param msg szöveg
     */
    public static void showOpenFailedMessageDialog(Component c, String msg) {
        Dialogs.showErrorMessageDialog(c, "Fájl megnyitása sikertelen:\n" + msg);
    }

    /**
     * Fájl mentése sikertelen hibaüzenet megjelenítése.
     *
     * @param c szülő
     * @param msg szöveg
     */
    public static void showSaveFailedMessageDialog(Component c, String msg) {
        Dialogs.showErrorMessageDialog(c, "Fájl mentése sikertelen:\n" + msg);
    }
    
    public static void error(String msg) {
        System.err.println("HIBA: " + msg);
    }
}
