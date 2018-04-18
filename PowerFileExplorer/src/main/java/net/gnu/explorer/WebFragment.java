package net.gnu.explorer;

import android.webkit.*;
import android.os.*;
import android.view.*;
import android.content.*;
import android.widget.*;
import android.util.*;
import android.net.*;
import java.io.*;

public class WebFragment extends Frag {

	private static final String TAG = "WebFragment";
	
    private WebView wv;
	private TextView status;
	private String url;
	
	WebFragment() {
		super();
		type = Frag.TYPE.WEB;
		title = "Web";
	}
	
    public static WebFragment newInstance(final String path) {
        final Bundle args = new Bundle();
        args.putString(ExplorerActivity.EXTRA_ABSOLUTE_PATH, path);
		args.putString("title", "Web");
		
        final WebFragment fragment = new WebFragment();
        fragment.setArguments(args);

        return fragment;
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		//setRetainInstance(true);
        Log.d(TAG, "onCreateView " + currentPathTitle + ", " + savedInstanceState);
		final View v = inflater.inflate(R.layout.webview, container, false);
		return v;
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
		
		wv = (WebView) v.findViewById(R.id.webview);
		status = (TextView) v.findViewById(R.id.statusView);
		//Bundle args = getArguments();
		//Log.d(TAG, "onViewCreated " + url + ", " + savedInstanceState + ", " + args);
		
		
        wv.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		wv.setScrollbarFadingEnabled(true);
		//wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		wv.setVerticalScrollBarEnabled(true);
		wv.setVerticalScrollbarOverlay(true);
		//wv.setMapTrackballToArrowKeys(false);
		//wv.setScrollIndicators(View.SCROLL_INDICATOR_RIGHT);
		
		final WebSettings webSettings = wv.getSettings();
		webSettings.setBuiltInZoomControls(true);
		//webSettings.setDefaultZoom(WebSettings.ZoomDensity.FAR);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSaveFormData(true);
		webSettings.setDefaultTextEncodingName("utf-8");
		webSettings.setAppCacheEnabled(false);
		webSettings.setDomStorageEnabled(false);
		webSettings.setSupportZoom(true);
		//webSettings.setLoadWithOverviewMode(false);
		//webSettings.setUseWideViewPort(true);
		updateColor(null);
		load2(savedInstanceState);
	}

	@Override
	public void load(final String path) {
		this.currentPathTitle = path;
		this.url = Uri.fromFile(new File(path)).toString();
		load2(null);
	}
	
	public void load2(final Bundle savedInstanceState) {
		final Bundle args = getArguments();
		Log.d(TAG, "onViewCreated " + currentPathTitle + ", " + args);
		if (savedInstanceState != null) {
			url = savedInstanceState.getString("url");
			//path = savedInstanceState.getString("path");
			//title = savedInstanceState.getString("title");
			Log.d(TAG, "onViewCreated.savedInstanceState " + currentPathTitle + ", " + currentPathTitle);
		} else if (args != null) {
			if (args.getString(ExplorerActivity.EXTRA_ABSOLUTE_PATH) != null) {
				currentPathTitle = args.getString(ExplorerActivity.EXTRA_ABSOLUTE_PATH);
				url = Uri.fromFile(new File(currentPathTitle)).toString();
			} else if (args.getString("url") != null) {
				url = args.getString("url");
			}
			title = args.getString("title");
			Log.d(TAG, "onViewCreated.arg " + currentPathTitle + ", " + url + ", " + args);
		}
		if (url != null) {
			status.setText(url);
			Log.d(TAG, "OriginalUrl " + wv.getOriginalUrl());
			if (url.equals(wv.getOriginalUrl())) {
				wv.reload();
			} else {
				wv.loadUrl(url);
			}
		}
	}
	
    @Override
	public void onSaveInstanceState(final Bundle outState) {
		//outState.putString("path", path);
		outState.putString("url", url);
		//outState.putString("title", title);
		Log.d(TAG, "onSaveInstanceState" + currentPathTitle + ", " + outState);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void updateColor(View rootView) {
		getView().setBackgroundColor(ExplorerActivity.BASE_BACKGROUND);
		status.setBackgroundColor(ExplorerActivity.BASE_BACKGROUND);
        status.setTextColor(ExplorerActivity.TEXT_COLOR);
        wv.setBackgroundColor(ExplorerActivity.BASE_BACKGROUND);
	}

}


