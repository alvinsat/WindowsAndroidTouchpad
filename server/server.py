# Python Ultra-Responsive UDP Touchpad Server
# Requirement: pip install pynput
import socket
import threading
import struct
from pynput.mouse import Controller, Button

mouse = Controller()

PORT = 8080
DISCOVERY_MSG = "TOUCHPAD_DISCOVER"
OFFER_MSG = "TOUCHPAD_OFFER"

# Auto-Discovery responder on port 8080
def start_discovery_responder():
    disc_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    disc_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    disc_socket.bind(('0.0.0.0', PORT))
    print(f"[Auto-Sync] Server discovery active on UDP port {PORT}...")
    while True:
        try:
            data, addr = disc_socket.recvfrom(1024)
            if data.decode('utf-8', errors='ignore').strip() == DISCOVERY_MSG:
                disc_socket.sendto(OFFER_MSG.encode('utf-8'), addr)
                print(f"[Auto-Sync] Handshake paired with Android at {addr[0]}")
        except Exception:
            pass

# High-frequency gesture receiver on port 8081
# Automatically supports BOTH string messages ("move,dx,dy") AND fast 5-byte binary datagrams!
def start_input_server():
    input_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    input_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    input_socket.bind(('0.0.0.0', PORT + 1))
    print(f"[Input Engine] Ready for high-speed touch input on UDP port {PORT + 1}...")
    
    while True:
        try:
            data, _ = input_socket.recvfrom(1024)
            if not data:
                continue

            # Check if binary packet opcode (0x01 = Move, 0x02 = Click, 0x03 = Scroll)
            op = data[0]
            if op == 0x01 and len(data) >= 5:
                # 5-byte binary move: [0x01, dx (short), dy (short)]
                dx, dy = struct.unpack('>hh', data[1:5])
                mouse.move(dx, dy)
            elif op == 0x02 and len(data) >= 2:
                # Binary click: [0x02, click_code]
                code = data[1]
                if code == 1:
                    mouse.click(Button.left, 1)
                elif code == 2:
                    mouse.click(Button.right, 1)
                elif code == 3:
                    mouse.click(Button.left, 2)
                elif code == 4:
                    mouse.press(Button.left)   # Drag start
                elif code == 5:
                    mouse.release(Button.left) # Drag end
                elif code == 6:
                    mouse.click(Button.middle, 1) # Middle mouse click
            elif op == 0x03 and len(data) >= 3:
                # Binary scroll: [0x03, dy (short)]
                dy, = struct.unpack('>h', data[1:3])
                mouse.scroll(0, dy)
            else:
                # Fallback ASCII string format ("move,dx,dy", "click,action", "scroll,dy")
                message = data.decode('utf-8', errors='ignore').strip()
                parts = message.split(',')
                event_type = parts[0]
                if event_type == "move" and len(parts) >= 3:
                    mouse.move(int(parts[1]), int(parts[2]))
                elif event_type == "click" and len(parts) >= 2:
                    action = parts[1]
                    if action == "left":
                        mouse.click(Button.left, 1)
                    elif action == "right":
                        mouse.click(Button.right, 1)
                    elif action == "double":
                        mouse.click(Button.left, 2)
                    elif action == "down":
                        mouse.press(Button.left)
                    elif action == "up":
                        mouse.release(Button.left)
                    elif action == "middle":
                        mouse.click(Button.middle, 1)
                elif event_type == "scroll" and len(parts) >= 2:
                    mouse.scroll(0, int(parts[1]))
        except Exception:
            pass

if __name__ == "__main__":
    threading.Thread(target=start_discovery_responder, daemon=True).start()
    start_input_server()