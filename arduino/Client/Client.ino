// gcd -> aktuális adat lekérdezése
// d20161215213457 -> tárolt adatok lekérdezése az adott időponttól
// #20160215@130125D312R-12V2.37M0.64&
// # dátum @ idő D irány nyers R irány referencia V feszültség nyers M meredekség &
// r160201180105 -> állományok törlése 2016.02.02 és 2018.01.05 között
// t20161215213457 -> idő beállítása 2016.12.15 - 21:34:57-re
// gwd -> szélirány lekérdezése
// dr123 -> új irányreferencia tárolása
// sr12.3 -> új sebességreferencia tárolása (lineáris regresszió esetén a meredekség)


#include "common.h"

// Weather Station Client (wsc)
#define CLIENT_DEVICE_ID "wsc"

// Rádió.
#include <SPI.h>
#include "nRF24L01.h"
#include "RF24.h"
RF24 radio(9,10);
byte addresses[][9] = {"pcToNode","nodeToPc"};

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

void sendDeviceID()
{
  sendDataOnSerial(CLIENT_DEVICE_ID);
}

// Beérkező soros port üzenet kezelése.
void handleSerialData(char *data) 
{

  // Eszköz azonosítójának a lekérdezése.
  if (String(data).startsWith("getDevID"))
  {
    sendDeviceID();
    return;
  }
  
  // Küldjük rádión.
  sendDataOnRadio(data);
}

// Beérkező rádió üzenet kezelése.
void handleRadioData(char *data) 
{
  
  // Küldjük a soros portra.
  sendDataOnSerial(data);
}

// Üzenet küldése rádión keresztül.
void sendDataOnRadio(char *data) 
{
  radio.stopListening();
  while(!radio.write(data, MAX_DATA_STRING_LENGTH));
  // HACK: valamiért szabálytalan időközönként minden második 
  //       üzenet nem érkezik meg, így minden üzenet küldése 
  //       után küldök valami feleslegeset is
  while(!radio.write(".", 1));
  radio.powerUp();
  radio.startListening();
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

  sendDeviceID();

  delay(200);
}

void loop() 
{

  // Soros port figyelése.
  if (stringComplete) 
  {
    inputString.toCharArray(data_string, inputString.length() + 1);
    handleSerialData(data_string);
    clearDataString();
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
