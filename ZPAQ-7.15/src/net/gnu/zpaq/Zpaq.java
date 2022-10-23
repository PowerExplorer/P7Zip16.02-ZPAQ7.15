package net.gnu.zpaq;

import java.io.*;
import android.util.*;
import android.content.Context;
import net.gnu.util.Util;
import org.magiclen.magiccommand.Command;
import net.gnu.util.CommandListener;
import net.gnu.util.FileUtil;
import java.util.Arrays;
import net.gnu.util.UpdateProgress;
import java.util.List;

public class Zpaq {
	
	private final String TAG = "Zpaq";
	
	public Command command;
	
	public native int runZpaq(String... args);
	public native String stringFromJNI(String outfile, String infile);
	public native void closeStreamJNI();
	
	static {
        System.loadLibrary("zpaq");
    }
	
	private String PRIVATE_PATH = "/data/data/net.gnu.zpaq";
	//public static String zpaqx86 = PRIVATE_PATH + "/zpaq-x86";
	private String zpaqarm = PRIVATE_PATH + "/zpaq-armeabi-v7a";
	
	private String mOutfile;
	private String mInfile;
	private String listFile;
	private String zpaq;
	
	public Zpaq(final Context ctx, final String tempDir) {
		Log.d(TAG, "PackageName: " + ctx.getPackageName());
		PRIVATE_PATH = "/data/data/" + ctx.getPackageName();
		zpaqarm = PRIVATE_PATH + "/zpaq-armeabi-v7a";
		
		if (tempDir == null || !new File(tempDir).mkdirs()) {
			final String files = ctx.getExternalFilesDir("zpaq").getAbsolutePath();
			mOutfile = files + "/zpaqOut.txt";
			mInfile = files + "/zpaqIn.txt";
			listFile = files + "/zpaqFileList.txt";
		} else {
			mOutfile = tempDir + "/zpaqOut.txt";
			mInfile = tempDir + "/zpaqIn.txt";
			listFile = tempDir + "/zpaqFileList.txt";
		}
		
		zpaq = zpaqarm;
		try {
			Log.d(TAG, "Assets dir " + Util.collectionToString(Arrays.asList(ctx.getAssets().list("")), true, "\n"));

			Command command;
			
//			if (!new File(zpaqx86).exists()) {
//				command = new Command("mkdir", "/data/data/net.gnu.zpaq/commands");//"/android_asset/"
//				command.setCommandListener(new CommandListener(command));
//				command.run();
//				FileUtil.is2File(ctx.getAssets().open("zpaq-x86"), zpaqx86);///android_asset/
//
//				command = new Command("chmod", "777", zpaqx86);
//				command.setCommandListener(new CommandListener(command));
//				command.run();
//				command = new Command(zpaqx86);
//				command.setCommandListener(new CommandListener(command));
//				command.run();
//			}

			if (!new File(zpaqarm).exists()) {
				FileUtil.is2File(ctx.getAssets().open("zpaq-armeabi-v7a"), zpaqarm);
				command = new Command("chmod", "777", zpaqarm);
				command.setCommandListener(new CommandListener(command));
				command.run();
				command = new Command(zpaqarm);
				command.setCommandListener(new CommandListener(command, new UpdateProgress() {
												   @Override
												   public void updateProgress(String[] args) {
													   Log.d(TAG, args[0]);
												   }
				}));
				command.run();
			}
			command = new Command(zpaqarm, "a", "/sdcard/zpaq6", "/sdcard/zpaq");
			command.setCommandListener(new CommandListener(command));
			command.run();

			command = new Command("ls", "-lr", PRIVATE_PATH);
			command.setCommandListener(new CommandListener(command));
			command.run();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public void initStream() throws IOException {
		resetFile(mOutfile);
		resetFile(mInfile);
		resetFile(listFile);
		stringFromJNI(mOutfile, mInfile);
	}

	private void resetFile(String f) throws IOException {
		File file = new File(f);
		File parentFile = file.getParentFile();
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		} else {
			file.delete();
		}
		file.createNewFile();
	}
	
	public Object[] runZpaq(boolean showDebug, String... args) throws IOException {
		try {
			initStream();

			if (args == null && args.length == 0) {
				return new Object[] {2, new StringBuilder()};
			}
			
			Log.d(TAG, "Call runZpaq(): " + Util.arrayToString(args, false, " "));
			
			int ret = runZpaq(args);
			Log.d(TAG, "runZpaq() ret " + ret);
			FileReader fileReader = new FileReader(mOutfile);
			BufferedReader br = new BufferedReader(fileReader, 32768);
			StringBuilder sb = new StringBuilder();
			if (!showDebug) {
				while (br.ready()) {
					sb.append(br.readLine()).append("\n");
				}
			} else {
				String readLine;
				while (br.ready()) {
					readLine = br.readLine();
					Log.d(TAG, readLine);
					sb.append(readLine).append("\n");
				}
			}
			return new Object[] {ret, sb};
		} finally {
			closeStreamJNI();
		}
	}

	public int compress(
		String archiveName, 
		String password, 
		String level, 
		String files, 
		String excludes,
		final List<String> otherArgs, 
		UpdateProgress update) {

		Log.i(TAG, archiveName + "," +
			  level + "," +
			  files + "," +
			  excludes + ", " + 
			  otherArgs
			  );

		if (password != null && password.trim().length() > 0) {
			otherArgs.add(0, password);
			otherArgs.add(0, "-key");
		}

		if (excludes != null && excludes.trim().length() > 0) {
			otherArgs.add("-not");
			otherArgs.addAll(Arrays.asList(excludes.split("\\|+\\s*")));
		}

		otherArgs.add(0, level);
		otherArgs.addAll(0, Arrays.asList(files.split("\\|+\\s*")));
		otherArgs.add(0, archiveName);
		otherArgs.add(0, "a");
		otherArgs.add(0, zpaq);

		//Log.d(TAG, Util.collectionToString(otherArgs, false, "\n"));
		command = new Command(otherArgs);
		Log.d(TAG, "command: " + command);
		CommandListener commandListener = new CommandListener(command, update);
		command.setCommandListener(commandListener);
		command.run();
		int ret = commandListener.ret;

		Log.d(TAG, "ret " + ret);

		return ret;
	}
	
	public int decompress(
		String archiveName, 
		String password, 
		String saveTo, 
		String mode, 
		String include, 
		String excludes,
		final List<String> otherArgs, 
		UpdateProgress update) {

		Log.i(TAG, archiveName + "," +
			  saveTo + "," +
			  excludes + ", " + 
			  otherArgs
			  );

		if (password != null && password.trim().length() > 0) {
			otherArgs.add(0, password);
			otherArgs.add(0, "-key");
		}
		
		if (include != null && include.trim().length() > 0) {
			otherArgs.add("-only");
			otherArgs.addAll(Arrays.asList(include.split("\\|+\\s+")));
		}
		
		if (excludes != null && excludes.trim().length() > 0) {
			otherArgs.add("-not");
			otherArgs.addAll(Arrays.asList(excludes.split("\\|+\\s+")));
		}
		
		otherArgs.add(0, saveTo);
		otherArgs.add(0, "-to");
		otherArgs.add(0, mode);
		otherArgs.add(0, archiveName);
		otherArgs.add(0, "x");
		otherArgs.add(0, zpaq);
		
		//Log.d(TAG, Util.collectionToString(otherArgs, false, "\n"));
		command = new Command(otherArgs);
		Log.d(TAG, "command: " + command);
		CommandListener commandListener = new CommandListener(command, update);
		command.setCommandListener(commandListener);
		command.run();
		int ret = commandListener.ret;

		Log.d(TAG, "ret " + ret);

		return ret;
	}
}
