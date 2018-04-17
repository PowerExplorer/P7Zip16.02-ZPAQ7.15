package net.gnu.p7zip;

import android.app.DialogFragment;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class GetFileListener implements OnClickListener {

	private DialogFragment frag;
	String action;
	private EditText filesET;
	private String title;
	private String suffix;
	private String mimes;
	private int requestCode;
	private boolean multi;

	public GetFileListener(DialogFragment frag, String action, String title, String suffix, String mimes, EditText filesET, int requestCode, boolean multi) {
		this.frag = frag;
		this.filesET = filesET;
		this.title = title;
		this.suffix = suffix;
		this.mimes = mimes;
		this.requestCode = requestCode;
		this.multi = multi;
		this.action = action;
	}

	public void onClick(final View v) {
		frag.dismiss();
		final Intent intent = new Intent(action);
		final String file = filesET.getText().toString();
		if (file.length() > 0) {
			intent.putExtra(MainActivity.PREVIOUS_SELECTED_FILES, file.split("\\|+\\s*"));
		}
		intent.putExtra(MainActivity.EXTRA_FILTER_FILETYPE, suffix);
		intent.putExtra(MainActivity.EXTRA_FILTER_MIMETYPE, mimes);
		intent.putExtra(MainActivity.EXTRA_MULTI_SELECT, multi);
		intent.putExtra(MainActivity.EXTRA_TITLE, title);
		frag.getActivity().startActivityForResult(intent, requestCode);
	}
}

