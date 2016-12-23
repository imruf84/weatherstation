package weatherclient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

/**
 * Adatok megjelenítésére szolgáló tábla.
 *
 * @author imruf84
 */
public final class WeatherDataTableModel extends AbstractTableModel {

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_STORE_DATE = 1;
    public static final int COLUMN_STORE_TIME = 2;
    public static final int COLUMN_DIRECTION = 3;
    public static final int COLUMN_SPEED = 4;
    public static final int COLUMN_RAW_VOLTAGE = 5;
    public static final int COLUMN_REF_VOLTAGE = 6;
    public static final int COLUMN_RAW_DIRECTION = 7;
    public static final int COLUMN_REF_DIRECTION = 8;
    public static final int COLUMN_RAW_DATA = 9;
    public static final int COLUMNS[] = {COLUMN_ID, COLUMN_STORE_DATE, COLUMN_STORE_TIME, COLUMN_DIRECTION, COLUMN_SPEED, COLUMN_RAW_VOLTAGE, COLUMN_REF_VOLTAGE, COLUMN_RAW_DIRECTION, COLUMN_REF_DIRECTION, COLUMN_RAW_DATA};
    /**
     * Dátumok tárolója.
     */
    private final ArrayList<WeatherListData> dataList;
    /**
     * Oszlopok nevei.
     */
    private ArrayList<String> columnNames;
    /**
     * Adatbáziskapcsolat.
     */
    private final Connection connection;
    /**
     * Napok szűrője.
     */
    private String daysFilter = "";

    /**
     * Konstruktor.
     *
     * @param connection adatbáziskapcsolat.
     */
    public WeatherDataTableModel(Connection connection) {
        this.columnNames = new ArrayList<>();
        this.dataList = new ArrayList<>();
        this.connection = connection;

        // Alapból nincs szűrő a napokra.
        setDaysFilter(new String[]{});
    }

    /**
     * Adatbáziskapcsolat lekérdezése.
     *
     * @return adatbáziskapcsolat
     */
    public Connection getConnection() {
        return connection;
    }

    @Override
    public int getRowCount() {
        return dataList.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        WeatherListData rowValue = dataList.get(rowIndex);
        Object value = null;
        switch (columnIndex) {
            case COLUMN_ID:
                value = rowValue.getId();
                break;
            case COLUMN_STORE_DATE:
                value = rowValue.getStoreDate();
                break;
            case COLUMN_STORE_TIME:
                value = rowValue.getStoreTime();
                break;
            case COLUMN_DIRECTION:
                value = rowValue.getDirection();
                break;
            case COLUMN_SPEED:
                value = rowValue.getSpeed();
                break;
            case COLUMN_RAW_VOLTAGE:
                value = rowValue.getRawVoltage();
                break;
            case COLUMN_REF_VOLTAGE:
                value = rowValue.getRefVoltage();
                break;
            case COLUMN_RAW_DIRECTION:
                value = rowValue.getRawDirection();
                break;
            case COLUMN_REF_DIRECTION:
                value = rowValue.getRefDirection();
                break;
            case COLUMN_RAW_DATA:
                value = rowValue.getRawDataString();
                break;
        }
        return value;
    }

    /**
     * Sor törlése.
     *
     * @param row sor sorszáma
     */
    public void removeRow(int row) {
        fireTableRowsDeleted(row, row);
    }

    /**
     * Minden adat törlése.
     */
    public void clear() {
        dataList.clear();
        while (getRowCount() > 0) {
            removeRow(0);
        }
    }

    /**
     * Napok szűrőjének a megadása.
     *
     * @param days napok tömbje
     */
    public void setDaysFilter(String days[]) {
        daysFilter = " AND storeDate IN (";
        for (int i = 0; i < days.length; i++) {
            daysFilter += "'" + days[i] + "'" + (i < days.length - 1 ? "," : "");
        }
        daysFilter += ") ";
    }

    /**
     * Napok szűrőjének a lekérdezése.
     *
     * @return napok szűrője
     */
    public String getDaysFilter() {
        return daysFilter;
    }

    /**
     * Adatok frissítése.
     *
     * @throws SQLException kivétel
     */
    public void refresh() throws SQLException {

        clear();
        ArrayList<String> values = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM data WHERE TRUE " + getDaysFilter() + " ORDER BY storeDate DESC, storeTime DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                for (int col = 0; col < md.getColumnCount(); col++) {
                    values.add(md.getColumnName(col + 1));
                }
                while (rs.next()) {
                    WeatherListData list = new WeatherListData(
                            rs.getString("ID"),
                            rs.getDate("storeDate"),
                            rs.getTime("storeTime"),
                            rs.getInt("direction"),
                            rs.getFloat("speed"),
                            rs.getFloat("rawVoltage"),
                            rs.getFloat("refVoltage"),
                            rs.getInt("rawDirection"),
                            rs.getInt("refDirection"),
                            rs.getString("rawData")
                    );
                    dataList.add(list);
                }
            }
        } finally {
            if (columnNames.size() != values.size()) {
                columnNames = values;
                fireTableStructureChanged();
            } else {
                fireTableDataChanged();
            }
        }

    }

}
