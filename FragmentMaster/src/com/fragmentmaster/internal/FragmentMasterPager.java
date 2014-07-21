package com.fragmentmaster.internal;

import android.annotation.SuppressLint;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.view.ViewPagerCompat;
import android.view.MotionEvent;
import android.view.View;

import com.fragmentmaster.app.FragmentMaster;
import com.nineoldandroids.view.ViewHelper;

/**
 * Real container of fragments.
 */
class FragmentMasterPager extends ViewPagerCompat {

	private FragmentMaster mFragmentMaster;

	private static final int ANIMATION_NONE = 0;
	private static final int ANIMATION_ENTER = 1;
	private static final int ANIMATION_EXIT = 2;

	private int mAnimationState = ANIMATION_NONE;

	// The position of primary item in the latest SCROLL_STATE_IDLE state.
	private int mLatestIdleItem = 0;
	private ViewPager.PageTransformer mPageTransformer = new ViewPager.PageTransformer() {
		@Override
		public void transformPage(View page, float position) {
			resetPage(page);
			if (mFragmentMaster.hasPageAnimator()) {
				if (position < -1 || position > 1) {
					ViewHelper.setAlpha(page, 0);
				} else {
					mFragmentMaster.getPageAnimator().transformPage(page,
							position, mAnimationState == ANIMATION_ENTER);
				}
			}
		}

		private void resetPage(View page) {
			ViewHelper.setAlpha(page, 1);
			ViewHelper.setTranslationX(page, 0);
			ViewHelper.setTranslationY(page, 0);
			ViewHelper.setScaleX(page, 1);
			ViewHelper.setScaleY(page, 1);
			ViewHelper.setRotation(page, 0);
			ViewHelper.setRotationX(page, 0);
			ViewHelper.setRotationY(page, 0);
			ViewHelper.setPivotX(page, page.getWidth() / 2);
			ViewHelper.setPivotY(page, page.getHeight() / 2);
		}

	};

	// Internal listener
	private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
		@Override
		public void onPageScrollStateChanged(int state) {
			if (mWrappedOnPageChangeListener != null) {
				mWrappedOnPageChangeListener.onPageScrollStateChanged(state);
			}

			if (state == ViewPager.SCROLL_STATE_IDLE) {
				mLatestIdleItem = getCurrentItem();
				mAnimationState = ANIMATION_NONE;
			}
		}
		@Override
		public void onPageScrolled(int position, float positionOffset,
				int positionOffsetPixels) {
			if (mWrappedOnPageChangeListener != null) {
				mWrappedOnPageChangeListener.onPageScrolled(position,
						positionOffset, positionOffsetPixels);
			}
		}

		@Override
		public void onPageSelected(int position) {
			if (mWrappedOnPageChangeListener != null) {
				mWrappedOnPageChangeListener.onPageSelected(position);
			}
		}
	};

	private OnPageChangeListener mWrappedOnPageChangeListener;

	public FragmentMasterPager(FragmentMaster fragmentMaster) {
		super(fragmentMaster.getActivity());
		mFragmentMaster = fragmentMaster;
		super.setOnPageChangeListener(mOnPageChangeListener);
		setPageTransformer(false, mPageTransformer);
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent ev) {
		if (mFragmentMaster.isSlideable()) {
			return super.onTouchEvent(ev);
		}
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mFragmentMaster.isSlideable()) {
			return super.onInterceptTouchEvent(ev);
		}
		return false;
	}

	@Override
	public void setOnPageChangeListener(OnPageChangeListener listener) {
		mWrappedOnPageChangeListener = listener;
	}

	@Override
	protected void onPageScrolled(int position, float offset, int offsetPixels) {
		if (mLatestIdleItem > position) {
			// The ViewPager is performing exiting.
			mAnimationState = ANIMATION_EXIT;
		} else if (mLatestIdleItem == position) {
			// The ViewPager is performing entering.
			mAnimationState = ANIMATION_ENTER;
		}
		super.onPageScrolled(position, offset, offsetPixels);
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		super.onRestoreInstanceState(state);
		mLatestIdleItem = getCurrentItem();
	}

}
