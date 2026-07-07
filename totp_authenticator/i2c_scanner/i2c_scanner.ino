// Quick I2C Address Scanner
// Upload this, open Serial Monitor at 9600 baud
// It will tell you the correct address for your LCD

#include <Wire.h>

void setup() {
  Wire.begin();
  Serial.begin(9600);
  Serial.println("I2C Scanner - Looking for your LCD...\n");
}

void loop() {
  byte count = 0;
  for (byte addr = 1; addr < 127; addr++) {
    Wire.beginTransmission(addr);
    if (Wire.endTransmission() == 0) {
      Serial.print("  FOUND device at address: 0x");
      if (addr < 16) Serial.print("0");
      Serial.println(addr, HEX);
      count++;
    }
  }
  if (count == 0) Serial.println("  No I2C devices found! Check wiring: SDA->A4, SCL->A5");
  else { Serial.print("\n  Total devices: "); Serial.println(count); }
  Serial.println("  (Scanning again in 5s...)\n");
  delay(5000);
}
