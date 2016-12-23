#include "common.h"

// Rádió.
#include <SPI.h>
#include "nRF24L01.h"
#include "RF24.h"
RF24 radio(9,10);
byte addresses[][9] = {"nodeToPc","pcToNode"};

// Rádió beállítása.
void setupRadio()
{
  radio.begin();
  radio.setRetries(15,15);
  radio.setChannel(30);
  radio.setPayloadSize(RF24_PAYLOAD_SIZE);
  radio.setDataRate(RF24_250KBPS);
  radio.setCRCLength(RF24_CRC_16);
  radio.setPALevel(RF24_PA_MAX);
  radio.openWritingPipe(addresses[0]);
  radio.openReadingPipe(1, addresses[1]);
  radio.startListening();
}

#include <HMC5883L_Simple.h>
// Iránytű.
HMC5883L_Simple Compass;

// Szélirány lekérdezése.
int16_t getWindDirection()
{
  return (int16_t)(Compass.GetHeadingDegrees());
}

void waitingForSerialResponse()
{
  inputString = "";
  stringComplete = false;
  sendDataOnSerial(".");
  while (!stringComplete) serialEvent();
}


// Beérkező soros port üzenet kezelése.
void handleSerialData(String data) 
{
  
  // Szélirányt kérdeznek le.
  if (data.startsWith("gwd"))
  {
    // Küldjük a szélirányt.
    debug("gwd:", true, false);
    int16_t wd = getWindDirection();
    sendDataOnSerial(wd);
    return;
  }

  // Rádión küldendő aktuális adat érkezett.
  if (data.startsWith(":"))
  {

    // Adatcsomag számának a lekérdezése.
    data.replace(":", "");
    int n = data.toInt();
    
    debug("data begin: ", true, false);
    debug(n, false, true);
    for (int i = 0; i <= n; i++)
    {
      waitingForSerialResponse();

      debug(inputString, true, true);
      sendDataOnRadio(inputString);
    }

    inputString = "";
    stringComplete = false;

    debug("data end", true, true);
    
    return;
  }

  // Sok tárolt adat fog érkezni.
  if (data.startsWith("[DATA_BEGIN]"))
  {
    // Tovább küldjük rádión.
    sendDataOnRadio(data);
    // Majd jelezzük, hogy kezdődhet a munka.
    sendDataOnSerial(".");
    return;
  }

  // Tárolt adatok küldésének vége.
  if (data.startsWith("[DATA_END]"))
  {
    sendDataOnRadio(data);
    return;
  }

  // Ismeretlen parancs.
  debug("Unknown command:", true, false);
  debug(data, false, true);
}

// Beérkező rádió üzenet kezelése.
void handleRadioData(char *data) 
{
  
  // Aktuális adatok lekérdezése.
  if (String(data).startsWith("gcd"))
  {
    // Továbbküldjük az igényt a másik modulnak...
    sendDataOnSerial(data);
    return;
  }

  // Új irányreferencia küldése tárolásra.
  if (String(data).startsWith("dr"))
  {
    sendDataOnSerial(data);
    return;
  }

  // Új sebességreferencia küldése tárolásra.
  if (String(data).startsWith("sr"))
  {
    sendDataOnSerial(data);
    return;
  }

  // Tárolt adatok lekérdezése.
  if (String(data).startsWith("d"))
  {
    sendDataOnSerial(data);
    return;
  }

  // Állomány(ok) törlése.
  if (String(data).startsWith("r"))
  {
    sendDataOnSerial(data);
    return;
  }

  // Idő beállítása.
  if (String(data).startsWith("t"))
  {
    sendDataOnSerial(data);
    return;
  }

  // Ismeretlen parancs.
  debug("Unknown command:", true, false);
  debug(data, false, true);
}

// Üzenet küldése rádión keresztül.
void sendDataOnRadio(char *data) 
{
  radio.stopListening();
  while(!radio.write(data, MAX_DATA_STRING_LENGTH));
  radio.powerUp();
  radio.startListening();
}

void sendDataOnRadio(String s) 
{
  inputString.toCharArray(data_string, inputString.length() + 1);
  sendDataOnRadio(data_string);
  clearDataString();
}

void setup() 
{
  Serial.begin(9600);
  while(!Serial);
  inputString.reserve(MAX_DATA_STRING_LENGTH);
  // RF Rádió.
  debug("Init Radio...", true, false);
  setupRadio();
  debug("OK", false, true);

  // Iránytű.
  debug("Init Compass...", true, false);
  Compass.SetSamplingMode(COMPASS_SINGLE);
  Compass.SetScale(COMPASS_SCALE_810);
  Compass.SetOrientation(COMPASS_HORIZONTAL_X_NORTH);
  debug("OK", false, true);
  
  delay(500);
}

void loop() 
{
  // Soros port figyelése.
  if (stringComplete) 
  {
    handleSerialData(inputString);
    inputString = "";
    stringComplete = false;
  }
  else
  // Rádió figyelése.
  if(radio.available()) 
  {
    radio.read(data_string, MAX_DATA_STRING_LENGTH);
    handleRadioData(data_string);
    clearDataString();
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
