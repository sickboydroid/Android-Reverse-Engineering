package com.tangledbytes.rebuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Shell {
	private final List<List<String>> commands = new ArrayList<>();
	private final File dirBuild;
	private final File originalApk;
	private final File unsignedApk;
	private final File signedApk;
	private final File fileCommand;
	private String appName;

	public Shell(File dirBuild, File fileOriginalApp, File fileCommand) throws IOException {
		this.originalApk = fileOriginalApp;
		this.dirBuild = dirBuild;
		this.fileCommand = fileCommand;
		unsignedApk = new File(dirBuild, getAppName() + "-unsigned.apk");
		signedApk = new File(dirBuild, getAppName() + "-signed.apk");
		if (fileOriginalApp.getAbsolutePath().equals(unsignedApk.getAbsolutePath()))
			throw new RuntimeException("Original and build output apk cannot be same. Try to change build directory");
		if (unsignedApk.exists())
			if (!unsignedApk.delete()) System.out.println("WARNING: Failed to delete " + unsignedApk);
		if (signedApk.exists())
			if (!signedApk.delete()) System.out.println("WARNING: Failed to delete " + signedApk);
		Files.copy(fileOriginalApp.toPath(), unsignedApk.toPath());
	}

	public String getAppName() {
		if (appName != null && !appName.isEmpty())
			return appName;
		appName = "";
		if (originalApk.getName().contains("apk"))
			appName = originalApk.getName().substring(0, originalApk.getName().lastIndexOf(".apk"));
		if (appName.isEmpty())
			appName = "app-mod.apk";
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public Shell compileSmali(File[] dirsSmali) {
		List<String> smaliCommand = new ArrayList<>();
		for (File dirSmali : dirsSmali) {
			if (!dirSmali.exists())
				throw new RuntimeException(new FileNotFoundException("Could not find directory '" + dirSmali + "'"));
			File outputDex = new File(dirBuild, dirSmali.getName() + ".dex");
			String command = String.format("echo 'smali %s -> %s...' && smali a %s -o %s",
					dirSmali.getName(), outputDex.getName(), dirSmali, outputDex);
			smaliCommand.add(command);
			smaliCommand.add(String.format("zip -0 -u -j %s %s ", unsignedApk, outputDex));
		}
		commands.add(smaliCommand);
		return this;
	}

	public Shell addFiles(File[] additionalFiles) {
		List<String> addFilesCommand = new ArrayList<>();
		for (File additionalFile : additionalFiles) {
			addFilesCommand.add(String.format("echo 'adding %s...'", additionalFile));
			addFilesCommand.add(String.format("zip -0 -u -j -q %s %s", unsignedApk, additionalFile));
		}
		commands.add(addFilesCommand);
		return this;
	}

	public void execute() throws IOException {
		for (List<String> commandBatch : commands) {
			StringBuilder strCommandBatch = new StringBuilder(": ");
			for (String command : commandBatch)
				strCommandBatch.append(" && ").append(command);
			shell(strCommandBatch.toString());
		}
		commands.clear();
	}

	public Shell clean() {
		List<String> cleanCommand = new ArrayList<>();
		cleanCommand.add("echo cleaning build dir...");
		cleanCommand.add("rm -rf " + dirBuild + "/* ");
		commands.add(cleanCommand);
		return this;
	}

	public Shell signOriginalApp(File keystore, String keystorePassword) {
		signApp(unsignedApk, signedApk, keystore, keystorePassword);
		return this;
	}

	public Shell installApps(boolean installOnEmulator, File[] companionApps) {
		List<String> installCommand = new ArrayList<>();
		// TODO: Handle multiple devices
		String target = installOnEmulator ? "-e" : "-d";
		StringBuilder command = new StringBuilder();
		command.append(String.format("adb %s install-multiple -r %s", target, signedApk));
		for (File companionApp : companionApps)
			command.append(" ").append(companionApp);
		installCommand.add(command.toString());
		commands.add(installCommand);
		return this;
	}

	private void shell(String command) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileCommand, true);
		fos.write((command + "\n").getBytes());
		fos.flush();
		fos.close();
	}

	public Shell signAllApps(File keystore, String keystorePassword, File[] companionApps) throws IOException {
		signOriginalApp(keystore, keystorePassword);
		for (File app : companionApps) {
			File unsignedApp = new File(app.getParentFile(), app.getName() + ".bak");
			if(unsignedApp.exists())
				unsignedApp.delete();
			Files.move(app.toPath(), unsignedApp.toPath());
			signApp(unsignedApp, app, keystore, keystorePassword);
		}
		return this;
	}

	public void signApp(File unsignedApk, File signedApk, File keystore, String keystorePassword) {
		File alignedApk = new File(unsignedApk.getParentFile(), unsignedApk.getName() + ".aligned");
		List<String> signCommand = new ArrayList<>();
		if (alignedApk.exists())
			signCommand.add("rm " + alignedApk + " && echo removed " + alignedApk.getName());
		signCommand.add(String.format("echo zipalign %s...", unsignedApk.getName()));
		signCommand.add(String.format("zipalign -p -f 4 %s %s", unsignedApk, alignedApk));
		signCommand.add(String.format("echo signing %s...", alignedApk.getName()));
		String apksigner = String.format("apksigner sign -ks %s --out %s %s", keystore, signedApk, alignedApk);
		if (keystorePassword != null && !keystorePassword.isEmpty())
			apksigner = "echo " + keystorePassword + " | " + apksigner;
		signCommand.add(apksigner);
		commands.add(signCommand);
	}
}
