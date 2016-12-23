#ifndef WEATHER_DATA_H
#define WEATHER_DATA_H

// Időkülönbség két tárolás között (másodpercben).
#define TIME_DELTA_TO_STORE (10 * 60)
// Időkülönbség két tárolás között ha nagy az előző mérés közti különbség (másodpercben).
#define TIME_DELTA_TO_STORE2 (2 * 60)
// Szélirány esetén a különbség, ami alapján tárolunk.
#define DIRECTION_DELTA_TO_STORE (5)
// Feszültség esetén a különbség, ami alapján tárolunk.
#define VOLTAGE_DELTA_TO_STORE (0.2)

typedef struct weather_data_s
{
  uint32_t unixTime;
  String dateString;
  String timeString;
  int16_t dir;
  int16_t dirR;
  float voltage;
  float voltageM;
};
typedef struct weather_data_s weather_data_t;

void initWeatherData(weather_data_t *d, weather_data_t *cpy)
{
  if (NULL == cpy)
  {
    d->unixTime = 0;
    d->dir = 0;
    d->dirR = 0;
    d->voltage = .0f;
    d->voltageM = .0f;
    return;
  }

  d->unixTime = cpy->unixTime;
  d->dir = cpy->dir;
  d->dirR = cpy->dirR;
  d->voltage = cpy->voltage;
  d->voltageM = cpy->voltageM;
}

bool isDataChanged(weather_data_t *dOld, weather_data_t *dNew)
{
  int16_t dDir = min(min(abs(dNew->dir - dOld->dir), abs(dNew->dir + 360 - dOld->dir)), abs(dNew->dir - (dOld->dir + 360)));
  return ((dNew->unixTime - dOld->unixTime >= TIME_DELTA_TO_STORE) || ((dNew->unixTime - dOld->unixTime >= TIME_DELTA_TO_STORE2) && ((dDir >= DIRECTION_DELTA_TO_STORE) || (fabs(dNew->voltage - dOld->voltage) >= VOLTAGE_DELTA_TO_STORE))));
}

String weatherDataToString(weather_data_t *d)
{
  String s = "#";

  s += d->dateString;
  s += "@";
  s += d->timeString;
  s += "D";
  s += d->dir;
  s += "R";
  s += d->dirR;
  s += "V";
  s += d->voltage;
  s += "M";
  s += d->voltageM;
  s += "&";

  return s;
}

#endif
