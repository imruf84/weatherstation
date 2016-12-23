package weatherclient;

import java.sql.Date;
import java.sql.Time;

/**
 * Adat lista formátuma.
 *
 * @author imruf84
 */
public class WeatherListData {

    /**
     * Azonosító.
     */
    private final String id;
    /**
     * Tárolás dátuma.
     */
    private final Date storeDate;
    /**
     * Tárolás ideje.
     */
    private final Time storeTime;
    /**
     * Irány.
     */
    private final int direction;
    /**
     * Sebesség.
     */
    private final float speed;
    /**
     * Mért feszültség.
     */
    private final float rawVoltage;
    /**
     * Referencia feszültség.
     */
    private final float refVoltage;
    /**
     * Mért irány.
     */
    private final int rawDirection;
    /**
     * Referencia irány.
     */
    private final int refDirection;
    /**
     * Forrásadat.
     */
    private final String rawDataString;

    /**
     * Konstruktor.
     *
     * @param id azonosító
     * @param storeDate tárolás dátuma
     * @param storeTime tárolás ideje
     * @param direction irány
     * @param speed sebesség
     * @param rawVoltage mért feszültség
     * @param refVoltage referencia feszültség
     * @param rawDirection mért irány
     * @param refDirection referencia irány
     * @param rawDataString forrásadat
     */
    public WeatherListData(
            String id,
            Date storeDate,
            Time storeTime,
            int direction,
            float speed,
            float rawVoltage,
            float refVoltage,
            int rawDirection,
            int refDirection,
            String rawDataString) {

        this.id = id;
        this.storeDate = storeDate;
        this.storeTime = storeTime;
        this.direction = direction;
        this.speed = speed;
        this.rawVoltage = rawVoltage;
        this.refVoltage = refVoltage;
        this.rawDirection = rawDirection;
        this.refDirection = refDirection;
        this.rawDataString = rawDataString;
    }

    /**
     * Azonosító lekérdezése.
     *
     * @return azonosító
     */
    public String getId() {
        return id;
    }

    /**
     * Tárolás dátumának a lekérdezése.
     *
     * @return tárolás datuma
     */
    public Date getStoreDate() {
        return storeDate;
    }

    /**
     * Tárolás időpontjának a lekérdezése.
     *
     * @return tárolás időpontja
     */
    public Time getStoreTime() {
        return storeTime;
    }

    /**
     * Mért feszültség lekérdezése.
     *
     * @return mért feszültség
     */
    public float getRawVoltage() {
        return rawVoltage;
    }

    /**
     * Referncia feszültség lekérdezése.
     *
     * @return referencia feszültség
     */
    public float getRefVoltage() {
        return refVoltage;
    }

    /**
     * Mért irány lekérdezése.
     *
     * @return mért irány
     */
    public int getRawDirection() {
        return rawDirection;
    }

    /**
     * Referencia irány lekérdezése.
     *
     * @return referencia irány
     */
    public int getRefDirection() {
        return refDirection;
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
     * Sebesség lekérdezése.
     *
     * @return sebesség
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Forrásadat lekérdezése.
     * 
     * @return forrásadat
     */
    public String getRawDataString() {
        return rawDataString;
    }

}
