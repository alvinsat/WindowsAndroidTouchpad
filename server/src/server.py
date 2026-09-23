# Python Ultra-Responsive UDP & USB Touchpad Server
# Requirement: pip install pynput
import socket
import threading
import struct
from pynput.mouse import Controller, Button

mouse = Controller()

PORT = 8080
DISCOVERY_MSG = "TOUCHPAD_DISCOVER"
OFFER_MSG = "TOUCHPAD_OFFER"

def handle_packet(data):
    if not data:
        return
    op = data[0]
    # 5-byte binary move: [0x01, dx (int16), dy (int16)]
    if op == 0x01 and len(data) >= 5:
        dx, dy = struct.unpack('>hh', data[1:5])
        mouse.move(dx, dy)
    # Binary click: [0x02, click_code]
    elif op == 0x02 and len(data) >= 2:
        code = data[1]
        if code == 1:   mouse.click(Button.left, 1)
        elif code == 2: mouse.click(Button.right, 1)
        elif code == 3: mouse.click(Button.left, 2)
        elif code == 4: mouse.press(Button.left)      # Drag start
        elif code == 5: mouse.release(Button.left)    # Drag end
        elif code == 6: mouse.click(Button.middle, 1) # Middle mouse
    # Binary vertical scroll: [0x03, dy (int16)]
    elif op == 0x03 and len(data) >= 3:
        dy, = struct.unpack('>h', data[1:3])
        mouse.scroll(0, dy)
    # Binary horizontal scroll: [0x04, dx (int16)]
    elif op == 0x04 and len(data) >= 3:
        dx, = struct.unpack('>h', data[1:3])
        mouse.scroll(dx, 0)
    else:
        # String format (newline separated for TCP/UDP)
        msg = data.decode('utf-8', errors='ignore').strip()
        for line in msg.split('\n'):
            line = line.strip()
            if not line:
                continue
            parts = line.split(',')
            t = parts[0]
            if t == "ping":
                continue  # Handshake keepalive ping from app
            elif t == "move" and len(parts) >= 3:
                mouse.move(int(parts[1]), int(parts[2]))
            elif t == "click" and len(parts) >= 2:
                act = parts[1]
                if act == "left":     mouse.click(Button.left, 1)
                elif act == "right":  mouse.click(Button.right, 1)
                elif act == "double": mouse.click(Button.left, 2)
                elif act == "down":   mouse.press(Button.left)
                elif act == "up":     mouse.release(Button.left)
                elif act == "middle": mouse.click(Button.middle, 1)
            elif t == "scroll" and len(parts) >= 2:
                mouse.scroll(0, int(parts[1]))
            elif t == "hscroll" and len(parts) >= 2:
                mouse.scroll(int(parts[1]), 0)

# Wi-Fi Auto-Discovery responder on port 8080
def start_discovery_responder():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('0.0.0.0', PORT))
    print(f"[Wi-Fi Sync] Server discovery active on UDP port {PORT}...")
    while True:
        try:
            data, addr = sock.recvfrom(1024)
            if data.decode('utf-8', errors='ignore').strip() == DISCOVERY_MSG:
                sock.sendto(OFFER_MSG.encode('utf-8'), addr)
                print(f"[Wi-Fi Sync] Paired with Android at {addr[0]}")
        except Exception:
            pass

# Wi-Fi & USB Tethering UDP Input on port 8081
def start_udp_input():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('0.0.0.0', PORT + 1))
    print(f"[UDP Engine] Ready for Wi-Fi / Tethering input on port {PORT + 1}...")
    while True:
        try:
            data, _ = sock.recvfrom(1024)
            handle_packet(data)
        except (KeyboardInterrupt, SystemExit):
            raise
        except Exception:
            pass

# USB Cable (Wired ADB reverse) TCP Input on port 8081
def start_tcp_input():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    sock.bind(('0.0.0.0', PORT + 1))
    sock.listen(5)
    print(f"[USB Engine] Ready for wired USB input (adb reverse) on port {PORT + 1}...")
    while True:
        try:
            client, addr = sock.accept()
            client.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            print(f"[USB Connected] Priority 1 Cable mode active from {addr[0]}!")
            threading.Thread(target=handle_tcp_client, args=(client,), daemon=True).start()
        except Exception:
            pass

def handle_tcp_client(client):
    while True:
        try:
            data = client.recv(1024)
            if not data:
                break
            handle_packet(data)
        except Exception:
            break
    client.close()
    print("[USB Disconnected] Cable mode disconnected, falling back to Wi-Fi / Tethering.")

if __name__ == "__main__":
    threading.Thread(target=start_discovery_responder, daemon=True).start()
    threading.Thread(target=start_tcp_input, daemon=True).start()
    try:
        start_udp_input()
    except KeyboardInterrupt:
        print("\n[Server] Touchpad server shut down cleanly.")