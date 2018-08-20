package com.example.space.hkm.ui;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.space.hkm.App;
import com.example.space.hkm.R;
import com.example.space.hkm.data.User;
import com.example.space.hkm.rest.RequestGenerator;

import java.util.LinkedHashMap;

public class ProfileDisplay extends Fragment {

    private static final int SHOW_LOGIN = 0;
    private static final int EDITS_TO_REQUEST = 5;
//    private static LinkedHashMap<Integer, EditEntry> loadedEdits = new LinkedHashMap<>();
    // The fields on User which are displayed on Profile Page
    public static final String[][] userFields = {{"Username", "username"}, {"First Name", "first_name"},
            {"Last Name", "last_name"}, {"Organization", "organization"}};

    private final RequestGenerator client = new RequestGenerator();
    private int editRequestCount = 0;
    private boolean loadingRecentEdits = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.profile, container, false);

        int switcherVisibility = App.hasSkinCode() ? View.GONE : View.VISIBLE;

        view.findViewById(R.id.change_instance_loggedin).setVisibility(switcherVisibility);

        registerHandlers(view);
        return view;
    }

    private void registerHandlers(final View view) {
        View.OnClickListener switchInstanceListener = v -> startActivity(new Intent(getActivity(), InstanceSwitcherActivity.class));
        view.findViewById(R.id.change_instance_loggedin).setOnClickListener(switchInstanceListener);

        view.findViewById(R.id.logout).setOnClickListener(v -> {
            App.getLoginManager().logOut(getActivity());
//            loadProfile(view, getActivity().getLayoutInflater());
        });

        view.findViewById(R.id.change_password).setOnClickListener(v -> startActivity(new Intent(getActivity(), ChangePassword.class)));


    }

    @Override
    public void onResume() {
        super.onResume();
        editRequestCount = 0;
        loadProfile(getView(), getActivity().getLayoutInflater());
    }

    private void loadProfile(View view, LayoutInflater inflater) {

        if (App.getLoginManager().isLoggedIn()) {
            User user = App.getLoginManager().loggedInUser;
            renderUserFields(view, inflater, user, userFields);

            view.findViewById(R.id.profile_activity_loggedin).setVisibility(View.VISIBLE);

            /*
             * Presently, OTM2 is not loading User Edits.
             *
             * NotifyingScrollView scroll =
             * (NotifyingScrollView)findViewById(R.id.userFieldsScroll);
             * scroll.setOnScrollToBottomListener(new
             * NotifyingScrollView.OnScrollToBottomListener() {
             *
             * @Override public void OnScrollToBottom() { addMoreEdits(); } });
             */
        } else {
            view.findViewById(R.id.profile_activity_loggedin).setVisibility(View.GONE);

        }
    }


    private void renderUserFields(View view, LayoutInflater inflater, User user, String[][] fieldNames) {
        LinearLayout fieldContainer = (LinearLayout) view.findViewById(R.id.profile_field_container);
//        renderRecentEdits(inflater);

        fieldContainer.removeAllViews();
        for (String[] fieldPair : fieldNames) {
            String label = fieldPair[0];
            String value = user.getField(fieldPair[1]).toString();

            View row = inflater.inflate(R.layout.plot_field_row, null);
            ((TextView) row.findViewById(R.id.field_label)).setText(label);
            row.setTag(fieldPair[1]);
            ((TextView) row.findViewById(R.id.field_value)).setText(value);

            fieldContainer.addView(row);
        }
    }


}