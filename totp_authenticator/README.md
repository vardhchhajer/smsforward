# 🔐 Arduino TOTP Authenticator
A hardware TOTP (Time-based One-Time Password) authenticator using an **Arduino Uno SMD** and a **16×2 I2C LCD display**. Functions like Google Authenticator, but on a dedicated hardware device.

## 📦 Components
| Component | Description |
|-----------|-------------|
| Arduino Uno SMD | Main microcontroller |
| 16×2 LCD + I2C Backpack (HW-61) | Display for TOTP codes |
| USB Cable | For programming and serial config |

## 🔌 Wiring Diagram
```
  16×2 LCD I2C Backpack        Arduino Uno
  ─────────────────────        ───────────
        GND  ───────────────→  GND
        VCC  ───────────────→  5V
        SDA  ───────────────→  A4
        SCL  ───────────────→  A5
```
> **Note**: The I2C address is set to `0x27` (most common). If your LCD stays blank after uploading, try changing it to `0x3F` in the sketch.

## 🚀 Setup Instructions
### Step 1: Install Arduino Libraries
Open Arduino IDE → **Sketch** → **Manage Libraries** → Install:
- **LiquidCrystal_I2C** by Frank de Brabander

### Step 2: Upload the Sketch
1. Open `totp_authenticator.ino` in Arduino IDE
2. Select **Board**: `Arduino Uno`
3. Select the correct **Port**
4. Click **Upload** ▶

### Step 3: Install Python Dependencies
```bash
pip install pyserial pyotp
```

### Step 4: Configure Your TOTP Secret
#### Interactive Mode (recommended):
```bash
python setup_totp.py
```
This launches a menu-driven wizard where you can:
- Set your TOTP secret key (Base32 format)
- Set a service label (e.g., "GitHub", "Google")
- Sync the clock
- Verify the generated code matches

#### CLI Mode:
```bash
# Full setup in one command
python setup_totp.py --key JBSWY3DPEHPK3PXP --label GitHub

# Just re-sync the time
python setup_totp.py --sync-only

# Specify port manually
python setup_totp.py --port COM3 --key YOUR_SECRET_KEY --label MyService
```

## 📋 How to Get Your TOTP Secret Key
When you enable 2FA on a service (GitHub, Google, etc.):
1. Look for the **"Can't scan QR code?"** or **"Manual entry"** option
2. Copy the Base32 secret key (looks like: `JBSWY3DPEHPK3PXP`)
3. Use this key with the Python setup tool

## 🖥️ LCD Display
```
┌────────────────┐
│🔒 GitHub       │  ← Service label
│123 456  ⏱▓▓▓__│  ← TOTP code + countdown
└────────────────┘
```
- **Row 1**: Lock icon + service name
- **Row 2**: 6-digit TOTP code (split for readability) + countdown bar

## ⚡ Serial Protocol
The Arduino accepts these commands over serial at 9600 baud:

| Command | Description | Response |
|---------|-------------|----------|
| `PING` | Check firmware | `PONG:TOTP_AUTH_V1` |
| `KEY:<hex>` | Set secret key (hex-encoded) | `OK:KEY_SAVED:<len>` |
| `TIME:<unix>` | Sync Unix timestamp | `OK:TIME_SYNCED:<time>` |
| `LABEL:<name>` | Set LCD label (max 16 chars) | `OK:LABEL_SET:<name>` |
| `STATUS` | Query device state | Key/time/label status |

## ⚠️ Important Notes
1. **Time Drift**: The Arduino uses `millis()` for timekeeping, which drifts ~1–2 seconds per hour. **Re-sync time periodically** by running:
   ```bash
   python setup_totp.py --sync-only
   ```

2. **Key Storage**: The TOTP secret is stored in EEPROM and persists across power cycles. You only need to set it once.

3. **Security**: The secret key is stored in plaintext on the Arduino's EEPROM. This device is intended as a learning project / convenience tool, not a high-security vault.

4. **Multiple Accounts**: This version supports one TOTP account at a time. To switch accounts, re-run the setup script with a new key.

## 🧪 Testing
Use this test secret to verify everything works:
```
Base32 Key: JBSWY3DPEHPK3PXP
```

```bash
python setup_totp.py --key JBSWY3DPEHPK3PXP --label Test --verify
```

The code shown on the LCD should match the code printed by the Python script.

## Features
- Hardware-based TOTP authenticator
- Compatible with Arduino Uno SMD and 16×2 I2C LCD display
- Supports interactive and CLI setup modes
- Stores TOTP secret in EEPROM for persistence
- Displays service label, TOTP code, and countdown on LCD

## Installation
1. Clone the repository: `git clone https://github.com/your-username/smsforward.git`
2. Install Arduino libraries: Open Arduino IDE → **Sketch** → **Manage Libraries** → Install: **LiquidCrystal_I2C** by Frank de Brabander
3. Install Python dependencies: `pip install pyserial pyotp`
4. Upload the Arduino sketch: Open `totp_authenticator.ino` in Arduino IDE, select **Board**: `Arduino Uno`, select the correct **Port**, and click **Upload** ▶
5. Run the Python setup script: `python setup_totp.py`

## Configuration
No environment variables are required for this project.

## Usage
1. Run the Python setup script: `python setup_totp.py`
2. Follow the interactive prompts to set your TOTP secret key, service label, and sync the clock
3. Verify the generated TOTP code matches the code displayed on the LCD

## Contributing
Contributions are welcome! Please submit a pull request with your changes and a brief description of what you've added or fixed.