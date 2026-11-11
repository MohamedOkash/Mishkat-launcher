import sys
import os
import zipfile

def pack_wgt(stage_dir, wgt_path):
    print(f"Packing {stage_dir} -> {wgt_path}")
    os.makedirs(os.path.dirname(os.path.abspath(wgt_path)), exist_ok=True)
    if os.path.exists(wgt_path):
        os.remove(wgt_path)
        
    file_count = 0
    with zipfile.ZipFile(wgt_path, 'w', zipfile.ZIP_DEFLATED) as z:
        for root, dirs, files in os.walk(stage_dir):
            for f in files:
                full_path = os.path.join(root, f)
                rel_path = os.path.relpath(full_path, stage_dir).replace('\\', '/')
                z.write(full_path, rel_path)
                file_count += 1
                
    size = os.path.getsize(wgt_path)
    print(f"Packed {file_count} files into {wgt_path} ({size:,} bytes)")

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: python pack_wgt.py <stage_dir> <wgt_path>")
        sys.exit(1)
    pack_wgt(sys.argv[1], sys.argv[2])
