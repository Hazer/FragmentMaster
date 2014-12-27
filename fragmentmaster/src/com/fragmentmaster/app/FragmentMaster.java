package com.fragmentmaster.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.KeyEventCompat2;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.fragmentmaster.animator.PageAnimator;
import com.fragmentmaster.app.ware.FragmentManagerWare;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class FragmentMaster<Fragment, FragmentManager> {

    private static final String TAG = "FragmentMaster";

    // The host activity.
    private final MasterActivity mActivity;

    private FragmentManagerWare<Fragment, FragmentManager> mFragmentManagerWare;

    private int mContainerResID = 0;

    private ViewGroup mContainer;

    private boolean mIsSlideable = false;

    private boolean mIsInstalled = false;

    private boolean mSticky = false;

    private boolean mHomeFragmentApplied = false;

    private PageAnimator mPageAnimator = null;

    // Fragments started by FragmentMaster.
    private ArrayList<IMasterFragment<Fragment>> mFragments = new ArrayList<>();

    private IMasterFragment<Fragment> mPrimaryFragment = null;

    private HashSet<IMasterFragment<Fragment>> mFinishPendingFragments = new HashSet<>();

    // Events callback
    private Callback mCallback = null;

    public interface Callback {

        public boolean dispatchKeyEvent(KeyEvent event);

        public boolean dispatchKeyShortcutEvent(KeyEvent event);

        public boolean dispatchTouchEvent(MotionEvent event);

        public boolean dispatchTrackballEvent(MotionEvent event);

        public boolean dispatchGenericMotionEvent(MotionEvent event);
    }

    protected FragmentMaster(MasterActivity activity, FragmentManagerWare<Fragment, FragmentManager> fragmentManagerWare) {
        mActivity = activity;
        mFragmentManagerWare = fragmentManagerWare;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public int getContainerResID() {
        return mContainerResID;
    }

    protected int getFragmentContainerId() {
        return getContainerResID();
    }

    public final void startFragmentForResult(IMasterFragment<Fragment> target,
                                             Request request, int requestCode) {
        ensureInstalled();

        IMasterFragment<Fragment> fragment = (IMasterFragment<Fragment>) mFragmentManagerWare.newFragment(getActivity(), request.getClassName());
        fragment.setRequest(request);
        fragment.setTargetFragment(
                target == null ? null : target.getFragment(), requestCode);
        mFragmentManagerWare.addFragment(getFragmentContainerId(), fragment.getFragment());
        mFragments.add(fragment);
        fragment.setPrimary(false);
        setUpAnimator(fragment);
        onFragmentStarted(fragment);
    }

    protected void setUpAnimator(IMasterFragment<Fragment> fragment) {
        PageAnimator pageAnimator = null;
        if (fragment != null) {
            pageAnimator = fragment.onCreatePageAnimator();
        }
        this.setPageAnimator(pageAnimator);
    }

    protected abstract void onFragmentStarted(IMasterFragment<Fragment> fragment);

    public final void finishFragment(IMasterFragment<Fragment> fragment, int resultCode,
                                     Request data) {
        ensureInstalled();
        throwIfNotInFragmentMaster(fragment);
        if (!mFinishPendingFragments.contains(fragment)) {
            mFinishPendingFragments.add(fragment);
        }
        onFinishFragment(fragment, resultCode, data);
    }

    /**
     * Check whether the specific fragment is in FragmentMaster.
     * <p/>
     * </p> If a fragment is not in FragmentMaster, it may not be started by
     * FragmentMaster or has been finished already.
     *
     * @param fragment The fragment to check.
     * @return If the fragment is in FragmentMaster, returns true; else returns
     * false.
     */
    public boolean isInFragmentMaster(IMasterFragment<Fragment> fragment) {
        return mFragments.indexOf(fragment) >= 0;
    }

    /**
     * Check whether the specific fragment is pending to be finished.
     *
     * @param fragment The fragment to check.
     * @return If the fragment is pending to be finished, returns true; else
     * returns false.
     */
    public boolean isFinishPending(IMasterFragment<Fragment> fragment) {
        return mFinishPendingFragments.contains(fragment);
    }

    private void throwIfNotInFragmentMaster(IMasterFragment<Fragment> fragment) {
        if (!isInFragmentMaster(fragment)) {
            throw new IllegalStateException("Fragment {" + fragment
                    + "} not currently in FragmentMaster.");
        }
    }

    protected void onFinishFragment(IMasterFragment<Fragment> fragment, int resultCode,
                                    Request data) {
        doFinishFragment(fragment);
        deliverFragmentResult(fragment, resultCode, data);
    }

    protected final void doFinishFragment(IMasterFragment<Fragment> fragment) {
        int index = mFragments.indexOf(fragment);
        if (index == 0 && mSticky) {
            mActivity.finish();
            return;
        }

        mFragmentManagerWare.removeFragment(fragment.getFragment());
        mFragments.remove(index);
        mFinishPendingFragments.remove(fragment);

        IMasterFragment<Fragment> f = null;
        for (int i = index; i < mFragments.size(); i++) {
            f = mFragments.get(i);
            IMasterFragment<Fragment> target = (IMasterFragment<Fragment>) f.getTargetFragment();
            if (target == fragment) {
                f.setTargetFragment(null, -1);
            }
        }

        onFragmentFinished(fragment);
    }

    protected void deliverFragmentResult(IMasterFragment<Fragment> fragment,
                                         int resultCode, Request data) {
        Fragment targetFragment = fragment.getTargetFragment();
        int requestCode = fragment.getTargetRequestCode();
        if (requestCode != -1 && targetFragment instanceof IMasterFragment) {
            dispatchFragmentResult((IMasterFragment<Fragment>) targetFragment,
                    fragment.getTargetRequestCode(), resultCode, data);
        }
    }

    private void dispatchFragmentResult(IMasterFragment<Fragment> who, int requestCode,
                                        int resultCode, Request data) {
        if (who.isFinishing()) {
            return;
        }
        if (who.getTargetChildFragment() == null) {
            who.onFragmentResult(requestCode, resultCode, data);
        } else {
            dispatchFragmentResult(who.getTargetChildFragment(), requestCode,
                    resultCode, data);
        }
        who.setTargetChildFragment(null);
    }

    private void ensureInstalled() {
        if (!isInstalled()) {
            throw new IllegalStateException("Haven't installed.");
        }
    }

    protected abstract void onFragmentFinished(IMasterFragment<Fragment> fragment);

    public IMasterFragment<Fragment> getPrimaryFragment() {
        return mPrimaryFragment;
    }

    protected final void setPrimaryFragment(IMasterFragment<Fragment> fragment) {
        if (fragment != mPrimaryFragment) {
            if (mPrimaryFragment != null) {
                mPrimaryFragment.setPrimary(false);
            }
            if (fragment != null) {
                fragment.setPrimary(true);
            }
            mPrimaryFragment = fragment;
            // Only the primary fragment can receive events callback.
            setCallback(fragment);
        }
    }

    public List<IMasterFragment<Fragment>> getFragments() {
        return mFragments;
    }

    protected void setPageAnimator(PageAnimator pageAnimator) {
        mPageAnimator = pageAnimator;
    }

    public PageAnimator getPageAnimator() {
        return mPageAnimator;
    }

    public boolean hasPageAnimator() {
        return mPageAnimator != null;
    }

    public final void install(int containerResID, Request homeRequest,
                              boolean sticky) {
        if (isInstalled()) {
            throw new IllegalStateException("Already installed!");
        } else {
            mContainerResID = containerResID;
            checkInstallProperties();
            performInstall(mContainer);
            mIsInstalled = true;

            if (homeRequest != null) {
                applyHomeFragment(homeRequest, sticky);
            }
        }
    }

    private void applyHomeFragment(Request homeRequest, boolean sticky) {
        mSticky = sticky;
        if (!mHomeFragmentApplied) {
            startFragmentForResult(null, homeRequest, -1);
            mHomeFragmentApplied = true;
        }
    }

    private void checkInstallProperties() {
        View container = mActivity.findViewById(mContainerResID);
        if (container == null) {
            throw new RuntimeException("No view found for id 0x"
                    + Integer.toHexString(mContainerResID));
        } else {
            mContainer = (ViewGroup) container;
        }
    }

    protected abstract void performInstall(ViewGroup container);

    public boolean isInstalled() {
        return mIsInstalled;
    }

    public final void setSlideable(boolean slideable) {
        mIsSlideable = slideable;
    }

    public boolean isSlideable() {
        return hasPageAnimator() && mIsSlideable;
    }

    Parcelable saveAllState() {
        FragmentMasterState state = new FragmentMasterState();
        Bundle fragments = null;
        for (int i = 0; i < mFragments.size(); i++) {
            Fragment f = mFragments.get(i).getFragment();
            if (f != null) {
                if (fragments == null) {
                    fragments = new Bundle();
                }
                String key = "f" + i;
                mFragmentManagerWare.putFragment(fragments, key, f);
            }
        }
        state.mFragments = fragments;
        state.mIsSlideable = mIsSlideable;
        state.mHomeFragmentApplied = mHomeFragmentApplied;

        logState();
        return state;
    }

    private void logState() {
        Log.d(TAG, "STATE FragmentMaster[" + mFragments.size()
                + "],  mIsSlideable[" + mIsSlideable
                + "], mHomeFragmentApplied[" + mHomeFragmentApplied + "]");
    }

    void restoreAllState(Parcelable state) {
        if (state != null) {
            FragmentMasterState fms = (FragmentMasterState) state;

            mFragments.clear();
            Bundle fragments = fms.mFragments;
            if (fragments != null) {
                Iterable<String> keys = fragments.keySet();
                for (String key : keys) {
                    if (key.startsWith("f")) {
                        int index = Integer.parseInt(key.substring(1));
                        IMasterFragment<Fragment> f = (IMasterFragment<Fragment>) mFragmentManagerWare
                                .getFragment(fragments, key);
                        if (f != null) {
                            while (mFragments.size() <= index) {
                                mFragments.add(null);
                            }
                            f.setMenuVisibility(false);
                            mFragments.set(index, f);
                        } else {
                            Log.w(TAG, "Bad fragment at key " + key);
                        }
                    }
                }
            }

            setSlideable(fms.mIsSlideable);
            mHomeFragmentApplied = fms.mHomeFragmentApplied;
        }
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public Callback getCallback() {
        return mCallback;
    }

    // ------------------------------------------------------------------------
    // Dispatch events
    // ------------------------------------------------------------------------

    protected boolean dispatchKeyEvent(KeyEvent event) {
        if (mCallback != null) {
            return mCallback.dispatchKeyEvent(event);
        }
        return mActivity.superDispatchKeyEvent(event);
    }

    final boolean dispatchKeyEventToWindow(KeyEvent event) {
        mActivity.onUserInteraction();
        Window win = mActivity.getWindow();
        if (win.superDispatchKeyEvent(event)) {
            return true;
        }
        return false;
    }

    final boolean dispatchKeyEventToActivity(KeyEvent event) {
        final View decor = mActivity.getWindow().getDecorView();
        return KeyEventCompat2.dispatch(event, mActivity, decor != null
                ? KeyEventCompat2.getKeyDispatcherState(decor)
                : null, mActivity);
    }

    protected boolean dispatchKeyShortcutEvent(KeyEvent event) {
        if (mCallback != null) {
            return mCallback.dispatchKeyShortcutEvent(event);
        }
        return mActivity.superDispatchKeyShortcutEvent(event);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    final boolean dispatchKeyShortcutEventToWindow(KeyEvent event) {
        mActivity.onUserInteraction();
        if (mActivity.getWindow().superDispatchKeyShortcutEvent(event)) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    final boolean dispatchKeyShortcutEventToActivity(KeyEvent event) {
        return mActivity.onKeyShortcut(event.getKeyCode(), event);
    }

    protected boolean dispatchTouchEvent(MotionEvent event) {
        if (mCallback != null) {
            return mCallback.dispatchTouchEvent(event);
        }
        return mActivity.superDispatchTouchEvent(event);
    }

    final boolean dispatchTouchEventToWindow(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mActivity.onUserInteraction();
        }
        if (mActivity.getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return false;
    }

    final boolean dispatchTouchEventToActivity(MotionEvent ev) {
        return mActivity.onTouchEvent(ev);
    }

    protected boolean dispatchGenericMotionEvent(MotionEvent ev) {
        if (mCallback != null) {
            return mCallback.dispatchGenericMotionEvent(ev);
        }
        return mActivity.superDispatchGenericMotionEvent(ev);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    final boolean dispatchGenericMotionEventToWindow(MotionEvent ev) {
        mActivity.onUserInteraction();
        if (mActivity.getWindow().superDispatchGenericMotionEvent(ev)) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    final boolean dispatchGenericMotionEventToActivity(MotionEvent ev) {
        return mActivity.onGenericMotionEvent(ev);
    }

    protected boolean dispatchTrackballEvent(MotionEvent ev) {
        if (mCallback != null) {
            return mCallback.dispatchTrackballEvent(ev);
        }
        return mActivity.superDispatchTrackballEvent(ev);
    }

    final boolean dispatchTrackballEventToWindow(MotionEvent ev) {
        mActivity.onUserInteraction();
        if (mActivity.getWindow().superDispatchTrackballEvent(ev)) {
            return true;
        }
        return false;
    }

    final boolean dispatchTrackballEventToActivity(MotionEvent ev) {
        return mActivity.onTrackballEvent(ev);
    }
}

final class FragmentMasterState implements Parcelable {

    Bundle mFragments;

    boolean mIsSlideable;

    boolean mHomeFragmentApplied;

    public FragmentMasterState() {
    }

    public FragmentMasterState(Parcel in) {
        mFragments = in.readBundle();
        mIsSlideable = in.readInt() == 0;
        mHomeFragmentApplied = in.readInt() == 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(mFragments);
        dest.writeInt(mIsSlideable ? 0 : 1);
        dest.writeInt(mHomeFragmentApplied ? 0 : 1);
    }

    public static final Parcelable.Creator<FragmentMasterState> CREATOR
            = new Parcelable.Creator<FragmentMasterState>() {
        public FragmentMasterState createFromParcel(Parcel in) {
            return new FragmentMasterState(in);
        }

        public FragmentMasterState[] newArray(int size) {
            return new FragmentMasterState[size];
        }
    };
}
