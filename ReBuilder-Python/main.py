#!/usr/bin/python3
import argparse, logging, stat, subprocess, sys
from argparse import ArgumentError
from pathlib import Path
from tempfile import NamedTemporaryFile

# Setup logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
consoleHandler = logging.StreamHandler(sys.stdout)
consoleHandler.setFormatter(logging.Formatter('%(name)s : %(levelname)s : %(asctime)s : \033[33m%(msg)s\033[0m', datefmt='%H:%M:%S'))
logger.addHandler(consoleHandler)

# Setup args
parser = argparse.ArgumentParser(prog='appbuilder',
                                 description='Build apk from decompiled files',
                                 epilog='%(prog)s was built by sickboy. Visit https://github.com/sickboydroid/app-builder for more info')
parser.add_argument('-b', '--build-dir', required=True, type=Path, metavar='DIR',help='directory where build files will be stored')
parser.add_argument('-c', '--source-dir', type=Path, metavar='DIR', help='directory where your source files are located', default='.')
parser.add_argument('-f', '--additional-file', type=Path, metavar=('SRC', 'DEST'), nargs=2, action='append',help='files to include in apk. (can be used multiple times) e.g: appbuilder ... -f mydata/foo.png assets/foo.png')
parser.add_argument('-i', '--install', action='store_true', help='install app to connected device')
parser.add_argument('-o', '--original-app', type=Path, help='original app whose mod you are making', required=True, metavar='PATH')
parser.add_argument('-e', '--emulator', action='store_true', help='prefer emulator over usb device')
parser.add_argument('-k', '--keystore', type=Path, metavar=('PATH', 'PASS'), nargs=2, help='path and password of keystore for signing apk')
parser.add_argument('--no-clean', action='store_true', help='keep build files even after a successful build')
parser.add_argument('-s', '--smali-dir', type=Path, metavar='DIR', action='append', help='smali directory that is to be complied into dex and added to apk')
parser.add_argument('-w', '--additional-app', type=Path, nargs='+', metavar='PATH', help='install generated app with this/these app(s). Useful for split apks')
# TODO: Add version
parser.add_argument('--version', action='version', version='%(prog)s VERSION', help='show version and exit')
sign_group = parser.add_mutually_exclusive_group()
sign_group.add_argument('--no-sign', action='store_true', help='do not sign original apk. Cannot be used with --install')
sign_group.add_argument('--sign-all', action='store_true',
                        help='sign additional apps as well (built apk is signed always unless --no-sign is specified)')

# Validate args
args = parser.parse_args()

if args.build_dir.samefile(args.source_dir):
    raise ArgumentError(None,'build directory and source directory cannot be same')
if not args.source_dir.exists():
    raise ArgumentError(None, f'src directory "{args.source_dir.absolute()}" could not be found')
if not args.original_app.exists():
    raise ArgumentError(None, f'original app "{args.original_app}" does not exist')
if args.no_sign:
    if args.install:
        raise ArgumentError(None, '--no-sign and --install cannot be used together')
    if args.keystore:
        logger.warning("Provided keystore is useless with --no-sign option")
if not args.no_sign:
    if not args.keystore:
        raise ArgumentError(None, 'No keystore provided. Cannot sign apk')
    if not args.keystore[0].exists():
        raise ArgumentError(None, f'keystore "{args.keystore.absolute()}" does not exists')
if args.sign_all:
    if not args.additional_app:
        logger.warning('--sign-all is useless without --additional-app argument')
    else:
        logger.info(f'signing apps other than original app is in-place. Original apps will be copied to "{str(args.build_dir / ".bak")}"')

# setup builder file
builder = Path(args.build_dir / 'run')
if not builder.exists():
    builder.touch(stat.S_IXUSR | stat.S_IWUSR | stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH)


class Shell:
    def __init__(self, build_dir : Path, original_app : Path, builder : Path,
                 keystore=None, keystore_pass=None):
        self.build_dir = build_dir
        # self.original_app = original_app
        self.builder = builder
        self.unsigned_app = Path(build_dir / (original_app.stem + '-unsigned.apk'))
        self.signed_app = Path(build_dir / (original_app.stem + '-signed.apk'))
        self.unsigned_app.unlink(missing_ok=True)
        self.signed_app.unlink(missing_ok=True)
        self.unsigned_app.write_bytes(original_app.read_bytes())
        self.keystore = keystore
        self.keystore_pass = keystore_pass
        self.commands = []

    def compile_smali(self, smali_dirs : list):
        for dir in smali_dirs:
            if not dir.exists():
                raise FileNotFoundError(f'smali dir "{str(dir)}" does not exist')
            output_dex = Path(self.build_dir / (dir.name + '.dex'))
            self.commands.append(f'echo "smali {dir.name} -> {output_dex.name}"')
            self.commands.append(f'smali a "{dir}" -o "{output_dex}"')
            self.commands.append(f'zip -juq0 {self.unsigned_app} {output_dex}')
    
    def add_additional_files(self, files : list):
        for file in files:
            self.commands.append(f'echo "adding {file}..."')
            self.commands.append(f'zip -juq0 {self.unsigned_app} {file}')

    def execute(self):
        for command in self.commands:
            output = subprocess.run(command, shell=True,  capture_output=True, text=True, check=False)
            if output.stdout:
                print(output.stdout, end='')
            if output.stderr:
                print(output.stderr, end='')
            if output.returncode != 0:
                logger.error(f'command "{command}" returned a non-zero error code')
                sys.exit(-1)
        self.commands.clear()
    
    def clean(self):
        for file in self.build_dir.iterdir():
            delete(file)
        def delete(path : Path):
            if path.is_file():
                path.unlink()
            for foo in path.iterdir():
                delete(foo)
            path.rmdir()
        delete(self.build_dir)
        logger.info('cleaned build directory')
    
    def sign_app(self, src : Path, dest : Path):
        with NamedTemporaryFile() as alignedApk:
            self.commands.append(f'echo "zipalign {src.stem}..."')
            self.commands.append(f'zipalign -pf 4 "{src}" "{alignedApk.name}"')
            self.commands.append(f'echo "signing {src.stem}..."')
            self.commands.append(f'echo {self.keystore_pass} | apksigner sign -ks {self.keystore} --out {dest} {alignedApk.name}')

    def install_apps(self, preferemu : bool, apps : list):
        target = '-e' if preferemu else '-d'
        self.commands.append(f'adb {target} install-multiple -r {" ".join(str(app) for app in apps)}')

    def sign_apps(self, srcs : list):
        for src in srcs:
            with NamedTemporaryFile() as tempDest:
                self.sign_app(src, tempDest.name)
        
# build
shell = Shell(args.build_dir, args.original_app, builder, args.keystore[0], args.keystore[1])
if args.smali_dir:
    shell.compile_smali(args.smali_dir)
if args.additional_file:
    shell.add_additional_files(args.additional_file)
if not args.no_sign:
    shell.sign_app(args.original_app, shell.signed_app)
    if args.sign_all:
        shell.sign_apps(args.additional_app)

if args.install:
    shell.install_apps(args.emulator, args.additional_app + [shell.signed_app])

if not args.no_clean:
    shell.clean()
shell.execute()