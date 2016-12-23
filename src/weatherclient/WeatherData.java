package weatherclient;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Időjárás adat.
 *
 * @author imruf84
 */
public class WeatherData {

    /**
     * Azonosító.
     */
    private String ID;
    /**
     * Forrásadat.
     */
    private final String rawDataString;
    /**
     * Tárolás ideje.
     */
    private Date storeDateTime = new Date();
    /**
     * Számított irány.
     */
    private int direction = 0;
    /**
     * Forrás irány.
     */
    private int directionRaw = 0;
    /**
     * Irányreferencia.
     */
    private int directionRef = 0;
    /**
     * Feszültség.
     */
    private float voltage = .0f;
    /**
     * Referencia feszültség.
     */
    private float voltageRef = .0f;
    /**
     * Sebesség.
     */
    private float speed = .0f;
    /**
     * Adat helyességének az ellenörzése.
     */
    private static final Pattern DATA_VALIDATOR_PATTERN = Pattern.compile("#\\d{8}@\\d{6}D\\-?\\d+R\\-?\\d+V\\d+\\.\\d+M\\d+\\.\\d+&");

    /**
     * Adat helyességének az ellenörzése.
     *
     * @param s adat
     * @return helyes adat esetén igaz egyébként hamis
     */
    public static boolean isDataStringValid(String s) {
        return DATA_VALIDATOR_PATTERN.matcher(s).matches();
    }

    /**
     * Konstruktor.
     *
     * @param dataStr forrásadat
     */
    public WeatherData(String dataStr) {
        this.rawDataString = dataStr;
        try {
            this.storeDateTime = new SimpleDateFormat("yyyyMMdd@HHmmss").parse(getValueBetween(dataStr, "#", "D"));
        } catch (ParseException ex) {
            Dialogs.showErrorMessageDialog(null, ex.getLocalizedMessage());
        }

        try {
            this.directionRaw = Integer.parseInt(getValueBetween(dataStr, "D", "R"));
            this.directionRef = Integer.parseInt(getValueBetween(dataStr, "R", "V"));
            // Irány kiszámítása.
            calulateDirection();
            this.voltage = Float.parseFloat(getValueBetween(dataStr, "V", "M"));
            this.voltageRef = Float.parseFloat(getValueBetween(dataStr, "M", "&"));
            // Sebesség kiszámítása.
            this.speed = this.voltage * this.voltageRef;
            // Kerekítünk századokra.
            this.speed = BigDecimal.valueOf(this.speed).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            // Azonosító generálása.
            this.ID = new SimpleDateFormat("yyyyMMddHHmmss").format(storeDateTime);
        } catch (NumberFormatException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    /**
     * Irány kiszámítása (normáljuk [0,360]-ba).
     */
    private void calulateDirection() {
        this.direction = (3 * 360 + (this.directionRaw - this.directionRef)) % 360;
    }

    @Override
    public String toString() {
        return "Dátum: " + new SimpleDateFormat("yyyy.MM.dd").format(storeDateTime) + " | Idő: " + new SimpleDateFormat("HH:mm:ss").format(storeDateTime) + " | Irány: " + direction + "°" + " | Sebesség: " + speed + "km/h";
    }

    /**
     * Két karakter között rész lekérdezése szövegből.
     *
     * @param data karakterlánc
     * @param from első karakter
     * @param to második karakter
     * @return a két karakter közötti rész
     */
    public static String getValueBetween(String data, String from, String to) {
        return data.substring(data.indexOf(from) + from.length(), data.indexOf(to));
    }

    /**
     * Irány lekérdezése.
     *
     * @return irány
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Mért irány lekérdezése.
     *
     * @return mért irány
     */
    public int getDirectionRaw() {
        return directionRaw;
    }

    /**
     * Referencia irány lekérdezése.
     *
     * @return referencia irány
     */
    public int getDirectionRef() {
        return directionRef;
    }

    /**
     * Mért feszültség lekérdezése.
     *
     * @return mért feszültség
     */
    public float getVoltageRaw() {
        return voltage;
    }

    /**
     * Referencia feszültség lekérdezése.
     *
     * @return referencia feszültség
     */
    public float getVoltageRef() {
        return voltageRef;
    }

    /**
     * Sebesség lekérdezése.
     *
     * @return sebesség
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Azonosító lekérdezése.
     *
     * @return azonosító
     */
    public String getId() {
        return this.ID;
    }

    /**
     * Forrásadat lekérdezése.
     *
     * @return forrásadat
     */
    public String getRawDataString() {
        return rawDataString;
    }

    /**
     * Adatsor átalakítása adatbázisban kezelhető formátummá.
     *
     * @return adatsor adatbázisban kezelhető formátuma
     */
    WeatherListData toWeatherListData() {
        return new WeatherListData(
                getId(),
                java.sql.Date.valueOf(new SimpleDateFormat("yyyy-MM-dd").format(storeDateTime)),
                java.sql.Time.valueOf(new SimpleDateFormat("HH:mm:ss").format(storeDateTime)),
                getDirection(),
                getSpeed(),
                getVoltageRaw(),
                getVoltageRef(),
                getDirectionRaw(),
                getDirectionRef(),
                getRawDataString()
        );
    }

}
