"""
JARVIS PC COMPANION SERVER (FastAPI + Windows Automation)
=========================================================
Run this Python script on your Windows PC to allow Jarvis Android Assistant
to securely execute authorized commands (Lock, Shutdown, Media, Apps, Scripts).

Setup Instructions:
-------------------
1. Install Python 3.9+ on Windows: https://www.python.org/
2. Install dependencies:
   pip install fastapi uvicorn pydantic
3. Run the server:
   python jarvis_pc_server.py
4. In the Jarvis Android app -> "PC Control" tab, enter:
   - Host: Your PC Local IPv4 address (e.g., 192.168.1.5)
   - Port: 8000
   - Auth Token: jarvis_secret_key (or customize below)
"""

import os
import sys
import platform
import subprocess
import webbrowser
import socket
from typing import Optional
from pydantic import BaseModel
from fastapi import FastAPI, Header, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

# Configurable Shared Secret Token (Must match token entered in Android App)
AUTH_SECRET_TOKEN = os.environ.get("JARVIS_AUTH_TOKEN", "jarvis_secret_key")

app = FastAPI(
    title="Jarvis Windows PC Companion",
    description="Authorized bridge for Jarvis Mobile Voice Assistant",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class PcActionRequest(BaseModel):
    action: str  # lock, shutdown, media, volume, open_url, open_app, script
    payload: Optional[str] = None

def verify_token(authorization: Optional[str] = Header(None)):
    if not authorization:
        raise HTTPException(status_code=401, detail="Missing Authorization Header")
    scheme, _, token = authorization.partition(" ")
    if scheme.lower() != "bearer" or token != AUTH_SECRET_TOKEN:
        raise HTTPException(status_code=403, detail="Invalid Jarvis Authentication Token")
    return token

def send_windows_key(vk_code: int):
    """Sends a virtual key event on Windows using ctypes (zero external dependencies)."""
    if platform.system() == "Windows":
        import ctypes
        user32 = ctypes.windll.user32
        KEYEVENTF_KEYUP = 0x0002
        user32.keybd_event(vk_code, 0, 0, 0)
        user32.keybd_event(vk_code, 0, KEYEVENTF_KEYUP, 0)

@app.get("/api/ping")
def ping(token: str = Depends(verify_token)):
    return {
        "status": "online",
        "machine_name": socket.gethostname(),
        "platform": platform.platform(),
        "message": f"Connected to {socket.gethostname()}"
    }

@app.post("/api/action")
def execute_action(req: PcActionRequest, token: str = Depends(verify_token)):
    action = req.action.lower()
    payload = req.payload or ""

    # 1. Lock Workstation
    if action in ["lock", "lock_pc"]:
        if platform.system() == "Windows":
            import ctypes
            ctypes.windll.user32.LockWorkStation()
            return {"status": "success", "message": "Windows PC workstation locked."}
        else:
            return {"status": "simulated", "message": "Lock workstation simulated on non-Windows host."}

    # 2. Media Controls
    # VK_MEDIA_PLAY_PAUSE = 0xB3, VK_MEDIA_NEXT_TRACK = 0xB0, VK_MEDIA_PREV_TRACK = 0xB1
    elif action in ["media_play_pause", "media"]:
        send_windows_key(0xB3)
        return {"status": "success", "message": "Toggled PC media play/pause."}

    elif action in ["media_next", "next"]:
        send_windows_key(0xB0)
        return {"status": "success", "message": "PC media skipped to next track."}

    elif action in ["media_prev", "prev"]:
        send_windows_key(0xB1)
        return {"status": "success", "message": "PC media returned to previous track."}

    # 3. Volume Controls
    # VK_VOLUME_UP = 0xAF, VK_VOLUME_DOWN = 0xAE, VK_VOLUME_MUTE = 0xAD
    elif action in ["volume_up", "vol_up"]:
        for _ in range(3):
            send_windows_key(0xAF)
        return {"status": "success", "message": "PC volume increased."}

    elif action in ["volume_down", "vol_down"]:
        for _ in range(3):
            send_windows_key(0xAE)
        return {"status": "success", "message": "PC volume decreased."}

    elif action in ["mute"]:
        send_windows_key(0xAD)
        return {"status": "success", "message": "PC volume muted."}

    # 4. Open URL in Default Browser
    elif action in ["open_url"]:
        url = payload if payload.startswith("http") else f"https://{payload}"
        webbrowser.open(url)
        return {"status": "success", "message": f"Opened {url} on PC."}

    # 5. Launch Authorized Application
    elif action in ["open_app"]:
        app_name = payload.lower().strip()
        app_map = {
            "notepad": "notepad.exe",
            "calculator": "calc.exe",
            "calc": "calc.exe",
            "cmd": "cmd.exe",
            "terminal": "wt.exe",
            "chrome": "chrome.exe",
            "explorer": "explorer.exe",
            "vscode": "code",
            "spotify": "spotify.exe"
        }
        cmd = app_map.get(app_name, app_name)
        try:
            subprocess.Popen(cmd, shell=True)
            return {"status": "success", "message": f"Launched {app_name} on PC."}
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to launch app: {str(e)}")

    # 6. Shutdown Workstation (Consequential action)
    elif action in ["shutdown", "shutdown_pc"]:
        if platform.system() == "Windows":
            # 30 second abortable shutdown timer
            subprocess.run(["shutdown", "/s", "/t", "30"], check=True)
            return {"status": "success", "message": "PC shutdown initiated (30-second abort window)."}
        else:
            return {"status": "simulated", "message": "PC shutdown simulated."}

    else:
        raise HTTPException(status_code=400, detail=f"Unsupported PC action: {action}")

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

if __name__ == "__main__":
    local_ip = get_local_ip()
    port = 8000
    print("=" * 65)
    print("       JARVIS WINDOWS PC COMPANION SERVER")
    print("=" * 65)
    print(f"Server IP:    {local_ip}")
    print(f"Port:         {port}")
    print(f"Auth Token:   {AUTH_SECRET_TOKEN}")
    print(f"URL:          http://{local_ip}:{port}")
    print("\nConfigure this IP and token in the Jarvis Mobile app under 'PC Control'.")
    print("Press CTRL+C to stop the server.")
    print("=" * 65)
    uvicorn.run(app, host="0.0.0.0", port=port)
