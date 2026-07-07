/*
 * ============================================================
 *  TOTP Authenticator — Arduino Uno SMD + 16×2 I2C LCD
 *  BIG DIGIT DISPLAY (2×2 per digit)
 * ============================================================
 *
 *  WIRING:  GND→GND  VCC→5V  SDA→A4  SCL→A5
 *  SERIAL:  9600 baud
 *    KEY:<hex>\n     — set TOTP secret
 *    TIME:<unix>\n   — sync clock
 *    LABEL:<name>\n  — set label
 *    PING / STATUS   — diagnostics
 *
 *  Library: LiquidCrystal_I2C (Frank de Brabander)
 */

#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <EEPROM.h>
#include "sha1_totp.h"

// ── LCD (try 0x3F if 0x27 doesn't work) ─────────────────────
LiquidCrystal_I2C lcd(0x27, 16, 2);

// ── TOTP config ─────────────────────────────────────────────
#define TOTP_TIMESTEP   30
#define TOTP_DIGITS      6
#define MAX_KEY_LEN     32
#define SHA1_HASH_SIZE  20

// ── EEPROM layout ───────────────────────────────────────────
#define EEPROM_KEYLEN_ADDR   0
#define EEPROM_KEY_ADDR      1
#define EEPROM_LABEL_LEN    33
#define EEPROM_LABEL_ADDR   34

// ── State ───────────────────────────────────────────────────
uint8_t  secretKey[MAX_KEY_LEN];
uint8_t  keyLen       = 0;
char     serviceLabel[17] = "TOTP";

uint32_t unixEpochAtSync = 0;
uint32_t millisAtSync    = 0;
bool     keySaved        = false;
bool     timeSynced      = false;

// ═════════════════════════════════════════════════════════════
//  BIG NUMBER FONT  (8 custom chars → 2×2 per digit)
// ═════════════════════════════════════════════════════════════

// 2px thick, split horizontal bars, 1px right margin
byte c_TL[8] = {B11110,B11110,B11000,B11000,B11000,B11000,B11000,B11000};
byte c_TR[8] = {B11110,B11110,B00110,B00110,B00110,B00110,B00110,B00110};
byte c_BL[8] = {B11000,B11000,B11000,B11000,B11000,B11000,B11110,B11110};
byte c_BR[8] = {B00110,B00110,B00110,B00110,B00110,B00110,B11110,B11110};
byte c_LF[8] = {B11110,B11110,B11000,B11000,B11000,B11000,B11110,B11110};
byte c_RF[8] = {B11110,B11110,B00110,B00110,B00110,B00110,B11110,B11110};
byte c_TB[8] = {B11110,B11110,B00000,B00000,B00000,B00000,B11110,B11110};
byte c_RV[8] = {B00110,B00110,B00110,B00110,B00110,B00110,B00110,B00110};

// Digit patterns: { top-left, top-right, bottom-left, bottom-right }
const uint8_t bigDigits[10][4] = {
  { 0,    1,    2,    3   },  // 0
  { ' ',  7,    ' ',  7   },  // 1
  { 6,    5,    4,    6   },  // 2
  { 6,    5,    6,    5   },  // 3
  { 2,    3,    ' ',  7   },  // 4
  { 4,    6,    6,    5   },  // 5
  { 4,    6,    4,    5   },  // 6
  { 0,    1,    ' ',  7   },  // 7
  { 4,    5,    4,    5   },  // 8
  { 4,    5,    6,    5   },  // 9
};

void drawBigDigit(uint8_t digit, uint8_t col) {
  if (digit > 9) return;
  lcd.setCursor(col, 0);
  lcd.write(bigDigits[digit][0]);
  lcd.write(bigDigits[digit][1]);
  lcd.setCursor(col, 1);
  lcd.write(bigDigits[digit][2]);
  lcd.write(bigDigits[digit][3]);
}

// ═════════════════════════════════════════════════════════════
//  TOTP Generation
// ═════════════════════════════════════════════════════════════

uint32_t generateTOTP(uint32_t unixTime) {
  uint32_t counter = unixTime / TOTP_TIMESTEP;
  uint8_t msg[8] = {0,0,0,0,
    (uint8_t)(counter>>24),(uint8_t)(counter>>16),
    (uint8_t)(counter>>8), (uint8_t)(counter)};
  uint8_t hash[SHA1_HASH_SIZE];
  hmac_sha1(secretKey, keyLen, msg, 8, hash);
  uint8_t off = hash[19] & 0x0F;
  uint32_t code = ((uint32_t)(hash[off]&0x7F)<<24)
                | ((uint32_t)hash[off+1]<<16)
                | ((uint32_t)hash[off+2]<<8)
                |  (uint32_t)hash[off+3];
  return code % 1000000;
}

uint32_t currentUnixTime() {
  return unixEpochAtSync + (millis() - millisAtSync) / 1000UL;
}

// ═════════════════════════════════════════════════════════════
//  EEPROM
// ═════════════════════════════════════════════════════════════

void saveKeyToEEPROM() {
  EEPROM.write(EEPROM_KEYLEN_ADDR, keyLen);
  for (uint8_t i=0;i<keyLen;i++) EEPROM.write(EEPROM_KEY_ADDR+i, secretKey[i]);
}

bool loadKeyFromEEPROM() {
  uint8_t n = EEPROM.read(EEPROM_KEYLEN_ADDR);
  if (n==0||n>MAX_KEY_LEN||n==0xFF) return false;
  keyLen=n;
  for (uint8_t i=0;i<keyLen;i++) secretKey[i]=EEPROM.read(EEPROM_KEY_ADDR+i);
  return true;
}

void saveLabelToEEPROM() {
  uint8_t n=strlen(serviceLabel); if(n>16)n=16;
  EEPROM.write(EEPROM_LABEL_LEN,n);
  for(uint8_t i=0;i<n;i++) EEPROM.write(EEPROM_LABEL_ADDR+i,serviceLabel[i]);
}

void loadLabelFromEEPROM() {
  uint8_t n=EEPROM.read(EEPROM_LABEL_LEN);
  if(n==0||n>16||n==0xFF){strcpy(serviceLabel,"TOTP");return;}
  for(uint8_t i=0;i<n;i++) serviceLabel[i]=EEPROM.read(EEPROM_LABEL_ADDR+i);
  serviceLabel[n]='\0';
}

// ═════════════════════════════════════════════════════════════
//  Hex helper
// ═════════════════════════════════════════════════════════════

uint8_t hexNibble(char c) {
  if(c>='0'&&c<='9')return c-'0';
  if(c>='a'&&c<='f')return c-'a'+10;
  if(c>='A'&&c<='F')return c-'A'+10;
  return 0;
}
uint8_t hexByte(char h, char l) { return (hexNibble(h)<<4)|hexNibble(l); }

// ═════════════════════════════════════════════════════════════
//  Serial commands
// ═════════════════════════════════════════════════════════════

String serialBuf = "";

void processSerial() {
  while (Serial.available()) {
    char c = Serial.read();
    if (c=='\n'||c=='\r') {
      if (serialBuf.length()>0) { handleCommand(serialBuf); serialBuf=""; }
    } else serialBuf += c;
  }
}

void handleCommand(String &cmd) {
  if (cmd.startsWith("KEY:")) {
    String hex = cmd.substring(4); hex.trim();
    uint8_t n = hex.length()/2;
    if (n==0||n>MAX_KEY_LEN) { Serial.println("ERR:INVALID_KEY_LENGTH"); return; }
    for (uint8_t i=0;i<n;i++) secretKey[i]=hexByte(hex.charAt(i*2),hex.charAt(i*2+1));
    keyLen=n; saveKeyToEEPROM(); keySaved=true;
    Serial.print("OK:KEY_SAVED:"); Serial.println(keyLen);
  }
  else if (cmd.startsWith("TIME:")) {
    String t = cmd.substring(5); t.trim();
    unixEpochAtSync = (uint32_t)t.toInt();
    millisAtSync = millis(); timeSynced = true;
    Serial.print("OK:TIME_SYNCED:"); Serial.println(unixEpochAtSync);
  }
  else if (cmd.startsWith("LABEL:")) {
    String l = cmd.substring(6); l.trim();
    l.toCharArray(serviceLabel, 17); saveLabelToEEPROM();
    Serial.print("OK:LABEL_SET:"); Serial.println(serviceLabel);
  }
  else if (cmd=="STATUS") {
    Serial.print("KEY:"); Serial.print(keySaved?"YES":"NO");
    Serial.print(" TIME:"); Serial.print(timeSynced?"YES":"NO");
    if(timeSynced){Serial.print(" UNIX:");Serial.print(currentUnixTime());}
    Serial.print(" LABEL:"); Serial.println(serviceLabel);
  }
  else if (cmd=="PING") { Serial.println("PONG:TOTP_AUTH_V1"); }
  else { Serial.println("ERR:UNKNOWN_CMD"); }
}

// ═════════════════════════════════════════════════════════════
//  Display
// ═════════════════════════════════════════════════════════════

uint32_t lastDisplayedCode = 0xFFFFFFFF;
int      lastCountdown     = -1;

void displayWaiting() {
  lcd.clear();
  lcd.setCursor(1, 0);
  lcd.print("TOTP  Waiting");
  lcd.setCursor(1, 1);
  lcd.print("Connect via USB");
}

void displayNeedSync() {
  lcd.clear();
  lcd.setCursor(2, 0);
  lcd.print("Key  loaded!");
  lcd.setCursor(1, 1);
  lcd.print("Sync time...");
}

void displayCode(uint32_t code, int secondsLeft) {
  if (code != lastDisplayedCode) {
    lastDisplayedCode = code;
    lastCountdown = -1; // Force counter update
    lcd.clear();

    char buf[7];
    sprintf(buf, "%06lu", code);

    // Full-width layout: 3-gap-3
    drawBigDigit(buf[0] - '0', 0);
    drawBigDigit(buf[1] - '0', 2);
    drawBigDigit(buf[2] - '0', 4);
    
    drawBigDigit(buf[3] - '0', 7);
    drawBigDigit(buf[4] - '0', 9);
    drawBigDigit(buf[5] - '0', 11);
  }

  // Counter at bottom right
  if (secondsLeft != lastCountdown) {
    lastCountdown = secondsLeft;
    lcd.setCursor(14, 1);
    if (secondsLeft < 10) lcd.print(" ");
    lcd.print(secondsLeft);
  }
}

// ═════════════════════════════════════════════════════════════
//  Setup & Loop
// ═════════════════════════════════════════════════════════════

void setup() {
  Serial.begin(9600);
  Serial.println("TOTP_AUTH_V1:READY");

  lcd.init();
  lcd.backlight();

  lcd.createChar(0, c_TL);
  lcd.createChar(1, c_TR);
  lcd.createChar(2, c_BL);
  lcd.createChar(3, c_BR);
  lcd.createChar(4, c_LF);
  lcd.createChar(5, c_RF);
  lcd.createChar(6, c_TB);
  lcd.createChar(7, c_RV);

  keySaved = loadKeyFromEEPROM();
  loadLabelFromEEPROM();

  if (keySaved) displayNeedSync();
  else          displayWaiting();
}

void loop() {
  processSerial();

  if (!keySaved) {
    static uint32_t t = 0;
    if (millis()-t > 5000) { displayWaiting(); t=millis(); }
    return;
  }
  if (!timeSynced) {
    static uint32_t t = 0;
    if (millis()-t > 2000) { displayNeedSync(); t=millis(); }
    return;
  }

  uint32_t now  = currentUnixTime();
  uint32_t code = generateTOTP(now);
  int left = TOTP_TIMESTEP - (int)(now % TOTP_TIMESTEP);

  displayCode(code, left);
  delay(250);
}
