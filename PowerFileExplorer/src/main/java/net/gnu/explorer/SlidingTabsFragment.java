package net.gnu.explorer;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.gnu.common.view.SlidingHorizontalScroll;
import android.support.v7.widget.*;
import android.support.v4.app.*;
import net.gnu.texteditor.*;
import net.gnu.explorer.SlidingTabsFragment.*;
import android.os.*;
import java.util.*;
import android.view.animation.*;
import android.content.Intent;
import android.widget.Button;
import android.graphics.PorterDuff;
import android.app.Activity;
import net.gnu.util.Util;
import com.amaze.filemanager.utils.OpenMode;

public class SlidingTabsFragment extends Fragment implements TabAction {

	private static final String TAG = "SlidingTabsFragment";
	private FragmentManager childFragmentManager;

	private SlidingHorizontalScroll mSlidingHorizontalScroll;

	private ViewPager mViewPager;
	public PagerAdapter pagerAdapter;
	int pageSelected = 1;

	private ArrayList<PagerItem> mTabs = new ArrayList<PagerItem>();
	static final enum Side {LEFT, RIGHT, MONO};
	Side side = Side.LEFT;
	int width;

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
							 final Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		Log.d(TAG, "onCreateView " + savedInstanceState);
		return inflater.inflate(R.layout.fragment_sample, container, false);
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final Bundle args = getArguments();
		Log.d(TAG, "onViewCreated args " + args + ", savedInstanceState " + savedInstanceState + ", " + side);

		mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
		if (childFragmentManager == null) {
			childFragmentManager = getChildFragmentManager();
		}

		if (savedInstanceState == null) {
			if (args == null) {
				//initContentFragmentTabs();
			} else {
				final int no = args.getInt("no");
				for (int i = 0; i < no; i++) {
					Log.d(TAG, no + " onViewCreated args.getString(ExplorerActivity.EXTRA_DIR_PATH" + i + ")=" + args.getString(ExplorerActivity.EXTRA_ABSOLUTE_PATH + i));
					ContentFragment cf = ContentFragment.newInstance(SlidingTabsFragment.this, 
																	 args.getString(ExplorerActivity.EXTRA_ABSOLUTE_PATH + i),
																	 args.getString(ExplorerActivity.EXTRA_FILTER_FILETYPE + i),
																	 args.getString(ExplorerActivity.EXTRA_FILTER_MIMETYPE + i),
																	 args.getBoolean(ExplorerActivity.EXTRA_MULTI_SELECT + i),
																	 args.getBundle("frag" + i));
					mTabs.add(new PagerItem(cf));
				}
			}

			Log.d(TAG, "onViewCreated " + mTabs);
			pagerAdapter = new PagerAdapter(childFragmentManager);
			mViewPager.setAdapter(pagerAdapter);
			Log.d(TAG, "mViewPager " + mViewPager);
			if (args != null) {
				mViewPager.setCurrentItem(args.getInt("pos", pageSelected), true);
			} else {
				mViewPager.setCurrentItem(pageSelected);
			}
		} else {
			mTabs.clear();
			final List<Fragment> fragments = childFragmentManager.getFragments();
			final String firstTag = savedInstanceState.getString("fake0");
			final String lastTag = savedInstanceState.getString("fakeEnd");
			String tag;
			PagerItem pagerItem;
			Frag frag;
			final int size = fragments.size();
			for (int i = 0; i < size; i++) {
				tag = savedInstanceState.getString(i + "");
				frag = (Frag) childFragmentManager.findFragmentByTag(tag);
				if (frag != null) {
					pagerItem = new PagerItem(frag);
					//Log.d(TAG, "onViewCreated frag " + i + ", " + tag + ", " + frag.getTag() + ", " + pagerItem.dir + ", " + frag);
					mTabs.add(pagerItem);
				}
			}
			if (firstTag != null) {
				mTabs.get(0).fakeFrag = (Frag) childFragmentManager.findFragmentByTag(firstTag);
				int tabSize = mTabs.size();
				mTabs.get(tabSize - 1).fakeFrag = (Frag) childFragmentManager.findFragmentByTag(lastTag);
			}

			//Log.d(TAG, "mTabs=" + mTabs);
			//Log.d(TAG, "fragments=" + fragments);
			pagerAdapter = new PagerAdapter(childFragmentManager);
			mViewPager.setAdapter(pagerAdapter);
			mViewPager.setCurrentItem(savedInstanceState.getInt("pos", pageSelected), true);
		}
		mViewPager.setOffscreenPageLimit(16);

		// Give the SlidingTabLayout the ViewPager, this must be done AFTER the
		// ViewPager has had it's PagerAdapter set.
		mSlidingHorizontalScroll = (SlidingHorizontalScroll) view.findViewById(R.id.sliding_tabs);
		mSlidingHorizontalScroll.fra = SlidingTabsFragment.this;
		tabClicks = new TabClicks(12);

		mSlidingHorizontalScroll.setViewPager(mViewPager);
		mSlidingHorizontalScroll.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

				@Override
				public void onPageScrolled(final int pageSelected, final float positionOffset,
										   final int positionOffsetPixel) {
//					Log.e("onPageScrolled", "pageSelected: " + pageSelected
//						+ ", positionOffset: " + positionOffset
//						+ ", positionOffsetPixel: " + positionOffsetPixel);
					if (positionOffset == 0 && positionOffsetPixel == 0) {
						final int size = mTabs.size();
						if (size > 1) {
							if (pageSelected == 0) {
								mViewPager.setCurrentItem(size, false);
							} else if (pageSelected == size + 1) {
								mViewPager.setCurrentItem(1, false);
							}
						}
					} 
				}

				@Override
				public void onPageSelected(final int position) {
					final int size = mTabs.size();
					Log.d(TAG, "onPageSelected: " + position + ", mTabs.size() " + size + ", side " + side);
//					if (position==3) {
//						throw new RuntimeException("here");
//					}
					pageSelected = position;
					if (size > 1) {
						if (position == 1 || position == size) {
							final int newpos = position == 1 ? (size - 1) : position == size ? 0 : (position - 1);
							final PagerItem pi = mTabs.get(newpos);
							Log.d(TAG, "onPageSelected: " + position + ", side " + side + ", pi.frag " + pi.frag + ", pi.fakeFrag " + pi.fakeFrag);
							if (pi.fakeFrag != null) {
								pi.fakeFrag.clone(pi.frag, true);
								if (pi.fakeFrag.status != null) {
									pi.fakeFrag.status.setBackgroundColor(ExplorerActivity.IN_DATA_SOURCE_2);
								}
								if (pi.frag instanceof FileFrag && ((FileFrag)pi.frag).gridLayoutManager != null) {
									final int index = ((FileFrag)pi.frag).gridLayoutManager.findFirstVisibleItemPosition();
									final View vi = ((FileFrag)pi.frag).listView.getChildAt(0); 
									final int top = (vi == null) ? 0 : vi.getTop();
									((FileFrag)pi.fakeFrag).gridLayoutManager.scrollToPositionWithOffset(index, top);
								}
							} else {
								pi.createFakeFragment();
							}
						}
					}
					final Frag createFragment = pagerAdapter.getItem(position);
					final Activity activ = getActivity();
					if (activ instanceof ExplorerActivity) {
						final ExplorerActivity activity = (ExplorerActivity) activ;
						if (createFragment.type == Frag.TYPE.EXPLORER) {
							activity.dir = createFragment.currentPathTitle;
						}
						if (createFragment.type == Frag.TYPE.EXPLORER) {
							if (side == Side.LEFT) {
								activity.curContentFrag = (ContentFragment) createFragment;
							} else {
								activity.curExplorerFrag = (ContentFragment) createFragment;
							}
						} else if (createFragment.type == Frag.TYPE.SELECTION) {
							activity.curSelectionFrag2 = (ContentFragment) createFragment;
						}
						createFragment.select(true);
						if (side == Side.LEFT) {
							//activity.slideFrag2.getCurrentFragment().select(false);
							//createFragment.horizontalDivider6 = activity.slideFrag2.getCurrentFragment().horizontalDivider6;
							//createFragment.rightCommands = activity.slideFrag2.getCurrentFragment().rightCommands;
							//Log.d(TAG, "createFragment.leftCommands: " + createFragment.leftCommands);
							//Log.d(TAG, "createFragment.rightCommands: " + createFragment.rightCommands);
							//createFragment.commands = createFragment.leftCommands;
							createFragment.right = activity.right;
							createFragment.left = activity.left;
							//createFragment.horizontalDivider6 = createFragment.horizontalDivider6;//createFragment.horizontalDivider11;
							createFragment.slidingTabsFragment.width = activity.leftSize;
						} else {
							//createFragment.horizontalDivider11 = activity.slideFrag.getCurrentFragment().horizontalDivider11;
							//createFragment.leftCommands = activity.slideFrag.getCurrentFragment().leftCommands;
							//Log.d(TAG, "createFragment.leftCommands: " + createFragment.leftCommands);
							//Log.d(TAG, "createFragment.rightCommands: " + createFragment.rightCommands);
							//if (activity.slideFrag2.pagerAdapter != null && activity.slideFrag2.getContentFragment2(Frag.TYPE.EXPLORER) == createFragment) {
							//createFragment.commands = createFragment.rightCommands;
							createFragment.right = activity.left;
							createFragment.left = activity.right;
							//createFragment.horizontalDivider6 = createFragment.horizontalDivider6;
							createFragment.slidingTabsFragment.width = -activity.leftSize;
						}

						if (createFragment.commands != null) {
							if (createFragment.selectedInList1.size() == 0 && 
								(((createFragment instanceof ContentFragment) && activity.COPY_PATH == null && activity.MOVE_PATH == null) 
								|| (!(createFragment instanceof ContentFragment)))) {
								if (createFragment.commands.getVisibility() == View.VISIBLE) {
									createFragment.horizontalDivider6.setVisibility(View.GONE);
									createFragment.commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.shrink_from_top));
									createFragment.commands.setVisibility(View.GONE);
								}
							} else {
								createFragment.commands.setAnimation(AnimationUtils.loadAnimation(activity, R.anim.grow_from_bottom));
								createFragment.commands.setVisibility(View.VISIBLE);
								createFragment.horizontalDivider6.setVisibility(View.VISIBLE);
								if (createFragment instanceof ContentFragment) {
									((ContentFragment)createFragment).updateDelPaste();
								}
							}
						} 
					} else {

					}
				}

				@Override
				public void onPageScrollStateChanged(final int state) {
					Log.d(TAG, "onPageScrollStateChanged1 state " + state + ", pageSelected " + pageSelected + ", " + side);
					if (state == 0) {
						final int size = mTabs.size();
						if (pageSelected == 0) {
							mViewPager.setCurrentItem(size, false);
							pageSelected = size;
						} else if (pageSelected == size + 1) {
							mViewPager.setCurrentItem(1, false);
							pageSelected = 1;
						}
					}
					Log.d(TAG, "onPageScrollStateChanged2 state " + state + ", pageSelected " + pageSelected + ", " + side);
				}
			});
		//Log.d(TAG, "mSlidingHorizontalScroll " + mSlidingHorizontalScroll);
		mSlidingHorizontalScroll.setCustomTabColorizer(new SlidingHorizontalScroll.TabColorizer() {
				@Override
				public int getIndicatorColor(int position) {
					return 0xffff0000;
				}
				@Override
				public int getDividerColor(int position) {
					return 0xff888888;
				}
			});
	}

	void initLeftContentFragmentTabs() {
		File storage = new File("/storage");
		File[] fs = storage.listFiles();

		//String[] st = sdCardPath.split(":");
		File f;
		Bundle bundle;
		ContentFragment fragment;
		for (int i = fs.length - 1; i >= 0; i--) {
			f = fs[i];
			Log.d(TAG, f + ".");
			if (f.canWrite()) {
				bundle = new Bundle();
				bundle.putString(ExplorerActivity.EXTRA_ABSOLUTE_PATH, f.getAbsolutePath());//EXTRA_DIR_PATH
				bundle.putString(ExplorerActivity.EXTRA_FILTER_FILETYPE, "*");
				bundle.putBoolean(ExplorerActivity.EXTRA_MULTI_SELECT, true);

				fragment = new ContentFragment();
				fragment.type = Frag.TYPE.EXPLORER;
				fragment.setArguments(bundle);
				fragment.slidingTabsFragment = this;
				mTabs.add(new PagerItem(fragment));//f.getAbsolutePath(), ".*", true, null));
			}
		}
	}

	void addTab(final String[] previousSelectedStr) {
		mTabs.add(new PagerItem(Frag.getFrag(this, Frag.TYPE.SELECTION, Util.arrayToString(previousSelectedStr, false, "|"))));
	}

	Fragment addTab(final String path, final String suffix, final String mimes, final boolean multi) {

		final Frag fragment = new ContentFragment();
		fragment.type = Frag.TYPE.EXPLORER;

		final Bundle bundle = new Bundle();
		bundle.putString(ExplorerActivity.EXTRA_ABSOLUTE_PATH, path == null ? "/storage" : path);//EXTRA_DIR_PATH
		bundle.putString(ExplorerActivity.EXTRA_FILTER_FILETYPE, suffix);
		bundle.putString(ExplorerActivity.EXTRA_FILTER_MIMETYPE, mimes);
		bundle.putBoolean(ExplorerActivity.EXTRA_MULTI_SELECT, multi);
		//bundle.putStringArray(ExplorerActivity.PREVIOUS_SELECTED_FILES, previousSelectedStr);
		fragment.setArguments(bundle);

		fragment.slidingTabsFragment = this;
		mTabs.add(new PagerItem(fragment));
		return fragment;
	}

	public boolean circular() {
		return true;
	}

	public void addTab(final Intent intent, String title) {
		if (title == null || title.length() == 0) {
			title = "Untitled " + ++Main.no + ".txt";
		}
		Log.d(TAG, "addTab1 pagerAdapter=" + pagerAdapter + ", filename=" + title + ", mTabs=" + mTabs);
		final Main main = Main.newInstance(intent, title, null);
		final PagerItem pagerItem = new PagerItem(main);
		main.slidingTabsFragment = this;

		mTabs.add(pageSelected, pagerItem);
		if (mViewPager != null) {
			pagerAdapter.notifyDataSetChanged();
			mViewPager.setCurrentItem(pagerAdapter.getCount() - 1);
			notifyTitleChange();
			//main.onPrepareOptionsMenu(((MainActivity)getActivity()).menu);
		}
		Log.d(TAG, "addTab2 " + title + ", " + mTabs);
	}

	public void addTab(final OpenMode openmode, final String path) {
		final Frag frag = Frag.getFrag(this, Frag.TYPE.EXPLORER, path);
		((ContentFragment)frag).openMode = openmode;
		final PagerItem pagerItem = new PagerItem(frag);
		addFrag(frag, pagerItem);
	}

	public void addTab(final Frag.TYPE t, final String path) {
		final PagerItem pagerItem;
		Frag frag = null;
		if (t == null) {
			frag = getCurrentFragment();
			if (getActivity() instanceof TextEditorActivity) {
				final Main main = Main.newInstance(null, "Untitled " + ++Main.no + ".txt", path);
				main.slidingTabsFragment = this;
				pagerItem = new PagerItem(main);
			} else if (frag.type == Frag.TYPE.EXPLORER) {
				pagerItem = new PagerItem(frag.clone(false));
			} else {
				return;
			}
		} else {
			frag = Frag.getFrag(this, t, path);
			pagerItem = new PagerItem(frag);
		}

		addFrag(frag, pagerItem);
	}

	private void addFrag(Frag frag, PagerItem pagerItem) {
		final FragmentTransaction ft = childFragmentManager.beginTransaction();
		final ArrayList<PagerItem> mTabs2 = new ArrayList<PagerItem>(mTabs);
		final int size = mTabs.size();
		if (size > 1) {
			int currentItem = mViewPager.getCurrentItem();
			Log.d(TAG, "addTab1 currentItem " + currentItem + ", dir=" + frag.currentPathTitle + ", mTabs=" + mTabs);

			PagerItem pi = mTabs.get(0);
			ft.remove(pi.fakeFrag);
			pi.fakeFrag = null;

			pi = mTabs.get(size - 1);
			ft.remove(pi.fakeFrag);
			pi.fakeFrag = null;

			for (int j = 0; j < size; j++) {
				ft.remove(mTabs.remove(0).frag);
			}
			pagerAdapter.notifyDataSetChanged();
			ft.commitNow();

			for (PagerItem pi2 : mTabs2) {
				mTabs.add(pi2);
			}
			mTabs.add(currentItem++, pagerItem);
			mViewPager.setAdapter(pagerAdapter);
			mViewPager.setCurrentItem(currentItem, false);
		} else {
			PagerItem remove = mTabs.remove(0);
			ft.remove(remove.frag);
			pagerAdapter.notifyDataSetChanged();
			ft.commitNow();
			mTabs.add(remove);
			mTabs.add(pagerItem);
			pagerAdapter.notifyDataSetChanged();
			mViewPager.setCurrentItem(mTabs.size());
		}
		notifyTitleChange();
		Log.d(TAG, "addTab2 " + frag.currentPathTitle + ", mViewPager.getCurrentItem() " + mViewPager.getCurrentItem() + ", " + mTabs);
	}

	public int size() {
		return mTabs.size();
	}

	public void closeTab(Frag m) {
		int i = 0;
		final ArrayList<PagerItem> mTabs2 = new ArrayList<PagerItem>(mTabs);
		for (PagerItem pi : mTabs) {
			if (pi.frag == m) {
				Log.i(TAG, "closeTab " + i);
				break;
			}
			i++;
		}
		Log.i(TAG, "closeTab " + i + ", " + m + ", " + mTabs);
		final FragmentTransaction ft = childFragmentManager.beginTransaction();
		SlidingTabsFragment.PagerItem pi;
		if (mTabs.size() > 1) {
			pi = mTabs.get(0);
			ft.remove(pi.fakeFrag);
			pi.fakeFrag = null;
			pi = mTabs.get(mTabs.size() - 1);
			ft.remove(pi.fakeFrag);
			pi.fakeFrag = null;
		}
		for (int j = mTabs2.size() - 1; j >= i; j--) {
			ft.remove(mTabs.remove(j).frag);
		}
		if (mTabs.size() == 1 && mTabs2.size() == 2) {
			pi = mTabs.remove(0);
			ft.remove(pi.frag);
			pi.fakeFrag = null;
		}
		//mTabs.clear();
		pagerAdapter.notifyDataSetChanged();
		ft.commitNow();

		mTabs2.remove(i);

		if (mTabs.size() == 0 && i > 0) {
			mTabs.add(mTabs2.get(0));
		}
		for (int j = i; j < mTabs2.size(); j++) {
			mTabs.add(mTabs2.get(j));
		}
		pagerAdapter.notifyDataSetChanged();
		mTabs2.clear();
		notifyTitleChange();
		mViewPager.setCurrentItem(i <= mTabs.size() - 1 && mTabs.size() > 1 ? i + 1: mTabs.size() == 1 ? 0 : i);
	}

	public void closeCurTab() {
		final Frag main = getCurrentFragment();
		Log.d(TAG, "closeCurTab " + main);
		closeTab(main);
	}

	public void closeOtherTabs() {
		final Frag curFrag = getCurrentFragment();
		Log.d(TAG, "closeOtherTabs " + curFrag);
		final int size = mTabs.size();
		final int curIndex = indexOfMTabs(curFrag);
		final int curExplore;
		final Activity activ = getActivity();
		if (curFrag.type == Frag.TYPE.EXPLORER || !(activ instanceof ExplorerActivity)) {
			curExplore = -1;
		} else {
			final ExplorerActivity activity = (ExplorerActivity) activ;
			if (side == Side.LEFT) {
				curExplore = indexOfMTabs(activity.curContentFrag);
				if (activity.curSelectionFrag != curFrag) {
					activity.curSelectionFrag = null;
				}
			} else 	{
				curExplore = indexOfMTabs(activity.curExplorerFrag);
				if (activity.curSelectionFrag2 != curFrag) {
					activity.curSelectionFrag2 = null;
				}
			}
		}
		final ArrayList<PagerItem> mTabs2 = new ArrayList<PagerItem>(mTabs);

		final FragmentTransaction ft = childFragmentManager.beginTransaction();
		SlidingTabsFragment.PagerItem pi = mTabs.get(0);
		ft.remove(pi.fakeFrag);
		pi.fakeFrag = null;
		pi = mTabs.get(size - 1);
		ft.remove(pi.fakeFrag);
		pi.fakeFrag = null;
		for (int j = 0; j < size; j++) {
			ft.remove(mTabs.remove(0).frag);
		}
		pagerAdapter.notifyDataSetChanged();
		ft.commitNow();

		if (curExplore >= 0) {
			mTabs.add(mTabs2.get(curExplore));
		}
		mTabs.add(mTabs2.get(curIndex));
		mTabs2.clear();
		pagerAdapter.notifyDataSetChanged();
		notifyTitleChange();
		mViewPager.setCurrentItem(0);
	}

	public Frag getCurrentFragment() {
		final int currentItem = mViewPager.getCurrentItem();
		Log.d(TAG, "getCurrentFragment = " + currentItem + ", " + side + ", " + mTabs);
		return pagerAdapter.getItem(currentItem);
	}

	public Frag getFragmentIndex(final int idx) {
		Log.d(TAG, "getContentFragment index " + idx + ", " + side + ", " + mTabs);
		return pagerAdapter.getItem(idx);
	}

	public int indexOfAdapter(final Frag frag) {
		int i = 0;
		for (PagerItem pi : mTabs) {
			//Log.d(TAG, "indexOf frag " + frag + ", pi.frag " + pi.frag);
			if (frag == pi.frag) {
				return mTabs.size() == 1 ? i : i + 1;
			} else {
				i++;
			}
		}
		return -1;
	}

	public int indexOfMTabs(final Frag frag) {
		int i = 0;
		for (PagerItem pi : mTabs) {
			//Log.d(TAG, "indexOf frag " + frag + ", pi.frag " + pi.frag);
			if (frag == pi.frag) {
				return i;
			} else {
				i++;
			}
		}
		return -1;
	}

	public int getFragIndex(final Frag.TYPE t) {
		final int count = pagerAdapter.getCount();
		if (count > 1) {
			for (int i = 1; i < count - 1; i++) {
				if (pagerAdapter.getItem(i).type == t) {
					return i;
				}
			}
			return -1;
		} else {
			return pagerAdapter.getItem(0).type == t ? 0 : -1;
		}
	}

	void updateLayout(final boolean changeTime) {
		Log.d(TAG, "updateLayout " + changeTime);
		for (PagerItem pi : mTabs) {
			if (pi.frag != null) {
				if (pi.frag instanceof FileFrag) {
					((FileFrag)pi.frag).refreshRecyclerViewLayoutManager();
				}
				if (changeTime && pi.frag.getContext() != null) {
					pi.frag.updateColor(null);
					if (pi.frag instanceof ContentFragment) {
						((ContentFragment)pi.frag).setDirectoryButtons();
					}
				}
				final int no = pi.frag.commands.getChildCount();
				Button b;
				for (int i = 0; i < no; i++) {
					b = (Button) pi.frag.commands.getChildAt(i);
					b.setTextColor(ExplorerActivity.TEXT_COLOR);
					b.getCompoundDrawables()[1].setAlpha(0xff);
					b.getCompoundDrawables()[1].setColorFilter(ExplorerActivity.TEXT_COLOR, PorterDuff.Mode.SRC_IN);
				}

			}
		}
	}

//	Bundle saveStates() {
//		final Bundle b = new Bundle();
//		final int size = mTabs.size();
//		b.putInt("no", size);
//		for (int i = 0; i < size; i++) {
//			Frag createFragment = mTabs.get(i).createFragment(this);
//			b.putString(ExplorerActivity.EXTRA_DIR_PATH + i, createFragment.currentPathTitle);
//			b.putString(ExplorerActivity.EXTRA_SUFFIX + i, createFragment.suffix);
//			b.putBoolean(ExplorerActivity.EXTRA_MULTI_SELECT + i, createFragment.multiFiles);
//			Bundle bfrag = new Bundle();
//			createFragment.onSaveInstanceState(bfrag);
//			b.putBundle("frag" + i, bfrag);
//		}
//		b.putInt("pos", mViewPager.getCurrentItem());
//		return b;
//	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		//Log.d(TAG, "onSaveInstanceState1 " + outState + ", " + childFragmentManager.getFragments());
		try {
			if (mTabs != null && mTabs.size() > 0) {
				int i = 0;
				for (PagerItem pi : mTabs) {
					Log.d(TAG, "onSaveInstanceState pi.frag.getTag() " + pi.frag.getTag() + ", " + side + ", " + pi);
					//childFragmentManager.putFragment(outState, "tabb" + i++, pi.frag);
					outState.putString(i++ + "", pi.frag.getTag());
					outState.putString(pi.frag.getTag(), pi.frag.currentPathTitle);
				}
				if (mTabs.size() > 1) {
					//Log.d(TAG, "fakeStart 0 tag" + mTabs.get(0).fakeFrag.getTag());
					outState.putString("fake0", mTabs.get(0).fakeFrag.getTag());
					//Log.d(TAG, "fakeEnd tag  " + mTabs.get(mTabs.size()-1).fakeFrag.getTag());
					outState.putString("fakeEnd", mTabs.get(mTabs.size() - 1).fakeFrag.getTag());
				}
			}
			outState.putInt("pos", mViewPager.getCurrentItem());
		} catch (Exception e) {
			// Logger.log(e,"puttingtosavedinstance",getActivity());
			e.printStackTrace();
		}
		//Log.d(TAG, "onSaveInstanceState2 " + outState + ", " + childFragmentManager);
	}


	public void notifyTitleChange() {
		mSlidingHorizontalScroll.setViewPager(mViewPager);
	}

	public void setCurrentItem(int pos, boolean smooth) {
		mViewPager.setCurrentItem(pos, smooth);
	}

	void addPagerItem(final Frag frag1) {
		mTabs.add(new PagerItem(frag1));
	}

	private class PagerItem implements Parcelable {
		private static final String TAG = "PagerItem";
		private Frag frag;
		private Frag fakeFrag;

		private PagerItem(final Frag frag1) {
			//Log.d(TAG, "tag=" + frag1.getTag() + ", " + frag1);
			this.frag = frag1;
			this.frag.slidingTabsFragment = SlidingTabsFragment.this;
		}

		protected PagerItem(Parcel in) {
			frag = (ContentFragment) in.readSerializable();
		}

		@Override
		public int describeContents() {
			return frag.type.ordinal();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeSerializable(frag);
		}

		public final Parcelable.Creator<PagerItem> CREATOR = new Parcelable.Creator<PagerItem>() {
			public PagerItem createFromParcel(Parcel in) {
				return new PagerItem(in);
			}

			public PagerItem[] newArray(int size) {
				return new PagerItem[size];
			}
		};

		@Override
		public Object clone() {
			return new PagerItem(frag.clone(true));
		}

		/**
		 * @return A new {@link Fragment} to be displayed by a {@link ViewPager}
		 */
//		private Frag createFragment(SlidingTabsFragment s) {
//			Log.d(TAG, "createFragment() " + frag);
////			if (frag == null) {
////				//frag = ContentFragment.newInstance(s, dir, suffix, multi, bundle);
////				frag = new Frag();
//			frag.slidingTabsFragment = s;
//			//}
////			if (fakeFrag != null) {
////				fakeFrag.clone(frag);
////			}
//			return frag;
//		}

		private Frag createFakeFragment() {
			Log.d(TAG, "createFakeFragment() fakeFrag " + fakeFrag + ", frag " + frag);
//			if (frag == null && fakeFrag == null) {
//				//fakeFrag = ContentFragment.newInstance(s, dir, suffix, multi, bundle);
//				fakeFrag = frag.clone();//createFragment(s).clone();
//			} else 
			if (fakeFrag == null) {
				//fakeFrag = ContentFragment.newOriFakeInstance(frag);
				fakeFrag = frag.clone(true);
			} else if (fakeFrag != null && frag != null) {
				fakeFrag.clone(frag, true);
				//fakeFrag.refreshDirectory();
			}
			//fakeFrag.slidingTabsFragment = s;
			return fakeFrag;
		}

		public String getTitle() {
			return frag.getTitle();
		}

		@Override
		public String toString() {
			return "frag=" + frag + ", fakeFrag=" + fakeFrag;
		}
	}

	public class PagerAdapter extends FragmentPagerAdapter {
		int numOfPages = 1;

		PagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public float getPageWidth(int position) {
			return 1f / numOfPages;
		}

		@Override
		public Frag getItem(final int position) {
			final int size = mTabs.size();
			Log.d(TAG, "getItem " + position + "/" + size + ", " + side);
			if (size > 1) {
				if (position == 0) {
					return mTabs.get(size - 1).createFakeFragment();
				} else if (position == size + 1) {
					return mTabs.get(0).createFakeFragment();
				} else {
					return mTabs.get(position - 1).frag;
				}
			} else {
				return mTabs.get(0).frag;
			}
		}

		@Override
		public int getCount() {
			final int size = mTabs.size();
			if (size > 1) {
				return size + 2;
			} else {
				return size;
			}
		}

		@Override
		public CharSequence getPageTitle(final int position) {
			final int size = mTabs.size();
			if (size > 1) {
				if (position == 0 || position == size + 1) {
					return "";
				} else {
					return mTabs.get(position - 1).getTitle();
				}
			} else {
				return mTabs.get(position).getTitle();
			}
		}

		@Override
		public int getItemPosition(final Object object) {
			for (PagerItem pi : mTabs) {
				if (pi.frag == object) {
					//Log.d(TAG, "getItemPosition POSITION_UNCHANGED" + ", " + object);
					return POSITION_UNCHANGED;
				}
			}
			//Log.d(TAG, "getItemPosition POSITION_NONE" + ", " + object);
			return POSITION_NONE;
		}
	}

}
