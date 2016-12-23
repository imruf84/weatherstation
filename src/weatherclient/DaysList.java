package weatherclient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;

/**
 * Napok listája.
 *
 * @author imruf84
 */
public class DaysList extends JList<String> {

    /**
     * Adatbáziskapcsolat.
     */
    private final Connection connection;

    /**
     * Konstruktor.
     *
     * @param connection adatbáziskapcsolat
     */
    public DaysList(Connection connection) {
        this.connection = connection;
        setModel(new DefaultListModel<>());
    }

    /**
     * Adatbáziskapcsolat lekérdezése.
     *
     * @return adatbáziskapcsolat
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Listakezelő lekérdezése.
     *
     * @return listakezelő
     */
    private DefaultListModel getListModel() {
        return (DefaultListModel) getModel();
    }

    /**
     * Lista törlése.
     */
    public void clear() {
        getListModel().clear();
    }

    /**
     * Lista frissítése.
     *
     * @throws SQLException kivétel
     */
    public void refresh() throws SQLException {
        List<String> selItems = getSelectedValuesList();
        clear();
        try (
                PreparedStatement ps = getConnection().prepareStatement("SELECT storeDate FROM data GROUP BY storeDate ORDER BY storeDate DESC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                getListModel().addElement(rs.getString(1));
            }

            setSelectedValues(selItems);
        }
    }

    /**
     * Kijelölt elemek meghatározása.
     *
     * @param selVals kijelölt elemek listája
     */
    public void setSelectedValues(List<String> selVals) {
        clearSelection();
        selVals.stream().map((value) -> getIndex(getModel(), value)).filter((index) -> (index >= 0)).forEach((index) -> {
            addSelectionInterval(index, index);
        });
        ensureIndexIsVisible(getSelectedIndex());
    }

    /**
     * Adott elem sorszámának lekérdezése.
     *
     * @param model listamodell
     * @param value érték
     * @return sorszám
     */
    public int getIndex(ListModel model, Object value) {
        if (value == null) {
            return -1;
        }
        if (model instanceof DefaultListModel) {
            return ((DefaultListModel) model).indexOf(value);
        }
        for (int i = 0; i < model.getSize(); i++) {
            if (value.equals(model.getElementAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Kijelölt napok lekérdezése.
     *
     * @return kijelölt napok tömbje
     */
    public String[] getSelectedDays() {
        return getSelectedValuesList().toArray(new String[0]);
    }

}
