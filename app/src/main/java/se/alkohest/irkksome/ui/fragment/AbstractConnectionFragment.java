package se.alkohest.irkksome.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import se.alkohest.irkksome.R;
import se.alkohest.irkksome.model.entity.IrkksomeConnection;

public abstract class AbstractConnectionFragment extends Fragment {
    public static final String CONNECTION_ARGUMENT = "CONNECTION";

    protected OnConnectPressedListener listener;
    protected IrkksomeConnection connection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            // Kan läsa upp irkksomeConnection.
        }
    }

    public void onButtonPressed() {
        if (listener != null) {
            listener.onConnectPressed(null);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (OnConnectPressedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public static AbstractConnectionFragment newInstance(int id) {
        switch (id) {
            case R.drawable.connection_icon_blue:
                return RegularConnectionFragment.newInstance();
            case R.drawable.connection_icon_purple:
                return IrssiProxyConnectionFragment.newInstance();
        }
        return null;
    }

    public interface OnConnectPressedListener {
        public void onConnectPressed(IrkksomeConnection irkksomeConnection);
    }
}
