# Python Ultra-Responsive UDP & USB Touchpad Server
# Native Win32 hardware injection: fixes dragging on Windows Notifications,
# Unity Editor dock tabs & sliders, Unreal Engine Slate & viewports, and 3rd-party windows.
import sys
import os
import socket
import threading
import struct

PORT = 8080
DISCOVERY_MSG = "TOUCHPAD_DISCOVER"
OFFER_MSG = "TOUCHPAD_OFFER"

IS_WINDOWS = (os.name == 'nt')

if IS_WINDOWS:
    import ctypes
    user32 = ctypes.windll.user32

    # Enable Per-Monitor DPI awareness so coordinates and deltas are never scaled
    try:
        ctypes.windll.shcore.SetProcessDpiAwareness(2)
    except Exception:
        try:
            user32.SetProcessDPIAware()
        except Exception:
            pass

    # Win32 hardware mouse_event flags
    MOUSEEVENTF_MOVE = 0x0001
    MOUSEEVENTF_LEFTDOWN = 0x0002
    MOUSEEVENTF_LEFTUP = 0x0004
    MOUSEEVENTF_RIGHTDOWN = 0x0008
    MOUSEEVENTF_RIGHTUP = 0x0010
    MOUSEEVENTF_MIDDLEDOWN = 0x0020
    MOUSEEVENTF_MIDDLEUP = 0x0040
    MOUSEEVENTF_WHEEL = 0x0800
    MOUSEEVENTF_HWHEEL = 0x01000

    VK_MENU = 0x12       # Alt
    VK_LEFT = 0x25       # Left Arrow
    VK_RIGHT = 0x27      # Right Arrow
    KEYEVENTF_KEYUP = 0x0002

    def mouse_move(dx, dy):
        # MOUSEEVENTF_MOVE injects hardware packets recognized by Unity, Unreal Engine,
        # and Windows DirectManipulation (Action Center & Notification banners)
        user32.mouse_event(MOUSEEVENTF_MOVE, int(dx), int(dy), 0, 0)

    def mouse_click(btn):
        if btn == "left":
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
        elif btn == "right":
            user32.mouse_event(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
        elif btn == "double":
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
        elif btn == "middle":
            user32.mouse_event(MOUSEEVENTF_MIDDLEDOWN, 0, 0, 0, 0)
            user32.mouse_event(MOUSEEVENTF_MIDDLEUP, 0, 0, 0, 0)
        elif btn == "down":
            user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
        elif btn == "up":
            user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)

    def mouse_scroll_v(dy):
        user32.mouse_event(MOUSEEVENTF_WHEEL, 0, 0, int(dy * 120), 0)

    def mouse_scroll_h(dx):
        user32.mouse_event(MOUSEEVENTF_HWHEEL, 0, 0, int(dx * 120), 0)

    def nav_back():
        user32.keybd_event(VK_MENU, 0, 0, 0)
        user32.keybd_event(VK_LEFT, 0, 0, 0)
        user32.keybd_event(VK_LEFT, 0, KEYEVENTF_KEYUP, 0)
        user32.keybd_event(VK_MENU, 0, KEYEVENTF_KEYUP, 0)
        print("[Event] 4-Finger Navigation: Backward (Alt+Left)")

    def nav_forward():
        user32.keybd_event(VK_MENU, 0, 0, 0)
        user32.keybd_event(VK_RIGHT, 0, 0, 0)
        user32.keybd_event(VK_RIGHT, 0, KEYEVENTF_KEYUP, 0)
        user32.keybd_event(VK_MENU, 0, KEYEVENTF_KEYUP, 0)
        print("[Event] 4-Finger Navigation: Forward (Alt+Right)")

else:
    # Cross-platform fallback for Linux / macOS using pynput
    try:
        from pynput.mouse import Controller as MouseController, Button
        from pynput.keyboard import Controller as KeyboardController, Key
        pynput_mouse = MouseController()
        pynput_keyboard = KeyboardController()

        def mouse_move(dx, dy):
            pynput_mouse.move(dx, dy)

        def mouse_click(btn):
            if btn == "left": pynput_mouse.click(Button.left, 1)
            elif btn == "right": pynput_mouse.click(Button.right, 1)
            elif btn == "double": pynput_mouse.click(Button.left, 2)
            elif btn == "middle": pynput_mouse.click(Button.middle, 1)
            elif btn == "down": pynput_mouse.press(Button.left)
            elif btn == "up": pynput_mouse.release(Button.left)

        def mouse_scroll_v(dy):
            pynput_mouse.scroll(0, dy)

        def mouse_scroll_h(dx):
            pynput_mouse.scroll(dx, 0)

        def nav_back():
            with pynput_keyboard.pressed(Key.alt):
                pynput_keyboard.press(Key.left)
                pynput_keyboard.release(Key.left)
            print("[Event] 4-Finger Navigation: Backward (Alt+Left)")

        def nav_forward():
            with pynput_keyboard.pressed(Key.alt):
                pynput_keyboard.press(Key.right)
                pynput_keyboard.release(Key.right)
            print("[Event] 4-Finger Navigation: Forward (Alt+Right)")
    except ImportError:
        print("[Warning] Install pynput with: pip install pynput (for non-Windows OS)")

def handle_packet(data):
    if not data:
        return
    op = data[0]
    if op == 0x01 and len(data) >= 5:
        dx, dy = struct.unpack('>hh', data[1:5])
        mouse_move(dx, dy)
    elif op == 0x02 and len(data) >= 2:
        code = data[1]
        if code == 1: mouse_click("left")
        elif code == 2: mouse_click("right")
        elif code == 3: mouse_click("double")
        elif code == 4: mouse_click("down")
        elif code == 5: mouse_click("up")
        elif code == 6: mouse_click("middle")
    elif op == 0x03 and len(data) >= 3:
        dy, = struct.unpack('>h', data[1:3])
        mouse_scroll_v(dy)
    elif op == 0x04 and len(data) >= 3:
        dx, = struct.unpack('>h', data[1:3])
        mouse_scroll_h(dx)
    elif op == 0x05 and len(data) >= 2:
        code = data[1]
        if code == 1: nav_back()
        elif code == 2: nav_forward()
    else:
        # String format (newline separated for TCP/UDP)
        msg = data.decode('utf-8', errors='ignore').strip()
        for line in msg.split('\n'):
            line = line.strip()
            if not line: continue
            parts = line.split(',')
            t = parts[0]
            if t == "ping":
                continue
            elif t == "move" and len(parts) >= 3:
                mouse_move(int(parts[1]), int(parts[2]))
            elif t == "click" and len(parts) >= 2:
                mouse_click(parts[1])
            elif t == "scroll" and len(parts) >= 2:
                mouse_scroll_v(int(parts[1]))
            elif t == "hscroll" and len(parts) >= 2:
                mouse_scroll_h(int(parts[1]))
            elif t == "nav" and len(parts) >= 2:
                act = parts[1]
                if act in ("back", "backward"):
                    nav_back()
                elif act == "forward":
                    nav_forward()

# Wi-Fi Auto-Discovery responder on port 8080
last_connected_ip = None

def start_discovery_responder():
    global last_connected_ip
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('0.0.0.0', PORT))
    while True:
        try:
            data, addr = sock.recvfrom(1024)
            if data.decode('utf-8', errors='ignore').strip() == DISCOVERY_MSG:
                sock.sendto(OFFER_MSG.encode('utf-8'), addr)
                if last_connected_ip != addr[0]:
                    last_connected_ip = addr[0]
                    print(f"[Event] Device paired via Wi-Fi/Network: {addr[0]}")
        except Exception:
            pass

# Wi-Fi & USB Tethering UDP Input on port 8081
def start_udp_input():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('0.0.0.0', PORT + 1))
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
    while True:
        try:
            client, addr = sock.accept()
            client.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            print("[Event] USB Cable mode connected (127.0.0.1)")
            threading.Thread(target=handle_tcp_client, args=(client,), daemon=True).start()
        except Exception:
            pass

def handle_tcp_client(client):
    while True:
        try:
            data = client.recv(1024)
            if not data: break
            handle_packet(data)
        except Exception:
            break
    client.close()
    print("[Event] USB Cable disconnected")

if __name__ == "__main__":
    print(f"[Event] Touchpad Server ready (UDP/TCP {PORT}-{PORT + 1})")
    threading.Thread(target=start_discovery_responder, daemon=True).start()
    threading.Thread(target=start_tcp_input, daemon=True).start()
    try:
        start_udp_input()
    except KeyboardInterrupt:
        print("\n[Event] Server stopped.")