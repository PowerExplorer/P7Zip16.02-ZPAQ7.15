package net.gnu.explorer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import net.gnu.androidutil.AndroidUtils;

import net.gnu.util.Util;
import com.amaze.filemanager.utils.*;
import android.content.*;
import android.preference.*;
import android.content.pm.*;
import android.support.v4.app.*;
import android.widget.*;
import android.text.*;
import android.view.animation.*;
import java.util.*;
import android.widget.AdapterView.*;
import android.support.v7.view.menu.*;
import android.content.res.*;
import android.support.v4.widget.SwipeRefreshLayout;
import net.gnu.explorer.ProcessFragment.*;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.GridLayoutManager;
import android.view.inputmethod.InputMethodManager;
import com.amaze.filemanager.utils.files.Futils;

public class ProcessFragment extends FileFrag implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

	private static final String TAG = "ProcessFragment";

	static final String[] Q = new String[]{"B", "KB", "MB", "GB", "T", "P", "E"};

	private PackageManager pk;
	private ArrayList<RunningAppProcessInfo> display_process = new ArrayList<RunningAppProcessInfo>();

	
	private ProcessAdapter adapter;
	//HashSet<String> selectedInList1 = new HashSet<>();
	private Drawable apkDrawable;
	private Spinner processType;
	private SearchFileNameTask searchTask = new SearchFileNameTask();
	private List<ProcessInfo> lpinfo = new LinkedList<>();
	private List<ProcessInfo> prevInfo = new LinkedList<>();
	private LinkedList<String> killList = new LinkedList<>();
	private LoadProcessTask proTask = new LoadProcessTask();//save, 0, 0

	private final String[] appTypeArr = new String[] {
		"All",
		"System App",
		"Updated System App",
		"User App",
		"Internal",
		"External Asec",
		"Foreground",
		"Background",
		"Visible",
		"Perceptible",
		"Service",
		"Sleep",
		"Gone",
		"Empty"};
	//private final ArrayList<ProcessInfo> tempSelectedInList1 = new ArrayList<>();

	private int theme1;
	private boolean fake = false;

	private TextSearch textSearch = new TextSearch();
	private ProcessSorter processSorter;

	public ProcessFragment() {
		super();
		type = Frag.TYPE.PROCESS;
		title = "Process";
	}

	void clone(final ProcessFragment frag) {
		if (!fake) {
			selectedInList1 = frag.selectedInList1;
			lpinfo = frag.lpinfo;
			prevInfo = frag.prevInfo;
			searchMode = frag.searchMode;
			searchVal = frag.searchVal;
			adapter = frag.adapter;
			if (listView != null && listView.getAdapter() != adapter) {
				listView.setAdapter(adapter);
			}
			fake = true;
		}
	}

	public void refreshRecyclerViewLayoutManager() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		return inflater.inflate(R.layout.pager_item_process, container, false);
	}

	@Override
	public void onViewCreated(View v, Bundle savedInstanceState) {
		super.onViewCreated(v, savedInstanceState);
		Log.d(TAG, "onViewCreated");
//Toast.makeText(ProcessManager.this," allocated size  = " + getAsString(Debug.getNativeHeapAllocatedSize()), 1).show();      
		pk = activity.getPackageManager();
		apkDrawable = getResources().getDrawable(R.drawable.ic_doc_apk);

		allCbx.setOnClickListener(this);
		icons.setOnClickListener(this);
		allName.setOnClickListener(this);
		//allDate.setOnClickListener(this);
		allSize.setOnClickListener(this);
		allType.setOnClickListener(this);

		processType = (Spinner) v.findViewById(R.id.processType);
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
			activity, android.R.layout.simple_spinner_item, appTypeArr);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		processType.setAdapter(spinnerAdapter);
		processType.setOnItemSelectedListener(new ItemSelectedListener());
		processType.setSelection(3);

		
		clearButton.setOnClickListener(this);
		searchButton.setOnClickListener(this);
		searchET.addTextChangedListener(textSearch);
		mSwipeRefreshLayout.setOnRefreshListener(this);

		//listView.setFastScrollEnabled(true);
		//mylist.setOnTouchListener(this);
		listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		adapter = new ProcessAdapter(lpinfo);
		listView.setAdapter(adapter);
		spanCount = AndroidUtils.getSharedPreference(getContext(), "SPAN_COUNT.ProcessFrag", 1);
		gridLayoutManager = new GridLayoutManager(fragActivity, spanCount);
		listView.setLayoutManager(gridLayoutManager);
		if (spanCount <= 2) {
			dividerItemDecoration = new GridDividerItemDecoration(fragActivity, true);
			//dividerItemDecoration = new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST, true, true);
			listView.addItemDecoration(dividerItemDecoration);
		}
		listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					//Log.d(TAG, "onScroll firstVisibleItem=" + firstVisibleItem + ", visibleItemCount=" + visibleItemCount + ", totalItemCount=" + totalItemCount);
					if (System.currentTimeMillis() - lastScroll > 50) {//!mScaling && 
						if (dy > activity.density << 4 && selStatus.getVisibility() == View.VISIBLE) {
							selStatus.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_bottom));
							selStatus.setVisibility(View.GONE);
							horizontalDivider0.setVisibility(View.GONE);
							horizontalDivider12.setVisibility(View.GONE);
							status.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_bottom));
							status.setVisibility(View.GONE);
						} else if (dy < -activity.density << 4 && selStatus.getVisibility() == View.GONE) {
							selStatus.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
							selStatus.setVisibility(View.VISIBLE);
							horizontalDivider0.setVisibility(View.VISIBLE);
							horizontalDivider12.setVisibility(View.VISIBLE);
							status.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
							status.setVisibility(View.VISIBLE);
						}
						lastScroll = System.currentTimeMillis();
					}
				}});
		final Bundle args = getArguments();
		
		if (args != null) {
			title = args.getString("title");
		}

		if (savedInstanceState == null) {
			//title = savedInstanceState.getString("title");
			//type = ContentFactory.TYPE.values()[savedInstanceState.getInt("type")];
			//} else {
			loadlist(false);
		}

//		Sp = PreferenceManager.getDefaultSharedPreferences(getContext());
//		int theme = Sp.getInt("theme", 0);
//		theme1 = theme == 2 ? PreferenceUtils.hourOfDay() : theme;
		updateColor(null);
		final String order = AndroidUtils.getSharedPreference(activity, "ProcessSorter.order", "Name ▲");
		allName.setText("Name");
		allSize.setText("Size");
		allType.setText("Status");
		switch (order) {
			case "Name ▼":
				processSorter = new ProcessSorter(ProcessSorter.BY_LABEL, ProcessSorter.DESC);
				allName.setText("Name ▼");
				break;
			case "Size ▲":
				processSorter = new ProcessSorter(ProcessSorter.BY_SIZE, ProcessSorter.ASC);
				allSize.setText("Size ▲");
				break;
			case "Size ▼":
				processSorter = new ProcessSorter(ProcessSorter.BY_SIZE, ProcessSorter.DESC);
				allSize.setText("Size ▼");
				break;
			case "Status ▲":
				processSorter = new ProcessSorter(ProcessSorter.BY_STATUS, ProcessSorter.ASC);
				allType.setText("Status ▲");
				break;
			case "Status ▼":
				processSorter = new ProcessSorter(ProcessSorter.BY_STATUS, ProcessSorter.DESC);
				allType.setText("Status ▼");
				break;
			default:
				processSorter = new ProcessSorter(ProcessSorter.BY_LABEL, ProcessSorter.ASC);
				allName.setText("Name ▲");
				break;
		}
	}

//	@Override
//	public void onSaveInstanceState(android.os.Bundle outState) {
//        super.onSaveInstanceState(outState);
//		Log.d(TAG, "onSaveInstanceState" + title + ", " + outState);
//		outState.putString("title", title);
//    }

	@Override
    public void onRefresh() {
        final Editable s = searchET.getText();
		if (s.length() > 0) {
			textSearch.afterTextChanged(s);
		} else {
			loadlist(false);
		}
    }

	public void updateColor(View rootView) {
		getView().setBackgroundColor(ExplorerActivity.BASE_BACKGROUND);
		icons.setColorFilter(ExplorerActivity.TEXT_COLOR);
		allName.setTextColor(ExplorerActivity.TEXT_COLOR);
		//allDate.setTextColor(ExplorerActivity.TEXT_COLOR);
		allSize.setTextColor(ExplorerActivity.TEXT_COLOR);
		allType.setTextColor(ExplorerActivity.TEXT_COLOR);
		searchET.setTextColor(ExplorerActivity.TEXT_COLOR);
		clearButton.setColorFilter(ExplorerActivity.TEXT_COLOR);
		searchButton.setColorFilter(ExplorerActivity.TEXT_COLOR);
		diskStatus.setTextColor(ExplorerActivity.TEXT_COLOR);
		selectionStatus1.setTextColor(ExplorerActivity.TEXT_COLOR);

		if (ExplorerActivity.BASE_BACKGROUND < 0xff808080) {
			processType.setPopupBackgroundResource(R.drawable.textfield_black);
		} else {
			processType.setPopupBackgroundResource(R.drawable.textfield_default_old);
		}

		horizontalDivider0.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
		horizontalDivider12.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
		horizontalDivider7.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);

		adapter.notifyDataSetChanged();

	}

	private class LoadProcessTask extends AsyncTask<Void, Void, List<ProcessInfo>> {
//		private int index, top;
//		private boolean save;

//		public LoadProcessTask(boolean save, int top, int index) {
//			this.save = save;
//			this.index = index;
//			this.top = top;
//		}

		protected void onPreExecute() {
//			availMem_label.setText("calculating...");
//			selectionStatus1.setText("Listing Processes...");
			if (!mSwipeRefreshLayout.isRefreshing()) {
				mSwipeRefreshLayout.setRefreshing(true);
			}
		}

		@Override
		protected List<ProcessInfo> doInBackground(Void... params) {
			final List<ProcessInfo> tempInfo = new LinkedList<>();

			display_process.clear();
			display_process.addAll(((ActivityManager)getContext().getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses());

			ApplicationInfo appinfo = null;
			String label;
			for (RunningAppProcessInfo r : display_process) {
				try {
					appinfo = pk.getApplicationInfo(r.processName, 0);
					label = appinfo.loadLabel(pk) + "";
				} catch (NameNotFoundException e1) {
					//e1.printStackTrace();
					label = r.processName.substring(r.processName.lastIndexOf(".") + 1);
				}
				if (appinfo != null) {
					tempInfo.add(new ProcessInfo(r, label, r.processName, r.importance, r.pid, new File(appinfo.publicSourceDir).length(), appinfo));
				} else {
					tempInfo.add(new ProcessInfo(r, label, r.processName, r.importance, r.pid, 0, null));
				}
			}

			return tempInfo;
		}

		@Override
		protected void onPostExecute(List<ProcessInfo> tempInfo) {
			if (isCancelled()) {
				return;
			}
			Collections.sort(tempInfo, processSorter);
			lpinfo.clear();
			lpinfo.addAll(prevInfo);
			prevInfo.clear();
			prevInfo.addAll(tempInfo);
			adapter.notifyDataSetChanged();
			update_labels(getContext());
			//listView.setSelectionFromTop(index, top);
			for (String kSt : killList) {
				boolean exist = false;
				for (ProcessInfo pi : lpinfo) {
					if (kSt.equals(pi.packageName)) {
						exist = true;
						break;
					}
				}
				if (exist) {
					showToast(kSt + " cannot be killed");
				} else {
					showToast(kSt + " was killed");
				}
			}
			killList.clear();
			new ItemSelectedListener().onItemSelected(null, null, processType.getSelectedItemPosition(), 0);
			if (mSwipeRefreshLayout.isRefreshing()) {
				mSwipeRefreshLayout.setRefreshing(false);
			}
		}
	}

	public void loadlist(boolean save) {
		if (proTask.getStatus() == AsyncTask.Status.RUNNING 
			|| proTask.getStatus() == AsyncTask.Status.PENDING) {
			proTask.cancel(true);
		}
//		if (save) {
//			final int index = listView.getFirstVisiblePosition();
//			final View vi = listView.getChildAt(0);
//			final int top = (vi == null) ? 0 : vi.getTop();
//			proTask = new LoadProcessTask(save, top, index);
//		} else {
		proTask = new LoadProcessTask();//save, 0, 0
		//}
		proTask.execute();
	}

	public void manageUi(boolean search) {
		if (search == true) {
			searchET.setHint("Search ");
			searchButton.setImageResource(R.drawable.ic_arrow_back_grey600);
			//topflipper.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_bottom));
			topflipper.setDisplayedChild(topflipper.indexOfChild(quickLayout));
			searchMode = true;
			searchET.requestFocus();
			imm.showSoftInput(searchET, InputMethodManager.SHOW_IMPLICIT);
		} else {
			imm.hideSoftInputFromWindow(searchET.getWindowToken(), 0);
			searchET.setText("");
			searchButton.setImageResource(R.drawable.ic_action_search);
			//topflipper.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
			topflipper.setDisplayedChild(topflipper.indexOfChild(processType));
			searchMode = false;//curContentFrag.
			loadlist(false);//slideFrag.getCurrentFragment().
		}
	}

	private class ItemSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			AsyncTask.Status status;
			synchronized (searchTask) {
				if ((status = searchTask.getStatus()) == AsyncTask.Status.RUNNING
					|| status == AsyncTask.Status.PENDING) {
					searchTask.cancel(true);
				}
				searchTask = new SearchFileNameTask();
				searchTask.execute(position);
			}
		}
		public void onNothingSelected(AdapterView<?> parent) {
		}
	}

	private class TextSearch implements TextWatcher {
		public void beforeTextChanged(CharSequence s, int start, int end, int count) {
		}

		public void afterTextChanged(final Editable text) {
			final String filesearch = text.toString();
			Log.d("quicksearch", "filesearch " + filesearch);
			if (filesearch.length() > 0) {
				if (searchTask != null
					&& searchTask.getStatus() == AsyncTask.Status.RUNNING) {
					searchTask.cancel(true);
				}
				searchTask = new SearchFileNameTask();
				searchTask.execute(filesearch);
			}
		}

		public void onTextChanged(CharSequence s, int start, int end, int count) {
		}
	}

	private class SearchFileNameTask extends AsyncTask<Object, Long, List<ProcessInfo>> {
		protected void onPreExecute() {
			if (!mSwipeRefreshLayout.isRefreshing()) {
				mSwipeRefreshLayout.setRefreshing(true);
			}
		}

		@Override
		protected List<ProcessInfo> doInBackground(Object... params) {
			final List<ProcessInfo> templpinfo = new LinkedList<>();
			if (params[0] instanceof String) {
				searchMode = true;
				searchVal = searchET.getText().toString();
				final String param = (String)params[0];
				for (ProcessInfo pi : prevInfo) {
					if (pi.label.contains(param) || pi.packageName.contains(param)) {
						templpinfo.add(pi);
					}
				}
			} else {
				int sel = params[0];
				if (sel == 0) {
					templpinfo.addAll(prevInfo);
				} else {
					for (ProcessInfo pi : prevInfo) {
						if (sel == 1 && pi.isSystemApp) {
							templpinfo.add(pi);
						} else if (sel == 2 && pi.isUpdatedSystemApp) {
							templpinfo.add(pi);
						} else if (sel == 3 && !pi.isSystemApp) {
							templpinfo.add(pi);
						} else if (sel == 4 && pi.isInternal) {
							templpinfo.add(pi);
						} else if (sel == 5 && pi.isExternalAsec) {
							templpinfo.add(pi);
						} else if (sel == 6 && pi.status == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
							templpinfo.add(pi);
						} else if (sel == 7 && pi.status == ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
							templpinfo.add(pi);
						} else if (sel == 8 && pi.status == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
							templpinfo.add(pi);
						} else if (sel == 9 && pi.status == ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
							templpinfo.add(pi);
						} else if (sel == 10 && pi.status == ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
							templpinfo.add(pi);
						} else if (sel == 11 && pi.status == 150) {
							templpinfo.add(pi);
						} else if (sel == 12 && pi.status == ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE) {
							templpinfo.add(pi);
						} else if (sel == 13 && pi.status == ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY) {
							templpinfo.add(pi);
						} 
					}
				}
			}
			return templpinfo;
		}

		@Override
		protected void onPostExecute(List<ProcessInfo> templpinfo) {
			if (isCancelled()) {
				return;
			}
			Collections.sort(templpinfo, processSorter);
			lpinfo.clear();
			lpinfo.addAll(templpinfo);
			update_labels(getContext());
			adapter.notifyDataSetChanged();
			if (mSwipeRefreshLayout.isRefreshing()) {
				mSwipeRefreshLayout.setRefreshing(false);
			}
		}
	}

	public void mainmenu(final View v) {
		final PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.inflate(R.menu.panel_commands);
		final Menu menu = popup.getMenu();
		if (!activity.multiFiles) {
			menu.findItem(R.id.horizontalDivider5).setVisible(false);
		}
		MenuItem mi = menu.findItem(R.id.clearSelection);
		if (selectedInList1.size() == 0) {
			mi.setEnabled(false);
		} else {
			mi.setEnabled(true);
		}
		mi = menu.findItem(R.id.rangeSelection);
		if (selectedInList1.size() > 1) {
			mi.setEnabled(true);
		} else {
			mi.setEnabled(false);
		}
		mi = menu.findItem(R.id.undoClearSelection);
		if (tempSelectedInList1.size() > 0) {
			mi.setEnabled(true);
		} else {
			mi.setEnabled(false);
		}
        mi = menu.findItem(R.id.hide);
		if (activity.left.getVisibility() == View.VISIBLE) {
			mi.setTitle("Hide");
		} else {
			mi.setTitle("2 panels");
		}
        mi = menu.findItem(R.id.biggerequalpanel);
		if (activity.left.getVisibility() == View.GONE || activity.right.getVisibility() == View.GONE) {
			mi.setEnabled(false);
		} else {
			mi.setEnabled(true);
			if (slidingTabsFragment.width <= 0) {
				mi.setTitle("Wider panel");
			} else {
				mi.setTitle("2 panels equal");
			}
		}
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					switch (item.getItemId()) {
						case R.id.rangeSelection:
							int min = Integer.MAX_VALUE, max = -1;
							int cur = -3;
							for (String s : selectedInList1) {
								//cur = lpinfo.indexOf(s);
								int i = 0;
								for (ProcessInfo pi : lpinfo) {
									if (s.equals(pi.packageName)) {
										cur = i;
									} else {
										i++;
									}
								}
								if (cur > max) {
									max = cur;
								}
								if (cur < min && cur >= 0) {
									min = cur;
								}
							}
							selectedInList1.clear();
							for (cur = min; cur <= max; cur++) {
								selectedInList1.add(lpinfo.get(cur).packageName);
							}
							adapter.notifyDataSetChanged();
							break;
						case R.id.inversion:
							tempSelectedInList1.clear();
							for (ProcessInfo f : lpinfo) {
								if (!selectedInList1.contains(f.packageName)) {
									tempSelectedInList1.add(f);
								}
							}
							selectedInList1.clear();
							for (ProcessInfo f : tempSelectedInList1) {
								selectedInList1.add(f.packageName);
							}
							adapter.notifyDataSetChanged();
							break;
						case R.id.clearSelection:
							tempSelectedInList1.clear();
							String label;
							ApplicationInfo appinfo;
							for (RunningAppProcessInfo r : display_process) {
								appinfo = null;
								if (selectedInList1.contains(r.processName)) {
									try {
										appinfo = pk.getApplicationInfo(r.processName, 0);
										label = appinfo.loadLabel(pk) + "";
									} catch (NameNotFoundException e1) {	
										//e1.printStackTrace();
										label = r.processName.substring(r.processName.lastIndexOf(".") + 1);
									}
									if (appinfo != null) {
										tempSelectedInList1.add(new ProcessInfo(r, label, r.processName, r.importance, r.pid, new File(appinfo.publicSourceDir).length(), appinfo));
									} else {
										tempSelectedInList1.add(new ProcessInfo(r, label, r.processName, r.importance, r.pid, 0, null));
									}
								}
							}
							selectedInList1.clear();
							adapter.notifyDataSetChanged();
							break;
						case R.id.undoClearSelection:
							selectedInList1.clear();
							for (ProcessInfo f : tempSelectedInList1) {
								selectedInList1.add(f.packageName);
							}
							tempSelectedInList1.clear();
							adapter.notifyDataSetChanged();
							break;
						case R.id.swap:
//							ExplorerActivity.SPAN_COUNT = 3;
//							AndroidUtils.setSharedPreference(getContext(), "SPAN_COUNT", ExplorerActivity.SPAN_COUNT);
							activity.swap(v);
							break;
						case R.id.hide: 
							if (activity.right.getVisibility() == View.VISIBLE) {
								if (activity.swap) {
									activity.left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
									activity.right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
								} else {
									activity.left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_in_right));
									activity.right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_out_right));
								}
								activity.left.setVisibility(View.GONE);
							} else {
								if (activity.swap) {
									activity.left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
									activity.right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
								} else {
									activity.left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_in_right));
									activity.right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_out_right));
								}
								activity.right.setVisibility(View.VISIBLE);
							}
//							final FragmentManager supportFragManager = activity.getSupportFragmentManager();
//							final FragmentTransaction transaction = supportFragManager.beginTransaction();
//							transaction.setCustomAnimations(R.animator.fragment_slide_left_enter,
//															R.animator.fragment_slide_left_exit,
//															R.animator.fragment_slide_right_enter,
//															R.animator.fragment_slide_right_exit);
//							activity.leftCommands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
//							rightCommands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
//							if (!activity.slideFrag.isHidden()) {
//								if (ExplorerActivity.SPAN_COUNT == 3)
//									ExplorerActivity.SPAN_COUNT = 6;
//								else
//									ExplorerActivity.SPAN_COUNT = 2;
//								transaction.hide(activity.slideFrag2);
//								transaction.commit();
//								activity.slideFrag.updateLayout(false);
//								rightCommands.setVisibility(View.GONE);
//								horizontalDivider6.setVisibility(View.GONE);
//							} else {
//								if (ExplorerActivity.SPAN_COUNT == 6)
//									ExplorerActivity.SPAN_COUNT = 3;
//								else
//									ExplorerActivity.SPAN_COUNT = 2;
//								transaction.show(activity.slideFrag);
//								transaction.commit();
//								activity.slideFrag2.updateLayout(false);
//								activity.leftCommands.setVisibility(View.VISIBLE);
//								activity.horizontalDivider11.setVisibility(View.VISIBLE);
//							}
							//AndroidUtils.setSharedPreference(getContext(), "SPAN_COUNT", ExplorerActivity.SPAN_COUNT);
							break;
						case R.id.biggerequalpanel:
							if (activity.leftSize <= 0) {
								//mi.setTitle("Wider panel");
								LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)activity.left.getLayoutParams();
								params.weight = 2.0f;
								activity.left.setLayoutParams(params);
								params = (LinearLayout.LayoutParams)activity.right.getLayoutParams();
								params.weight = 1.0f;
								activity.right.setLayoutParams(params);
								activity.leftSize = 1;
								if (left == activity.left) {
									slidingTabsFragment.width = -1;
								} else {
									slidingTabsFragment.width = 1;
								}
							} else {
								LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)activity.left.getLayoutParams();
								params.weight = 1.0f;
								activity.left.setLayoutParams(params);
								params = (LinearLayout.LayoutParams)activity.right.getLayoutParams();
								params.weight = 1.0f;
								activity.right.setLayoutParams(params);
								activity.leftSize = 0;
								slidingTabsFragment.width = 0;
							}
							AndroidUtils.setSharedPreference(activity, "biggerequalpanel", activity.leftSize);
					}
					update_labels(getContext());
					return true;
				}
			});
		popup.show();
	}

	public void fromActivity(final View v) {
		//final Futils utils = utilsProvider.getFutils();
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.inflate(R.menu.process);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					switch (item.getItemId()) {
						case R.id.kill:
							killList.clear();
							for (String p : selectedInList1) {
								try {
									for (ProcessInfo pi : lpinfo) {
										if (pi.packageName.equals(p)) {
											AndroidUtils.killProcess(activity, pi.pid, p);
											killList.add(p);
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							loadlist(true);
							Toast.makeText(activity, selectedInList1.size() + " processes were killed !", Toast.LENGTH_SHORT).show();
							break;
						case R.id.share:
							ArrayList<File> arrayList2 = new ArrayList<File>();
							ArrayList<String> apkPath = AndroidUtils.getApkPath(activity);
							Collections.sort(apkPath);
							for (String pi : selectedInList1) {
								int binarySearch = Collections.binarySearch(apkPath, pi);
								if (binarySearch >= 0) {
									arrayList2.add(new File(apkPath.get(binarySearch)));
								}
							}
//							int color1 = Color.parseColor(PreferenceUtils
//														  .getAccentString(sharedPref));
//							new Futils().shareFiles(arrayList2, activity, theme1,
//													color1);
							new Futils().shareFiles(arrayList2, activity, activity.getAppTheme(), accentColor);
							break;
						case R.id.backup:
							Toast.makeText(
								getContext(),
								getResources().getString(R.string.copyingapk)
								+ AppsFragment.BACKUP_PATH, Toast.LENGTH_LONG).show();
							PackageManager pm = activity.getPackageManager();
							for (String pi : selectedInList1) {
								PackageInfo info = AndroidUtils.getPackageInfo(activity, pi);
								if (info != null) {
									ApplicationInfo applicationInfo = info.applicationInfo;
									if (applicationInfo != null) {
										AppsFragment.backup(applicationInfo.publicSourceDir, applicationInfo.loadLabel(pm) + "",
															info.versionName, ProcessFragment.this);
									} else {
										Toast.makeText(activity, pi + " cannot be accessed", Toast.LENGTH_SHORT).show();
									}
								} else {
									Toast.makeText(activity, pi + " cannot be accessed", Toast.LENGTH_SHORT).show();
								}
							}
							break;
						case R.id.unins:
							for (String pi : selectedInList1) {
								AndroidUtils.uninstall(activity, pi);
							}
					}
					return true;
				}
			});
		popup.show();
	}

	@Override
	public void onClick(View p1) {
		switch (p1.getId()) {
			case R.id.allCbx:
				final boolean all = !allCbx.isSelected();
				allCbx.setSelected(all);
				if (all) {
					allCbx.setImageResource(R.drawable.ic_accept);
				} else {
					allCbx.setImageResource(R.drawable.dot);
				}
				adapter.toggleChecked(all);
				update_labels(getContext());
				break;
			case R.id.allName:
				if (allName.getText().toString().equals("Name ▲")) {
					allName.setText("Name ▼");
					processSorter = new ProcessSorter(ProcessSorter.BY_LABEL, ProcessSorter.DESC);
					AndroidUtils.setSharedPreference(getContext(), "ProcessSorter.order", "Name ▼");
				} else {
					allName.setText("Name ▲");
					processSorter = new ProcessSorter(ProcessSorter.BY_LABEL, ProcessSorter.ASC);
					AndroidUtils.setSharedPreference(getContext(), "ProcessSorter.order", "Name ▲");
				}
				allSize.setText("Size");
				allType.setText("Status");
				Collections.sort(lpinfo, processSorter);
				adapter.notifyDataSetChanged();
				break;
			case R.id.allType:
				if (allType.getText().toString().equals("Status ▲")) {
					allType.setText("Status ▼");
					processSorter = new ProcessSorter(ProcessSorter.BY_STATUS, ProcessSorter.DESC);
					AndroidUtils.setSharedPreference(getContext(), "ProcessSorter.order", "Status ▼");
				} else {
					allType.setText("Status ▲");
					processSorter = new ProcessSorter(ProcessSorter.BY_STATUS, ProcessSorter.ASC);
					AndroidUtils.setSharedPreference(getContext(), "ProcessSorter.order", "Status ▲");
				}
				allName.setText("Name");
				allSize.setText("Size");
				Collections.sort(lpinfo, processSorter);
				adapter.notifyDataSetChanged();
				break;
			case R.id.allSize:
				if (allSize.getText().toString().equals("Size ▲")) {
					allSize.setText("Size ▼");
					processSorter = new ProcessSorter(ProcessSorter.BY_SIZE, ProcessSorter.DESC);
					AndroidUtils.setSharedPreference(getContext(), "ProcessSorter.order", "Size ▼");
				} else {
					allSize.setText("Size ▲");
					processSorter = new ProcessSorter(ProcessSorter.BY_SIZE, ProcessSorter.ASC);
					AndroidUtils.setSharedPreference(getContext(), "ProcessSorter.order", "Size ▲");
				}
				allName.setText("Name");
				allType.setText("Status");
				Collections.sort(lpinfo, processSorter);
				adapter.notifyDataSetChanged();
				break;
			case R.id.icons:
				mainmenu(p1);
				break;
			case R.id.search:
				searchMode = !searchMode;
				manageUi(searchMode);
				break;
			case R.id.clear:
				searchET.setText("");
				break;
		}
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "onOptionsItemSelected");
		return super.onOptionsItemSelected(item);
    }


	@Override
	public void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
		if (Build.VERSION.SDK_INT > 23) {
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
			intentFilter.addDataScheme("package");
			activity.registerReceiver(br, intentFilter);
//			if (pk == null) {
//				pk = getContext().getPackageManager();
//			}
		}
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		if (Build.VERSION.SDK_INT <= 23) {
			final IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
			intentFilter.addDataScheme("package");
			activity.registerReceiver(br, intentFilter);
//			if (pk == null) {
//				pk = getContext().getPackageManager();
//			}
		}
	}

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
		super.onPause();
		if (Build.VERSION.SDK_INT <= 23) {
			searchTask.cancel(true);
			proTask.cancel(true);
			activity.unregisterReceiver(br);
		}
    }

	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
		if (Build.VERSION.SDK_INT > 23) {
			searchTask.cancel(true);
			proTask.cancel(true);
			activity.unregisterReceiver(br);
		}
	}

	private final BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                loadlist(true);
            }
		}
    };

	private void update_labels(final Context context) {
		if (context != null) {
			final MemoryInfo mem_info = new ActivityManager.MemoryInfo();
			((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mem_info);
			diskStatus.setText(String.format("Available memory: %s B", Util.nf.format((mem_info.availMem)) + "/" + Util.nf.format(mem_info.totalMem)));
			//numProc_label.setText("Number of processes: " + display_process.size());
			selectionStatus1.setText(selectedInList1.size() + "/" + lpinfo.size() + "/" + display_process.size());
		}
	}

	private class ProcessAdapter extends RecyclerAdapter<ProcessInfo, ProcessAdapter.ViewHolder> implements OnClickListener, OnLongClickListener {

		public ProcessAdapter(List<ProcessInfo> lpinfo) {
			//loadProcess();
			super(lpinfo);//, R.layout.list_item_process, lpinfo);
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			View ll;
			TextView name;
			TextView items;
			TextView attr;
			//TextView lastModified;
			TextView type;
			ImageButton cbx;
			ImageView image;
			ImageButton more;

			ViewHolder(View convertView) {
				super(convertView);
				name = (TextView) convertView.findViewById(R.id.name);
				items = (TextView) convertView.findViewById(R.id.items);
				attr = (TextView) convertView.findViewById(R.id.attr);
				//lastModified = (TextView) convertView.findViewById(R.id.lastModified);
				type = (TextView) convertView.findViewById(R.id.type);
				cbx = (ImageButton) convertView.findViewById(R.id.cbx);
				image = (ImageView)convertView.findViewById(R.id.icon);
				more = (ImageButton)convertView.findViewById(R.id.more);
				convertView.setTag(this);
				ll = convertView;

				ll.setOnClickListener(ProcessAdapter.this);
				more.setOnClickListener(ProcessAdapter.this);
				cbx.setOnClickListener(ProcessAdapter.this);

				ll.setOnLongClickListener(ProcessAdapter.this);
				more.setOnLongClickListener(ProcessAdapter.this);
				cbx.setOnLongClickListener(ProcessAdapter.this);
			}
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_process, parent, false);
			// set the view's size, margins, paddings and layout parameters
			final ViewHolder vh = new ViewHolder(v);
			return vh;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {
			final ProcessInfo pi = lpinfo.get(position);

			holder.cbx.setTag(pi);
			holder.more.setTag(pi);

			holder.more.setColorFilter(ExplorerActivity.TEXT_COLOR);
			holder.name.setTextColor(ExplorerActivity.DIR_COLOR);
			holder.items.setTextColor(ExplorerActivity.TEXT_COLOR);
			holder.attr.setTextColor(ExplorerActivity.TEXT_COLOR);
			//holder.lastModified.setTextColor(ExplorerActivity.TEXT_COLOR);
			holder.type.setTextColor(ExplorerActivity.TEXT_COLOR);

			holder.name.setText(pi.label);
			//holder.lastModified.setText(pi.pid + "");
			holder.attr.setText(pi.packageName);
			holder.items.setText(Util.nf.format(pi.size) + " B");

			int importance = pi.status;
			if (importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
				holder.type.setText("Foreground");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE) {
				holder.type.setText("Foreground Service");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
				holder.type.setText("Background");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
				holder.type.setText("Visible");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE) {
				holder.type.setText("Perceptible");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_SERVICE) {
				holder.type.setText("Service");
			} else if (importance == 150) {//}RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING) {
				holder.type.setText("Sleep");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_GONE) {
				holder.type.setText("Gone");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE) {
				holder.type.setText("Can't save state");
			} else if (importance == RunningAppProcessInfo.IMPORTANCE_EMPTY) {
				holder.type.setText("Empty");
			}

			try {
				holder.image.setImageDrawable(pk.getApplicationIcon(pi.packageName));
			} catch (NameNotFoundException e) {
				holder.image.setImageResource(R.drawable.ic_doc_apk);
			}

			boolean checked = selectedInList1.contains(pi.packageName);
			if (checked) {
				holder.ll.setBackgroundColor(ExplorerActivity.IN_DATA_SOURCE_2);
				holder.cbx.setSelected(true);
				holder.cbx.setImageResource(R.drawable.ic_accept);
			} else if (selectedInList1.size() > 0) {
				holder.ll.setBackgroundColor(ExplorerActivity.BASE_BACKGROUND);
				holder.cbx.setSelected(false);
				holder.cbx.setImageResource(R.drawable.ready);
			} else {
				holder.ll.setBackgroundResource(R.drawable.ripple);
				holder.cbx.setSelected(false);
				holder.cbx.setImageResource(R.drawable.dot);
			}
		}
		
		public void toggleChecked(boolean checked, ProcessInfo packageInfo) {
			if (checked) {
				selectedInList1.add(packageInfo.packageName);
			} else {
				selectedInList1.remove(packageInfo.packageName);
			}
			notifyDataSetChanged();
			update_labels(getContext());
			final boolean all = selectedInList1.size() == display_process.size();
			allCbx.setSelected(all);
			if (all) {
				allCbx.setImageResource(R.drawable.ic_accept);
			} else if (selectedInList1.size() > 0) {
				allCbx.setImageResource(R.drawable.ready);
			} else {
				allCbx.setImageResource(R.drawable.dot);
			}
			if (selectedInList1.size() > 0) {
				if (commands.getVisibility() == View.GONE) {
					commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_bottom));
					commands.setVisibility(View.VISIBLE);
					horizontalDivider6.setVisibility(View.VISIBLE);
				}
			} else if (commands.getVisibility() == View.VISIBLE) {
				horizontalDivider6.setVisibility(View.GONE);
				commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
				commands.setVisibility(View.GONE);
			}
		}

		public void toggleChecked(boolean b) {
			if (b) {
				selectedInList1.clear();
				for (RunningAppProcessInfo r : display_process) {
					selectedInList1.add(r.processName);
				}
				if (selectedInList1.size() > 0 && commands.getVisibility() == View.GONE) {
					commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_bottom));
					commands.setVisibility(View.VISIBLE);
					horizontalDivider6.setVisibility(View.VISIBLE);
				}
			} else {
				selectedInList1.clear();
				if (commands.getVisibility() == View.VISIBLE) {
					horizontalDivider6.setVisibility(View.GONE);
					commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
					commands.setVisibility(View.GONE);
				}
			}
			notifyDataSetChanged();
		}

		@Override
		public boolean onLongClick(final View p1) {
			final Object tag = p1.getTag();
			if (tag instanceof ProcessInfo) {
				ProcessInfo tag2 = (ProcessInfo) tag;
				toggleChecked(!selectedInList1.contains(tag2.packageName), tag2);
			} else {
				ProcessInfo tag2 = (ProcessInfo) ((ViewHolder) tag).cbx.getTag();
				toggleChecked(!selectedInList1.contains(tag2.packageName), tag2);
			}
			return false;
		}

		@Override
		public void onClick(final View view) {
			Log.d(TAG, view.getTag() + ".");
			if (selectedInList1.size() > 0) {
				onLongClick(view);
				return;
			}
			if (view.getId() == R.id.cbx) {
				view.setSelected(!view.isSelected());
				toggleChecked(view.isSelected(), (ProcessInfo) view.getTag());
			} else if (view.getId() == R.id.more) {
				final ProcessInfo pinfo = (ProcessInfo) view.getTag();
				final MenuBuilder menuBuilder = new MenuBuilder(activity);
				final MenuInflater inflater = new MenuInflater(activity);
				inflater.inflate(R.menu.process, menuBuilder);
				final MenuPopupHelper optionsMenu = new MenuPopupHelper(activity , menuBuilder, allSize);
				optionsMenu.setForceShowIcon(true);
				menuBuilder.setCallback(new MenuBuilder.Callback() {
						@Override
						public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
							switch (item.getItemId()) {
								case R.id.kill:
									try {
										AndroidUtils.killProcess(ProcessFragment.this.getContext(), pinfo.pid, pinfo.packageName);
										killList.clear();
										killList.add(pinfo.packageName);
									} catch (Exception e) {
										Toast.makeText(ProcessFragment.this.getContext(), "couldn't kill the process ", Toast.LENGTH_SHORT).show();
									}
									//Toast.makeText(ProcessFragment.this.getContext(), pinfo.label + " was killed !", Toast.LENGTH_SHORT).show();
									loadlist(true);
									break;
								case R.id.open:
									Intent i = pk.getLaunchIntentForPackage(pinfo.packageName);
									if (i != null)
										startActivity(i);
									else
										Toast.makeText(ProcessFragment.this.getContext(), "Could not launch", Toast.LENGTH_SHORT).show();
									break;
								case R.id.backup:
									final PackageInfo info = AndroidUtils.getPackageInfo(activity, pinfo.packageName);
									if (info != null) {
										ApplicationInfo applicationInfo = info.applicationInfo;
										if (applicationInfo != null) {
											AppsFragment.backup(applicationInfo.publicSourceDir, applicationInfo.loadLabel(activity.getPackageManager()) + "",
																info.versionName, ProcessFragment.this);
										} else {
											Toast.makeText(activity, pinfo.packageName + " cannot be accessed", Toast.LENGTH_SHORT).show();
										}
									} else {
										Toast.makeText(activity, pinfo.packageName + " cannot be accessed", Toast.LENGTH_SHORT).show();
									}
									break;
								case R.id.unins:
									try {
										Intent uninstall_intent= new Intent(Intent.ACTION_DELETE);
										uninstall_intent.setData(Uri.parse("package:" + pinfo.packageName));
										startActivity(uninstall_intent);
									} catch (Exception e) {
										Toast.makeText(ProcessFragment.this.getContext(), "Can't Uninstall" , Toast.LENGTH_SHORT).show();
									}
									break;
								case R.id.info:
									//Toast.makeText(ProcessManager.this, "Process : "+display_process.get(position).processName +" lru : " +display_process.get(position).lru + " Pid :  " +display_process.get(position).pid, Toast.LENGTH_SHORT).show();	
									final AlertDialog alert1 = new AlertDialog.Builder(ProcessFragment.this.getContext()).create();
									alert1.setTitle("Process Info");
									alert1.setIcon(AndroidUtils.getProcessIcon(pk, pinfo.packageName, apkDrawable));
									alert1.setMessage("Process : " + pinfo.packageName + " \nlru : " + pinfo.runningAppProcessInfo.lru + "\nPid : " + pinfo.pid);
									alert1.show();
									break;
								case R.id.detail:
									final int apiLevel = Build.VERSION.SDK_INT;
									Intent intent = new Intent();
									if (apiLevel >= 9) {
										startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
																 Uri.parse("package:" + pinfo.packageName)));
									} else {
										final String appPkgName = (apiLevel == 8 ? "pkg" : "com.android.settings.ApplicationPkgName");
										intent.setAction(Intent.ACTION_VIEW);
										intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
										intent.putExtra(appPkgName, pinfo.packageName);
										startActivity(intent);
									}
									break;
								case R.id.play:
									Intent intent1 = new Intent(Intent.ACTION_VIEW);
									intent1.setData(Uri.parse("market://details?id=" + pinfo.packageName));
									activity.startActivity(intent1);
									break;
								case R.id.share:
									ArrayList<File> arrayList2 = new ArrayList<File>();
									arrayList2.add(new File(pinfo.path));
									//int color1 = Color.parseColor(PreferenceUtils.getAccentString(sharedPref));
									//new Futils().shareFiles(arrayList2, activity, theme1, color1);
									new Futils().shareFiles(arrayList2, activity, activity.getAppTheme(), accentColor);
									break;
							}
							return true;
						}
						@Override
						public void onMenuModeChange(MenuBuilder menu) {}
					});
				optionsMenu.show();
			} else {
				final int apiLevel = Build.VERSION.SDK_INT;
				Intent intent = new Intent();
				final ProcessInfo pinfo = (ProcessInfo) ((ViewHolder) view.getTag()).cbx.getTag();
				if (apiLevel >= 9) {
					startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
											 Uri.parse("package:" + pinfo.packageName)));
				} else {
					final String appPkgName = (apiLevel == 8 ? "pkg" : "com.android.settings.ApplicationPkgName");
					intent.setAction(Intent.ACTION_VIEW);
					intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
					intent.putExtra(appPkgName, pinfo.packageName);
					startActivity(intent);
				}
			}
		}

	}
}

