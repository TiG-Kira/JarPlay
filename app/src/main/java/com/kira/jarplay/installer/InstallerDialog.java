package com.kira.jarplay.installer;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * @deprecated Use the Compose InstallerDialog composable instead.
 * This class is kept for backward compatibility with old code paths.
 */
@Deprecated
public class InstallerDialog extends DialogFragment {
    private static final String ARG_URI = "InstallerDialog.uri";
    private static final String ARG_ID = "InstallerDialog.id";

    public static InstallerDialog newInstance(Uri uri) {
        InstallerDialog fragment = new InstallerDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        fragment.setArguments(args);
        return fragment;
    }

    public static InstallerDialog newInstance(int id) {
        InstallerDialog fragment = new InstallerDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dismissAllowingStateLoss();
    }
}