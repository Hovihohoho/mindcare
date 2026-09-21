"""Fetch the fixed public SSAQS v1 archive; never execute dataset contents."""
import hashlib
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parent
URL = 'https://zenodo.org/records/18706837/files/SSAQS%20dataset.zip?download=1'
MD5 = '3a95e68847192c062daf80df25cceb83'  # Publisher's integrity checksum, not authentication.


def main():
    archive = ROOT / 'data' / 'ssaqs.zip'
    archive.parent.mkdir(parents=True, exist_ok=True)
    if not archive.exists():
        with urllib.request.urlopen(URL, timeout=120) as source:
            archive.write_bytes(source.read())
    if hashlib.md5(archive.read_bytes()).hexdigest() != MD5:
        raise ValueError('Archive does not match the published SSAQS v1 checksum')
    destination = (archive.parent / 'ssaqs').resolve()
    with zipfile.ZipFile(archive) as dataset:
        for entry in dataset.infolist():
            if not (destination / entry.filename).resolve().is_relative_to(destination):
                raise ValueError('Unsafe archive path')
        dataset.extractall(destination)
    print(f'Verified dataset: {destination}')


if __name__ == '__main__':
    main()
