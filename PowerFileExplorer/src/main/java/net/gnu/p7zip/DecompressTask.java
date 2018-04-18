package net.gnu.p7zip;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import android.app.*;
import net.gnu.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import net.gnu.zpaq.Zpaq;
import net.gnu.androidutil.ForegroundService;

public class DecompressTask extends AsyncTask<String, String, String> implements UpdateProgress {

	private Activity activity = null;
	DecompressFragment decompFrag;
	private String fList = null;

	private String command;
	private String saveTo;
	private String password;
	private String include;
	private String exclude;
	private String szmode = "";
	private String zpaqmode = "";
	List<String> otherArgs;
	private String[] modes = new String[]{"-aoa", "-aos", "-aou", "-aot"};
	private String[] zpaqmodes = new String[]{"-force", "", "", ""};
	private long start;
	PowerManager.WakeLock wl;
	private Andro7za andro7za;
	private Zpaq zpaq;
	
	private static final String TAG = "DecompressTask";

	public DecompressTask(DecompressFragment decompFrag) {
		this.decompFrag = decompFrag;
		this.activity = decompFrag.getActivity();
		andro7za = new Andro7za(activity);
		zpaq = new Zpaq(activity);
		this.fList = decompFrag.fileET.getText().toString();
		this.saveTo = decompFrag.saveToET.getText().toString();
		this.include = decompFrag.includeET.getText().toString();
		this.exclude = decompFrag.excludeET.getText().toString();
		this.password = decompFrag.passwordET.getText().toString();
		String[] otherArg = decompFrag.otherArgsET.getText().toString().split("\\s+");
		otherArgs = new ArrayList<String>(Arrays.asList(otherArg));
		szmode = modes[decompFrag.overwriteModeSpinner.getSelectedItemPosition()];
		zpaqmode = zpaqmodes[decompFrag.overwriteModeSpinner.getSelectedItemPosition()];
		command = decompFrag.extractWithFullPathsCB.isChecked() ? "x" : "e";
	}

	protected String doInBackground(String... urls) {
		start = System.currentTimeMillis();
		PowerManager pm = (PowerManager)activity.getSystemService(
			Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		try {
			wl.acquire();
			List<String> fiList = Arrays.asList(fList.split("\\|+\\s*"));
			File f = new File(saveTo);
			if (!f.exists()) {
				f.mkdirs();
			}
			int ret = 0;
			List<String> args = new ArrayList<>();
			for (String archiveName : fiList) {
				publishProgress("Extracting " + archiveName);
				if (new File(archiveName).exists()) {
					args.clear();
					args.addAll(otherArgs);
					sb = new StringBuilder();
					rowNum = 0;
					if (archiveName.toLowerCase().endsWith(".zpaq")) {
						ret = zpaq.decompress(archiveName, password, saveTo, zpaqmode, include, exclude, args, this);
					} else {
						ret = andro7za.extractInEx(command, archiveName, password, szmode, saveTo, include, exclude, args, this);
						if (ret == 1) {
							publishProgress("Warning " + archiveName);
							return "Warning";
						} else if (ret == 2) {
							publishProgress("Fatal error " + archiveName);
							return "Fatal error";
						} else if (ret == 7) {
							publishProgress("Command line error " + archiveName);
							return "Command line error";
						} else if (ret == 8) {
							publishProgress("Not enough memory for operation" + archiveName);
							return "Not enough memory for operation";
						} else if (ret == 255) {
							publishProgress("User stopped the process " + archiveName);
							return "User stopped the process";
						}
					}
				} else {
					publishProgress(archiveName + " is not existed");
				}
			}
			return "Decompression finished";
		} catch (Throwable e) {
			e.printStackTrace();
			String message = "Decompression is not successful" + e.getMessage();
			return message;
		} finally {
			wl.release();
			activity.stopService(new Intent(activity, ForegroundService.class));
		}
	}

	@Override
	protected void onCancelled(String result) {
		Log.i(TAG, "onCancelled");
		if (wl != null && wl.isHeld()) {
			andro7za.command.stopAll();
			wl.release();
			activity.stopService(new Intent(activity, ForegroundService.class));
		}
	}

	@Override
	public void updateProgress(String...values) {
		publishProgress(values);
	}

	@Override
	protected void onPostExecute(String result) {
		Toast.makeText(activity, result, Toast.LENGTH_LONG).show();
		//Toast.makeText(activity, "Operation took " + Util.nf.format(System.currentTimeMillis() - start) + " milliseconds", Toast.LENGTH_LONG).show();
		decompFrag.mBtnOK.setText("Decompress");
		sb.append(result);
		if (wl != null && wl.isHeld()) {
			wl.release();
			activity.stopService(new Intent(activity, ForegroundService.class));
		}
		decompFrag.statusTV.setText(sb + ". Operation took " + Util.nf.format(System.currentTimeMillis() - start) + " milliseconds");
		Log.d(TAG, result);
	}
	private int rowNum = 0;
	private StringBuilder sb = new StringBuilder();
	protected void onProgressUpdate(String... progress) {
		if (progress != null && progress.length > 0 && progress[0] != null && progress[0].trim().length() > 0) {

//			if (progress[0].indexOf("\n") >= 0) {
//				++rowNum;
//			}
			if (++rowNum > 6) {
				sb = new StringBuilder(sb.substring(sb.indexOf("\n") + 1));
				//decompFrag.statusTV.setText("");
			} 
			sb.append(progress[0]).append("\n");
			decompFrag.statusTV.setText(sb);
			Log.d(TAG, progress[0]);
		}
	}
}

