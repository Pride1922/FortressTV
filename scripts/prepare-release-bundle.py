"""Prepare local release files; never upload or change a live update feed."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
from urllib.parse import urlparse


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--aapt', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    gradle = (root / 'app/build.gradle.kts').read_text()
    version = re.search(r'versionName\s*=\s*"([^"]+)"', gradle).group(1)
    code = int(re.search(r'versionCode\s*=\s*(\d+)', gradle).group(1))
    default_download_url = f'https://github.com/Pride1922/FortressTV/releases/download/v{version}/fortresstv.apk'
    download_url = os.environ.get('APK_DOWNLOAD_URL', '').strip() or default_download_url
    parsed = urlparse(download_url)
    if parsed.scheme != 'https' or not parsed.netloc or parsed.username or parsed.password:
        raise SystemExit('The APK download URL must be a valid HTTPS URL (no embedded credentials).')
    notes = os.environ.get('RELEASE_NOTES', '').strip()
    if not notes:
        raise SystemExit('Release notes are required.')
    if not args.apk.is_file() or args.apk.stat().st_size == 0:
        raise SystemExit('The signed APK is missing or empty.')
    badging = subprocess.run([str(args.aapt), 'dump', 'badging', str(args.apk)],
                             check=True, capture_output=True, text=True).stdout
    package = re.search(r"^package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging, re.MULTILINE)
    if not package or package.groups() != ('com.fortresstv.player', str(code), version):
        raise SystemExit('APK package or version does not match the committed FortressTV configuration.')
    source = root / 'third_party/ffmpeg'
    source_files = [source / name for name in (
        'ffmpeg-6.0-source.tar.gz', 'COPYING.LGPLv2.1', 'build-config.txt', 'README.md'
    )] + [root / 'THIRD_PARTY_NOTICES.md']
    for path in source_files:
        if not path.is_file():
            raise SystemExit('Required corresponding-source file is missing: ' + path.name)
    if args.output.exists():
        raise SystemExit('Output directory already exists; choose a fresh staging directory.')
    args.output.mkdir(parents=True)
    apk = args.output / 'fortresstv.apk'
    shutil.copyfile(args.apk, apk)
    checksum = hashlib.sha256(apk.read_bytes()).hexdigest()
    metadata = dict(version=version, versionCode=code, notes=notes,
                    downloadUrl=download_url, sha256=checksum)
    (args.output / 'version.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (args.output / 'source').mkdir()
    for path in source_files:
        shutil.copyfile(path, args.output / 'source' / path.name)
    print(f'Prepared FortressTV {version} (versionCode {code}); SHA-256: {checksum}')
    print('Staged files only. Verify the public APK checksum before publishing version.json.')


if __name__ == '__main__':
    main()
