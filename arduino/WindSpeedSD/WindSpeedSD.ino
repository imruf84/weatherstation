#include "common.h"

#define STORE_DATA_TO_SD_CARD

// Óra.
#include <DS3231.h>
DS3231 rtc(A1, A0);

// Feszültség mérő.
#include <Wire.h>
#include <Adafruit_INA219.h>
Adafruit_INA219 ina219;

// SD kártya.
#include <SPI.h>
#include <SdFat.h>
SdFat sd;

// EEPROM dolgok.
#include <EEPROMex.h>
int16_t dirRef = 0;
float speedRef = 0.0f;
// Irányreferencia tárolása.
int16_t storeDirRef(int16_t ref)
{
  EEPROM.writeInt(0, ref);
  return ref;
}

// Irányreferencia lekérdezése.
int16_t readDirRef()
{
  return EEPROM.readInt(0);
}

// Sebességreferencia tárolása.
float storeSpeedRef(float ref)
{
  EEPROM.writeFloat(sizeof(uint16_t), ref);
  return ref;
}

// Sebességreferencia lekérdezése.
float readSpeedRef()
{
  return EEPROM.readFloat(sizeof(uint16_t));
}

// Időjárás adatok.
#include "weatherData.h"
weather_data_t prevData;
void getWeatherData(struct weather_data_s *d)
{

  debug("getWeatherData", true, true);
  
  initWeatherData(d, NULL);
  d->unixTime = rtc.getUnixTime(rtc.getTime());
  d->dateString = rtc.getDateStr(FORMAT_LONG, FORMAT_BIGENDIAN, '-');
  d->dateString.replace("-", "");
  d->timeString = rtc.getTimeStr();
  d->timeString.replace(":", "");
  d->voltage = fabs(ina219.getShuntVoltage_mV());
  if (d->voltage <= 0.05f) d->voltage = .0f;
  d->voltageM = speedRef;

  sendDataOnSerial("gwd");

  waitingForSerialResponse();
  
  d->dir = inputString.toInt();
  d->dirR = dirRef;

  inputString = "";
  stringComplete = false;
}

void sendLargeString(String ls)
{
  debug("big data begin", true, true);
  debug(ls, false, true);
  uint8_t n = (uint8_t)(ls.length() / (RF24_PAYLOAD_SIZE - 1));
  String s = ":";
  s += n;
  sendDataOnSerial(s);
  for (uint8_t i = 0; i <= n; i++)
  {
    waitingForSerialResponse();
        
    uint8_t from = i * (RF24_PAYLOAD_SIZE - 1);
    uint8_t to = min(i * (RF24_PAYLOAD_SIZE - 1) + (RF24_PAYLOAD_SIZE - 1), ls.length());
    debug(from, true, false);
    debug(" ", false, false);
    debug(to, false, false);
    debug(" ", false, false);
    debug(ls.substring(from, to), true, true);

    // HACK: fapados kell, mert nincs elég memória substring-re
    for (uint8_t j = from; j < to; j++) sendDataOnSerial2(ls.charAt(j));
    sendDataOnSerial("");

  }
  
  inputString = "";
  stringComplete = false;

  debug("big data end", true, true);
}

void waitingForSerialResponse()
{
  inputString = "";
  stringComplete = false;
  while (!stringComplete) serialEvent();
}

// Beérkező soros port üzenet kezelése.
void handleSerialData(String data) 
{

  // Új irányreferencia tárolásra.
  if (data.startsWith("dr"))
  {
    data.replace("dr", "");
    dirRef = storeDirRef(data.toInt());
    return;
  }

  // Új sebességreferencia tárolásra.
  if (data.startsWith("sr"))
  {
    data.replace("sr", "");
    speedRef = storeSpeedRef(data.toFloat());
    return;
  }
  
  // Aktuális adatok lekérdezése.
  if (data.startsWith("gcd"))
  {

    weather_data_t d;
    getWeatherData(&d);
    sendLargeString(weatherDataToString(&d));

    return;
  }

  // Tárolt adatok lekérdezése.
  if (data.startsWith("d"))
  {
    sendDataOnSerial("[DATA_BEGIN]");
    waitingForSerialResponse();

    data.replace("d", "");
    unsigned long reqDateLong = atol(data.substring(0, 8).c_str());
    unsigned long reqTimeLong = atol(data.substring(8, 14).c_str());

    SdFile file;
    char filename[15];
    sd.chdir("/", true);
    sd.vwd()->rewind();
    while (file.openNext(sd.vwd(), O_READ))
    {
      file.getName(filename, 14);
      // Alapból valami System Volume-t is kilistáz, amire nincs szükségünk.
      if (strcmp(filename, "System Volume"))
      {
        // Fájlnév átalakítása számmá.
        unsigned long dateLong = strtol(filename, NULL, 10);
        
        // Ha megegyezik a dátum, vagy későbbi, akkor feldolgozzuk.
        if (dateLong >= reqDateLong)
        {
          // Beolvassuk a fájl tartalmát.
          while (file.available())
          {
            String dataRow = "";
            char cd;
            // Sor beolvasása.
            while ('\n' != (cd = file.read())){dataRow.concat(cd);}

            // Rögzítés időpontjának átalakítása számmá.
            unsigned long timeLong = atol(dataRow.substring(10, 16).c_str());
            
            // Ha az időpont későbbi, akkor küldjük a sort.
            if (((timeLong > reqTimeLong) && (dateLong == reqDateLong)) || (dateLong > reqDateLong))
            {
              sendLargeString(dataRow);
            }
          }   
        }
      }
      file.close();
    }
    
    sendDataOnSerial("[DATA_END]");
    
    return;
  }

  // Állomány(ok) törlése.
  if (data.startsWith("r"))
  {
    data.replace("r", "");
    unsigned long from = atol(String("20" + data.substring(0, 6)).c_str());
    unsigned long to = atol(String("20" + data.substring(6, 12)).c_str());

    // Végig haladunk a fájlokon.
    SdFile file;
    char filename[15];
    sd.chdir("/", true);
    sd.vwd()->rewind();
    while (file.openNext(sd.vwd(), O_READ))
    {
      file.getName(filename, 14);
      // Alapból valami System Volume-t is kilistáz, amire nincs szükségünk.
      bool rem = false;
      if (strcmp(filename, "System Volume"))
      {
        // Fájlnév átalakítása számmá.
        unsigned long dateLong = strtol(filename, NULL, 10);
        
        // Ha az adott intervallumon belül van, akkor töröljük.
        if (dateLong >= min(from, to) && dateLong <= max(to, from))
        {
          rem = true;
          file.close();

          sd.remove(filename);
          Serial.print("REMOVE -> ");
          Serial.println(filename);
        }
        
      }
      if (!rem)
      {
        file.close();
      }
    }
    
    return;
  }

  // Dátum és idő beállítása.
  if (data.startsWith("t"))
  {
    data.replace("t", "");
    
    uint16_t year = atoi(data.substring(0, 4).c_str());
    uint8_t mon = atoi(data.substring(4, 6).c_str());
    uint8_t day = atoi(data.substring(6, 8).c_str());
    uint8_t h = atoi(data.substring(8, 10).c_str());
    uint8_t m = atoi(data.substring(10, 12).c_str());
    uint8_t s = atoi(data.substring(12, 14).c_str());

    rtc.setDate(day, mon, year);
    rtc.setTime(h, m, s);
    
    Serial.print("TIME -> ");
    Serial.print(year);
    Serial.print(".");
    Serial.print(mon);
    Serial.print(".");
    Serial.print(day);
    Serial.print(" - ");
    Serial.print(h);
    Serial.print(":");
    Serial.print(m);
    Serial.print(":");
    Serial.print(s);
    Serial.println();
    
    return;
  }

  // Ismeretlen parancs.
  debug("Unknown command:", true, false);
  debug(data, false, true);
}

void setup() 
{

  Serial.begin(9600);
  while(!Serial);
  inputString.reserve(MAX_DATA_STRING_LENGTH);

  dirRef = readDirRef();
  speedRef = readSpeedRef();

  // Óra.
  debug("Init Clock...", true, false);
  rtc.begin();
  debug("OK", false, true);

  // Feszültségmérő.
  debug("Init Voltage meter...", true, false);
  ina219.begin();
  debug("OK", false, true);

  // SD kártya.
  debug("Init SD card...", true, false);
  if (!sd.begin(4, SPI_HALF_SPEED)) {
    if (sd.card()->errorCode()) {
      debug("INIT FAILED!", false, true);
      while(true) delay(100);
    }
    if (sd.vol()->fatType() == 0) {
      debug("NO VALID PARTITION FOUND!", false, true);
      while(true) delay(100);
    }
    if (!sd.vwd()->isOpen()) {
      debug("OPENING ROOT PARTITION FAILED!", false, true);
      while(true) delay(100);
    }
  }
  debug("OK", false, true);

  // Előző adat nullázása.
  //getWeatherData(&prevData);
  initWeatherData(&prevData, NULL);

  delay(1000);
}

// Idővel kapcsolatos dolgok.
unsigned long previousMillis = 0;
#define INTERVAL 1000

void loop() 
{

  // Soros port figyelése.
  if (stringComplete) 
  {
    handleSerialData(inputString);
    inputString = "";
    stringComplete = false;
  }

  // Eltellt idő lekérdezése.
  unsigned long currentMillis = millis();
  // Ha eltellt egy bizonyos idő, akkor lekérdezzük az aktuális adatokat.
  if ((unsigned long)(currentMillis - previousMillis) >= INTERVAL) {
    // Előző idő tárolása.
    previousMillis = currentMillis;

    // Aktuális adat lekérdezése.
    weather_data_t currentData;
    getWeatherData(&currentData);
    
    // Ha van változás, akkor tároljuk.
    if (isDataChanged(&prevData, &currentData))
    {
      debug("data save:", true, false);
      debug(weatherDataToString(&currentData), false, true);

      // Mentés fájlba.
      char c[9];
      currentData.dateString.toCharArray(c, currentData.dateString.length() + 1);

      #ifdef STORE_DATA_TO_SD_CARD
        File f = sd.open(c, FILE_WRITE);
        if (f)
        {
          // Adatok fájlba írása.
          f.print(weatherDataToString(&currentData));
          f.print("\n");
       
          // Fájl bezárása.
          f.close();
          Serial.println("SD <- " + weatherDataToString(&currentData));
        }
      #endif

      // Másolat készítése.
      initWeatherData(&prevData, &currentData);
    }
  }

}

void serialEvent() 
{
  while (Serial.available()) 
  {
    char inChar = (char)Serial.read();
    if (inChar == '\n') 
    {
      stringComplete = true;
    }
    else
    {
      inputString += inChar;
    }
  }
}
