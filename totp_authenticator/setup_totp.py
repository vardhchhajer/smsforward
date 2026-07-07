#!/usr/bin/env python3
"""
═══════════════════════════════════════════════════════════════
  TOTP Authenticator — Python Configuration Tool
═══════════════════════════════════════════════════════════════

  Configures the Arduino TOTP Authenticator over USB serial.
  • Sets the TOTP secret key (Base32-encoded, as given by services)
  • Syncs the current system time to the Arduino
  • Optionally sets a service label for the LCD display
  • Can verify codes match between PC and Arduino

  Requirements:
    pip install pyserial pyotp

  Usage:
    python setup_totp.py                     # Interactive mode
    python setup_totp.py --port COM3         # Specify port
    python setup_totp.py --key JBSWY3DPEHPK3PXP --label GitHub
"""

import argparse
import sys
import time
import struct
import hashlib
import hmac
import base64
import re

try:
    import serial
    import serial.tools.list_ports
except ImportError:
    print("ERROR: pyserial not installed. Run: pip install pyserial")
    sys.exit(1)

try:
    import pyotp
    HAS_PYOTP = True
except ImportError:
    HAS_PYOTP = False


# ─── TOTP reference implementation (fallback if pyotp missing) ───

def generate_totp_local(secret_bytes, unix_time, period=30, digits=6):
    """Generate TOTP code locally for verification."""
    counter = unix_time // period
    msg = struct.pack(">Q", counter)
    h = hmac.new(secret_bytes, msg, hashlib.sha1).digest()
    offset = h[19] & 0x0F
    code = struct.unpack(">I", h[offset:offset + 4])[0]
    code &= 0x7FFFFFFF
    code %= 10 ** digits
    return code


def base32_to_hex(b32_key):
    """Convert Base32 secret to hex string for the Arduino."""
    # Clean the key: remove spaces, dashes, uppercase
    b32_key = b32_key.replace(" ", "").replace("-", "").upper()
    # Pad to multiple of 8
    while len(b32_key) % 8 != 0:
        b32_key += "="
    raw = base64.b32decode(b32_key)
    return raw.hex(), raw


def find_arduino_port():
    """Auto-detect Arduino serial port."""
    ports = serial.tools.list_ports.comports()
    arduino_ports = []

    for p in ports:
        desc = (p.description or "").lower()
        mfr  = (p.manufacturer or "").lower()
        if any(kw in desc for kw in ["arduino", "ch340", "cp210", "usb-serial", "ft232"]):
            arduino_ports.append(p.device)
        elif any(kw in mfr for kw in ["arduino", "wch", "silicon labs", "ftdi"]):
            arduino_ports.append(p.device)

    if len(arduino_ports) == 1:
        return arduino_ports[0]
    elif len(arduino_ports) > 1:
        print("\n  Multiple Arduino-compatible ports found:")
        for i, p in enumerate(arduino_ports):
            print(f"    [{i+1}] {p}")
        choice = input(f"  Select port [1-{len(arduino_ports)}]: ").strip()
        try:
            return arduino_ports[int(choice) - 1]
        except (ValueError, IndexError):
            return arduino_ports[0]
    else:
        # Show all ports
        if ports:
            print("\n  Available serial ports:")
            for i, p in enumerate(ports):
                print(f"    [{i+1}] {p.device} — {p.description}")
            choice = input(f"  Select port [1-{len(ports)}]: ").strip()
            try:
                return ports[int(choice) - 1].device
            except (ValueError, IndexError):
                pass
        return None


def open_serial(port, baudrate=9600, timeout=3):
    """Open serial connection with reset wait."""
    ser = serial.Serial(port, baudrate, timeout=timeout)
    time.sleep(2)  # Wait for Arduino reset after serial connection
    # Flush any boot messages
    ser.reset_input_buffer()
    return ser


def send_command(ser, cmd, expect_prefix="OK"):
    """Send command and wait for response."""
    ser.reset_input_buffer()
    full_cmd = cmd + "\n"
    ser.write(full_cmd.encode("ascii"))
    ser.flush()

    # Wait for response
    deadline = time.time() + 5
    while time.time() < deadline:
        if ser.in_waiting > 0:
            line = ser.readline().decode("ascii", errors="replace").strip()
            if line:
                return line
        time.sleep(0.05)
    return None


def ping_device(ser):
    """Check if the Arduino is running the TOTP firmware."""
    response = send_command(ser, "PING")
    if response and "PONG:TOTP_AUTH" in response:
        return True
    return False


def sync_time(ser):
    """Send current Unix timestamp to the Arduino."""
    unix_time = int(time.time())
    response = send_command(ser, f"TIME:{unix_time}")
    if response and "OK:TIME_SYNCED" in response:
        print(f"  ✓ Time synced: {unix_time} (UTC)")
        return True
    else:
        print(f"  ✗ Time sync failed: {response}")
        return False


def set_key(ser, b32_key):
    """Convert Base32 key and send to Arduino."""
    try:
        hex_str, raw_bytes = base32_to_hex(b32_key)
    except Exception as e:
        print(f"  ✗ Invalid Base32 key: {e}")
        return False, None

    response = send_command(ser, f"KEY:{hex_str}")
    if response and "OK:KEY_SAVED" in response:
        print(f"  ✓ Key saved ({len(raw_bytes)} bytes)")
        return True, raw_bytes
    else:
        print(f"  ✗ Key save failed: {response}")
        return False, None


def set_label(ser, label):
    """Set the service label on the LCD."""
    label = label[:16]  # Truncate to LCD width
    response = send_command(ser, f"LABEL:{label}")
    if response and "OK:LABEL_SET" in response:
        print(f"  ✓ Label set: {label}")
        return True
    else:
        print(f"  ✗ Label set failed: {response}")
        return False


def verify_code(raw_bytes):
    """Generate a TOTP code locally for verification."""
    now = int(time.time())
    remaining = 30 - (now % 30)

    if HAS_PYOTP:
        b32 = base64.b32encode(raw_bytes).decode("ascii")
        totp = pyotp.TOTP(b32)
        code = totp.now()
    else:
        code = f"{generate_totp_local(raw_bytes, now):06d}"

    print(f"\n  ┌─────────────────────────┐")
    print(f"  │  Current TOTP: {code}    │")
    print(f"  │  Valid for: {remaining:2d}s          │")
    print(f"  └─────────────────────────┘")
    print(f"  Compare this with your Arduino display.")


# ─── Interactive Mode ────────────────────────────────────────

def interactive_mode(port=None):
    """Run the interactive setup wizard."""
    print()
    print("  ╔═══════════════════════════════════════╗")
    print("  ║   TOTP Authenticator Setup Wizard     ║")
    print("  ║   Arduino Uno + 16×2 LCD              ║")
    print("  ╚═══════════════════════════════════════╝")
    print()

    # Find port
    if not port:
        port = find_arduino_port()
    if not port:
        print("  ✗ No serial port found. Is the Arduino connected?")
        sys.exit(1)

    print(f"  → Connecting to {port}...")

    try:
        ser = open_serial(port)
    except serial.SerialException as e:
        print(f"  ✗ Cannot open {port}: {e}")
        sys.exit(1)

    # Ping
    if ping_device(ser):
        print("  ✓ Arduino TOTP Authenticator detected!")
    else:
        print("  ⚠ Device did not respond to PING (may still work)")

    print()

    # Main menu loop
    while True:
        print("  ┌───────────────────────────────────┐")
        print("  │  [1] Set TOTP secret key           │")
        print("  │  [2] Sync time                     │")
        print("  │  [3] Set service label              │")
        print("  │  [4] Full setup (key + time + label)│")
        print("  │  [5] Verify current code            │")
        print("  │  [6] Device status                  │")
        print("  │  [7] Re-sync time only              │")
        print("  │  [Q] Quit                           │")
        print("  └───────────────────────────────────┘")
        print()
        choice = input("  Select option: ").strip().upper()
        print()

        if choice == "1":
            b32 = input("  Enter Base32 secret key: ").strip()
            set_key(ser, b32)

        elif choice == "2":
            sync_time(ser)

        elif choice == "3":
            label = input("  Enter service name (max 16 chars): ").strip()
            set_label(ser, label)

        elif choice == "4":
            b32 = input("  Enter Base32 secret key: ").strip()
            success, raw = set_key(ser, b32)
            if success:
                label = input("  Enter service name (e.g. GitHub, Google): ").strip()
                if label:
                    set_label(ser, label)
                sync_time(ser)
                if raw:
                    verify_code(raw)
                print("\n  ✓ Setup complete! Your Arduino is generating TOTP codes.")

        elif choice == "5":
            b32 = input("  Enter Base32 secret key (to verify locally): ").strip()
            try:
                _, raw = base32_to_hex(b32)
                verify_code(raw)
            except Exception as e:
                print(f"  ✗ Invalid key: {e}")

        elif choice == "6":
            resp = send_command(ser, "STATUS", expect_prefix="KEY")
            print(f"  Device status: {resp}")

        elif choice == "7":
            sync_time(ser)

        elif choice in ("Q", "QUIT", "EXIT"):
            break

        print()

    ser.close()
    print("  Goodbye!\n")


# ─── CLI Mode ────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Configure Arduino TOTP Authenticator"
    )
    parser.add_argument("--port", "-p", help="Serial port (e.g. COM3, /dev/ttyUSB0)")
    parser.add_argument("--key", "-k", help="Base32 TOTP secret key")
    parser.add_argument("--label", "-l", help="Service label for LCD (max 16 chars)")
    parser.add_argument("--sync-only", action="store_true",
                        help="Only sync time, don't set key")
    parser.add_argument("--verify", action="store_true",
                        help="Generate local TOTP code for verification")

    args = parser.parse_args()

    # If no key/sync args, run interactive mode
    if not args.key and not args.sync_only and not args.verify:
        interactive_mode(args.port)
        return

    # CLI mode
    port = args.port or find_arduino_port()
    if not port:
        print("ERROR: No serial port found. Use --port to specify.")
        sys.exit(1)

    ser = open_serial(port)

    if not ping_device(ser):
        print("WARNING: Device did not respond to PING")

    raw_bytes = None

    if args.key:
        success, raw_bytes = set_key(ser, args.key)
        if not success:
            sys.exit(1)

    if args.label:
        set_label(ser, args.label)

    # Always sync time when setting key
    if args.key or args.sync_only:
        sync_time(ser)

    if args.verify and args.key:
        _, raw = base32_to_hex(args.key)
        verify_code(raw)

    ser.close()


if __name__ == "__main__":
    main()
