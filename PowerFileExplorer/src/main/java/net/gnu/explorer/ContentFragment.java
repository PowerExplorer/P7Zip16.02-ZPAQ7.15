package net.gnu.explorer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.*;
import android.widget.*;
import android.os.*;
import android.graphics.drawable.*;
import android.content.*;
import android.view.View.*;
import android.util.*;
import java.io.*;
import android.text.*;
import android.graphics.*;
import android.app.*;
import android.view.*;
import android.net.*;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentManager;
import net.gnu.util.*;
import net.gnu.androidutil.*;
import net.gnu.explorer.R;
import com.amaze.filemanager.ui.icons.*;
import com.amaze.filemanager.utils.*;
import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.activities.*;
import com.amaze.filemanager.services.asynctasks.*;
import android.preference.*;
import com.tekinarslan.sample.*;
import net.gnu.texteditor.*;
import android.support.v4.view.*;

import android.view.animation.*;
import android.widget.LinearLayout.*;
import android.support.v7.view.menu.*;
import android.widget.ImageView.*;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.*;
import android.content.res.*;
import java.lang.ref.*;
import android.support.v4.widget.SwipeRefreshLayout;
import net.gnu.explorer.ContentFragment.*;
import net.dongliu.apk.parser.*;
import net.dongliu.apk.parser.bean.*;
import com.amaze.filemanager.ui.LayoutElement;
import android.view.inputmethod.InputMethodManager;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbException;
import jcifs.smb.SmbAuthException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.amaze.filemanager.exceptions.CloudPluginException;
import android.database.Cursor;
import android.provider.MediaStore;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.exceptions.RootNotPermittedException;
import com.amaze.filemanager.fragments.CloudSheetFragment;
import com.amaze.filemanager.database.models.EncryptedEntry;
import com.amaze.filemanager.database.CryptHandler;
import com.amaze.filemanager.fragments.preference_fragments.Preffrag;
import com.amaze.filemanager.services.EncryptService;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.filesystem.MediaStoreHack;
import com.amaze.filemanager.utils.SmbStreamer.Streamer;
import android.media.RingtoneManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.net.MalformedURLException;
import com.amaze.filemanager.database.CloudHandler;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.v4.content.ContextCompat.checkSelfPermission;
import java.util.regex.Pattern;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.amaze.filemanager.utils.files.CryptUtil;
import com.amaze.filemanager.database.UtilsHandler;
import com.amaze.filemanager.ui.dialogs.GeneralDialogCreation;

public class ContentFragment extends FileFrag implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ContentFragment";
	
	private static final int REQUEST_CODE_STORAGE_PERMISSION = 101;
    	
	private ScaleGestureDetector mScaleGestureDetector;
	private ImageButton dirMore;
	private TextView mMessageView;
	
	private SearchFileNameTask searchTask = new SearchFileNameTask();
	private TextSearch textSearch = new TextSearch();
	
	private HorizontalScrollView scrolltext;
	private LinearLayout mDirectoryButtons;
	private ImageButton removeBtn;
	private ImageButton removeAllBtn;
	private ImageButton addBtn;
	private ImageButton addAllBtn;
	private LinearLayout selectionCommands;
	private LoadFiles loadList = new LoadFiles();
	private int file_count, folder_count, columns;
	private int sortby, dsort, asc;
    private String smbPath;
	private boolean mRetainSearchTask = false;
	private FileListSorter fileListSorter;
	private LinkedList<Map<String, Object>> backStack = new LinkedList<>();
	private LinkedList<String> history = new LinkedList<>();
	private FileObserver mFileObserver;
	private Drawable drawableDelete;
	private Drawable drawablePaste;
	String dirTemp4Search = "";
	public boolean selection, results = false, SHOW_HIDDEN, CIRCULAR_IMAGES, SHOW_PERMISSIONS, SHOW_SIZE, SHOW_LAST_MODIFIED;
	String suffix = "*"; // "*" : files + folders,  "" only folder, ".*" only file "; *" split pattern
	String mimes = "*/*";
	boolean multiFiles = true;
	
	boolean mWriteableOnly;
	
	String[] previousSelectedStr;
	
	//int totalCount, progress;
	private boolean noMedia = false;
	private boolean displayHidden = true;
	private Pattern suffixPattern;
	
	@Override
	public String toString() {
		return "type " + type + ", fake=" + fake + ", suffix=" + suffix + ", mimes=" + mimes + ", multi=" + multiFiles + ", " + currentPathTitle + ", " + super.toString();
	}

	public static ContentFragment newInstance(final SlidingTabsFragment s, final String dir, final String suffix, final String mimes, final boolean multiFiles, Bundle bundle) {//, int se) {//FragmentActivity ctx, 
        //Log.d(TAG, "newInstance dir " + dir + ", suffix " + suffix + ", multiFiles " + multiFiles);

		if (bundle == null) {
			bundle = new Bundle();
		}
		bundle.putString(ExplorerActivity.EXTRA_ABSOLUTE_PATH, dir);//EXTRA_DIR_PATH
		bundle.putString(ExplorerActivity.EXTRA_FILTER_FILETYPE, suffix);
		bundle.putBoolean(ExplorerActivity.EXTRA_MULTI_SELECT, multiFiles);

		final ContentFragment fragment = new ContentFragment();
		fragment.setArguments(bundle);
		fragment.currentPathTitle = dir;
		fragment.suffix = suffix.trim().toLowerCase();
		fragment.suffix = fragment.suffix == null ? "*" : fragment.suffix.toLowerCase();
		String suffixSpliter = fragment.suffix.replaceAll("[;\\s\\*\\.\\\\b]+", "|");
		suffixSpliter = suffixSpliter.startsWith("|") ? suffixSpliter.substring(1) : suffixSpliter;
		suffixSpliter = ".*?(" + suffixSpliter + ")";
		fragment.suffixPattern = Pattern.compile(suffixSpliter);

		fragment.mimes = mimes;
		fragment.mimes = fragment.mimes == null ? "*/*" : fragment.mimes.toLowerCase();
		fragment.multiFiles = multiFiles;
		fragment.slidingTabsFragment = s;
        Log.d(TAG, "newInstance " + fragment);
		return fragment;
    }

	@Override
	public void load(String path) {
		changeDir(path, false);
	}

	@Override
	public Frag clone(boolean fake) {
		final ContentFragment frag = new ContentFragment();
		frag.clone(this, fake);
		return frag;
	}

	@Override
	public void clone(final Frag frag2, final boolean fake) {
		Log.d(TAG, "clone " + frag2 + ", listView " + listView + ", srcAdapter " + srcAdapter + ", gridLayoutManager " + gridLayoutManager);
		final ContentFragment frag = (ContentFragment) frag2;
		type = frag.type;
		currentPathTitle = frag.currentPathTitle;
		
		suffix = frag.suffix;
		String suffixSpliter = suffix.replaceAll("[;\\s\\*\\.\\\\b]+", "|");
		suffixSpliter = suffixSpliter.startsWith("|") ? suffixSpliter.substring(1) : suffixSpliter;
		suffixSpliter = ".*?(" + suffixSpliter + ")";
		suffixPattern = Pattern.compile(suffixSpliter);
		
		mimes = frag.mimes;
		multiFiles = frag.multiFiles;
		slidingTabsFragment = frag.slidingTabsFragment;
		this.fake = fake;
		if (fake) {
			dataSourceL1 = frag.dataSourceL1;
			selectedInList1 = frag.selectedInList1;
			tempSelectedInList1 = frag.tempSelectedInList1;
			tempOriDataSourceL1 = frag.tempOriDataSourceL1;
		} else {
			dataSourceL1.clear();
			dataSourceL1.addAll(frag.dataSourceL1);
			selectedInList1.clear();
			selectedInList1.addAll(frag.selectedInList1);
			tempSelectedInList1.clear();
			tempSelectedInList1.addAll(frag.tempSelectedInList1);
			tempOriDataSourceL1.clear();
			tempOriDataSourceL1.addAll(frag.tempOriDataSourceL1);
		}
		spanCount = frag.spanCount;
		dataSourceL2 = frag.dataSourceL2;
		tempPreviewL2 = frag.tempPreviewL2;
		searchMode = frag.searchMode;
		searchVal = frag.searchVal;
		dirTemp4Search = frag.dirTemp4Search;
		srcAdapter = frag.srcAdapter;
		if (listView != null && listView.getAdapter() != srcAdapter) {
			listView.setAdapter(srcAdapter);
		}

		if (allCbx != null) {
			if (dataSourceL1.size() == 0) {
				nofilelayout.setVisibility(View.VISIBLE);
				mSwipeRefreshLayout.setVisibility(View.GONE);
			} else {
				nofilelayout.setVisibility(View.GONE);
				mSwipeRefreshLayout.setVisibility(View.VISIBLE);
			}
			if (type == Frag.TYPE.EXPLORER) {
				setDirectoryButtons();
			}
			allName.setText(frag.allName.getText());
			allType.setText(frag.allType.getText());
			allDate.setText(frag.allDate.getText());
			allSize.setText(frag.allSize.getText());
			final int size = selectedInList1.size();
			//Log.d(TAG, "clone " + type + ", size " + size + ", dataSourceL1.size() " + dataSourceL1.size());
			if (size > 0) {
				if (size == dataSourceL1.size()) {
					allCbx.setImageResource(R.drawable.ic_accept);
				} else {
					allCbx.setImageResource(R.drawable.ready);
				}
			} else {
				allCbx.setImageResource(R.drawable.dot);
			}
			
			if (gridLayoutManager == null || gridLayoutManager.getSpanCount() != spanCount) {
				listView.removeItemDecoration(dividerItemDecoration);
				listView.invalidateItemDecorations();
				gridLayoutManager = new GridLayoutManager(fragActivity, spanCount);
				listView.setLayoutManager(gridLayoutManager);
			}
			final int index = frag.gridLayoutManager.findFirstVisibleItemPosition();
			final View vi = frag.listView.getChildAt(0); 
			final int top = (vi == null) ? 0 : vi.getTop();
			gridLayoutManager.scrollToPositionWithOffset(index, top);
			if (frag.selStatus != null) {
				final int visibility = frag.selStatus.getVisibility();
				if (selStatus.getVisibility() != visibility) {
					selStatus.setVisibility(visibility);
					horizontalDivider0.setVisibility(visibility);
					horizontalDivider12.setVisibility(visibility);
					status.setVisibility(visibility);
				}
				selectionStatus1.setText(frag.selectionStatus1.getText());
				diskStatus.setText(frag.diskStatus.getText());
			}
		}
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
        //Log.d(TAG, "onCreateView fake=" + fake + ", dir=" + dir + ", " + savedInstanceState);
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.pager_item, container, false);
    }

	@Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated " + toString() + ", savedInstanceState=" + savedInstanceState);
		super.onViewCreated(v, savedInstanceState);
		
        final Bundle args = getArguments();

		final int fragIndex;
		final String order;
		if (type == Frag.TYPE.EXPLORER) {
			fragIndex = slidingTabsFragment.indexOfMTabs(this);
			if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) {
				order = AndroidUtils.getSharedPreference(activity, "ContentFragSortType" + fragIndex, "Name ▲");
				spanCount = AndroidUtils.getSharedPreference(activity, "ContentFrag.SPAN_COUNT" + fragIndex, 1);
			} else {
				order = AndroidUtils.getSharedPreference(activity, "ExplorerFragSortType" + fragIndex, "Name ▲");
				spanCount = AndroidUtils.getSharedPreference(activity, "ExplorerFrag.SPAN_COUNT" + fragIndex, 1);
			} 
		} else {
			fragIndex = -1;
			if (slidingTabsFragment.side == SlidingTabsFragment.Side.RIGHT) {
				order = AndroidUtils.getSharedPreference(activity, "ContentFrag2SortTypeR", "Name ▲");
				spanCount = AndroidUtils.getSharedPreference(activity, "ContentFrag2.SPAN_COUNTR", 1);
			} else {
				order = AndroidUtils.getSharedPreference(activity, "ContentFrag2SortTypeL", "Name ▲");
				spanCount = AndroidUtils.getSharedPreference(activity, "ContentFrag2.SPAN_COUNTL", 1);
			}
		}
		Log.d(TAG, "onViewCreated index " + fragIndex + ", " + toString() + ", " + "args=" + args);
		//Log.d(TAG, "sharedPreference " + fragIndex + ", " + order);
		
		SHOW_HIDDEN = sharedPref.getBoolean("showHidden", true);

		scrolltext = (HorizontalScrollView) v.findViewById(R.id.scroll_text);
		mDirectoryButtons = (LinearLayout) v.findViewById(R.id.directory_buttons);
		dirMore = (ImageButton) v.findViewById(R.id.dirMore);
		drawableDelete = activity.getDrawable(R.drawable.ic_delete_white_36dp);
		drawablePaste = activity.getDrawable(R.drawable.ic_content_paste_white_36dp);

		if (type == Frag.TYPE.SELECTION) {
			removeBtn = (ImageButton) v.findViewById(R.id.remove);
			removeAllBtn = (ImageButton) v.findViewById(R.id.removeAll);
			addBtn = (ImageButton) v.findViewById(R.id.add);
			addAllBtn = (ImageButton) v.findViewById(R.id.addAll);
			selectionCommands = (LinearLayout) v.findViewById(R.id.selectionCommands);
			topflipper.setDisplayedChild(topflipper.indexOfChild(selectionCommands));
			dirMore.setVisibility(View.GONE);
			if (dataSourceL1.size() == 0) {
				searchButton.setEnabled(false);
				nofilelayout.setVisibility(View.VISIBLE);
				mSwipeRefreshLayout.setVisibility(View.GONE);
			} else {
				searchButton.setEnabled(true);
				nofilelayout.setVisibility(View.GONE);
				mSwipeRefreshLayout.setVisibility(View.VISIBLE);
			}
			removeBtn.setOnClickListener(this);
			removeAllBtn.setOnClickListener(this);
			addBtn.setOnClickListener(this);
			addAllBtn.setOnClickListener(this);
		} else {
			dirMore.setOnClickListener(this);
			topflipper.setDisplayedChild(topflipper.indexOfChild(scrolltext));
		}

		allCbx.setOnClickListener(this);
		icons.setOnClickListener(this);
		allName.setOnClickListener(this);
		allDate.setOnClickListener(this);
		allSize.setOnClickListener(this);
		allType.setOnClickListener(this);

		listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
					//Log.d(TAG, "onScrolled dx=" + dx + ", dy=" + dy + ", density=" + activity.density);
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
				}
			});
		listView.setHasFixedSize(true);
		listView.setItemViewCacheSize(20);
		listView.setDrawingCacheEnabled(true);
		listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		
        DefaultItemAnimator animator = new DefaultItemAnimator();
		animator.setAddDuration(500);
		//animator.setRemoveDuration(500);
        listView.setItemAnimator(animator);

		//scrolltext = (HorizontalScrollView) v.findViewById(R.id.scroll_text);
		//mDirectoryButtons = (LinearLayout) v.findViewById(R.id.directory_buttons);
		//diskStatus = (TextView) v.findViewById(R.id.diskStatus);

		clearButton.setOnClickListener(this);
		searchButton.setOnClickListener(this);

		searchET.addTextChangedListener(textSearch);
		mSwipeRefreshLayout.setOnRefreshListener(this);
		mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {

				@Override
                public boolean onScale(ScaleGestureDetector detector) {
                    Log.d(TAG, "onScale getCurrentSpan " + detector.getCurrentSpan() + ", getPreviousSpan " + detector.getPreviousSpan() + ", getTimeDelta " + detector.getTimeDelta());
					//if (detector.getCurrentSpan() > 300 && detector.getTimeDelta() > 50) {
					//Log.d(TAG, "onScale " + (detector.getCurrentSpan() - detector.getPreviousSpan()) + ", getTimeDelta " + detector.getTimeDelta());
					//mScaling = true;
					mSwipeRefreshLayout.setEnabled(false);
					if (detector.getCurrentSpan() - detector.getPreviousSpan() < -80 * activity.density) {
						if (spanCount == 1) {
							spanCount = 2;
							setRecyclerViewLayoutManager();
							mSwipeRefreshLayout.setEnabled(true);
							return true;
						} else if (spanCount == 2 && slidingTabsFragment.width >= 0) {
							if (right.getVisibility() == View.GONE || left.getVisibility() == View.GONE) {
								spanCount = 8;
							} else {
								spanCount = 4;
							}
							setRecyclerViewLayoutManager();
							mSwipeRefreshLayout.setEnabled(true);
							return true;
						}
					} else if (detector.getCurrentSpan() - detector.getPreviousSpan() > 80 * activity.density) {
						if ((spanCount == 4 || spanCount == 8)) {
							spanCount = 2;
							setRecyclerViewLayoutManager();
							mSwipeRefreshLayout.setEnabled(true);
							return true;
						} else if (spanCount == 2) {
							spanCount = 1;
							setRecyclerViewLayoutManager();
							mSwipeRefreshLayout.setEnabled(true);
							return true;
						} 
					}
                    //}
                    //mScaling = false;
					mSwipeRefreshLayout.setEnabled(true);
					return false;
                }
            });

		listView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                   	//Log.d(TAG, "onTouch " + event);
					select(true);
//					if (type == -1) {
//						activity.slideFrag2.getCurrentFragment().select(false);
//					} else {
//						activity.slideFrag2.getCurrentFragment().select(true);
//					}
					mScaleGestureDetector.onTouchEvent(event);
                    return false;
                }
            });

		if (args != null) {
			if (currentPathTitle == null) {//"".equals(currentPathTitle) || 
				currentPathTitle = args.getString(ExplorerActivity.EXTRA_ABSOLUTE_PATH);//EXTRA_DIR_PATH);
			} else {
				args.putString(ExplorerActivity.EXTRA_ABSOLUTE_PATH, currentPathTitle);//EXTRA_DIR_PATH
			}
			//Log.d(TAG, "onViewCreated.dir " + dir);
			suffix = args.getString(ExplorerActivity.EXTRA_FILTER_FILETYPE, "*").trim().toLowerCase();
			String suffixSpliter = suffix.replaceAll("[;\\s\\*\\.\\\\b]+", "|");
			suffixSpliter = suffixSpliter.startsWith("|") ? suffixSpliter.substring(1) : suffixSpliter;
			suffixSpliter = ".*?(" + suffixSpliter + ")";
			suffixPattern = Pattern.compile(suffixSpliter);
			
			mimes = args.getString(ExplorerActivity.EXTRA_FILTER_MIMETYPE);
			mimes = mimes == null ? "*/*" : mimes.toLowerCase();
			//Log.d(TAG, "onViewCreated.suffix " + suffix);
			multiFiles = args.getBoolean(ExplorerActivity.EXTRA_MULTI_SELECT);
			//Log.d(TAG, "onViewCreated.multiFiles " + multiFiles);
			if (savedInstanceState == null && args.getStringArrayList("dataSourceL1") != null) {
				savedInstanceState = args;
			}

			if (!multiFiles) {
				allCbx.setVisibility(View.GONE);
			}
        }

		allName.setText("Name");
		allSize.setText("Size");
		allDate.setText("Date");
		allType.setText("Type");
		switch (order) {
			case "Name ▼":
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.NAME, FileListSorter.DESCENDING);
				allName.setText("Name ▼");
				break;
			case "Date ▲":
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.DATE, FileListSorter.ASCENDING);
				allDate.setText("Date ▲");
				break;
			case "Date ▼":
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.DATE, FileListSorter.DESCENDING);
				allDate.setText("Date ▼");
				break;
			case "Size ▲":
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.SIZE, FileListSorter.ASCENDING);
				allSize.setText("Size ▲");
				break;
			case "Size ▼":
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.SIZE, FileListSorter.DESCENDING);
				allSize.setText("Size ▼");
				break;
			case "Type ▲":
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.TYPE, FileListSorter.ASCENDING);
				allType.setText("Type ▲");
				break;
			case "Type ▼":
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.TYPE, FileListSorter.DESCENDING);
				allType.setText("Type ▼");
				break;
			default:
				fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.NAME, FileListSorter.ASCENDING);
				allName.setText("Name ▲");
				break;
		}

		//Log.d(TAG, "onViewCreated " + this + ", ctx=" + getContext());
		if (savedInstanceState != null && savedInstanceState.getString(ExplorerActivity.EXTRA_ABSOLUTE_PATH) != null) {//EXTRA_DIR_PATH
			currentPathTitle = savedInstanceState.getString(ExplorerActivity.EXTRA_ABSOLUTE_PATH);//EXTRA_DIR_PATH
			suffix = savedInstanceState.getString(ExplorerActivity.EXTRA_FILTER_FILETYPE, "*");
			mimes = savedInstanceState.getString(ExplorerActivity.EXTRA_FILTER_MIMETYPE, "*/*");
			multiFiles = savedInstanceState.getBoolean(ExplorerActivity.EXTRA_MULTI_SELECT, true);

			allCbx.setEnabled(savedInstanceState.getBoolean("allCbx.isEnabled"));
			setRecyclerViewLayoutManager();
			Log.d(TAG, "configurationChanged " + activity.configurationChanged);
			if (type == Frag.TYPE.EXPLORER && !fake) {// && !activity.configurationChanged
				//updateDir(currentPathTitle, ContentFragment.this);
				setDirectoryButtons();
			}
			final int index  = savedInstanceState.getInt("index");
			final int top  = savedInstanceState.getInt("top");
			Log.d(TAG, "index = " + index + ", " + top);
			gridLayoutManager.scrollToPositionWithOffset(index, top);
		} else {
			//srcAdapter = new ArrAdapter(dataSourceL1);
			//listView1.setAdapter(srcAdapter);
			setRecyclerViewLayoutManager();
			if (type == Frag.TYPE.EXPLORER && !fake) {
				changeDir(currentPathTitle, false);
			}
		}
		updateColor(v);
	}

	@Override
    public void onRefresh() {
        final Editable s = searchET.getText();
		if (s.length() > 0) {
			textSearch.afterTextChanged(s);
		} else if (type == Frag.TYPE.EXPLORER) {
        	changeDir(currentPathTitle, false);
		} else {
			LayoutElement f;
			boolean changed = false;
        	for (int i = dataSourceL1.size() - 1; i >= 0; i--) {
				f = dataSourceL1.get(i);
				if (!f.bf.f.exists()) {
					changed = true;
					dataSourceL1.remove(i);
					selectedInList1.remove(f);
					tempOriDataSourceL1.remove(f);
					tempSelectedInList1.remove(f);
					if (tempPreviewL2 != null && f.path.equals(tempPreviewL2.path)) {
						tempPreviewL2 = null;
					}
				}
			}
			if (changed) {
				srcAdapter.notifyDataSetChanged();
				selectionStatus1.setText(selectedInList1.size()  + "/" + dataSourceL1.size());
			}
			if (mSwipeRefreshLayout.isRefreshing()) {
				mSwipeRefreshLayout.setRefreshing(false);
			}
		}
    }

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		//Log.d(TAG, "onSaveInstanceState " + indexOf + ", fake=" + fake + ", " + currentPathTitle + ", " + outState);
		if (fake) {
			return;
		}
		if (type == Frag.TYPE.EXPLORER) {
			final int fragIndex = slidingTabsFragment.indexOfMTabs(this);
			if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) {
				AndroidUtils.setSharedPreference(activity, "ContentFrag.SPAN_COUNT" + fragIndex, spanCount);
			} else {
				AndroidUtils.setSharedPreference(activity, "ExplorerFrag.SPAN_COUNT" + fragIndex, spanCount);
			} 
		} else {
			if (slidingTabsFragment.side == SlidingTabsFragment.Side.RIGHT) {
				AndroidUtils.setSharedPreference(activity, "ContentFrag2.SPAN_COUNTR", spanCount);
			} else {
				AndroidUtils.setSharedPreference(activity, "ContentFrag2.SPAN_COUNTL", spanCount);
			}
		}
		//Log.d(TAG, "SPAN_COUNT.ContentFrag" + activity.slideFrag.indexOf(this));
		
//		if (tempPreviewL2 != null) {
//			outState.putString("tempPreviewL2", tempPreviewL2.path);
//		}
		outState.putString(ExplorerActivity.EXTRA_ABSOLUTE_PATH, currentPathTitle);//EXTRA_DIR_PATH
		outState.putString(ExplorerActivity.EXTRA_FILTER_FILETYPE, suffix);
		outState.putString(ExplorerActivity.EXTRA_FILTER_MIMETYPE, mimes);
		outState.putBoolean(ExplorerActivity.EXTRA_MULTI_SELECT, multiFiles);
//		outState.putStringArrayList("selectedInList1", Util.collectionFile2StringArrayList(selectedInList1));
//		outState.putStringArrayList("dataSourceL1", Util.collectionFile2StringArrayList(dataSourceL1));
//		outState.putBoolean("searchMode", searchMode);
//		outState.putString("searchVal", quicksearch.getText().toString());
//		outState.putString("dirTemp4Search", dirTemp4Search);
		outState.putBoolean("allCbx.isEnabled", allCbx.isEnabled());

		final int index = gridLayoutManager.findFirstVisibleItemPosition();
        final View vi = listView.getChildAt(0); 
        final int top = (vi == null) ? 0 : vi.getTop();
		outState.putInt("index", index);
		outState.putInt("top", top);
		//Log.d(TAG, "onSaveInstanceState index = " + index + ", " + top);
		super.onSaveInstanceState(outState);
	}

//	@Override
//	public void onViewStateRestored(Bundle savedInstanceState) {
//		//Log.d(TAG, "onViewStateRestored " + savedInstanceState);
//		if (imageLoader == null) {
//			activity = (ExplorerActivity)getActivity();
//			imageLoader = new ImageThreadLoader(activity);
//		}
//		super.onViewStateRestored(savedInstanceState);
//	}

	Map<String, Object> onSaveInstanceState() {
		Map<String, Object> outState = new TreeMap<>();
		//Log.d(TAG, "Map onSaveInstanceState " + dir + ", " + outState);
		outState.put(ExplorerActivity.EXTRA_ABSOLUTE_PATH, currentPathTitle);//EXTRA_DIR_PATH
		outState.put(ExplorerActivity.EXTRA_FILTER_FILETYPE, suffix);
		outState.put(ExplorerActivity.EXTRA_FILTER_MIMETYPE, mimes);
		outState.put(ExplorerActivity.EXTRA_MULTI_SELECT, multiFiles);
		outState.put("selectedInList1", selectedInList1);
		outState.put("dataSourceL1", dataSourceL1);
		outState.put("searchMode", searchMode);
		outState.put("searchVal", searchET.getText().toString());
		outState.put("dirTemp4Search", dirTemp4Search);
		outState.put("allCbx.isEnabled", allCbx.isEnabled());

		final int index = gridLayoutManager.findFirstVisibleItemPosition();
        final View vi = listView.getChildAt(0); 
        final int top = (vi == null) ? 0 : vi.getTop();
		outState.put("index", index);
		outState.put("top", top);

        return outState;
	}

	void reload(Map<String, Object> savedInstanceState) {
		Log.d(TAG, "reload " + savedInstanceState + currentPathTitle);
		currentPathTitle = (String) savedInstanceState.get(ExplorerActivity.EXTRA_ABSOLUTE_PATH);//EXTRA_DIR_PATH
		suffix = (String) savedInstanceState.get(ExplorerActivity.EXTRA_FILTER_FILETYPE);
		mimes = (String) savedInstanceState.get(ExplorerActivity.EXTRA_FILTER_MIMETYPE);
		multiFiles = savedInstanceState.get(ExplorerActivity.EXTRA_MULTI_SELECT);
		selectedInList1.clear();
		selectedInList1.addAll((ArrayList<LayoutElement>) savedInstanceState.get("selectedInList1"));
		dataSourceL1.clear();
		dataSourceL1.addAll((ArrayList<LayoutElement>) savedInstanceState.get("dataSourceL1"));
		if (type == Frag.TYPE.SELECTION) {
			tempOriDataSourceL1.clear();
			tempOriDataSourceL1.addAll(dataSourceL1);
		}
		searchMode = savedInstanceState.get("searchMode");
		searchVal = (String) savedInstanceState.get("searchVal");
		dirTemp4Search = (String) savedInstanceState.get("dirTemp4Search");
		//listView1.setSelectionFromTop(savedInstanceState.getInt("index"),
		//savedInstanceState.getInt("top"));
		allCbx.setEnabled(savedInstanceState.get("allCbx.isEnabled"));
		srcAdapter.notifyDataSetChanged();

		setRecyclerViewLayoutManager();
		gridLayoutManager.scrollToPositionWithOffset(savedInstanceState.get("index"), savedInstanceState.get("top"));

		updateDir(currentPathTitle, ContentFragment.this);
	}

	@Override
    public void onPause() {
        //Log.d(TAG, "onPause " + toString());
		super.onPause();
		if (imageLoader != null) {
			imageLoader.stopThread();
		}
		//loadList.cancel(true);
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

	private boolean hasPermissions() {
        return checkSelfPermission(getActivity(), WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        //setLoading(true);
        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
    }

    /**
     * Switch to permission request mode.
     */
    private void showPermissionDenied() {
        //setLoading(false);
        Toast.makeText(getActivity(), R.string.details_permissions, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_STORAGE_PERMISSION:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    refresh();
                } else {
                    showPermissionDenied();
                }
                break;
        }
    }
	
	public void refresh() {
        if (hasPermissions()) {
            // Cancel and GC previous scanner so that it doesn't load on top of the
            // new list.
            // Race condition seen if a long list is requested, and a short list is
            // requested before the long one loads.
//            mScanner.cancel();
//            mScanner = null;

            // Indicate loading and start scanning.
//            setLoading(true);
//            renewScanner().start();
        } else {
            requestPermissions();
        }
    }

	public void updateList() {
		Log.d(TAG, "updateList " + currentPathTitle + ", " + this);
		if (type == Frag.TYPE.EXPLORER) {
			if (currentPathTitle != null) {
				changeDir(currentPathTitle, false);
			} else {
				updateDir(dirTemp4Search, this);
			}
		}
    }

	void setDirectoryButtons() {
		Log.d(TAG, "setDirectoryButtons " + type + ", " + currentPathTitle);
		//topflipper.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));

		if (currentPathTitle != null) {
			mDirectoryButtons.removeAllViews();
			String[] parts = currentPathTitle.split("/");

			final TextView ib = new TextView(activity);
			final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
			layoutParams.gravity = Gravity.CENTER;
			ib.setLayoutParams(layoutParams);
			ib.setBackgroundResource(R.drawable.ripple);
			ib.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			ib.setText("/");
			ib.setTag("/");
			ib.setMinEms(2);
			ib.setPadding(0, 4, 0, 4);
			ib.setTextColor(ExplorerActivity.TEXT_COLOR);
			// ib.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
			ib.setGravity(Gravity.CENTER);
			ib.setOnClickListener(new View.OnClickListener() {
					public void onClick(View view) {
						changeDir("/", true);

					}
				});
			mDirectoryButtons.addView(ib);

			String folder = "";
			View v;
			TextView b = null;
			for (int i = 1; i < parts.length; i++) {
				folder += "/" + parts[i];
				v = activity.getLayoutInflater().inflate(R.layout.dir, null);
				b = (TextView) v.findViewById(R.id.name);
				b.setText(parts[i]);
				b.setTag(folder);
				b.setTextColor(ExplorerActivity.TEXT_COLOR);
				b.setOnClickListener(new View.OnClickListener() {
						public void onClick(View view) {
							String dir2 = (String) view.getTag();
							changeDir(dir2, true);

						}
					});
				mDirectoryButtons.addView(v);
				scrolltext.postDelayed(new Runnable() {
						public void run() {
							scrolltext.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
						}
					}, 100L);
			}
			AndroidUtils.setOnTouchListener(mDirectoryButtons, this);
			if (b != null) {
				b.setOnLongClickListener(new OnLongClickListener() {
						@Override
						public boolean onLongClick(View p1) {
							final EditText editText = new EditText(activity);
							final CharSequence clipboardData = AndroidUtils.getClipboardData(activity);
							if (clipboardData.length() > 0 && clipboardData.charAt(0) == '/') {
								editText.setText(clipboardData);
							} else {
								editText.setText(currentPathTitle);
							}
							final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT);
							layoutParams.gravity = Gravity.CENTER;
							editText.setLayoutParams(layoutParams);
							editText.setSingleLine(true);
							editText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
							editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
							editText.setMinEms(2);
							//editText.setGravity(Gravity.CENTER);
							final int density = 8 * (int)getResources().getDisplayMetrics().density;
							editText.setPadding(density, density, density, density);

							AlertDialog dialog = new AlertDialog.Builder(activity)
								.setIconAttribute(android.R.attr.dialogIcon)
								.setTitle("Go to...")
								.setView(editText)
								.setPositiveButton(R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										String name = editText.getText().toString();
										Log.d(TAG, "new " + name);
										File newF = new File(name);
										if (newF.exists()) {
											if (newF.isDirectory()) {
												currentPathTitle = name;
												changeDir(currentPathTitle, true);
											} else {
												currentPathTitle = newF.getParent();
												changeDir(newF.getParentFile().getAbsolutePath(), true);
											}
											dialog.dismiss();
										} else {
											showToast("\"" + newF + "\" does not exist. Please choose another name");
										}
									}
								})
								.setNegativeButton(R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int whichButton) {
										dialog.dismiss();
									}
								}).create();
							dialog.show();
							return true;
						}
					});
			}
		}
	}

	public void updateDir(String d, FileFrag cf) {//ExploreFragment
		Log.d(TAG, "updateDir " + d + ", " + cf);
		if (openMode != OpenMode.CUSTOM) {
			setDirectoryButtons();
			activity.dir = d;
		}
		
		if (cf == activity.slideFrag.getCurrentFragment()) {
			activity.curContentFrag = (ContentFragment) cf;
			activity.slideFrag.notifyTitleChange();
		} else if (activity.slideFrag2 != null && cf == activity.slideFrag2.getCurrentFragment()) {
			//title = new File(d).getName();
			activity.curExplorerFrag = (ContentFragment) cf;
			activity.slideFrag2.notifyTitleChange();
		}
	}

    /**
     * Sets up a FileObserver to watch the current directory.
     */
	FileObserver createFileObserver(String path) {
        return new FileObserver(path, FileObserver.CREATE | FileObserver.DELETE
								| FileObserver.MOVED_FROM | FileObserver.MOVED_TO
								| FileObserver.DELETE_SELF | FileObserver.MOVE_SELF
								| FileObserver.CLOSE_WRITE) {
            @Override
            public void onEvent(int event, String path) {
                if (path != null) {
                    Util.debug(TAG, "FileObserver received event %d, CREATE = 256;DELETE = 512;DELETE_SELF = 1024;MODIFY = 2;MOVED_FROM = 64;MOVED_TO = 128; path %s", event, path);
					activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateList();
							}
						});
                }
            }
        };
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume index " + activity.slideFrag.indexOfMTabs(this) + ", " + /*activity.slideFrag2.indexOfMTabs(this) + ", " + */ slidingTabsFragment.side + ", " + type + ", fake=" + fake + ", " + currentPathTitle + ", dirTemp4Search=" + dirTemp4Search);
		super.onResume();
		if (type == Frag.TYPE.EXPLORER) {
			getView().setBackgroundColor(ExplorerActivity.BASE_BACKGROUND);
			if (mFileObserver != null) {
				mFileObserver.stopWatching();
			}
			if (currentPathTitle == null) {
				mFileObserver = createFileObserver(dirTemp4Search);
			} else {
				mFileObserver = createFileObserver(currentPathTitle);
			}
			mFileObserver.startWatching();
			activity = (ExplorerActivity)getActivity();

			selectionStatus1.setText(selectedInList1.size() 
									 + "/" + dataSourceL1.size());

			final File curDir = new File(currentPathTitle == null ? dirTemp4Search : currentPathTitle);
			diskStatus.setText(
				"Free " + Util.nf.format(curDir.getFreeSpace() / (1 << 20))
				+ " MiB. Used " + Util.nf.format((curDir.getTotalSpace() - curDir.getFreeSpace()) / (1 << 20))
				+ " MiB. Total " + Util.nf.format(curDir.getTotalSpace() / (1 << 20)) + " MiB");
		} else {
			activity.curContentFrag.dataSourceL2 = dataSourceL1;
			if (activity.curContentFrag.srcAdapter != null) {
				activity.curContentFrag.srcAdapter.notifyDataSetChanged();
			}
			updateColor(null);
		}
	}

	public void updateColor(View rootView) {
		getView().setBackgroundColor(ExplorerActivity.BASE_BACKGROUND);
		icons.setColorFilter(ExplorerActivity.TEXT_COLOR);
		allName.setTextColor(ExplorerActivity.TEXT_COLOR);
		allDate.setTextColor(ExplorerActivity.TEXT_COLOR);
		allSize.setTextColor(ExplorerActivity.TEXT_COLOR);
		allType.setTextColor(ExplorerActivity.TEXT_COLOR);
		selectionStatus1.setTextColor(ExplorerActivity.TEXT_COLOR);
		diskStatus.setTextColor(ExplorerActivity.TEXT_COLOR);
		searchET.setTextColor(ExplorerActivity.TEXT_COLOR);
		clearButton.setColorFilter(ExplorerActivity.TEXT_COLOR);
		searchButton.setColorFilter(ExplorerActivity.TEXT_COLOR);
		noFileImage.setColorFilter(ExplorerActivity.TEXT_COLOR);
		noFileText.setTextColor(ExplorerActivity.TEXT_COLOR);
		dirMore.setColorFilter(ExplorerActivity.TEXT_COLOR);
		horizontalDivider0.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
		horizontalDivider12.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
		horizontalDivider7.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
		if (type == Frag.TYPE.SELECTION) {
			addBtn.setColorFilter(ExplorerActivity.TEXT_COLOR);
			addAllBtn.setColorFilter(ExplorerActivity.TEXT_COLOR);
			removeBtn.setColorFilter(ExplorerActivity.TEXT_COLOR);
			removeAllBtn.setColorFilter(ExplorerActivity.TEXT_COLOR);
		}
	}

	void updateL2() {
		Collections.sort(dataSourceL1, fileListSorter);
		updateTemp();
	}

	void updateTemp() {
		tempOriDataSourceL1.clear();
		tempOriDataSourceL1.addAll(dataSourceL1);
	}

	void removeAllDS2(final Collection<LayoutElement> c) {
		dataSourceL1.removeAll(c);
		tempOriDataSourceL1.removeAll(c);
	}

	void clearDS2() {
		dataSourceL1.clear();
		tempOriDataSourceL1.clear();
	}

	@Override
    public String getTitle() {
		Log.d(TAG, "openMode " + openMode + ", " + currentPathTitle + ", " + openMode.equals(openMode.CUSTOM) + ", getTitle() " + this);
		if (type == Frag.TYPE.EXPLORER) {
			if (currentPathTitle == null) {
				return "";
			} else if ("/".equals(currentPathTitle)) {
				return "/";
			} else if (openMode.equals(openMode.CUSTOM)) {
				String path = null;
				switch (Integer.parseInt(currentPathTitle)) {
					case 0:
						path = "Images";
						break;
					case 1:
						path = "Videos";
						break;
					case 2:
						path = "Audio";
						break;
					case 3:
						path = "Docs";
						break;
					case 4:
						path = "Apk";
						break;
					case 5:
						path = "Recent";
						break;
					case 6:
						path = "Recent Files";
						break;
				}
				return path;
			} else {
				return new File(currentPathTitle).getName();
			}
		} else {
			return title;
		}
	}

	
	public ArrayList<LayoutElement> addToSmb(SmbFile[] mFile, String path) throws SmbException {
        ArrayList<LayoutElement> a = new ArrayList<>(mFile.length);
        //if (searchHelper.size() > 500) searchHelper.clear();
        for (SmbFile aMFile : mFile) {
            if (dataUtils.getHiddenfiles().contains(aMFile.getPath()))
                continue;
            String name = aMFile.getName();
            name = (aMFile.isDirectory() && name.endsWith("/")) ? name.substring(0, name.length() - 1) : name;
            if (path.equals(smbPath)) {
                if (name.endsWith("$")) continue;
            }
            if (aMFile.isDirectory()) {
                folder_count++;
//                LayoutElement layoutElement = new LayoutElement(folder, name, aMFile.getPath(),
//																"", "", "", 0, false, aMFile.lastModified() + "", true);
                LayoutElement layoutElement = new LayoutElement(name, aMFile.getPath(),
																 "", "", /*"", */aMFile.length(), /*false, */aMFile.lastModified(), true);
				layoutElement.setMode(OpenMode.SMB);
                //searchHelper.add(layoutElement.generateBaseFile());
                a.add(layoutElement);
            } else {
                file_count++;
                try {
//                    LayoutElement layoutElement = new LayoutElement(
//						Icons.loadMimeIcon(aMFile.getPath(), !IS_LIST, res), name,
//						aMFile.getPath(), "", "", Formatter.formatFileSize(getContext(),
//																		   aMFile.length()), aMFile.length(), false,
//						aMFile.lastModified() + "", false);
                    LayoutElement layoutElement = new LayoutElement(
						//Icons.loadMimeIcon(mFile[i].getPath(), !IS_LIST, res), 
						name,
						aMFile.getPath(), "", "", //Formatter.formatFileSize(getContext(), mFile[i].length()), 
						aMFile.length(), //false,
						aMFile.lastModified(), false);
						layoutElement.setMode(OpenMode.SMB);
                    //searchHelper.add(layoutElement.generateBaseFile());
                    a.add(layoutElement);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return a;
    }
	
    public void reauthenticateSmb() {
        if (smbPath != null) {
            try {
                activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							int i;
							if ((i = dataUtils.containsServer(smbPath)) != -1) {
								activity.showSMBDialog(dataUtils.getServers().get(i)[0], smbPath, true);
							}
						}
					});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	public void updateDelPaste() {
		if (selectedInList1.size() > 0 && deletePastesBtn.getCompoundDrawables()[1] != drawableDelete) {
			deletePastesBtn.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
			deletePastesBtn.setCompoundDrawablesWithIntrinsicBounds(null, drawableDelete, null, null);
			deletePastesBtn.setText("Delete");
		} else if (selectedInList1.size() == 0 && deletePastesBtn.getCompoundDrawables()[1] != drawablePaste) {
			deletePastesBtn.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
			deletePastesBtn.setCompoundDrawablesWithIntrinsicBounds(null, drawablePaste, null, null);
			deletePastesBtn.setText("Paste");
		}
		deletePastesBtn.getCompoundDrawables()[1].setColorFilter(ExplorerActivity.TEXT_COLOR, PorterDuff.Mode.SRC_IN);
	}


	public void loadlist(String path, /*boolean back, */OpenMode openMode) {
//        if (mActionMode != null) {
//            mActionMode.finish();
//        }
        /*if(openMode==-1 && android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches())
		 bindDrive(path);
		 else */
//        if (loadList != null) loadList.cancel(true);
//        loadList = new LoadFiles();//LoadList(activity, activity, /*back, */this, openMode);
//        loadList.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (path));
		changeDir(path, true);
		this.openMode = openMode;
    }

	private void addFiles(final ContentFragment curContentFrag, final ContentFragment curSelectionFrag2) {
		curContentFrag.dataSourceL2 = curSelectionFrag2.dataSourceL1;
		if (curContentFrag.selectedInList1.size() > 0) {
			if (multiFiles) {
				String st2;
				//File file;
				int size;
				for (LayoutElement file : curContentFrag.selectedInList1) {
					//file = new File(st);
					if (file.isDirectory) {
						size = curSelectionFrag2.dataSourceL1.size();
						st2 = file.path + "/";
						for (int i = 0; i >= 0 && i < size; i++) {
							if (curSelectionFrag2.dataSourceL1.get(i).path.startsWith(st2)) {
								curSelectionFrag2.dataSourceL1.remove(i);
								curSelectionFrag2.tempOriDataSourceL1.remove(i);
								i--;
								size--;
							}
						}
					}
					if (!curSelectionFrag2.dataSourceL1.contains(file)
						&& file.bf.exists()) {
						curSelectionFrag2.dataSourceL1.add(file);
						curSelectionFrag2.tempOriDataSourceL1.add(file);
					}
				}
				boolean allInclude = true;

				//final String dirSt = dir.endsWith("/") ? dir : dir + "/";
				for (LayoutElement st : curContentFrag.dataSourceL1) {
					if (!curSelectionFrag2.dataSourceL1.contains(st)) {
						allInclude = false;
						break;
					}
				}
				if (allInclude) {
					curContentFrag.setAllCbxChecked(true);// allCbx.setChecked(true);
					curContentFrag.allCbx.setEnabled(false);// allCbx.setEnabled(false);
				}
			} else {
				curSelectionFrag2.dataSourceL1.clear();
				curSelectionFrag2.dataSourceL1.addAll(curContentFrag.selectedInList1);
				curSelectionFrag2.tempOriDataSourceL1.clear();
				curSelectionFrag2.tempOriDataSourceL1.addAll(curContentFrag.selectedInList1);
			}
			curSelectionFrag2.updateL2();
			curSelectionFrag2.allCbx.setSelected(false);
			if (curSelectionFrag2.selectedInList1.size() == 0) {
				curSelectionFrag2.allCbx.setImageResource(R.drawable.dot);
			} else {
				curSelectionFrag2.allCbx.setImageResource(R.drawable.ready);
			}
			curSelectionFrag2.allCbx.setEnabled(true);
			curContentFrag.selectedInList1.clear();
			if (curSelectionFrag2.dataSourceL1.size() == 0) {
				curSelectionFrag2.searchButton.setEnabled(false);
				curSelectionFrag2.nofilelayout.setVisibility(View.VISIBLE);
				curSelectionFrag2.mSwipeRefreshLayout.setVisibility(View.GONE);
			} else {
				if (curSelectionFrag2.gridLayoutManager == null || curSelectionFrag2.gridLayoutManager.getSpanCount() != curSelectionFrag2.spanCount) {
					curSelectionFrag2.gridLayoutManager = new GridLayoutManager(activity, curSelectionFrag2.spanCount);
					curSelectionFrag2.listView.setLayoutManager(curSelectionFrag2.gridLayoutManager);
				}
				curSelectionFrag2.listView.removeItemDecoration(curSelectionFrag2.dividerItemDecoration);
				curSelectionFrag2.listView.invalidateItemDecorations();
				if (curSelectionFrag2.spanCount <= 2) {
					curSelectionFrag2.dividerItemDecoration = new GridDividerItemDecoration(activity, true);
					curSelectionFrag2.listView.addItemDecoration(curSelectionFrag2.dividerItemDecoration);
				}
				curSelectionFrag2.searchButton.setEnabled(true);
				curSelectionFrag2.nofilelayout.setVisibility(View.GONE);
				curSelectionFrag2.mSwipeRefreshLayout.setVisibility(View.VISIBLE);
			}
			curContentFrag.notifyDataSetChanged();// srcAdapter.notifyDataSetChanged();
			curSelectionFrag2.notifyDataSetChanged();// destAdapter.notifyDataSetChanged();
			curContentFrag.selectionStatus1
				.setText(curContentFrag.selectedInList1.size() + "/"
						 + curContentFrag.dataSourceL1.size());
			curSelectionFrag2.selectionStatus1
				.setText(curSelectionFrag2.selectedInList1.size() + "/"
						 + curSelectionFrag2.dataSourceL1.size());
		}
	}

	private void addAllFiles(final ContentFragment curContentFrag, final ContentFragment curSelectionFrag2) {
		curContentFrag.dataSourceL2 = curSelectionFrag2.dataSourceL1;
		final String dirSt = activity.dir.endsWith("/") ? activity.dir : activity.dir + "/";
		Log.d(TAG, "addAllFiles " + dirSt);
		if (multiFiles) {
			String st3;
			//File file;
			int size;
			for (LayoutElement file : curContentFrag.dataSourceL1) {
				if (file.isDirectory) {
					st3 = file.path + "/"; 
					size = curSelectionFrag2.dataSourceL1.size();
					for (int i = 0; i >= 0 && i < size; i++) {
						if (curSelectionFrag2.dataSourceL1.get(i).path.startsWith(st3)) {
							curSelectionFrag2.dataSourceL1.remove(i);
							curSelectionFrag2.tempOriDataSourceL1.remove(i);
							i--;
							size--;
						}
					}
				}
				if (!curSelectionFrag2.dataSourceL1.contains(file)
					&& file.bf.exists() && file.bf.f.canRead()) {
					curSelectionFrag2.dataSourceL1.add(file);
					curSelectionFrag2.tempOriDataSourceL1.add(file);
				}
			}

			curContentFrag.setAllCbxChecked(true);// allCbx.setChecked(true);
			curContentFrag.selectedInList1.clear();
			curSelectionFrag2.updateL2();
			curContentFrag.notifyDataSetChanged();// srcAdapter.notifyDataSetChanged();
			curSelectionFrag2.notifyDataSetChanged();// destAdapter.notifyDataSetChanged();
			curContentFrag.allCbx.setEnabled(false);// allCbx.setEnabled(false);
			curSelectionFrag2.allCbx.setSelected(false);

			if (curSelectionFrag2.dataSourceL1.size() == 0) {
				curSelectionFrag2.searchButton.setEnabled(false);
				curSelectionFrag2.nofilelayout.setVisibility(View.VISIBLE);
				curSelectionFrag2.mSwipeRefreshLayout.setVisibility(View.GONE);
			} else {
				if (curSelectionFrag2.gridLayoutManager == null || curSelectionFrag2.gridLayoutManager.getSpanCount() != curSelectionFrag2.spanCount) {
					curSelectionFrag2.gridLayoutManager = new GridLayoutManager(activity, curSelectionFrag2.spanCount);
					curSelectionFrag2.listView.setLayoutManager(curSelectionFrag2.gridLayoutManager);
				}
				curSelectionFrag2.listView.removeItemDecoration(curSelectionFrag2.dividerItemDecoration);
				curSelectionFrag2.listView.invalidateItemDecorations();
				if (curSelectionFrag2.spanCount <= 2) {
					curSelectionFrag2.dividerItemDecoration = new GridDividerItemDecoration(activity, true);
					curSelectionFrag2.listView.addItemDecoration(curSelectionFrag2.dividerItemDecoration);
				}
				curSelectionFrag2.searchButton.setEnabled(true);
				curSelectionFrag2.nofilelayout.setVisibility(View.GONE);
				curSelectionFrag2.mSwipeRefreshLayout.setVisibility(View.VISIBLE);
			}
			curSelectionFrag2.allCbx.setEnabled(true);
			curContentFrag.selectionStatus1
				.setText(curContentFrag.dataSourceL1.size() + "/"
						 + curContentFrag.dataSourceL1.size());
			curSelectionFrag2.selectionStatus1
				.setText(curSelectionFrag2.selectedInList1.size() + "/"
						 + curSelectionFrag2.dataSourceL1.size());
		} else {
			LayoutElement file = curContentFrag.dataSourceL1.get(0);//new File(dirSt, curContentFrag.dataSourceL1.get(0));
			if (curContentFrag.dataSourceL1.size() == 1 && file.bf.exists()
				&& !file.isDirectory) {
				curSelectionFrag2.dataSourceL1.clear();
				curSelectionFrag2.dataSourceL1.add(curContentFrag.dataSourceL1.get(0));

				curSelectionFrag2.tempOriDataSourceL1.clear();
				curSelectionFrag2.tempOriDataSourceL1.add(curContentFrag.dataSourceL1.get(0));

				curContentFrag.selectedInList1.clear();
				curContentFrag.notifyDataSetChanged();// srcAdapter.notifyDataSetChanged();
				curSelectionFrag2.notifyDataSetChanged();// destAdapter.notifyDataSetChanged();
			}
		}
	}

	private void removeAllFiles(final ContentFragment curContentFrag, final ContentFragment curSelectionFrag2) {
		curContentFrag.dataSourceL2 = curSelectionFrag2.dataSourceL1;
		//curContentFrag.srcAdapter.dataSourceL2 = curContentFrag.dataSourceL2;
		curSelectionFrag2.dataSourceL1.clear();
		curSelectionFrag2.tempOriDataSourceL1.clear();
		curSelectionFrag2.selectedInList1.clear();
		curContentFrag.allCbx.setEnabled(true);// allCbx.setEnabled(true);
		curSelectionFrag2.allCbx.setSelected(false);//.setChecked(false);
		curSelectionFrag2.allCbx.setImageResource(R.drawable.dot);
		curSelectionFrag2.allCbx.setEnabled(false);
		curSelectionFrag2.nofilelayout.setVisibility(View.VISIBLE);
		curSelectionFrag2.mSwipeRefreshLayout.setVisibility(View.GONE);
		curSelectionFrag2.searchButton.setEnabled(false);
		curSelectionFrag2.listView.removeItemDecoration(curSelectionFrag2.dividerItemDecoration);
		curSelectionFrag2.listView.invalidateItemDecorations();
		curSelectionFrag2.notifyDataSetChanged();// destAdapter.notifyDataSetChanged();
		curContentFrag.notifyDataSetChanged();// srcAdapter.notifyDataSetChanged();
		curContentFrag.selectionStatus1
			.setText(curContentFrag.selectedInList1.size() + "/"
					 + curContentFrag.dataSourceL1.size());
		curSelectionFrag2.selectionStatus1
			.setText(curSelectionFrag2.selectedInList1.size() + "/"
					 + curSelectionFrag2.dataSourceL1.size());
	}

	private void removeFiles(final ContentFragment curContentFrag, final ContentFragment curSelectionFrag2) {
		curContentFrag.dataSourceL2 = curSelectionFrag2.dataSourceL1;
		if (curSelectionFrag2.selectedInList1.size() > 0) {
			curSelectionFrag2.allCbx.setImageResource(R.drawable.dot);
			if (curSelectionFrag2.selectedInList1.size() == curSelectionFrag2.dataSourceL1.size()) {
				curSelectionFrag2.allCbx.setSelected(false);//.setChecked(false);
				curSelectionFrag2.allCbx.setEnabled(false);
			}
			if (multiFiles) {
				curSelectionFrag2.removeAllDS2(curSelectionFrag2.selectedInList1);// dataSourceL2.removeAll(selectedInList2);
			} else {
				curSelectionFrag2.clearDS2();// dataSourceL2.clear();
			}
			curContentFrag.allCbx.setEnabled(true);// allCbx.setEnabled(true);
			curSelectionFrag2.selectedInList1.clear();
			if (curSelectionFrag2.dataSourceL1.size() == 0) {
				curSelectionFrag2.searchButton.setEnabled(false);
				curSelectionFrag2.nofilelayout.setVisibility(View.VISIBLE);
				curSelectionFrag2.mSwipeRefreshLayout.setVisibility(View.GONE);
			} 
			curSelectionFrag2.notifyDataSetChanged();// destAdapter.notifyDataSetChanged();
			curContentFrag.notifyDataSetChanged();// srcAdapter.notifyDataSetChanged();
			curContentFrag.selectionStatus1
				.setText(curContentFrag.selectedInList1.size() + "/"
						 + curContentFrag.dataSourceL1.size());
			curSelectionFrag2.selectionStatus1
				.setText(curSelectionFrag2.selectedInList1.size() + "/"
						 + curSelectionFrag2.dataSourceL1.size());
		}
	}

	@Override
	public void onClick(final View p1) {
		//Log.d(TAG, "onClick " + this + ", " + type);
		super.onClick(p1);
		switch (p1.getId()) {
			case R.id.remove:
				if (!activity.swap) {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.RIGHT) {
						removeFiles(activity.curContentFrag, this);
					} else {
						addFiles(activity.curExplorerFrag, this);
					}
				} else {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) {
						removeFiles(activity.curExplorerFrag, this);
					} else {
						addFiles(activity.curContentFrag, this);
					}
				}
				break;
			case R.id.removeAll:
				if (!activity.swap) {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.RIGHT) {
						removeAllFiles(activity.curContentFrag, this);
					} else {
						addAllFiles(activity.curExplorerFrag, this);
					}
				} else {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) {
						removeAllFiles(activity.curExplorerFrag, this);
					} else {
						addAllFiles(activity.curContentFrag, this);
					}
				}
				break;
			case R.id.add:
				if (!activity.swap) {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.RIGHT) {
						addFiles(activity.curContentFrag, this);
					} else {
						removeFiles(activity.curExplorerFrag, this);
					}
				} else {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) {
						addFiles(activity.curExplorerFrag, this);
					} else {
						removeFiles(activity.curContentFrag, this);
					}
				}
				break;
			case R.id.addAll:
				if (!activity.swap) {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.RIGHT) {
						addAllFiles(activity.curContentFrag, this);
					} else {
						removeAllFiles(activity.curExplorerFrag, this);
					}
				} else {
					if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) {
						addAllFiles(activity.curExplorerFrag, this);
					} else {
						removeAllFiles(activity.curContentFrag, this);
					}
				}
				break;
			case R.id.allCbx:
				if (multiFiles) {
					selectedInList1.clear();
					if (!allCbx.isSelected()) {//}.isChecked()) {
						allCbx.setSelected(true);
						//String st;// = dir.endsWith("/") ? dir : dir + "/";
						for (LayoutElement f : dataSourceL1) {
							//st = f.getAbsolutePath();
							if (f.bf.f.canRead() && (dataSourceL2 == null || !dataSourceL2.contains(f))) {
								selectedInList1.add(f);
							}
						}
						if (selectedInList1.size() > 0 && commands.getVisibility() == View.GONE) {
							commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_bottom));
							commands.setVisibility(View.VISIBLE);
							horizontalDivider6.setVisibility(View.VISIBLE);
						}
					} else {
						allCbx.setSelected(false);
						if (activity.COPY_PATH == null && activity.MOVE_PATH == null && commands.getVisibility() == View.VISIBLE) {
							horizontalDivider6.setVisibility(View.GONE);
							commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
							commands.setVisibility(View.GONE);
						}
					}
					selectionStatus1.setText(selectedInList1.size() 
											 + "/" + dataSourceL1.size());
					srcAdapter.notifyDataSetChanged();
					updateDelPaste();
				}
				break;
			case R.id.allName:
				if (allName.getText().toString().equals("Name ▲")) {
					allName.setText("Name ▼");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.NAME, FileListSorter.DESCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Name ▼");
				} else {
					allName.setText("Name ▲");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.NAME, FileListSorter.ASCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Name ▲");
				}
				//Log.d(TAG, "activity.slideFrag.indexOf " + activity.slideFrag.indexOf(ContentFragment.this));
				allDate.setText("Date");
				allSize.setText("Size");
				allType.setText("Type");
				Collections.sort(dataSourceL1, fileListSorter);
				srcAdapter.notifyDataSetChanged();
				break;
			case R.id.allType:
				if (allType.getText().toString().equals("Type ▲")) {
					allType.setText("Type ▼");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.TYPE, FileListSorter.DESCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Type ▼");
				} else {
					allType.setText("Type ▲");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.TYPE, FileListSorter.ASCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Type ▲");
				}
				allName.setText("Name");
				allDate.setText("Date");
				allSize.setText("Size");
				Collections.sort(dataSourceL1, fileListSorter);
				srcAdapter.notifyDataSetChanged();
				break;
			case R.id.allDate:
				if (allDate.getText().toString().equals("Date ▲")) {
					allDate.setText("Date ▼");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.DATE, FileListSorter.DESCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Date ▼");
				} else {
					allDate.setText("Date ▲");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.DATE, FileListSorter.ASCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Date ▲");
				}
				allName.setText("Name");
				allSize.setText("Size");
				allType.setText("Type");
				Collections.sort(dataSourceL1, fileListSorter);
				srcAdapter.notifyDataSetChanged();
				break;
			case R.id.allSize:
				if (allSize.getText().toString().equals("Size ▲")) {
					allSize.setText("Size ▼");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.SIZE, FileListSorter.DESCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Size ▼");
				} else {
					allSize.setText("Size ▲");
					fileListSorter = new FileListSorter(FileListSorter.DIR_TOP, FileListSorter.SIZE, FileListSorter.ASCENDING);
					AndroidUtils.setSharedPreference(activity, (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) ? ("ContentFragSortType" + activity.slideFrag.indexOfMTabs(ContentFragment.this)) : ("ExplorerFragSortType" + activity.slideFrag2.indexOfMTabs(ContentFragment.this)), "Size ▲");
				}
				allName.setText("Name");
				allDate.setText("Date");
				allType.setText("Type");
				Collections.sort(dataSourceL1, fileListSorter);
				srcAdapter.notifyDataSetChanged();
				break;
			case R.id.icons:
				moreInPanel(p1);
				break;
			case R.id.search:
				searchButton();
				break;
			case R.id.clear:
				searchET.setText("");
				break;
			case R.id.dirMore:
				final MenuBuilder menuBuilder = new MenuBuilder(activity);
				final MenuInflater inflater = new MenuInflater(activity);
				inflater.inflate(R.menu.storage, menuBuilder);
				final MenuPopupHelper optionsMenu = new MenuPopupHelper(activity , menuBuilder, dirMore);
				optionsMenu.setForceShowIcon(true);
				MenuItem mi = menuBuilder.findItem(R.id.otg);
				if (true) {
					mi.setEnabled(true);
				} else {
					mi.setEnabled(false);
				}
				
				mi = menuBuilder.findItem(R.id.microsd);
				if (new File("/storage/MicroSD").exists()) {
					mi.setEnabled(true);
				} else {
					mi.setEnabled(false);
				}

				final int size = menuBuilder.size();
				for (int i = 0; i < size;i++) {
					final Drawable icon = menuBuilder.getItem(i).getIcon();
					icon.setFilterBitmap(true);
					icon.setColorFilter(ExplorerActivity.TEXT_COLOR, PorterDuff.Mode.SRC_IN);
				}
				
				menuBuilder.setCallback(new MenuBuilder.Callback() {
						@Override
						public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
							Log.d(TAG, item.getTitle() + ".");
							switch (item.getItemId())  {
								case R.id.sdcard:
									changeDir("/sdcard", true);
									break;
								case R.id.microsd:
									changeDir("/storage/MicroSD", true);
									break;
								case R.id.newFolder:
									activity.mainActivityHelper.add(MainActivityHelper.NEW_FOLDER);
									break;
								case R.id.newFile:
									activity.mainActivityHelper.add(MainActivityHelper.NEW_FILE);
									break;
								}
							return true;
						}
						@Override
						public void onMenuModeChange(MenuBuilder menu) {}
					});
				optionsMenu.show();
				break;
		}
	}

	private class TextSearch implements TextWatcher {
		public void beforeTextChanged(CharSequence s, int start, int end, int count) {
		}

		public void afterTextChanged(final Editable text) {
			final String filesearch = text.toString();
			Log.d(TAG, "quicksearch " + filesearch);
			if (filesearch.length() > 0) {
				if (searchTask.getStatus() == AsyncTask.Status.RUNNING) {
					searchTask.cancel(true);
				}
				searchTask = new SearchFileNameTask();
				searchTask.execute(filesearch);
			}
		}

		public void onTextChanged(CharSequence s, int start, int end, int count) {
		}
	}

	public void refreshRecyclerViewLayoutManager() {
		setRecyclerViewLayoutManager();
		horizontalDivider0.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
		horizontalDivider12.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
		horizontalDivider7.setBackgroundColor(ExplorerActivity.DIVIDER_COLOR);
	}

	void setRecyclerViewLayoutManager() {
        Log.d(TAG, "setRecyclerViewLayoutManager " + gridLayoutManager);
		if (listView == null) {
			return;
		}
		int scrollPosition = 0, top = 0;
        // If a layout manager has already been set, get current scroll position.
        if (gridLayoutManager != null) {
			scrollPosition = gridLayoutManager.findFirstVisibleItemPosition();
			final View vi = listView.getChildAt(0); 
			top = (vi == null) ? 0 : vi.getTop();
		}
		//final Context context = getContext();
		gridLayoutManager = new GridLayoutManager(fragActivity, spanCount);
		listView.removeItemDecoration(dividerItemDecoration);
		listView.invalidateItemDecorations();
		if (spanCount <= 2) {
			dividerItemDecoration = new GridDividerItemDecoration(fragActivity, true);
			listView.addItemDecoration(dividerItemDecoration);
		}

		srcAdapter = new ArrAdapter(this, dataSourceL1);
		listView.setAdapter(srcAdapter);

		listView.setLayoutManager(gridLayoutManager);
		gridLayoutManager.scrollToPositionWithOffset(scrollPosition, top);
	}

	void trimBackStack() {
		final int size = backStack.size() / 2;
		Log.d(TAG, "trimBackStack " + size);
		for (int i = 0; i < size; i++) {
			backStack.remove(0);
		}
	}

	@Override
	public void onLowMemory() {
		Log.d(TAG, "onLowMemory " + Runtime.getRuntime().freeMemory());
		super.onLowMemory();
		trimBackStack();
	}

	boolean back() {
		Map<String, Object> softBundle;
		Log.d(TAG, "back " + backStack.size());
		if (backStack.size() > 1 && (softBundle = backStack.pop()) != null && softBundle.get("dataSourceL1") != null) {
			Log.d(TAG, "back " + softBundle);
			reload(softBundle);
			return true;
		} else {
			return false;
		}
	}

	public void changeDir(final String curDir, final boolean doScroll) {
		Log.d(TAG, "changeDir " + curDir + ", doScroll " + doScroll + ", " + type + ", " + slidingTabsFragment.side);
		loadList.cancel(true);
		searchTask.cancel(true);
		loadList = new LoadFiles();
		loadList.execute(curDir, doScroll);
	}

	private void manageUi(boolean search) {
		if (search == true) {
			searchButton.setImageResource(R.drawable.ic_arrow_back_grey600);
			topflipper.setDisplayedChild(topflipper.indexOfChild(quickLayout));
			if (type == Frag.TYPE.SELECTION) {
				searchET.setHint("Search");
				searchMode = true;
			} else {
				searchET.setHint("Search " + ((currentPathTitle != null) ? new File(currentPathTitle).getName() : new File(dirTemp4Search).getName()));
				setSearchMode(true);
			}
			searchET.requestFocus();
			//imm.showSoftInput(quicksearch, InputMethodManager.SHOW_FORCED);
			imm.showSoftInput(searchET, InputMethodManager.SHOW_IMPLICIT);
			//imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
			//activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		} else {
			imm.hideSoftInputFromWindow(searchET.getWindowToken(), 0);
			searchET.setText("");
			searchButton.setImageResource(R.drawable.ic_action_search);
			if (type == Frag.TYPE.SELECTION) {
				topflipper.setDisplayedChild(topflipper.indexOfChild(selectionCommands));
				searchMode = false;
				dataSourceL1.clear();
				dataSourceL1.addAll(tempOriDataSourceL1);
			} else {
				topflipper.setDisplayedChild(topflipper.indexOfChild(scrolltext));
				setSearchMode(false);
				updateList();
			}
		}
	}

	public void searchButton() {
		searchMode = !searchMode;
		manageUi(searchMode);
	}
	
//    private void updateProgress(int progress, int maxProgress) {
//        // Only update the progress bar every n steps...
//        if ((progress % 50) == 0) {
//            // Also don't update for the first second.
//            
//            // Okay, send an update.
//            Message msg = handler.obtainMessage(MESSAGE_SET_PROGRESS);
//            msg.arg1 = progress;
//            msg.arg2 = maxProgress;
//            msg.sendToTarget();
//        }
//    }


    protected void updateNoAccessMessage(boolean showMessage) {
        mMessageView.setVisibility(showMessage ? View.VISIBLE : View.GONE);
    }
	

//	private class FileListMessageHandler extends Handler {
//        @Override
//        public void handleMessage(Message msg) {
//
//            switch (msg.what) {
//                case DirectoryScanner.MESSAGE_SHOW_DIRECTORY_CONTENTS:
//                    if (getActivity() == null) {
//                        return;
//                    }
//
//                    DirectoryContents c = (DirectoryContents) msg.obj;
//                    mFiles.clear();
//                    mFiles.addAll(c.listSdCard);
//                    mFiles.addAll(c.listDir);
//                    mFiles.addAll(c.listFile);
//
//                    mAdapter.notifyDataSetChanged();
//                    updateNoAccessMessage(c.noAccess);
//
//
//                    if (mPreviousDirectory != null) {
//                        selectInList(mPreviousDirectory);
//                    } else {
//                        // Reset list position.
//                        if (!mFiles.isEmpty() && getView() != null) {
//                            getListView().setSelection(0);
//                        }
//                    }
//                    setLoading(false);
//                    updateClipboardInfo();
//                    if (resourceCallback != null) {
//                        resourceCallback.onTransitionToIdle();
//                    }
//                    break;
//                case DirectoryScanner.MESSAGE_SET_PROGRESS:
//                    // ignore
//                    break;
//            }
//        }
//    }
	
	
	private class LoadFiles extends AsyncTask<Object, String, List<LayoutElement>> {

		private Boolean doScroll;
		
		@Override
		protected void onPreExecute() {
			mSwipeRefreshLayout.setRefreshing(true);
		}

		@Override
		protected List<LayoutElement> doInBackground(Object... params) {
			String path = (String) params[0];
			doScroll = (Boolean) params[1];
			
			noMedia = false;
			List<LayoutElement> dataSourceL1a = new LinkedList<>();
			
			if (currentPathTitle == null) {
				return null;
			}
			Log.d(TAG, "LoadFiles.doInBackground " + path + ", " + openMode + ", " + ContentFragment.this);
			folder_count = 0;
			file_count = 0;

			if (openMode == OpenMode.UNKNOWN) {
				HFile hFile = new HFile(OpenMode.UNKNOWN, path);
				hFile.generateMode(activity);

				if (hFile.isLocal()) {
					openMode = OpenMode.FILE;
				} else if (hFile.isSmb()) {
					openMode = OpenMode.SMB;
					smbPath = path;
				} else if (hFile.isOtgFile()) {
					openMode = OpenMode.OTG;
				} else if (hFile.isBoxFile()) {
					openMode = OpenMode.BOX;
				} else if (hFile.isDropBoxFile()) {
					openMode = OpenMode.DROPBOX;
				} else if (hFile.isGoogleDriveFile()) {
					openMode = OpenMode.GDRIVE;
				} else if (hFile.isOneDriveFile()) {
					openMode = OpenMode.ONEDRIVE;
				} else if (hFile.isCustomPath())
					openMode = OpenMode.CUSTOM;
				else if (android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches()) {
					openMode = OpenMode.ROOT;
				}
			}

			switch (openMode) {
				case SMB:
					HFile hFile = new HFile(OpenMode.SMB, path);
					try {
						SmbFile[] smbFile = hFile.getSmbFile(5000).listFiles();
						dataSourceL1a = addToSmb(smbFile, path);
						openMode = OpenMode.SMB;
					} catch (SmbAuthException e) {
						if (!e.getMessage().toLowerCase().contains("denied"))
							reauthenticateSmb();
						publishProgress(e.getLocalizedMessage());
					} catch (SmbException | NullPointerException e) {
						publishProgress(e.getLocalizedMessage());
						e.printStackTrace();
					}
					break;
				case CUSTOM:
					ArrayList<BaseFile> arrayList = null;
					switch (Integer.parseInt(path)) {
						case 0:
							arrayList = listImages();
							break;
						case 1:
							arrayList = listVideos();
							break;
						case 2:
							arrayList = listaudio();
							break;
						case 3:
							arrayList = listDocs();
							break;
						case 4:
							arrayList = listApks();
							break;
						case 5:
							arrayList = listRecent();
							break;
						case 6:
							arrayList = listRecentFiles();
							break;
					}
					
					if (arrayList != null) {
						dataSourceL1a = addTo(arrayList);
					} else 
						return new ArrayList<LayoutElement>(0);
					break;
				case OTG:
					dataSourceL1a = addTo(listOtg(path));
					openMode = OpenMode.OTG;
					break;
				case DROPBOX:
					CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
					try {
						dataSourceL1a = addTo(listCloud(path, cloudStorageDropbox, OpenMode.DROPBOX));
					} catch (CloudPluginException e) {
						e.printStackTrace();
						return new ArrayList<LayoutElement>(0);
					}
					break;
				case BOX:
					CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
					try {
						dataSourceL1a = addTo(listCloud(path, cloudStorageBox, OpenMode.BOX));
					} catch (CloudPluginException e) {
						e.printStackTrace();
						return new ArrayList<LayoutElement>(0);
					}
					break;
				case GDRIVE:
					CloudStorage cloudStorageGDrive = dataUtils.getAccount(OpenMode.GDRIVE);
					try {
						dataSourceL1a = addTo(listCloud(path, cloudStorageGDrive, OpenMode.GDRIVE));
					} catch (CloudPluginException e) {
						e.printStackTrace();
						return new ArrayList<LayoutElement>(0);
					}
					break;
				case ONEDRIVE:
					CloudStorage cloudStorageOneDrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
					try {
						dataSourceL1a = addTo(listCloud(path, cloudStorageOneDrive, OpenMode.ONEDRIVE));
					} catch (CloudPluginException e) {
						e.printStackTrace();
						return new ArrayList<LayoutElement>(0);
					}
					break;
				default:
					// we're neither in OTG not in SMB, load the list based on root/general filesystem
					dataSourceL1a = new LinkedList<LayoutElement>();
					try {
						File curDir = new File(path);
						while (curDir != null && !curDir.exists()) {
							publishProgress(curDir.getAbsolutePath() + " is not existed");
							curDir = curDir.getParentFile();
						}
						if (curDir == null) {
							publishProgress("Current directory is not existed. Change to root");
							curDir = new File("/");
						}

						final String curPath = curDir.getAbsolutePath();
						if (!dirTemp4Search.equals(curPath)) {
							if (backStack.size() > ExplorerActivity.NUM_BACK) {
								backStack.remove(0);
							}
							final Map<String, Object> bun = onSaveInstanceState();
							backStack.push(bun);

							history.remove(curPath);
							if (history.size() > ExplorerActivity.NUM_BACK) {
								history.remove(0);
							}
							history.push(curPath);

							activity.historyList.remove(curPath);
							if (activity.historyList.size() > ExplorerActivity.NUM_BACK) {
								activity.historyList.remove(0);
							}
							activity.historyList.push(curPath);
							tempPreviewL2 = null;
						}
						currentPathTitle = curPath;
						dirTemp4Search = currentPathTitle;
						//Log.d(TAG, Util.collectionToString(history, true, "\n"));

						if (mFileObserver != null) {
							mFileObserver.stopWatching();
						}
						mFileObserver = createFileObserver(currentPathTitle);
						mFileObserver.startWatching();
						if (tempPreviewL2 != null && !tempPreviewL2.bf.f.exists()) {
							tempPreviewL2 = null;
						}

						
						ArrayList<BaseFile> files = RootHelper.getFilesList(currentPathTitle, ThemedActivity.rootMode, SHOW_HIDDEN,
                            new RootHelper.GetModeCallBack() {
                                @Override
                                public void getMode(OpenMode mode) {
                                    openMode = mode;
                                }
                            });
							
						String fName;
						boolean isDirectory;
						final ArrayList<String> hiddenfiles = dataUtils.getHiddenfiles();
						for (BaseFile f : files) {
							fName = f.getName();
							isDirectory = f.isDirectory();

							// It's the noMedia file. Raise the flag.
							if (!noMedia && fName.equalsIgnoreCase(".nomedia")) {
								noMedia = true;
							}

							//If the user doesn't want to display hidden files and the file is hidden, ignore this file.
							if (!displayHidden && f.f.isHidden()) {
								continue;
							}
							if (!hiddenfiles.contains(f.getPath())) {

								//Log.d(TAG, "f.f=" + f.f + ", mimes=" + mimes + ", suffix=" + suffix + ", getMimeType=" + MimeTypes.getMimeType(f.f) + ", " + ((mimes + "").indexOf(MimeTypes.getMimeType(f.f) + "") >= 0));
								if (isDirectory) {
									folder_count++;
									if (!mWriteableOnly || f.f.canWrite()) {
										dataSourceL1a.add(new LayoutElement(f));
									}
								} else if (suffix.length() > 0) {//!mDirectoriesOnly
									if (".*".equals(suffix) ||
										"*".equals(suffix) ||
										mimes.indexOf("*/*") > 0 || 
										mimes.indexOf(MimeTypes.getMimeType(f.f) + "") >= 0) {
										dataSourceL1a.add(new LayoutElement(f));
										file_count++;
									} else {//if (suffix != null) 
										//lastIndexOfDot = fName.lastIndexOf(".");
										//ext = lastIndexOfDot >= 0 ? fName.substring(++lastIndexOfDot) : "";
										//Log.d(TAG, "ext=" + ext + ", " + (Arrays.binarySearch(suffixes, ext.toLowerCase()) >= 0));
										if (suffixPattern.matcher(fName).matches()) {//}suffix.matches(".*?\\b" + ext + "\\b.*?")) {
											dataSourceL1a.add(new LayoutElement(f));
											file_count++;
										}
									}
								}
							}
						}
						
					} catch (RootNotPermittedException e) {
						//AppConfig.toast(c, c.getString(R.string.rootfailure));
						return null;
					}
					break;
			}
			if (dataSourceL1a != null && !(openMode == OpenMode.CUSTOM && ((currentPathTitle).equals("5") || (currentPathTitle).equals("6"))))
				Collections.sort(dataSourceL1a, fileListSorter);

//			if (openMode != OpenMode.CUSTOM)
//				DataUtils.addHistoryFile(currentPathTitle);
			return dataSourceL1a;
		}

		@Override
		protected void onPostExecute(List<LayoutElement> dataSourceL1a) {
			//Log.d(TAG, "LoadFiles.onPostExecute.dataSourceL1a=" + Util.collectionToString(dataSourceL1a, false, "\n"));
			if (currentPathTitle != null) {
				if (currentPathTitle.startsWith("/")) {
					final File curDir = new File(currentPathTitle);
					diskStatus.setText(
						"Free " + Util.nf.format(curDir.getFreeSpace() / (1 << 20))
						+ " MiB. Used " + Util.nf.format((curDir.getTotalSpace() - curDir.getFreeSpace()) / (1 << 20))
						+ " MiB. Total " + Util.nf.format(curDir.getTotalSpace() / (1 << 20)) + " MiB");
				}
				dataSourceL1.clear();
				Collections.sort(dataSourceL1a, fileListSorter);
				dataSourceL1.addAll(dataSourceL1a);
				selectedInList1.clear();
			}
			if (status.getVisibility() == View.GONE) {
				if (selStatus != null) {
					selStatus.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
					selStatus.setVisibility(View.VISIBLE);
				} else {
					selectionStatus1.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
					selectionStatus1.setVisibility(View.VISIBLE);
				}
				horizontalDivider0.setVisibility(View.VISIBLE);
				horizontalDivider12.setVisibility(View.VISIBLE);
				status.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
				status.setVisibility(View.VISIBLE);
			}

			if (multiFiles) {
				boolean allInclude = (dataSourceL2 != null && dataSourceL1a.size() > 0) ? true : false;
				if (allInclude) {
					for (LayoutElement st : dataSourceL1a) {
						if (!dataSourceL2.contains(st)) {
							allInclude = false;
							break;
						}
					}
				}

				if (allInclude) {
					allCbx.setSelected(true);
					allCbx.setImageResource(R.drawable.ic_accept);
					allCbx.setEnabled(false);
				} else {
					allCbx.setSelected(false);
					allCbx.setImageResource(R.drawable.dot);
					allCbx.setEnabled(true);
				}
			}

			if (activity.COPY_PATH == null && activity.MOVE_PATH == null && commands != null && commands.getVisibility() == View.VISIBLE) {
				horizontalDivider6.setVisibility(View.GONE);
				commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
				commands.setVisibility(View.GONE);
			} else if ((activity.COPY_PATH != null || activity.MOVE_PATH != null) && commands != null && commands.getVisibility() == View.GONE) {
				horizontalDivider6.setVisibility(View.VISIBLE);
				commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_bottom));
				commands.setVisibility(View.VISIBLE);
				updateDelPaste();
			}

			//Log.d("changeDir dataSourceL1", Util.collectionToString(dataSourceL1, true, "\r\n"));
			listView.setActivated(true);
			srcAdapter.notifyDataSetChanged();
			if (doScroll) {
				gridLayoutManager.scrollToPosition(0);
			}

			if (allCbx.isSelected()) {//}.isChecked()) {
				selectionStatus1.setText(dataSourceL1.size() 
										 + "/" + dataSourceL1.size());
			} else {
				selectionStatus1.setText(selectedInList1.size() 
										 + "/" + dataSourceL1.size());
			}
			Log.d(TAG, "LoadFiles.onPostExecute " + currentPathTitle);

			updateDir(currentPathTitle, ContentFragment.this);
			mSwipeRefreshLayout.setRefreshing(false);
			
			if (dataSourceL1.size() == 0) {
				nofilelayout.setVisibility(View.VISIBLE);
				mSwipeRefreshLayout.setVisibility(View.GONE);
			} else {
				nofilelayout.setVisibility(View.GONE);
				mSwipeRefreshLayout.setVisibility(View.VISIBLE);
			}

		}

		@Override
		public void onProgressUpdate(String... message) {
			Toast.makeText(activity, message[0], Toast.LENGTH_SHORT).show();
		}

		private ArrayList<LayoutElement> addTo(ArrayList<BaseFile> baseFiles) {
			ArrayList<LayoutElement> items = new ArrayList<>();

			for (int i = 0; i < baseFiles.size(); i++) {
				BaseFile baseFile = baseFiles.get(i);
				//File f = new File(ele.getPath());

				if (!dataUtils.getHiddenfiles().contains(baseFile.getPath())) {
					final LayoutElement layoutElement = new LayoutElement(baseFile);
					layoutElement.setMode(baseFile.getMode());
					items.add(layoutElement);
					if (baseFile.isDirectory()) {
						folder_count++;
					} else {
						file_count++;
					}
				}
			}
			return items;
		}

		private ArrayList<BaseFile> listaudio() {
			String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
			String[] projection = {
                MediaStore.Audio.Media.DATA
			};

			Cursor cursor = activity.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);

			ArrayList<BaseFile> songs = new ArrayList<>();
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				do {
					String path = cursor.getString(cursor.getColumnIndex
												   (MediaStore.Files.FileColumns.DATA));
					BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
					if (strings != null) songs.add(strings);
				} while (cursor.moveToNext());
			}
			cursor.close();
			return songs;
		}

		private ArrayList<BaseFile> listImages() {
			ArrayList<BaseFile> songs = new ArrayList<>();
			final String[] projection = {MediaStore.Images.Media.DATA};
			final Cursor cursor = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
																	  projection, null, null, null);
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				do {
					String path = cursor.getString(cursor.getColumnIndex
												   (MediaStore.Files.FileColumns.DATA));
					BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
					if (strings != null) songs.add(strings);
				} while (cursor.moveToNext());
			}
			cursor.close();
			return songs;
		}

		private ArrayList<BaseFile> listVideos() {
			ArrayList<BaseFile> songs = new ArrayList<>();
			final String[] projection = {MediaStore.Images.Media.DATA};
			final Cursor cursor = activity.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
																	  projection, null, null, null);
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				do {
					String path = cursor.getString(cursor.getColumnIndex
												   (MediaStore.Files.FileColumns.DATA));
					BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
					if (strings != null) songs.add(strings);
				} while (cursor.moveToNext());
			}
			cursor.close();
			return songs;
		}

		private ArrayList<BaseFile> listRecentFiles() {
			ArrayList<BaseFile> songs = new ArrayList<>();
			final String[] projection = {MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED};
			Calendar c = Calendar.getInstance();
			c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
			Date d = c.getTime();
			Cursor cursor = activity.getContentResolver().query(MediaStore.Files
															  .getContentUri("external"), projection,
															  null,
															  null, null);
			if (cursor == null) return songs;
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				do {
					String path = cursor.getString(cursor.getColumnIndex
												   (MediaStore.Files.FileColumns.DATA));
					File f = new File(path);
					if (d.compareTo(new Date(f.lastModified())) != 1 && !f.isDirectory()) {
						BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
						if (strings != null) songs.add(strings);
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
			Collections.sort(songs, new Comparator<BaseFile>() {
					@Override
					public int compare(BaseFile lhs, BaseFile rhs) {
						return -1 * Long.valueOf(lhs.getDate()).compareTo(rhs.getDate());

					}
				});
			if (songs.size() > 20)
				for (int i = songs.size() - 1; i > 20; i--) {
					songs.remove(i);
				}
			return songs;
		}

		private ArrayList<BaseFile> listApks() {
			ArrayList<BaseFile> songs = new ArrayList<>();
			final String[] projection = {MediaStore.Files.FileColumns.DATA};

			Cursor cursor = activity.getContentResolver()
                .query(MediaStore.Files.getContentUri("external"), projection, null, null, null);
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				do {
					String path = cursor.getString(cursor.getColumnIndex
												   (MediaStore.Files.FileColumns.DATA));
					if (path != null && path.endsWith(".apk")) {
						BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
						if (strings != null) songs.add(strings);
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
			return songs;
		}

		private ArrayList<BaseFile> listRecent() {
			UtilsHandler utilsHandler = new UtilsHandler(activity);
			final ArrayList<String> paths = utilsHandler.getHistoryList();
			ArrayList<BaseFile> songs = new ArrayList<>();
			for (String f : paths) {
				if (!f.equals("/")) {
					BaseFile a = RootHelper.generateBaseFile(new File(f), SHOW_HIDDEN);
					a.generateMode(activity);
					if (a != null && !a.isSmb() && !(a).isDirectory() && a.exists())
						songs.add(a);
				}
			}
			return songs;
		}

		private ArrayList<BaseFile> listDocs() {
			ArrayList<BaseFile> songs = new ArrayList<>();
			final String[] projection = {MediaStore.Files.FileColumns.DATA};
			Cursor cursor = activity.getContentResolver().query(MediaStore.Files.getContentUri("external"),
																projection, null, null, null);
			String[] types = new String[]{".pdf", ".xml", ".html", ".asm", ".text/x-asm", ".def", ".in", ".rc",
                ".list", ".log", ".pl", ".prop", ".properties", ".rc",
                ".doc", ".docx", ".msg", ".odt", ".pages", ".rtf", ".txt", ".wpd", ".wps"};
			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
				do {
					String path = cursor.getString(cursor.getColumnIndex
												   (MediaStore.Files.FileColumns.DATA));
					if (path != null && contains(types, path)) {
						BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
						if (strings != null) songs.add(strings);
					}
				} while (cursor.moveToNext());
			}
			cursor.close();
			return songs;
		}

		/**
		 * Lists files from an OTG device
		 *
		 * @param path the path to the directory tree, starts with prefix {@link com.amaze.filemanager.utils.OTGUtil#PREFIX_OTG}
		 *             Independent of URI (or mount point) for the OTG
		 * @return a list of files loaded
		 */
		private ArrayList<BaseFile> listOtg(String path) {

			return OTGUtil.getDocumentFilesList(path, activity);
		}

		private boolean contains(String[] types, String path) {
			for (String string : types) {
				if (path.endsWith(string)) return true;
			}
			return false;
		}

		private ArrayList<BaseFile> listCloud(String path, CloudStorage cloudStorage, OpenMode openMode)
		throws CloudPluginException {
			if (!CloudSheetFragment.isCloudProviderAvailable(activity))
				throw new CloudPluginException();

			return CloudUtil.listFiles(path, cloudStorage, openMode);
		}
	}
	
	
//	class LoadFiles extends AsyncTask<Object, String, Void> {
//		//private File curDir;
//		private Boolean doScroll;
//		private ArrayList<LayoutElement> dataSourceL1a = new ArrayList<>();
//
//		static final private int PROGRESS_STEPS = 50;
//		
//		@Override
//		protected void onPreExecute() {
//			if (!mSwipeRefreshLayout.isRefreshing()) {
//				mSwipeRefreshLayout.setRefreshing(true);
//			}
//		}
//
//		@Override
//		protected Void doInBackground(final Object... params) {
//			currentPathTitle = (String) params[0];
//			doScroll = (Boolean) params[1];
//			noMedia = false;
//			if (currentPathTitle == null) {
//				return null;
//			}
//			Log.d(TAG, "LoadFiles.doInBackground " + currentPathTitle + ", " + ContentFragment.this);
//			folder_count = 0;
//			file_count = 0;
//			if (openmode == OpenMode.UNKNOWN) {
//				HFile hFile = new HFile(OpenMode.UNKNOWN, currentPathTitle);
//				hFile.generateMode(activity);
//				if (hFile.isLocal()) {
//					openmode = OpenMode.FILE;
//				} else if (hFile.isSmb()) {
//					openmode = OpenMode.SMB;
//					smbPath = currentPathTitle;
//				} else if (hFile.isOtgFile()) {
//					openmode = OpenMode.OTG;
//				} else if (hFile.isBoxFile()) {
//					openmode = OpenMode.BOX;
//				} else if (hFile.isDropBoxFile()) {
//					openmode = OpenMode.DROPBOX;
//				} else if (hFile.isGoogleDriveFile()) {
//					openmode = OpenMode.GDRIVE;
//				} else if (hFile.isOneDriveFile()) {
//					openmode = OpenMode.ONEDRIVE;
//				} else if (hFile.isCustomPath())
//					openmode = OpenMode.CUSTOM;
//				else if (android.util.Patterns.EMAIL_ADDRESS.matcher(currentPathTitle).matches()) {
//					openmode = OpenMode.ROOT;
//				}
//			}
//
//			switch (openmode) {
//				case SMB:
//					HFile hFile = new HFile(OpenMode.SMB, currentPathTitle);
//					try {
//						SmbFile[] smbFile = hFile.getSmbFile(5000).listFiles();
//						dataSourceL1a = addToSmb(smbFile, currentPathTitle);
//						openmode = OpenMode.SMB;
//					} catch (SmbAuthException e) {
//						if (!e.getMessage().toLowerCase().contains("denied"))
//							reauthenticateSmb();
//						publishProgress(e.getLocalizedMessage());
//					} catch (SmbException | NullPointerException e) {
//						publishProgress(e.getLocalizedMessage());
//						e.printStackTrace();
//					}
//					break;
//				case CUSTOM:
//					ArrayList<BaseFile> arrayList = null;
//					switch (Integer.parseInt(currentPathTitle)) {
//						case 0:
//							arrayList = listImages();
//							break;
//						case 1:
//							arrayList = listVideos();
//							break;
//						case 2:
//							arrayList = listaudio();
//							break;
//						case 3:
//							arrayList = listDocs();
//							break;
//						case 4:
//							arrayList = listApks();
//							break;
//						case 5:
//							arrayList = listRecent();
//							break;
//						case 6:
//							arrayList = listRecentFiles();
//							break;
//					}
//
//					currentPathTitle = String.valueOf(Integer.parseInt(currentPathTitle));
//
//					try {
//						if (arrayList != null)
//							dataSourceL1a = addTo(arrayList);
//						else return null;// new ArrayList<LayoutElements>();
//					} catch (Exception e) {
//					}
//					break;
//				case OTG:
//					dataSourceL1a = addTo(listOtg(currentPathTitle));
//					openmode = OpenMode.OTG;
//					break;
//				case DROPBOX:
//
//					CloudStorage cloudStorageDropbox = DataUtils.getAccount(OpenMode.DROPBOX);
//
//					try {
//						dataSourceL1a = addTo(listCloud(currentPathTitle, cloudStorageDropbox, OpenMode.DROPBOX));
//					} catch (CloudPluginException e) {
//						e.printStackTrace();
//						return null;// new ArrayList<LayoutElements>();
//					}
//					break;
//				case BOX:
//					CloudStorage cloudStorageBox = DataUtils.getAccount(OpenMode.BOX);
//
//					try {
//						dataSourceL1a = addTo(listCloud(currentPathTitle, cloudStorageBox, OpenMode.BOX));
//					} catch (CloudPluginException e) {
//						e.printStackTrace();
//						return null;// new ArrayList<LayoutElements>();
//					}
//					break;
//				case GDRIVE:
//					CloudStorage cloudStorageGDrive = DataUtils.getAccount(OpenMode.GDRIVE);
//
//					try {
//						dataSourceL1a = addTo(listCloud(currentPathTitle, cloudStorageGDrive, OpenMode.GDRIVE));
//					} catch (CloudPluginException e) {
//						e.printStackTrace();
//						return null;// new ArrayList<LayoutElements>();
//					}
//					break;
//				case ONEDRIVE:
//					CloudStorage cloudStorageOneDrive = DataUtils.getAccount(OpenMode.ONEDRIVE);
//
//					try {
//						dataSourceL1a = addTo(listCloud(currentPathTitle, cloudStorageOneDrive, OpenMode.ONEDRIVE));
//					} catch (CloudPluginException e) {
//						e.printStackTrace();
//						return null;// new ArrayList<LayoutElements>();
//					}
//					break;
//				default:
//					// we're neither in OTG not in SMB, load the list based on root/general filesystem
//					try {
//
//						File curDir = new File(currentPathTitle);
//						while (curDir != null && !curDir.exists()) {
//							publishProgress(curDir.getAbsolutePath() + " is not existed");
//							curDir = curDir.getParentFile();
//						}
//						if (curDir == null) {
//							publishProgress("Current directory is not existed. Change to root");
//							curDir = new File("/");
//						}
//
//						final String curPath = curDir.getAbsolutePath();
//						if (!dirTemp4Search.equals(curPath)) {
//							if (backStack.size() > ExplorerActivity.NUM_BACK) {
//								backStack.remove(0);
//							}
//							final Map<String, Object> bun = onSaveInstanceState();
//							backStack.push(bun);
//
//							history.remove(curPath);
//							if (history.size() > ExplorerActivity.NUM_BACK) {
//								history.remove(0);
//							}
//							history.push(curPath);
//
//							activity.historyList.remove(curPath);
//							if (activity.historyList.size() > ExplorerActivity.NUM_BACK) {
//								activity.historyList.remove(0);
//							}
//							activity.historyList.push(curPath);
//							tempPreviewL2 = null;
//						}
//						currentPathTitle = curPath;
//						dirTemp4Search = currentPathTitle;
//						//Log.d(TAG, Util.collectionToString(history, true, "\n"));
//
//						if (mFileObserver != null) {
//							mFileObserver.stopWatching();
//						}
//						mFileObserver = createFileObserver(currentPathTitle);
//						mFileObserver.startWatching();
//						if (tempPreviewL2 != null && !tempPreviewL2.bf.f.exists()) {
//							tempPreviewL2 = null;
//						}
//
//						ArrayList<BaseFile> files = RootHelper.getFilesList(currentPathTitle, ThemedActivity.rootMode, SHOW_HIDDEN,
//                            new RootHelper.GetModeCallBack() {
//								@Override
//								public void getMode(OpenMode mode) {
//									openmode = mode;
//								}
//							});
//						//List<File> files = FileUtil.currentFileFolderListing(curDir);
//						//Log.d("filesListing", Util.collectionToString(files, true, "\r\n"));
//						
//						
//						String fName;
//						
//						//final int size = files.size();
//						boolean isDirectory;
//						for (BaseFile f : files) {
//							fName = f.getName();
//							isDirectory = f.isDirectory();
////							if (isDirectory) {
////								folder_count++;
////							} else {
////								file_count++;
////							}
//							//updateProgress(folder_count + file_count, size);
//
//							// It's the noMedia file. Raise the flag.
//							if (!noMedia && fName.equalsIgnoreCase(".nomedia")) {
//								noMedia = true;
//							}
//							
//							//If the user doesn't want to display hidden files and the file is hidden, ignore this file.
//							if (!displayHidden && f.f.isHidden()) {
//								continue;
//							}
//
//							Log.d(TAG, "f.f=" + f.f + ", mimes="+mimes +", suffix=" + suffix + ", getMimeType=" + MimeTypes.getMimeType(f.f) + ", " + ((mimes+"").indexOf(MimeTypes.getMimeType(f.f)+"") >= 0));
//							if (isDirectory) {
//								if (!mWriteableOnly || f.f.canWrite()) {
//									dataSourceL1a.add(new LayoutElement(f));
//								}
//							} else if (suffix.length() > 0) {//!mDirectoriesOnly
//								if (".*".equals(suffix) ||
//									"*".equals(suffix) ||
//									mimes.indexOf("*/*") > 0 || 
//									mimes.indexOf(MimeTypes.getMimeType(f.f)+"") >= 0) {
//									dataSourceL1a.add(new LayoutElement(f));
//								} else {//if (suffix != null) 
//									//lastIndexOfDot = fName.lastIndexOf(".");
//									//ext = lastIndexOfDot >= 0 ? fName.substring(++lastIndexOfDot) : "";
//									//Log.d(TAG, "ext=" + ext + ", " + (Arrays.binarySearch(suffixes, ext.toLowerCase()) >= 0));
//									if (suffixPattern.matcher(fName).matches()) {//}suffix.matches(".*?\\b" + ext + "\\b.*?")) {
//										dataSourceL1a.add(new LayoutElement(f));
//									}
//									
//									
//								}
//							}
//							
////							if (f.exists()) {
////								fName = f.getName();
////								//Log.d("changeDir fName", fName + ", isDir " + f.isDirectory());
////								if (f.isDirectory()) {
////									dataSourceL1a.add(new LayoutElements(f));
////								} else {
////									if (suffix.length() > 0) {
////										if (".*".equals(suffix)) {
////											dataSourceL1a.add(new LayoutElements(f));
////										} else {
////											lastIndexOfDot = fName.lastIndexOf(".");
////											if (lastIndexOfDot >= 0) {
////												ext = fName.substring(lastIndexOfDot);
////												if ((Arrays.binarySearch(suffixes, ext.toLowerCase()) >= 0)) {
////													dataSourceL1a.add(new LayoutElements(f));
////												}
////											}
////										}
////									}
////								}
////							}
//						}
//						// điền danh sách vào allFiles
//
//						//dataSourceL1a = addTo(files);
//						dirTemp4Search = currentPathTitle;
//					} catch (RootNotPermittedException e) {
//						//AppConfig.toast(c, c.getString(R.string.rootfailure));
//						return null;
//					}
//					break;
//			}
//
//			if (dataSourceL1a != null && !(openmode == OpenMode.CUSTOM && ((currentPathTitle).equals("5") || (currentPathTitle).equals("6"))))
//				Collections.sort(dataSourceL1a, fileListSorter);//(dsort, sortby, asc));
//
//			if (openMode != OpenMode.CUSTOM)
//				DataUtils.addHistoryFile(currentPathTitle);
//
//			//curDir = (File) params[0];
//			//curDir = new File(path);
////			doScroll = (Boolean) params[1];
////			
////			while (curDir != null && !curDir.exists()) {
////				publishProgress(curDir.getAbsolutePath() + " is not existed");
////				curDir = curDir.getParentFile();
////			}
////			if (curDir == null) {
////				publishProgress("Current directory is not existed. Change to root");
////				curDir = new File("/");
////			}
////			
////			final String curPath = curDir.getAbsolutePath();
////			if (!dirTemp4Search.equals(curPath)) {
////				if (backStack.size() > ExplorerActivity.NUM_BACK) {
////					backStack.remove(0);
////				}
////				final Map<String, Object> bun = onSaveInstanceState();
////				backStack.push(bun);
////				
////				history.remove(curPath);
////				if (history.size() > ExplorerActivity.NUM_BACK) {
////					history.remove(0);
////				}
////				history.push(curPath);
////				
////				activity.historyList.remove(curPath);
////				if (activity.historyList.size() > ExplorerActivity.NUM_BACK) {
////					activity.historyList.remove(0);
////				}
////				activity.historyList.push(curPath);
////				tempPreviewL2 = null;
////			}
////			path = curPath;
////			dirTemp4Search = path;
////			//Log.d(TAG, Util.collectionToString(history, true, "\n"));
////			
////			if (mFileObserver != null) {
////				mFileObserver.stopWatching();
////			}
////			mFileObserver = createFileObserver(path);
////			mFileObserver.startWatching();
////			if (tempPreviewL2 != null && !tempPreviewL2.bf.f.exists()) {
////				tempPreviewL2 = null;
////			}
////			
////			List<File> files = FileUtil.currentFileFolderListing(curDir);
////			//Log.d("filesListing", Util.collectionToString(files, true, "\r\n"));
////			if (files != null) {	// always dir, already checked
////				// tìm danh sách các file có ext thích hợp
////				//Log.d("suffix", suffix);
////				String[] suffixes = suffix.toLowerCase().split("; *");
////				Arrays.sort(suffixes);
////				for (File f : files) {
////					if (f.exists()) {
////						String fName = f.getName();
////						//Log.d("changeDir fName", fName + ", isDir " + f.isDirectory());
////						if (f.isDirectory()) {
////							dataSourceL1a.add(new LayoutElements(f));
////						} else {
////							if (suffix.length() > 0) {
////								if (".*".equals(suffix)) {
////									dataSourceL1a.add(new LayoutElements(f));
////								} else {
////									int lastIndexOf = fName.lastIndexOf(".");
////									if (lastIndexOf >= 0) {
////										String ext = fName.substring(lastIndexOf);
////										boolean chosen = Arrays.binarySearch(suffixes, ext.toLowerCase()) >= 0;
////										if (chosen) {
////											dataSourceL1a.add(new LayoutElements(f));
////										}
////									}
////								}
////							}
////						}
////					}
////				}
////				// điền danh sách vào allFiles
////			}
//			//Log.d(TAG, "changeDir dataSourceL1a.size=" + dataSourceL1a.size() + ", fake=" + fake + ", " + path);
//			//String dirSt = dir.getText().toString();
//
//			return null;
//		}
//
//		private ArrayList<LayoutElement> addTo(ArrayList<BaseFile> baseFiles) {
//			ArrayList<LayoutElement> a = new ArrayList<>();
//			for (int i = 0; i < baseFiles.size(); i++) {
//				BaseFile baseFile = baseFiles.get(i);
//				//File f = new File(ele.getPath());
//				//String size = "";
//				if (!DataUtils.hiddenfiles.contains(baseFile.getPath())) {
//					if (baseFile.isDirectory()) {
//						//size = "";
//
////                    Bitmap lockBitmap = BitmapFactory.decodeResource(ma.getResources(),
////                            R.drawable.ic_folder_lock_white_36dp);
//						//BitmapDrawable lockBitmapDrawable = new BitmapDrawable(ma.getResources(), lockBitmap);
//
//						LayoutElement layoutElements = activity.getFutils()
//                            .newElement(//baseFile.getName().endsWith(CryptUtil.CRYPT_EXTENSION) ? lockBitmapDrawable : ma.folder,
//							baseFile.getPath(), baseFile.getPermission(), baseFile.getLink(), /*size, */baseFile.f.length(), true, //false,
//                            baseFile.getDate());
//						layoutElements.setMode(baseFile.getMode());
//						a.add(layoutElements);
//						folder_count++;
//					} else {
//						long longSize = 0;
//						try {
//							if (baseFile.getSize() != -1) {
//								longSize = baseFile.getSize();
//								//size = Formatter.formatFileSize(c, longSize);
//							} else {
//								//size = "";
//								longSize = 0;
//							}
//						} catch (NumberFormatException e) {
//							//e.printStackTrace();
//						}
//						try {
//							LayoutElement layoutElements = activity.getFutils().newElement(//Icons.loadMimeIcon(baseFile.getPath(), !ma.IS_LIST, ma.res), 
//								baseFile.getPath(), baseFile.getPermission(),
//                                baseFile.getLink(), /*size, */longSize, false, /*false, */baseFile.getDate());
//							layoutElements.setMode(baseFile.getMode());
//							a.add(layoutElements);
//							file_count++;
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//					}
//				}
//			}
//			return a;
//		}
//
//		protected void onProgressUpdate(String...values) {
//			showToast(values[0]);
//		}
//
//		protected void onPostExecute(Object result) {
//			if (currentPathTitle != null) {
//				if (currentPathTitle.startsWith("/")) {
//					final File curDir = new File(currentPathTitle);
//					diskStatus.setText(
//						"Free " + Util.nf.format(curDir.getFreeSpace() / (1 << 20))
//						+ " MiB. Used " + Util.nf.format((curDir.getTotalSpace() - curDir.getFreeSpace()) / (1 << 20))
//						+ " MiB. Total " + Util.nf.format(curDir.getTotalSpace() / (1 << 20)) + " MiB");
//				}
//				dataSourceL1.clear();
//				Collections.sort(dataSourceL1a, fileListSorter);
//				dataSourceL1.addAll(dataSourceL1a);
//				dataSourceL1a.clear();
//				selectedInList1.clear();
//			}
//			if (status.getVisibility() == View.GONE) {
//				if (selStatus != null) {
//					selStatus.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
//					selStatus.setVisibility(View.VISIBLE);
//				} else {
//					selectionStatus1.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
//					selectionStatus1.setVisibility(View.VISIBLE);
//				}
//				horizontalDivider0.setVisibility(View.VISIBLE);
//				horizontalDivider12.setVisibility(View.VISIBLE);
//				status.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_top));
//				status.setVisibility(View.VISIBLE);
//			}
//
//			if (multiFiles) {
//				boolean allInclude = (dataSourceL2 != null && dataSourceL1a.size() > 0) ? true : false;
//				if (allInclude) {
//					for (LayoutElement st : dataSourceL1a) {
//						if (!dataSourceL2.contains(st)) {
//							allInclude = false;
//							break;
//						}
//					}
//				}
//
//				if (allInclude) {
//					allCbx.setSelected(true);
//					allCbx.setImageResource(R.drawable.ic_accept);
//					allCbx.setEnabled(false);
//				} else {
//					allCbx.setSelected(false);
//					allCbx.setImageResource(R.drawable.dot);
//					allCbx.setEnabled(true);
//				}
//			}
//
//			if (activity.COPY_PATH == null && activity.MOVE_PATH == null && commands != null && commands.getVisibility() == View.VISIBLE) {
//				horizontalDivider.setVisibility(View.GONE);
//				commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
//				commands.setVisibility(View.GONE);
//			} else if ((activity.COPY_PATH != null || activity.MOVE_PATH != null) && commands != null && commands.getVisibility() == View.GONE) {
//				horizontalDivider.setVisibility(View.VISIBLE);
//				commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_bottom));
//				commands.setVisibility(View.VISIBLE);
//				updateDelPaste();
//			}
//
//			//Log.d("changeDir dataSourceL1", Util.collectionToString(dataSourceL1, true, "\r\n"));
//			listView.setActivated(true);
//			srcAdapter.notifyDataSetChanged();
//			if (doScroll) {
//				gridLayoutManager.scrollToPosition(0);
//			}
//
//			if (allCbx.isSelected()) {//}.isChecked()) {
//				selectionStatus1.setText(dataSourceL1.size() 
//										 + "/" + dataSourceL1.size());
//			} else {
//				selectionStatus1.setText(selectedInList1.size() 
//										 + "/" + dataSourceL1.size());
//			}
//			Log.d(TAG, "LoadFiles.onPostExecute " + currentPathTitle);
//
//			updateDir(currentPathTitle, ContentFragment.this);
//			if (mSwipeRefreshLayout.isRefreshing()) {
//				mSwipeRefreshLayout.setRefreshing(false);
//			}
//			if (dataSourceL1.size() == 0) {
//				nofilelayout.setVisibility(View.VISIBLE);
//				mSwipeRefreshLayout.setVisibility(View.GONE);
//			} else {
//				nofilelayout.setVisibility(View.GONE);
//				mSwipeRefreshLayout.setVisibility(View.VISIBLE);
//			}
//
//		}
//
//		ArrayList<BaseFile> listaudio() {
//			String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
//			String[] projection = {
//                MediaStore.Audio.Media.DATA
//			};
//
//			Cursor cursor = activity.getContentResolver().query(
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                projection,
//                selection,
//                null,
//                null);
//
//			ArrayList<BaseFile> songs = new ArrayList<>();
//			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
//				do {
//					String path = cursor.getString(cursor.getColumnIndex
//												   (MediaStore.Files.FileColumns.DATA));
//					BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
//					if (strings != null) songs.add(strings);
//				} while (cursor.moveToNext());
//			}
//			cursor.close();
//			return songs;
//		}
//
//		ArrayList<BaseFile> listImages() {
//			ArrayList<BaseFile> songs = new ArrayList<>();
//			final String[] projection = {MediaStore.Images.Media.DATA};
//			final Cursor cursor = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//																	  projection, null, null, null);
//			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
//				do {
//					String path = cursor.getString(cursor.getColumnIndex
//												   (MediaStore.Files.FileColumns.DATA));
//					BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
//					if (strings != null) songs.add(strings);
//				} while (cursor.moveToNext());
//			}
//			cursor.close();
//			return songs;
//		}
//
//		ArrayList<BaseFile> listVideos() {
//			ArrayList<BaseFile> songs = new ArrayList<>();
//			final String[] projection = {MediaStore.Images.Media.DATA};
//			final Cursor cursor = activity.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//																	  projection, null, null, null);
//			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
//				do {
//					String path = cursor.getString(cursor.getColumnIndex
//												   (MediaStore.Files.FileColumns.DATA));
//					BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
//					if (strings != null) songs.add(strings);
//				} while (cursor.moveToNext());
//			}
//			cursor.close();
//			return songs;
//		}
//
//		ArrayList<BaseFile> listRecentFiles() {
//			ArrayList<BaseFile> songs = new ArrayList<>();
//			final String[] projection = {MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.DATE_MODIFIED};
//			Calendar c = Calendar.getInstance();
//			c.set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR) - 2);
//			Date d = c.getTime();
//			Cursor cursor = activity.getContentResolver().query(MediaStore.Files
//																.getContentUri("external"), projection,
//																null,
//																null, null);
//			if (cursor == null) return songs;
//			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
//				do {
//					String path = cursor.getString(cursor.getColumnIndex
//												   (MediaStore.Files.FileColumns.DATA));
//					File f = new File(path);
//					if (d.compareTo(new Date(f.lastModified())) != 1 && !f.isDirectory()) {
//						BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
//						if (strings != null) songs.add(strings);
//					}
//				} while (cursor.moveToNext());
//			}
//			cursor.close();
//			Collections.sort(songs, new Comparator<BaseFile>() {
//					@Override
//					public int compare(BaseFile lhs, BaseFile rhs) {
//						return -1 * Long.valueOf(lhs.getDate()).compareTo(rhs.getDate());
//
//					}
//				});
//			if (songs.size() > 20)
//				for (int i = songs.size() - 1; i > 20; i--) {
//					songs.remove(i);
//				}
//			return songs;
//		}
//
//		ArrayList<BaseFile> listApks() {
//			ArrayList<BaseFile> songs = new ArrayList<>();
//			final String[] projection = {MediaStore.Files.FileColumns.DATA};
//
//			Cursor cursor = activity.getContentResolver().query(MediaStore.Files
//																.getContentUri("external"), projection,
//																null,
//																null, null);
//			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
//				do {
//					String path = cursor.getString(cursor.getColumnIndex
//												   (MediaStore.Files.FileColumns.DATA));
//					if (path != null && path.endsWith(".apk")) {
//						BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
//						if (strings != null) songs.add(strings);
//					}
//				} while (cursor.moveToNext());
//			}
//			cursor.close();
//			return songs;
//		}
//
//		ArrayList<BaseFile> listRecent() {
//			final HistoryManager history = new HistoryManager(activity, "Table2");
//			final ArrayList<String> paths = history.readTable(DataUtils.HISTORY);
//			history.end();
//			ArrayList<BaseFile> songs = new ArrayList<>();
//			for (String f : paths) {
//				if (!f.equals("/")) {
//					BaseFile a = RootHelper.generateBaseFile(new File(f), SHOW_HIDDEN);
//					a.generateMode(activity);
//					if (a != null && !a.isSmb() && !(a).isDirectory() && a.exists())
//						songs.add(a);
//				}
//			}
//			return songs;
//		}
//
//		ArrayList<BaseFile> listDocs() {
//			ArrayList<BaseFile> songs = new ArrayList<>();
//			final String[] projection = {MediaStore.Files.FileColumns.DATA};
//			Cursor cursor = activity.getContentResolver().query(MediaStore.Files.getContentUri("external"),
//																projection, null, null, null);
//			String[] types = new String[]{".pdf", ".xml", ".html", ".asm", ".text/x-asm", ".def", ".in", ".rc",
//                ".list", ".log", ".pl", ".prop", ".properties", ".rc",
//                ".doc", ".docx", ".msg", ".odt", ".pages", ".rtf", ".txt", ".wpd", ".wps"};
//			if (cursor.getCount() > 0 && cursor.moveToFirst()) {
//				do {
//					String path = cursor.getString(cursor.getColumnIndex
//												   (MediaStore.Files.FileColumns.DATA));
//					if (path != null && contains(types, path.toLowerCase())) {
//						BaseFile strings = RootHelper.generateBaseFile(new File(path), SHOW_HIDDEN);
//						if (strings != null) songs.add(strings);
//					}
//				} while (cursor.moveToNext());
//			}
//			cursor.close();
//			return songs;
//		}
//
//		/**
//		 * Lists files from an OTG device
//		 * @param path the path to the directory tree, starts with prefix {@link com.amaze.filemanager.utils.OTGUtil#PREFIX_OTG}
//		 *             Independent of URI (or mount point) for the OTG
//		 * @return a list of files loaded
//		 */
//		ArrayList<BaseFile> listOtg(String path) {
//
//			return OTGUtil.getDocumentFilesList(path, activity);
//		}
//
//		boolean contains(String[] types, String path) {
//			for (String string : types) {
//				if (path.endsWith(string)) return true;
//			}
//			return false;
//		}
//
//		private ArrayList<BaseFile> listCloud(String path, CloudStorage cloudStorage, OpenMode openMode)
//		throws CloudPluginException {
//			if (!CloudSheetFragment.isCloudProviderAvailable(activity))
//				throw new CloudPluginException();
//
//			return CloudUtil.listFiles(path, cloudStorage, openMode);
//		}
//	}

	private class SearchFileNameTask extends AsyncTask<String, Long, ArrayList<LayoutElement>> {
		protected void onPreExecute() {
			if (!mSwipeRefreshLayout.isRefreshing()) {
				mSwipeRefreshLayout.setRefreshing(true);
			}
			if (type == Frag.TYPE.SELECTION) {
				searchMode = true;
			} else {
				setSearchMode(true);
			}
			searchVal = searchET.getText().toString();
			showToast("Searching...");
			dataSourceL1.clear();
			srcAdapter.notifyDataSetChanged();
		}

		@Override
		protected ArrayList<LayoutElement> doInBackground(String... params) {
			Log.d("SearchFileNameTask", "dirTemp4Search " + dirTemp4Search);
			final ArrayList<LayoutElement> tempAppList = new ArrayList<>();
			if (type == Frag.TYPE.SELECTION) {
				final Collection<LayoutElement> c = FileUtil.getFilesBy(tempOriDataSourceL1, params[0], true);
				//Log.d(TAG, "getFilesBy " + Util.collectionToString(c, true, "\n"));
				tempAppList.addAll(c);
			} else {
				File file = new File(dirTemp4Search);

				if (file.exists()) {
					Collection<File> c = FileUtil.getFilesBy(file.listFiles(), params[0], true);
					Log.d(TAG, "getFilesBy " + Util.collectionToString(c, true, "\n"));
					for (File le : c) {
						tempAppList.add(new LayoutElement(le));
					}
					//addAllDS1(Util.collectionFile2CollectionString(c));// dataSourceL1.addAll(Util.collectionFile2CollectionString(c));curContentFrag.
					// Log.d("dataSourceL1 new task",
					// Util.collectionToString(dataSourceL1, true, "\n"));
				} else {
					showToast(dirTemp4Search + " is not existed");
				}
			}
			Collections.sort(tempAppList, fileListSorter);
			return tempAppList;
		}

		@Override
		protected void onPostExecute(ArrayList<LayoutElement> result) {

			if (mSwipeRefreshLayout.isRefreshing()) {
				mSwipeRefreshLayout.setRefreshing(false);
			}
			dataSourceL1.addAll(result);
			selectedInList1.clear();
			srcAdapter.notifyDataSetChanged();
			selectionStatus1.setText(selectedInList1.size() 
									 + "/" + dataSourceL1.size());
			File file = new File(dirTemp4Search);
			diskStatus.setText(
				"Free " + Util.nf.format(file.getFreeSpace() / (1 << 20))
				+ " MiB. Used " + Util.nf.format((file.getTotalSpace() - file.getFreeSpace()) / (1 << 20))
				+ " MiB. Total " + Util.nf.format(file.getTotalSpace() / (1 << 20)) + " MiB");
			if (dataSourceL1.size() == 0) {
				nofilelayout.setVisibility(View.VISIBLE);
				mSwipeRefreshLayout.setVisibility(View.GONE);
			} else {
				nofilelayout.setVisibility(View.GONE);
				mSwipeRefreshLayout.setVisibility(View.VISIBLE);
			}
		}
	}

    private void setSearchMode(final boolean search) {
		Log.d(TAG, "setSearchMode " + searchMode + ", " + currentPathTitle + ", " + dirTemp4Search);
		if (search) {
			if (currentPathTitle != null) {
				dirTemp4Search = currentPathTitle;
			}
			currentPathTitle = null;
			searchMode = true;
		} else {
			currentPathTitle = dirTemp4Search;
			searchMode = false;
		}
		Log.d(TAG, "setSearchMode " + searchMode + ", " + currentPathTitle + ", " + dirTemp4Search);
	}

	void setAllCbxChecked(boolean en) {
		allCbx.setSelected(en);
		if (en) {
			allCbx.setImageResource(R.drawable.ic_accept);
		} else {
			allCbx.setImageResource(R.drawable.dot);
		}
	}

	private void moreInPanel(final View v) {
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
		if (right.getVisibility() == View.VISIBLE) {
			mi.setTitle("Hide");
		} else {
			mi.setTitle("2 panels");
		}
        mi = menu.findItem(R.id.biggerequalpanel);
		if (left.getVisibility() == View.GONE || right.getVisibility() == View.GONE) {
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
					Log.d(TAG, item.getTitle() + ".");
					switch (item.getItemId())  {
						case (R.id.hiddenfiles):
//							for (LayoutElement le : selectedInList1) {
//								dataUtils.addHiddenFile(le.path);
//										if (new File(le.path).isDirectory()) {
//											File f1 = new File(le.path + "/.nomedia");
//											if (!f1.exists()) {
//												try {
//													com.amaze.filemanager.filesystem.FileUtil.mkfile(f1, activity);
//													//activity.mainActivityHelper.mkFile(new HFile(OpenMode.FILE, le.path), Frag.this);
//												} catch (Exception e) {
//													e.printStackTrace();
//												}
//											}
//											Futils.scanFile(le.path, getActivity());
//										}
//							}
//							updateList();
							GeneralDialogCreation.showHiddenDialog(activity.dataUtils, activity.getFutils(), ContentFragment.this, activity.getAppTheme());
							break;
						case R.id.rangeSelection:
							int min = Integer.MAX_VALUE, max = -1;
							int cur = -3;
							for (LayoutElement s : selectedInList1) {
								cur = dataSourceL1.indexOf(s);
								if (cur > max) {
									max = cur;
								}
								if (cur < min && cur >= 0) {
									min = cur;
								}
							}
							selectedInList1.clear();
							for (cur = min; cur <= max; cur++) {
								selectedInList1.add(dataSourceL1.get(cur));
							}
							srcAdapter.notifyDataSetChanged();
							break;
						case R.id.inversion:
							tempSelectedInList1.clear();
							for (LayoutElement f : dataSourceL1) {
								if (!selectedInList1.contains(f)) {
									tempSelectedInList1.add(f);
								}
							}
							selectedInList1.clear();
							selectedInList1.addAll(tempSelectedInList1);
							srcAdapter.notifyDataSetChanged();
							break;
						case R.id.clearSelection:
							tempSelectedInList1.clear();
							tempSelectedInList1.addAll(selectedInList1);
							selectedInList1.clear();
							srcAdapter.notifyDataSetChanged();
							break;
						case R.id.undoClearSelection:
							selectedInList1.clear();
							selectedInList1.addAll(tempSelectedInList1);
							tempSelectedInList1.clear();
							srcAdapter.notifyDataSetChanged();
							break;
						case R.id.swap:
//							if (spanCount == 8) {
//								spanCount = 4;
//							}
//							AndroidUtils.setSharedPreference(getContext(), "SPAN_COUNT", spanCount);
							activity.swap(v);
							break;
						case R.id.hide: 
							if (right.getVisibility() == View.VISIBLE && left.getVisibility() == View.VISIBLE) {
								if (spanCount == 4) {
									spanCount = 8;
									setRecyclerViewLayoutManager();
								}
								if (activity.swap) {
									left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
									right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
								} else {
									left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_in_right));
									right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_out_right));
								}
								if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT)
									left.setVisibility(View.GONE);
								else
									right.setVisibility(View.GONE);
							} else {
								if (spanCount == 8) {
									spanCount = 4;
									setRecyclerViewLayoutManager();
								}
								if (activity.swap) {
									left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
									right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_left));
								} else {
									left.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_in_right));
									right.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.slide_out_right));
								}
								right.setVisibility(View.VISIBLE);
							}
							break;
						case R.id.biggerequalpanel:
							if (activity.leftSize <= 0) {
								if (slidingTabsFragment.side == SlidingTabsFragment.Side.LEFT) {
									LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)activity.left.getLayoutParams();
									params.weight = 1.0f;
									activity.left.setLayoutParams(params);
									params = (LinearLayout.LayoutParams)activity.right.getLayoutParams();
									params.weight = 2.0f;
									activity.right.setLayoutParams(params);
									activity.leftSize = 1;
									if (left == activity.left) {
										slidingTabsFragment.width = 1;
										//activity.leftSize = width.width;
										activity.slideFrag2.width = -slidingTabsFragment.width;
									} else {
										slidingTabsFragment.width = -1;
										//activity.leftSize = -width.width;
										activity.slideFrag2.width = -slidingTabsFragment.width;
									}
								} else {
									LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)activity.left.getLayoutParams();
									params.weight = 2.0f;
									activity.left.setLayoutParams(params);
									params = (LinearLayout.LayoutParams)activity.right.getLayoutParams();
									params.weight = 1.0f;
									activity.right.setLayoutParams(params);
									activity.leftSize = 1;
									if (left == activity.left) {
										slidingTabsFragment.width = -1;
										//activity.leftSize = -width.width;
										activity.slideFrag.width = -slidingTabsFragment.width;
									} else {
										slidingTabsFragment.width = 1;
										//activity.leftSize = width.width;
										activity.slideFrag.width = -slidingTabsFragment.width;
									}
								}
							} else {
								LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)left.getLayoutParams();
								params.weight = 1.0f;
								left.setLayoutParams(params);
								params = (LinearLayout.LayoutParams)right.getLayoutParams();
								params.weight = 1.0f;
								right.setLayoutParams(params);
								activity.leftSize = 0;
								//width.width = 0;
								activity.slideFrag.width = 0;
								activity.slideFrag2.width = 0;
							}
							activity.curSelectionFrag2.setRecyclerViewLayoutManager();
							activity.curExplorerFrag.setRecyclerViewLayoutManager();
							AndroidUtils.setSharedPreference(activity, "biggerequalpanel", activity.leftSize);

					}
					return true;
				}
			});
		popup.show();
	}
}
