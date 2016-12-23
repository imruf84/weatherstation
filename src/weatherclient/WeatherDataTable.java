package weatherclient;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * Időjárásadat táblázata.
 *
 * @author imruf84
 */
public final class WeatherDataTable extends JTable {

    /**
     * Konstruktor.
     *
     * @param connection adatbáziskapcsolat
     * @throws SQLException kivétel
     */
    public WeatherDataTable(final Connection connection) throws SQLException {
        super(new WeatherDataTableModel(connection));

        // Csak egy sor lehet kijelölve egyszerre.
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Napok szűrőjének a megadása.
     *
     * @param days napok tömbje
     */
    public void setDaysFilter(String days[]) {
        getWeatherModel().setDaysFilter(days);
    }

    /**
     * Napok szűrőjének a lekérdezése.
     *
     * @return napok szűrője
     */
    public String getDaysFilter() {
        return getWeatherModel().getDaysFilter();
    }

    /**
     * Adatbáziskapcsolat lekérdezése.
     *
     * @return adatbáziskapcsolat
     */
    public Connection getConnection() {
        return getWeatherModel().getConnection();
    }

    /**
     * Adatmodell lekérdezése.
     *
     * @return adatmodell
     */
    public WeatherDataTableModel getWeatherModel() {
        return (WeatherDataTableModel) getModel();
    }

    /**
     * Oszlop elrejtése
     *
     * @param index oszlop sorszáma
     */
    void hideColumn(int index) {
        getColumnModel().getColumn(index).setWidth(0);
        getColumnModel().getColumn(index).setMinWidth(0);
        getColumnModel().getColumn(index).setMaxWidth(0);
    }

    /**
     * Oszlop átnevezése.
     *
     * @param index oszlop sorszáma
     * @param newName oszlop új neve
     */
    void renameColumn(int index, String newName) {
        getColumnModel().getColumn(index).setHeaderValue(newName);
    }

    /**
     * Adatok frissítése.
     *
     * @throws SQLException kivétel
     */
    void refresh() throws SQLException {
        getWeatherModel().refresh();
        afterRefresh();
    }

    /**
     * Frissítés utáni események.
     */
    void afterRefresh() {
        // Néhány oszlop eltüntetése.
        hideColumn(WeatherDataTableModel.COLUMN_ID);
        hideColumn(WeatherDataTableModel.COLUMN_RAW_VOLTAGE);
        hideColumn(WeatherDataTableModel.COLUMN_REF_VOLTAGE);
        hideColumn(WeatherDataTableModel.COLUMN_RAW_DIRECTION);
        hideColumn(WeatherDataTableModel.COLUMN_REF_DIRECTION);
        hideColumn(WeatherDataTableModel.COLUMN_RAW_DATA);

        // Oszlopok átnevezése.
        renameColumn(WeatherDataTableModel.COLUMN_STORE_DATE, "Dátum");
        renameColumn(WeatherDataTableModel.COLUMN_STORE_TIME, "Idő");
        renameColumn(WeatherDataTableModel.COLUMN_DIRECTION, "Irány [°]");
        renameColumn(WeatherDataTableModel.COLUMN_SPEED, "Sebesség [km/h]");
    }

    /**
     * Adott sor adatának lekérdezése.
     * 
     * @param rowIndex sor száma
     * @return adat
     * @throws SQLException kivétel
     */
    public WeatherListData getWeatherData(int rowIndex) throws SQLException {
        try (
                Statement st = getConnection().createStatement(); 
                ResultSet rs = st.executeQuery("SELECT rawData FROM DATA WHERE ID = '" + getValueAt(rowIndex, WeatherDataTableModel.COLUMN_ID) + "'")) {
            if (rs.first()) {
                return new WeatherData(rs.getString("rawData")).toWeatherListData();
            }
        }
        
        return null;
    }
    
}
