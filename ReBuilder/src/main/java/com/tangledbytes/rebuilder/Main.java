package com.tangledbytes.rebuilder;

import picocli.CommandLine;
import picocli.CommandLine.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
	@Option(names = {"-s", "--smali-dir"}, paramLabel = "DIR", description = "dir which is to be compiled. It can be called multiple times")
	File[] smaliDirs;

	@Option(names= {"-f", "--addition-file"}, paramLabel = "FILE", description = "add/update the file in app. It can be used multiple times")
	File[] additionalFiles;

	@Option(names= {"-o", "--original-app"}, required = true, paramLabel = "APP", description = "original app whose mod you are making")
	File originalApp;

	@Option(names={"-b", "--build-dir"}, paramLabel = "DIR", description = "build directory")
	File buildDir = new File("./build");

	@Option(names = {"-c", "--source-dir"}, paramLabel = "DIR", description = "directory where your sources are present. Default is current dir")
	File srcDir = new File(".");

	@Option(names={"-n", "--app-name"}, paramLabel = "NAME", description = "this name will be used for generated apps")
	String appName;

	@Option(names={"-k", "--keystore-path"}, paramLabel = "PATH", description = "path to keystore to be used for signing app(s)")
	File keystore;
	@Option(names={"-p", "--keystore-password"}, paramLabel = "PASSWORD", description = "password of provided keystore")
	String keystorePassword;

	@Option(names = {"-h", "--help"}, usageHelp = true, description = "show help")
	boolean helpRequested;

	@Option(names = {"--no-sign"}, description = "do not sign compiled app")
	boolean doNotSignApk;

	@Option(names = {"--no-clean"}, description = "keep build files")
	boolean keepBuildFiles;

	@Option(names = {"-i", "--install"}, description = "install apk after building")
	boolean installApp;

	@Option(names = {"--sign-all"}, description = "sign original as well as apps provided with --install-with option")
	boolean signAllApps;

	@Option(names = {"-w","--install-with"}, paramLabel = "APP", description = {
			"install the compiled app with this app.",
			"Useful when the app is split-apk",
			"It can be used multiple times"
	})
	File[] splitApps;

	@Option(names = {"--install-on-emulator"}, description = "Install app on emulator instead of connected device")
	boolean installOnEmulator;

	// File where the final command is written
	final File runFile = new File(buildDir,"run");

	public static void main(String[] args) throws IOException, InterruptedException {
		Main main = CommandLine.populateCommand(new Main(), args);
		if(main.helpRequested) {
			CommandLine.usage(main, System.out);
			return;
		}
		main.validateOptions();
		main.setupRunFile();
		main.build();
	}

	private void validateOptions() {
		if(buildDir.getAbsoluteFile().equals(srcDir.getAbsoluteFile()))
			throw new RuntimeException(new IllegalStateException("build directory and source directory should be separate"));
		if(!buildDir.exists())
			if(!buildDir.mkdirs())
				System.out.println("WARNING: Failed to create build directory. build-dir="+buildDir);
		if(!srcDir.exists())
			throw new RuntimeException(new FileNotFoundException("Could not find source dir. src-dir="+srcDir));
		if(!originalApp.exists())
			throw new RuntimeException(new FileNotFoundException("Could not find original app. app=" + originalApp));
		if(doNotSignApk && installApp)
			throw new RuntimeException(new IllegalStateException("You cannot install app without signing it first"));
		if((signAllApps || installApp || !doNotSignApk) && (keystore == null || !keystore.exists()))
			throw new RuntimeException(new IllegalStateException("App cannot be installed/signed until you provide the keystore"));
		if (keystore != null && !keystore.exists()) {
			System.out.println("WARNING: Provided keystore does not exist. App(s) will be left unsigned");
			keystore = null;
		}
		if(keystorePassword != null && keystore == null)
			System.out.println("WARNING: Keystore password is of no use without keystore file itself");
		if(!installApp && splitApps != null && splitApps.length > 0)
			System.out.println("WARNING: Provided apps are of no use when you don't install apps");
		if(signAllApps && doNotSignApk) {
			System.out.println("WARNING: Using both --sign-all and --no-sign make no sense");
			signAllApps = false;
		}
		if(signAllApps && splitApps == null)
			System.out.println("WARNING: --sign-all is of no use without additional apps");
		if(signAllApps)
			System.out.println("WARNING: Signing apps other than original app is in-place. Old apps will be stored as APP_NAME.apk.bak");
		if(installOnEmulator && !installApp)
			installApp = true;
	}

	private void setupRunFile() throws IOException {
		if(runFile.exists())
			if (!runFile.delete())
				System.out.println("WARNING: Failed to delete last generated 'command' file.");
		if(!runFile.createNewFile())
			System.out.println("WARNING: Cannot create 'command' file.");
		if(!runFile.canExecute())
			if(!runFile.setExecutable(true))
				System.out.println("Cannot set the run file executable.");
	}

	public void build() throws IOException {
		Shell shell = new Shell(buildDir, originalApp, runFile);
		if(appName != null)
			shell.setAppName(appName);
		if(smaliDirs != null)
			shell.compileSmali(smaliDirs).execute();
		if(additionalFiles != null)
			shell.addFiles(additionalFiles).execute();
		if(!doNotSignApk) {
			if(signAllApps && splitApps != null)
				shell.signAllApps(keystore, keystorePassword, splitApps).execute();
			else
				shell.signOriginalApp(keystore, keystorePassword).execute();
		}
		if(installApp)
			shell.installApps(installOnEmulator, splitApps).execute();
		if(!keepBuildFiles)
			shell.clean().execute();
	}
}