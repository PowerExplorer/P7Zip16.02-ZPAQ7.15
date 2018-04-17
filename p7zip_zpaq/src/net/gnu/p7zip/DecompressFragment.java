package net.gnu.p7zip;

import java.io.Serializable;
import java.util.Arrays;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import net.gnu.p7zip.R;
import net.gnu.util.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import net.gnu.util.FileUtil;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class DecompressFragment extends DialogFragment implements Serializable, OnItemSelectedListener, OnCheckedChangeListener {

	private static final long serialVersionUID = 8239392959025238961L;

	private static final String TAG = "DecompressFragment";

	String[] modes = new String[] {
		"Overwrite All existing files without prompt (-aoa)",
		"Skip extracting of existing files (-aos)",
		"aUto rename extracting file (-aou)",
		"auto rename existing file (-aot)"};

	String files = "";
	String saveTo = "";
	String include = "";
	String exclude = "";
	String otherArgs = "";
	transient String password = "";
	int overwriteMode = 0;
	String command = "x";

	transient Button mBtnConfirm;
	private transient Button mBtnCancel;
	private transient Button filesBtn;
	private transient Button saveToBtn;
	private transient ImageButton historyBtn;
	private transient ImageButton historySaveBtn;
	
	transient EditText fileET;
	transient EditText saveToET;
	transient EditText includeET;
	transient EditText excludeET;
	transient EditText otherArgsET;
	transient ShowHidePasswordEditText passwordET;
	transient Spinner overwriteModeSpinner;
	transient CheckBox extractWithFullPathsCB;

	private transient OnFragmentInteractionListener mListener;
	transient TextView statusTV;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate " + savedInstanceState);
		super.onCreate(savedInstanceState);

		if (this.getShowsDialog()) {
			setStyle(DialogFragment.STYLE_NO_TITLE, 0);
		} else {
			setHasOptionsMenu(true);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView " + savedInstanceState);
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.decompress, container, false);
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d(TAG, "onViewCreated " + savedInstanceState);
		super.onViewCreated(view, savedInstanceState);

		mBtnConfirm = (Button) view.findViewById(R.id.okDir);
		mBtnCancel = (Button) view.findViewById(R.id.cancelDir);
		fileET = (EditText) view.findViewById(R.id.files);
		saveToET = (EditText) view.findViewById(R.id.saveTo);
		includeET = (EditText) view.findViewById(R.id.include);
		excludeET = (EditText) view.findViewById(R.id.exclude);
		otherArgsET = (EditText) view.findViewById(R.id.otherParametersET);
		passwordET = (ShowHidePasswordEditText) view.findViewById(R.id.password);
		overwriteModeSpinner = (Spinner) view.findViewById(R.id.overwrite);
		filesBtn = (Button) view.findViewById(R.id.filesBtn);
		saveToBtn = (Button) view.findViewById(R.id.saveToBtn);
		statusTV = (TextView) view.findViewById(R.id.status);
		extractWithFullPathsCB = (CheckBox) view.findViewById(R.id.extractWithFullPathsCB);
		historySaveBtn = (ImageButton) view.findViewById(R.id.historySaveBtn);
		historyBtn = (ImageButton) view.findViewById(R.id.historyBtn);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
			overwriteModeSpinner.getContext(), android.R.layout.simple_spinner_item, modes);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		overwriteModeSpinner.setAdapter(adapter);
		overwriteModeSpinner.setOnItemSelectedListener(this);
		extractWithFullPathsCB.setOnCheckedChangeListener(this);
		
		Log.d(TAG, "onViewCreated files " + files + ", fileET " + fileET.getText() + ", saveTo " + saveTo + ", saveToET " + saveToET.getText());
		//Log.d(TAG, Util.arrayToString(fileET.getText().toString().split("\\|+\\s*"), true, "\n"));
		
		historyBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View p1) {

				}
			});

		historySaveBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View p1) {

				}
			});

		filesBtn.setOnClickListener(new GetFileListener(this, MainActivity.ACTION_PICK_FILE, 
														MainActivity.ZIP_TITLE, 
														MainActivity.ZIP_SUFFIX, 
														"",
														fileET, 
														MainActivity.FILES_REQUEST_CODE, 
														MainActivity.MULTI_FILES));
		mBtnConfirm.setOnClickListener(new OkBtnListener(this, mListener));

		mBtnCancel.setOnClickListener(new CancelBtnListener(this, mListener));

		saveToBtn.setOnClickListener(new GetFileListener(this, MainActivity.ACTION_PICK_DIRECTORY, 
														 "Output Folder", 
														 "", 
														 "", 
														 saveToET, 
														 MainActivity.SAVETO_REQUEST_CODE,
														 !MainActivity.MULTI_FILES));
		
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, "onSaveInstanceState " + outState);
		super.onSaveInstanceState(outState);
		if (outState != null) {
			outState.putString("password", passwordET.getText().toString());
		}
	}

	public void onViewStateRestored(Bundle savedInstanceState) {
		Log.d(TAG, "onViewStateRestored files " + files + ", fileET " + fileET.getText() + ", saveTo " + saveTo + ", saveToET " + saveToET.getText());
		super.onViewStateRestored(savedInstanceState);
		if (savedInstanceState != null) {
			passwordET.setText(savedInstanceState.getString("password"));
		}
		restore();
	}

	public void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		save();
	}

	@Override
	public void onCheckedChanged(CompoundButton p1, boolean p2) {
		if (p1.getId() == R.id.extractWithFullPathsCB) {
			if (p1.isChecked()) {
				command = "x";
			} else {
				command = "e";
			}
		}
	}

	public void onItemSelected(
		AdapterView<?> parent, View view, int position, long id) {
		overwriteMode = overwriteModeSpinner.getSelectedItemPosition();
		Log.i("on overwriteMode", overwriteMode + "");
	}

	public void onNothingSelected(AdapterView<?> parent) {
	}

	void save() {
		Log.d(TAG, "save " + this);
		if (fileET != null) {
			files = fileET.getText().toString();
			saveTo = saveToET.getText().toString();
			include = includeET.getText().toString();
			exclude = excludeET.getText().toString();
			otherArgs = otherArgsET.getText().toString();
			overwriteMode = overwriteModeSpinner.getSelectedItemPosition();
			command = extractWithFullPathsCB.isChecked() ? "x" : "e";
			//password = passwordET.getText().toString();
			try {
				FileOutputStream fos = new FileOutputStream("/data/data/net.gnu.p7zip/" + DecompressFragment.class.getSimpleName() + ".ser");
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(this);
				FileUtil.flushClose(bos, fos);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static DecompressFragment newInstance() {

		File fi = new File("/data/data/net.gnu.p7zip/" + DecompressFragment.class.getSimpleName() + ".ser");
		Log.d(TAG, fi.getAbsolutePath() + ", exist " + fi.exists() + ", length " + fi.length());
		DecompressFragment decompressFrag = null;
		if (fi.exists() && fi.length() > 0) {
			try	{
				FileInputStream fis = new FileInputStream(fi);
				BufferedInputStream bis = new BufferedInputStream(fis);
				ObjectInputStream ois = new ObjectInputStream(bis);
				decompressFrag = (DecompressFragment) ois.readObject();
				FileUtil.close(ois, bis, fis);
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			decompressFrag = new DecompressFragment();
		}
		Log.d(TAG, "newInstance " + decompressFrag);
		return decompressFrag;
	}

	private void restore() {
		fileET.setText(files);
		saveToET.setText(saveTo);
		includeET.setText(include);
		excludeET.setText(exclude);
		otherArgsET.setText(otherArgs);
		//passwordET.setText(password);
		overwriteModeSpinner.setSelection(overwriteMode);

		if ("x".equals(command)) {
			extractWithFullPathsCB.setChecked(true);
		} else {
			extractWithFullPathsCB.setChecked(false);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		Log.e(TAG, "onAttach");
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity
										 + " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.e(TAG, "onDetach");
		mListener = null;
	}
}

