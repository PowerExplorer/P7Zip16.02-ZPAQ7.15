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
import android.widget.RadioGroup;
import net.gnu.util.Util;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import net.gnu.util.FileUtil;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.ObjectOutputStream;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.os.AsyncTask;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;
import android.widget.ImageButton;

public class CompressFragment extends DialogFragment implements Serializable, OnItemSelectedListener, OnCheckedChangeListener, TextWatcher, OnClickListener, android.widget.RadioGroup.OnCheckedChangeListener {

	private static final long serialVersionUID = 3972884849642358507L;

	private static final String TAG = "CompressFragment";

	private transient static final String[] levels = new String[] {"Fastest, biggest (-mx0)", " Very fast (-mx1)", "Normal (-mx3)", "Slow (-mx5)", "Very Slow (-mx7)", "Slowest, smallest (-mx9)"};
	//transient static final String[] types = new String[]{"7z", "zip", "bz2", "gz", "tar", "wim", "swm", "xz", "zipx", "jar", "xpi", "odt", "ods", "docx", "xlsx", "epub"};
	//private transient static final String[] volumes = new String[]{"bytes", "kilobytes", "megabytes", "gigabytes"};

	String files = "";
	String saveTo = "";
	String volumeVal = "";
	int volumeUnit;
	String excludes = "";
	
	int level = 3;
	int type = 0;
	String otherParameters = "";
	transient String password = "";
	
	private transient OnFragmentInteractionListener mListener;

	transient Button mBtnOK;
	private transient Button mBtnCancel;
	private transient Button filesBtn;
	private transient Button saveToBtn;
	private transient ImageButton historyBtn;
	private transient ImageButton historySaveBtn;
	transient EditText fileET;
	transient EditText saveToET;
	//transient EditText fNameET;
	transient ShowHidePasswordEditText passwordET;
	//transient ShowHidePasswordEditText passwordET2;
	transient Spinner compressLevelSpinner;
	transient RadioGroup typeRadioGroup;
	transient RadioGroup volUnitRadioGroup;
	transient EditText volumeValET;
	transient EditText excludeET;

	transient EditText otherParametersET;
	transient EditText solidArchiveET;
	//transient EditText workingDirectoryET;
	//transient EditText archiveNameMaskET;
	
	transient CheckBox encryptFileNamesCB;
	transient CheckBox deleteFilesAfterArchivingCB;
	//transient TextView passwordCB;
	transient CheckBox solidArchiveCB;
//	transient CheckBox testCB;
//	transient CheckBox createSeparateArchivesCB;
//	transient CheckBox archiveNameMaskCB;
	//transient CheckBox workingDirectoryCB;
	//transient CheckBox otherParametersCB;
	//transient Button workingDirectoryBtn;
	
	String solidArchive = "";
//	private String test;
//	private String createSeparateArchives;
//	private String archiveNameMask;
	//String workingDirectory;
	
	transient TextView statusTV;

	String deleteFilesAfterArchiving = "";
	String encryptFileNames = "";
	transient CompressTask compressTask;
	
	@Override
	public void onClick(final View p1) {
		if (p1.getId() == R.id.mode) {
			final View view = getView();
			if (((ToggleButton)p1).isChecked()) {
				view.findViewById(R.id.advancedLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.basicLayout).setVisibility(View.GONE);
			} else {
				view.findViewById(R.id.basicLayout).setVisibility(View.VISIBLE);
				view.findViewById(R.id.advancedLayout).setVisibility(View.GONE);
			}
		}
	}

	
	@Override
	public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) {
	}

	@Override
	public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
		if (p1.length() > 0) {
			encryptFileNamesCB.setEnabled(true);
		} else {
			encryptFileNamesCB.setChecked(false);
			encryptFileNamesCB.setEnabled(false);
			encryptFileNames = "";
		}
	}

	@Override
	public void afterTextChanged(Editable p1) {
	}

	@Override
	public void onCheckedChanged(RadioGroup p1, int p2) {
		if (p1.getCheckedRadioButtonId() == R.id.sevenz) {
			solidArchiveCB.setEnabled(true);
		} else {
			solidArchiveET.setText("-ms=off");
			solidArchiveCB.setChecked(false);
			solidArchiveCB.setEnabled(false);
		}
		if (p1.getCheckedRadioButtonId() != R.id.zpaq) {
			deleteFilesAfterArchivingCB.setEnabled(true);
		} else {
			deleteFilesAfterArchivingCB.setChecked(false);
			deleteFilesAfterArchivingCB.setEnabled(false);
		}
	}
	
	@Override
	public void onCheckedChanged(CompoundButton checkBox, boolean p2) {
		switch (checkBox.getId()) {
//			case (R.id.passwordCB) : {
//					if (checkBox.isChecked()) {
//						passwordET.setEnabled(true);
//						encryptFileNamesCB.setEnabled(true);
//					} else {
//						passwordET.setText("");
//						passwordET.setEnabled(false);
//						encryptFileNamesCB.setChecked(false);
//						encryptFileNamesCB.setEnabled(false);
//						encryptFileNames = "";
//					}
//					break;
//				}
			case (R.id.deleteFilesAfterArchivingCB) : {
					if (checkBox.isChecked()) {
						deleteFilesAfterArchiving = "-sdel";
					} else {
						deleteFilesAfterArchiving = "";
					}
					break;
				}
			case (R.id.encryptFileNamesCB) : {
					if (checkBox.isChecked()) {
						encryptFileNames = "-mhe=on";
					} else {
						encryptFileNames = "";
					}
					break;
				}
//			case (R.id.otherParametersCB) : {
//					if (checkBox.isChecked()) {
//						otherParametersET.setEnabled(true);
//						otherParametersET.setText("-mqs=on");
//					} else {
//						otherParametersET.setText("");
//						otherParametersET.setEnabled(false);
//					}
//					break;
//				}
			case (R.id.solidArchiveCB) : {
					if (checkBox.isChecked()) {
						solidArchiveET.setEnabled(true);
						solidArchiveET.setText("-mse");
					} else {
						solidArchiveET.setText("-ms=off");
						solidArchiveET.setEnabled(false);
					}
					break;
				}
//			case (R.id.testCB) : {
////					if (checkBox.isChecked()) {
////						otherArgsET.setEnabled(true);
////					} else {
////						otherArgsET.setText("");
////						otherArgsET.setEnabled(false);
////					}
//					break;
//				}
//			case (R.id.createSeparateArchivesCB) : {
////					if (checkBox.isChecked()) {
////						otherArgsET.setEnabled(true);
////					} else {
////						otherArgsET.setText("");
////						otherArgsET.setEnabled(false);
////					}
//					break;
//				}
//			case (R.id.archiveNameMaskCB) : {
////					if (checkBox.isChecked()) {
////						otherArgsET.setEnabled(true);
////					} else {
////						otherArgsET.setText("");
////						otherArgsET.setEnabled(false);
////					}
//					break;
//				}
//			case (R.id.workingDirectoryCB) : {
//					if (checkBox.isChecked()) {
//						workingDirectoryET.setEnabled(true);
//						workingDirectoryBtn.setEnabled(true);
//					} else {
//						workingDirectoryET.setText("");
//						workingDirectoryET.setEnabled(false);
//						workingDirectoryBtn.setEnabled(false);
//					}
//					break;
//				}
		}
	}

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

		return inflater.inflate(R.layout.compress, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Log.d(TAG, "onViewCreated " + savedInstanceState);
		super.onViewCreated(view, savedInstanceState);
		mBtnOK = (Button) view.findViewById(R.id.okDir);
		mBtnCancel = (Button) view.findViewById(R.id.cancelDir);
		fileET = (EditText) view.findViewById(R.id.files);
		
		saveToET = (EditText) view.findViewById(R.id.saveTo);
		volumeValET = (EditText) view.findViewById(R.id.volumeVal);
		excludeET = (EditText) view.findViewById(R.id.exclude);
		otherParametersET = (EditText) view.findViewById(R.id.otherParametersET);
		passwordET = (ShowHidePasswordEditText) view.findViewById(R.id.password);
		
		solidArchiveET = (EditText) view.findViewById(R.id.solidArchiveET);
		//workingDirectoryET = (EditText) view.findViewById(R.id.workingDirectoryET);
		//archiveNameMaskET = (EditText) view.findViewById(R.id.archiveNameMaskET);
		view.findViewById(R.id.mode).setOnClickListener(this);
		compressLevelSpinner = (Spinner) view.findViewById(R.id.level);
		typeRadioGroup = (RadioGroup) view.findViewById(R.id.type);
		volUnitRadioGroup = (RadioGroup) view.findViewById(R.id.volumeUnit);
		filesBtn = (Button) view.findViewById(R.id.filesBtn);
		saveToBtn = (Button) view.findViewById(R.id.saveToBtn);
		statusTV = (TextView) view.findViewById(R.id.status);
		historySaveBtn = (ImageButton) view.findViewById(R.id.historySaveBtn);
		historyBtn = (ImageButton) view.findViewById(R.id.historyBtn);
		
		deleteFilesAfterArchivingCB = (CheckBox)view.findViewById(R.id.deleteFilesAfterArchivingCB);
		encryptFileNamesCB = (CheckBox)view.findViewById(R.id.encryptFileNamesCB);
		//otherParametersCB = (CheckBox)view.findViewById(R.id.otherParametersCB);
		//passwordCB = (TextView)view.findViewById(R.id.passwordCB);
		solidArchiveCB = (CheckBox)view.findViewById(R.id.solidArchiveCB);
		//testCB = (CheckBox)view.findViewById(R.id.testCB);
		//createSeparateArchivesCB = (CheckBox)view.findViewById(R.id.createSeparateArchivesCB);
		//archiveNameMaskCB = (CheckBox)view.findViewById(R.id.archiveNameMaskCB);
		//workingDirectoryCB = (CheckBox)view.findViewById(R.id.workingDirectoryCB);
		//workingDirectoryBtn = (Button)view.findViewById(R.id.workingDirectoryBtn);

		typeRadioGroup.setOnCheckedChangeListener(this);
		deleteFilesAfterArchivingCB.setOnCheckedChangeListener(this);

		encryptFileNamesCB.setOnCheckedChangeListener(this);

		//otherParametersCB.setOnCheckedChangeListener(this);

		passwordET.addTextChangedListener(this);

		solidArchiveCB.setOnCheckedChangeListener(this);

//		testCB.setOnCheckedChangeListener(this);
//
//		createSeparateArchivesCB.setOnCheckedChangeListener(this);
//
//		archiveNameMaskCB.setOnCheckedChangeListener(this);

		//workingDirectoryCB.setOnCheckedChangeListener(this);
		if (compressTask == null || compressTask.isCancelled() || compressTask.getStatus() == AsyncTask.Status.FINISHED) {
			mBtnOK.setText("Compress");
		} else {
			mBtnOK.setText("Cancel");
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
			compressLevelSpinner.getContext(), android.R.layout.simple_spinner_item, levels);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		compressLevelSpinner.setAdapter(adapter);
		compressLevelSpinner.setOnItemSelectedListener(this);

		Log.d(TAG, "onViewCreated files " + files + ", fileET " + fileET.getText() + ", saveTo " + saveTo + ", saveToET " + saveToET.getText());
		
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
		
		filesBtn.setOnClickListener(new GetFileListener(this, MainActivity.ACTION_MULTI_SELECT, MainActivity.ALL_SUFFIX_TITLE, 
														MainActivity.ALL_SUFFIX, 
														"*/*",
														fileET, 
														MainActivity.FILES_REQUEST_CODE, 
														MainActivity.MULTI_FILES));

		mBtnOK.setOnClickListener(new OkBtnListener(this, mListener));

		mBtnCancel.setOnClickListener(new CancelBtnListener(this, mListener));

		saveToBtn.setOnClickListener(new GetFileListener(this, MainActivity.ACTION_MULTI_SELECT, "Output Folder", 
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

	public void onItemSelected(
		AdapterView<?> parent, View view, int position, long id) {
		//Log.d(TAG, view + ", " + parent);
		if (parent == compressLevelSpinner) {
			level = compressLevelSpinner.getSelectedItemPosition();
			Log.d(TAG, "onItemSelected compressLevel" + level);
		}
	}

	public void onNothingSelected(AdapterView<?> parent) {
	}

	void save() {
		Log.d(TAG, "save " + this);
		if (fileET != null) {
			files = fileET.getText().toString();
			saveTo = saveToET.getText().toString();
			volumeVal = volumeValET.getText().toString();
			excludes = excludeET.getText().toString();
			level = compressLevelSpinner.getSelectedItemPosition();
			type = typeRadioGroup.getCheckedRadioButtonId();
			volumeUnit = volUnitRadioGroup.getCheckedRadioButtonId();
			
			deleteFilesAfterArchiving = deleteFilesAfterArchivingCB.isChecked() ? "-sdel" : "";
			encryptFileNames = encryptFileNamesCB.isChecked() ? "-mhe=on" : "";
			solidArchive = solidArchiveET.getText().toString();
//			test = testCB.getText().toString();
//			createSeparateArchives = createSeparateArchivesCB.getText().toString();
//			archiveNameMask = archiveNameMaskET.getText().toString();
			//workingDirectory = workingDirectoryET.getText().toString();
			otherParameters = otherParametersET.getText().toString();
			
			try {
				FileOutputStream fos = new FileOutputStream("/data/data/net.gnu.p7zip/" + CompressFragment.class.getSimpleName() + ".ser");
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(this);
				FileUtil.flushClose(bos, fos);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static CompressFragment newInstance() {
		Log.d(TAG, "newInstance()");
		File fi = new File("/data/data/net.gnu.p7zip/" + CompressFragment.class.getSimpleName() + ".ser");
		Log.d(TAG, fi.getAbsolutePath() + ", exist " + fi.exists() + ", length " + fi.length());
		CompressFragment compressFrag = null;
		if (fi.exists() && fi.length() > 0) {
			try	{
				FileInputStream fis = new FileInputStream(fi);
				BufferedInputStream bis = new BufferedInputStream(fis);
				ObjectInputStream ois = new ObjectInputStream(bis);
				compressFrag = (CompressFragment) ois.readObject();
				FileUtil.close(ois, bis, fis);
				//Log.d(TAG, "newInstance(), compressFrag " + compressFrag);
				
				//restore(compressFrag);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			compressFrag = new CompressFragment();
		}
		Log.d(TAG, "newInstance() 2 " + compressFrag);
		return compressFrag;
	}

	void restore() {
		Log.d(TAG, "restore() 3 " + this);
		fileET.setText(files);
		saveToET.setText(saveTo);
		volumeValET.setText(volumeVal);
		excludeET.setText(excludes);
		
		typeRadioGroup.check(type <= 0 ? R.id.sevenz : type);
		volUnitRadioGroup.check(volumeUnit);
		compressLevelSpinner.setSelection(level);

		deleteFilesAfterArchivingCB.setChecked(deleteFilesAfterArchiving.equals("-sdel") ? true : false);
		encryptFileNamesCB.setChecked(passwordET.getText().length()>0 && encryptFileNames.equals("-mhe=on") ? true : false);
		solidArchiveET.setText(solidArchive);
		solidArchiveCB.setChecked(solidArchive != null && !solidArchive.trim().equals("-ms=off"));

//		test = testCB.getText().toString();
//		createSeparateArchives = createSeparateArchivesCB.getText().toString();
//		archiveNameMaskET.setText(compressFrag.archiveNameMask);
//		workingDirectoryET.setText(compressFrag.workingDirectory);
//		boolean dir = compressFrag.workingDirectory.length() > 0;
//		workingDirectoryET.setEnabled(dir);
//		workingDirectoryBtn.setEnabled(dir);
//		workingDirectoryCB.setChecked(dir);

		otherParametersET.setText(otherParameters);
//		boolean param = compressFrag.otherParameters.length() > 0;
//		otherParametersET.setEnabled(param);
		//otherParametersCB.setChecked(param);
		Log.d(TAG, "restore() 4 " + this);
		
	}
	@Override
	public String toString() {
		return "files " + files + ", fileET " + (fileET == null?null:fileET.getText())+ ", saveTo " + saveTo + ", saveToET " + (saveToET == null?null:saveToET.getText());
	}

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "onAttach " + activity);
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity
										 + " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		Log.d(TAG, "onDetach");
		super.onDetach();
		mListener = null;
	}
}
