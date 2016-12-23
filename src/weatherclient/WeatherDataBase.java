package weatherclient;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;

/**
 * Adatok tárolására szolágló adatbázis.
 *
 * @author imruf84
 */
public class WeatherDataBase {

    /**
     * Adatbáziskapcsolat.
     */
    private final Connection connection;

    /**
     * Konstruktor.
     *
     * @throws ClassNotFoundException kivétel
     * @throws SQLException kivétel
     */
    public WeatherDataBase() throws ClassNotFoundException, SQLException {

        Class.forName("org.h2.Driver");
        connection = DriverManager.getConnection("jdbc:h2:./data/data;trace_level_file=0;AUTO_SERVER=TRUE", "sa", "");
        createTables();
    }

    /**
     * Adatbázis táblák létrehozása.
     *
     * @throws SQLException kivétel
     */
    private void createTables() throws SQLException {
        try (Statement stat = connection.createStatement()) {
            stat.execute("CREATE TABLE IF NOT EXISTS DATA (ID VARCHAR(14), storeDate DATE, storeTime TIME, direction INT, speed REAL, rawVoltage REAL, refVoltage REAL, rawDirection INT, refDirection INT, rawData TEXT, PRIMARY KEY (ID))");
        }
    }

    /**
     * Adat tárolása.
     *
     * @param data adat
     * @throws SQLException kivétel
     * @throws ParseException kivétel
     */
    public void storeData(final WeatherData data) throws SQLException, ParseException {
        // #20160215@130125D312R-12V2.37M0.64&
        // # dátum @ idő D irány nyers R irány referencia V feszültség nyers M meredekség &

        // Hibás adatot nem tárolunk.
        if (!WeatherData.isDataStringValid(data.getRawDataString())) {
            System.err.println("WARNING: Failed to store invalid data [" + data.getRawDataString() + "]");
            return;
        }

        WeatherListData ld = data.toWeatherListData();

        try (PreparedStatement ps = getConnection().prepareStatement("MERGE INTO DATA KEY (ID) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, ld.getId());
            ps.setDate(2, ld.getStoreDate());
            ps.setTime(3, ld.getStoreTime());
            ps.setInt(4, ld.getDirection());
            ps.setFloat(5, ld.getSpeed());
            ps.setFloat(6, ld.getRawVoltage());
            ps.setFloat(7, ld.getRefVoltage());
            ps.setInt(8, ld.getRawDirection());
            ps.setInt(9, ld.getRefDirection());
            ps.setString(10, ld.getRawDataString());
            ps.execute();
        }
    }

    /**
     * Adatábziskapcsolat lekérdezése.
     *
     * @return adatbáziskapcsolat
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Legkésőbbi bejegyzett időpont lekérdezése.
     *
     * @return időpont
     * @throws SQLException kivétel
     */
    public String getLatestDateTime() throws SQLException {
        String s = "00000000000000";
        try (
                Statement st = getConnection().createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM DATA ORDER BY STOREDATE DESC, STORETIME DESC LIMIT 1")) {

            if (rs.next()) {
                s = rs.getString("ID");
            }

        }

        return s;
    }

    /**
     * Adatok törlése az adott időintervallumban.
     *
     * @param fromDate mettől
     * @param toDate meddig
     * @throws SQLException kivétel
     */
    public void removeData(String fromDate, String toDate) throws SQLException {
        try (Statement st = getConnection().createStatement()) {
            st.execute("DELETE FROM DATA WHERE storeDate BETWEEN '" + fromDate + "' AND '" + toDate + "'");
        }
    }

}
