package android.support.v4.app;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ThemeListFragment extends ListFragment {

    protected ContextThemeWrapper mContextThemeWrapper;

    @Override
    View performCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContextThemeWrapper = FragmentThemeHelper.createContextThemeWrapper(getActivity());
        return super.performCreateView(
                inflater.cloneInContext(mContextThemeWrapper), container,
                savedInstanceState);
    }

}
