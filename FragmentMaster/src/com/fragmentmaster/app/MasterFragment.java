package com.fragmentmaster.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.KeyEventCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * The base fragment
 */
public class MasterFragment extends Fragment
		implements
			KeyEvent.Callback,
			FragmentMaster.Callback {
	private static final String TAG = "MasterFragment";

	/** Standard fragment result: operation canceled. */
	public static final int RESULT_CANCELED = 0;
	/** Standard fragment result: operation succeeded. */
	public static final int RESULT_OK = -1;

	private static final String BUNDLE_KEY_REQUEST = "FragmentMaster:REQUEST";
	private static final String BUNDLE_KEY_TARGET_CHILD_FRAGMENT = "FragmentMaster:TARGET_CHILD_FRAGMENT";

	private MasterActivity mActivity;
	private boolean mStateSaved = false;

	private int mResultCode = RESULT_CANCELED;
	private Request mResultData = null;

	// SoftInputMode, SOFT_INPUT_ADJUST_UNSPECIFIED is default.
	private int mSoftInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED;

	private MasterFragment mTargetChildFragment;

	private boolean mIsUserActive = false;

	private boolean mFinished = false;

	public MasterFragment() {
	}

	@Override
	public void onAttach(Activity activity) {
		if (activity instanceof MasterActivity) {
			mActivity = (MasterActivity) activity;
		}
		super.onAttach(activity);
	}

	public void onDetach() {
		mActivity = null;
		super.onDetach();
	}

	public MasterActivity getMasterActivity() {
		return mActivity;
	}

	public FragmentMaster getFragmentMaster() {
		return mActivity == null ? null : mActivity.getFragmentMaster();
	}

	/**
	 * Starts a specific fragment.
	 */
	public void startFragment(Class<? extends MasterFragment> clazz) {
		startFragmentForResult(new Request(clazz), -1);
	}

	/**
	 * Starts a fragment.
	 * 
	 * @param request
	 *            The request.
	 */
	public void startFragment(Request request) {
		startFragmentForResult(request, -1);
	}

	public void startFragmentForResult(Class<? extends MasterFragment> clazz,
			int requestCode) {
		startFragmentForResult(new Request(clazz), requestCode);
	}

	public void startFragmentForResult(Request request, int requestCode) {
		checkState();
		if (getParentFragment() instanceof MasterFragment) {
			((MasterFragment) getParentFragment()).startFragmentFromChild(this,
					request, requestCode);
		} else {
			getFragmentMaster().startFragmentForResult(this, request,
					requestCode);
		}
	}

	private void checkState() {
		if (mActivity == null) {
			throw new IllegalStateException("Fragment " + this
					+ " not attached to MasterActivity!");
		}
	}

	private void startFragmentFromChild(MasterFragment childFragment,
			Request request, int requestCode) {
		if (requestCode != -1) {
			mTargetChildFragment = childFragment;
		}
		startFragmentForResult(request, requestCode);
	}

	protected void onFragmentResult(int requestCode, int resultCode,
			Request data) {
	}

	/**
	 * Call this to set the result that your fragment will return to its caller.
	 */
	public final void setResult(int resultCode) {
		synchronized (this) {
			mResultCode = resultCode;
			mResultData = null;
		}
	}

	/**
	 * Call this to set the result that your fragment will return to its caller.
	 */
	public final void setResult(int resultCode, Request data) {
		synchronized (this) {
			mResultCode = resultCode;
			mResultData = data;
		}
	}

	public void finish() {
		checkState();

		int resultCode;
		Request resultData;
		synchronized (this) {
			resultCode = mResultCode;
			resultData = mResultData;
		}
		FragmentMaster fragmentMaster = getFragmentMaster();
		fragmentMaster.finishFragment(this);
		deliverFragmentResult(this, resultCode, resultData);
		mFinished = true;
	}

	private void deliverFragmentResult(MasterFragment fragment, int resultCode,
			Request data) {
		Fragment targetFragment = fragment.getTargetFragment();
		int requestCode = fragment.getTargetRequestCode();
		if (requestCode != -1 && targetFragment instanceof MasterFragment) {
			MasterFragment who = (MasterFragment) targetFragment;
			who.dispatchFragmentResult(fragment.getTargetRequestCode(),
					resultCode, data);
		}
	}

	void dispatchFragmentResult(int requestCode, int resultCode, Request data) {
		if (mTargetChildFragment == null) {
			onFragmentResult(requestCode, resultCode, data);
		} else {
			mTargetChildFragment.dispatchFragmentResult(requestCode,
					resultCode, data);
		}
		mTargetChildFragment = null;
	}

	public boolean isFinishing() {
		return mFinished;
	}

	/**
	 * Called when the fragment has detected the user's press of the back key.
	 * The default implementation simply finishes the current fragment, but you
	 * can override this to do whatever you want.
	 */
	public void onBackPressed() {
		finish();
	}

	public Request getRequest() {
		return (Request) getArguments().get(BUNDLE_KEY_REQUEST);
	}

	public void setRequest(Request newRequest) {
		Bundle bundle = getArguments();
		bundle.putParcelable(BUNDLE_KEY_REQUEST, newRequest);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTargetChildFragment != null) {
			getChildFragmentManager().putFragment(outState,
					BUNDLE_KEY_TARGET_CHILD_FRAGMENT, mTargetChildFragment);
		}
		mStateSaved = true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mStateSaved = false;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (savedInstanceState != null) {
			mTargetChildFragment = (MasterFragment) getChildFragmentManager()
					.getFragment(savedInstanceState,
							BUNDLE_KEY_TARGET_CHILD_FRAGMENT);
		}
		// Set the fragmentRootView's "clickable" to true to avoid
		// touch events to be passed to the views behind the fragment.
		view.setClickable(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mStateSaved = false;
	}

	@Override
	public void onStart() {
		super.onStart();
		mStateSaved = false;
	}

	@Override
	public void onResume() {
		super.onResume();
		mStateSaved = false;
	}

	/**
	 * Whether the state have been saved by system.
	 */
	public boolean hasStateSaved() {
		return mStateSaved;
	}

	public void setSoftInputMode(int mode) {
		if (mSoftInputMode != mode) {
			mSoftInputMode = mode;
			invalidateWindowConfiguration();
		}
	}

	public int getSoftInputMode() {
		return mSoftInputMode;
	}

	@Override
	public void setUserVisibleHint(boolean isVisibleToUser) {
		super.setUserVisibleHint(isVisibleToUser);
		if (!mIsUserActive && isVisibleToUser) {
			invalidateWindowConfiguration();
			onUserActive();
		} else if (mIsUserActive && !isVisibleToUser) {
			onUserLeave();
		}
		mIsUserActive = isVisibleToUser;
	}

	public void invalidateWindowConfiguration() {
		if (getActivity() != null) {
			getActivity().getWindow().setSoftInputMode(mSoftInputMode);
		}
	}

	public boolean isUserActive() {
		return mIsUserActive;
	}

	/**
	 * Called when user has come to this fragment.
	 */
	public void onUserActive() {
		Log.d(TAG, "onUserActive: " + this.toString());
	}

	/**
	 * Called when user has left this fragment.
	 */
	public void onUserLeave() {
		Log.d(TAG, "onUserLeave: " + this.toString());
	}

	public void setSlideEnable(boolean enable) {
		checkState();
		getFragmentMaster().setSlideEnable(enable);
	}

	// ------------------------------------------------------------------------
	// Dispatch events
	// ------------------------------------------------------------------------

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (getFragmentMaster().dispatchKeyEventToWindow(event)) {
			return true;
		}

		View view = getView();
		boolean handled = KeyEventCompat.dispatch(event, this, view != null
				? KeyEventCompat.getKeyDispatcherState(view)
				: null, this);
		if (handled) {
			return true;
		}

		return getFragmentMaster().dispatchKeyEventToActivity(event);
	}

	@Override
	public boolean dispatchKeyShortcutEvent(KeyEvent event) {
		if (getFragmentMaster().dispatchKeyEventToWindow(event)) {
			return true;
		}
		if (onKeyShortcut(event.getKeyCode(), event)) {
			return true;
		}
		return getFragmentMaster().dispatchKeyShortcutEventToActivity(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (getFragmentMaster().dispatchTouchEventToWindow(ev)) {
			return true;
		}
		if (onTouchEvent(ev)) {
			return true;
		}
		return getFragmentMaster().dispatchTouchEventToActivity(ev);
	}

	@Override
	public boolean dispatchTrackballEvent(MotionEvent ev) {
		if (getFragmentMaster().dispatchTrackballEventToWindow(ev)) {
			return true;
		}
		if (onTrackballEvent(ev)) {
			return true;
		}
		return getFragmentMaster().dispatchTrackballEventToActivity(ev);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent ev) {
		if (getFragmentMaster().dispatchGenericMotionEventToWindow(ev)) {
			return true;
		}
		if (onGenericMotionEvent(ev)) {
			return true;
		}
		return getFragmentMaster().dispatchGenericMotionEventToActivity(ev);
	}

	// ------------------------------------------------------------------------
	// Handle events
	// ------------------------------------------------------------------------

	public boolean onTouchEvent(MotionEvent ev) {
		return false;
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) {
			if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
				onBackPressed();
				return true;
			}
		} else {
			event.startTracking();
		}
		return false;
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			if (keyCode == KeyEvent.KEYCODE_BACK && event.isTracking()
					&& !event.isCanceled()) {
				onBackPressed();
				return true;
			}
		}
		return false;
	}

	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return false;
	}

	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return false;
	}

	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		return false;
	}

	public boolean onTrackballEvent(MotionEvent event) {
		return false;
	}

	public boolean onGenericMotionEvent(MotionEvent event) {
		return false;
	}

}
