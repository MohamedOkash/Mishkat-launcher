import socket
import struct
import re

TV_IP = "192.168.1.8"
SDB_PORT = 26101

def make_packet(cmd: str, a0: int, a1: int, data: bytes = b"") -> bytes:
    ci = struct.unpack("<I", cmd.encode("ascii"))[0]
    return struct.pack("<IIIIII", ci, a0, a1, len(data), sum(data) & 0xFFFFFFFF, ci ^ 0xFFFFFFFF) + data

def read_packet(s: socket.socket):
    hdr = s.recv(24)
    if len(hdr) < 24:
        return None, 0, 0, b""
    cmd, a0, a1, dlen, crc, magic = struct.unpack("<IIIIII", hdr)
    cmd_str = struct.pack("<I", cmd).decode("ascii", errors="ignore")
    data = b""
    while len(data) < dlen:
        chunk = s.recv(dlen - len(data))
        if not chunk:
            break
        data += chunk
    return cmd_str, a0, a1, data

def run_sdb_command(cmd_str: str, timeout: float = 4.0) -> str:
    s = socket.socket()
    s.settimeout(timeout)
    s.connect((TV_IP, SDB_PORT))
    
    # Handshake
    s.sendall(make_packet("CNXN", 0x01000000, 4096, b"host::\x00"))
    read_packet(s)
    
    payload = f"shell:{cmd_str}\x00".encode("ascii")
    s.sendall(make_packet("OPEN", 1, 0, payload))
    cmd, r_a0, r_a1, r_data = read_packet(s)
    
    out = []
    if cmd == "OKAY":
        remote_id = r_a0
        while True:
            try:
                c, a0, a1, d = read_packet(s)
                if not c or c == "CLSE":
                    break
                if c == "WRTE":
                    out.append(d.decode("utf-8", errors="ignore"))
                    s.sendall(make_packet("OKAY", 1, remote_id))
            except socket.timeout:
                break
    s.close()
    return "".join(out)

if __name__ == "__main__":
    raw = run_sdb_command("0 vd_applist")
    blocks = raw.split("---------------------------------------------------------------------------------------------")
    print(f"{'AppID':<15} | {'Size':<8} | {'Removable':<10} | {'Title':<25} | {'TizenID'}")
    print("-" * 85)
    for b in blocks:
        aid = re.search(r"app_id\s*=(\d+)", b)
        title = re.search(r"app_title\s*=([^\r\n-]+)", b)
        tid = re.search(r"app_tizen_id\s*=([^\r\n-]+)", b)
        rem = re.search(r"is_removable\s*=(\d+)", b)
        size = re.search(r"app_size\s*=(\d+)", b)
        if aid and title:
            sz = f"{round(int(size.group(1))/(1024*1024), 1)} MB" if size and size.group(1).isdigit() else "0 MB"
            r = "YES" if rem and rem.group(1) == "1" else "NO"
            t = title.group(1).strip()
            ti = tid.group(1).strip() if tid else ""
            print(f"{aid.group(1):<15} | {sz:<8} | {r:<10} | {t:<25} | {ti}")
