#ifndef WS_COMMON_H
#define WS_COMMON_H

#define DEBUG_SERIAL_PRINT
#undef DEBUG_SERIAL_PRINT

#ifdef DEBUG_SERIAL_PRINT
  #define debug(msg,d,ln) ({if(d)Serial.print("DEBUG: ");Serial.print(msg);if(ln)Serial.print("\n");})
#else 
  #define debug(msg,d,ln)
#endif

// Ideiglenes tároló.
#define RF24_PAYLOAD_SIZE 16
#define MAX_DATA_STRING_LENGTH (RF24_PAYLOAD_SIZE * 2)
char data_string[MAX_DATA_STRING_LENGTH];
// Ideiglenes adatok törlése.
void clearDataString()
{
  strcpy(data_string, "");
}

// Soros port kezelése.
String inputString = "";
boolean stringComplete = false;
#define sendDataOnSerial(d) ({Serial.println(d);})



#endif
