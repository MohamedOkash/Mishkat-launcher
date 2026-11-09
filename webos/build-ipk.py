"""
Mishkat Launcher - LG webOS IPK Builder
Builds a standard compliant webOS .ipk package without requiring webOS CLI SDK.
"""

import os
import io
import tarfile
import struct

def pad60(s):
    return s.ljust(16) + '0'.ljust(12) + '0'.ljust(6) + '0'.ljust(6) + '100644'.ljust(8)

def create_ar_header(filename, size):
    # ar header is 60 bytes: name(16), timestamp(12), owner(6), group(6), mode(8), size(10), magic(2)
    name = (filename + '/').ljust(16)[:16]
    timestamp = str(int(1700000000)).ljust(12)
    owner = '0'.ljust(6)
    group = '0'.ljust(6)
    mode = '100644'.ljust(8)
    sz = str(size).ljust(10)
    magic = '`\n'
    return (name + timestamp + owner + group + mode + sz + magic).encode('ascii')

def build_ipk(stage_dir, output_ipk):
    # 1. debian-binary
    debian_binary = b"2.0\n"

    # 2. control.tar.gz
    control_content = (
        "Package: com.mishkat.launcher\n"
        "Version: 1.6.0\n"
        "Section: misc\n"
        "Priority: optional\n"
        "Architecture: all\n"
        "Maintainer: Mohamed Okash <mohamed.okash1998@gmail.com>\n"
        "Description: Mishkat Smart TV Launcher for LG webOS\n"
    ).encode('utf-8')

    control_tar_buf = io.BytesIO()
    with tarfile.open(fileobj=control_tar_buf, mode="w:gz") as tar:
        ti = tarfile.TarInfo(name="./control")
        ti.size = len(control_content)
        ti.mode = 0o644
        tar.addfile(ti, io.BytesIO(control_content))
    control_tar_bytes = control_tar_buf.getvalue()

    # 3. data.tar.gz
    data_tar_buf = io.BytesIO()
    with tarfile.open(fileobj=data_tar_buf, mode="w:gz") as tar:
        base_install_dir = "usr/palm/applications/com.mishkat.launcher"
        for root, dirs, files in os.walk(stage_dir):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, stage_dir).replace("\\", "/")
                archive_name = f"./{base_install_dir}/{rel_path}"
                tar.add(full_path, arcname=archive_name)
    data_tar_bytes = data_tar_buf.getvalue()

    # 4. Assemble ar archive (.ipk)
    with open(output_ipk, 'wb') as f:
        # AR Magic
        f.write(b"!<arch>\n")

        # Member 1: debian-binary
        f.write(create_ar_header("debian-binary", len(debian_binary)))
        f.write(debian_binary)
        if len(debian_binary) % 2 != 0:
            f.write(b"\n")

        # Member 2: control.tar.gz
        f.write(create_ar_header("control.tar.gz", len(control_tar_bytes)))
        f.write(control_tar_bytes)
        if len(control_tar_bytes) % 2 != 0:
            f.write(b"\n")

        # Member 3: data.tar.gz
        f.write(create_ar_header("data.tar.gz", len(data_tar_bytes)))
        f.write(data_tar_bytes)
        if len(data_tar_bytes) % 2 != 0:
            f.write(b"\n")

    print(f"Created LG webOS IPK: {output_ipk} ({os.path.getsize(output_ipk)} bytes)")

if __name__ == "__main__":
    import sys
    stage = sys.argv[1] if len(sys.argv) > 1 else "tv-web"
    out = sys.argv[2] if len(sys.argv) > 2 else "dist/com.mishkat.launcher_1.6.0_all.ipk"
    build_ipk(stage, out)
