package com.richard.officenavigation.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.richard.officenavigation.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DirectoryChooserDialog extends Dialog {

	private static final String TAG = DirectoryChooserDialog.class
			.getSimpleName();
	private String mInitialDirectory, mTitle;

	private OnConfirmDirectoryChooseListener mListener;

	private Button mBtnConfirm;
	private Button mBtnCancel;
	private ImageButton mBtnNavUp;
	private TextView mTvSelectedFolder, mTvTitle;
	private ListView mListDirectories;

	private ArrayAdapter<String> mListDirectoriesAdapter;
	private ArrayList<String> mFilenames;
	/**
	 * The directory that is currently being shown.
	 */
	private File mSelectedDir;
	private File[] mFilesInDir;
	private FileObserver mFileObserver;
	private Context mContext;

	public static DirectoryChooserDialog newInstance(Context context,
			String initDir, String title) {
		DirectoryChooserDialog dialog = new DirectoryChooserDialog(context);
		dialog.mInitialDirectory = initDir;
		dialog.mTitle = title;
		return dialog;
	}

	public static DirectoryChooserDialog newInstance(Context context,
			String initDir, String title,
			OnConfirmDirectoryChooseListener listener) {
		DirectoryChooserDialog dialog = newInstance(context, initDir, title);
		dialog.setOnConfirmDirectoryChooseListener(listener);
		return dialog;
	}

	public DirectoryChooserDialog(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		View view = View.inflate(getContext(), R.layout.dialog_dir_chooser,
				null);
		setContentView(view);

		mBtnConfirm = (Button) view.findViewById(R.id.btn_confirm);
		mBtnCancel = (Button) view.findViewById(R.id.btn_cancel);
		mBtnNavUp = (ImageButton) view.findViewById(R.id.btn_nav_up);
		mTvSelectedFolder = (TextView) view
				.findViewById(R.id.tv_selected_folder);
		mTvTitle = (TextView) view.findViewById(R.id.tv_selected_folder_title);
		mListDirectories = (ListView) view.findViewById(R.id.directoryList);

		if (mTitle != null) {
			mTvTitle.setText(mTitle);
		}

		mBtnConfirm.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isValidFile(mSelectedDir)) {
					returnSelectedFolder();
				}
			}
		});

		mBtnCancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		mListDirectories.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View view,
					int position, long id) {
				debug("Selected index: %d", position);
				if (mFilesInDir != null && position >= 0
						&& position < mFilesInDir.length) {
					changeDirectory(mFilesInDir[position]);
				}
			}
		});

		mBtnNavUp.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				File parent;
				if (mSelectedDir != null
						&& (parent = mSelectedDir.getParentFile()) != null) {
					changeDirectory(parent);
				}
			}
		});

		mFilenames = new ArrayList<>();
		mListDirectoriesAdapter = new ArrayAdapter<>(mContext,
				R.layout.folder_list_item, mFilenames);
		mListDirectories.setAdapter(mListDirectoriesAdapter);

		final File initialDir;
		if (mInitialDirectory != null
				&& isValidFile(new File(mInitialDirectory))) {
			initialDir = new File(mInitialDirectory);
		} else {
			initialDir = Environment.getExternalStorageDirectory();
		}

		changeDirectory(initialDir);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mFileObserver != null) {
			mFileObserver.stopWatching();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (((ContextThemeWrapper) mContext).getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			Point outSize = new Point();
			((Activity) mContext).getWindowManager().getDefaultDisplay()
					.getSize(outSize);
			getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					outSize.y / 2);
		}
		if (mFileObserver != null) {
			mFileObserver.startWatching();
		}
	}

	private void debug(String message, Object... args) {
		Log.d(TAG, String.format(message, args));
	}

	/**
	 * Change the directory that is currently being displayed.
	 * 
	 * @param dir
	 *            The file the activity should switch to. This File must be
	 *            non-null and a directory, otherwise the displayed directory
	 *            will not be changed
	 */
	private void changeDirectory(File dir) {
		if (dir == null) {
			debug("Could not change folder: dir was null");
		} else if (!dir.isDirectory()) {
			debug("Could not change folder: dir is no directory");
		} else {
			File[] contents = dir.listFiles();
			if (contents != null) {
				int numDirectories = 0;
				for (File f : contents) {
					if (f.isDirectory()) {
						numDirectories++;
					}
				}
				mFilesInDir = new File[numDirectories];
				mFilenames.clear();
				for (int i = 0, counter = 0; i < numDirectories; counter++) {
					if (contents[counter].isDirectory()) {
						mFilesInDir[i] = contents[counter];
						mFilenames.add(contents[counter].getName());
						i++;
					}
				}
				Arrays.sort(mFilesInDir);
				Collections.sort(mFilenames);
				mSelectedDir = dir;
				mTvSelectedFolder.setText(dir.getAbsolutePath());
				mListDirectoriesAdapter.notifyDataSetChanged();
				mFileObserver = createFileObserver(dir.getAbsolutePath());
				mFileObserver.startWatching();
				debug("Changed directory to %s", dir.getAbsolutePath());
			} else {
				debug("Could not change folder: contents of dir were null");
			}
		}
		refreshButtonState();
	}

	/**
	 * Refresh the contents of the directory that is currently shown.
	 */
	private void refreshDirectory() {
		if (mSelectedDir != null) {
			changeDirectory(mSelectedDir);
		}
	}

	/**
	 * Sets up a FileObserver to watch the current directory.
	 */
	private FileObserver createFileObserver(String path) {
		return new FileObserver(path, FileObserver.CREATE | FileObserver.DELETE
				| FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {

			@Override
			public void onEvent(int event, String path) {
				debug("FileObserver received event %d", event);
				final Activity activity = (Activity) mContext;

				if (activity != null) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							refreshDirectory();
						}
					});
				}
			}
		};
	}

	/**
	 * Changes the state of the buttons depending on the currently selected file
	 * or folder.
	 */
	private void refreshButtonState() {
		final Activity activity = (Activity) mContext;
		if (activity != null && mSelectedDir != null) {
			mBtnConfirm.setEnabled(isValidFile(mSelectedDir));
		}
	}

	/**
	 * Returns the selected folder as a result to the activity the fragment's
	 * attached to. The selected folder can also be null.
	 */
	private void returnSelectedFolder() {
		if (mSelectedDir != null) {
			debug("Returning %s as result", mSelectedDir.getAbsolutePath());
			if (mListener != null)
				mListener.OnConfirmDirectoryChoose(mSelectedDir
						.getAbsolutePath());
		}
		dismiss();
	}

	/**
	 * Returns true if the selected file or directory would be valid selection.
	 */
	private boolean isValidFile(File file) {
		return (file != null && file.isDirectory() && file.canRead() && file
				.canWrite());
	}

	@Nullable
	public OnConfirmDirectoryChooseListener getOnConfirmDirectoryChooseListener() {
		return mListener;
	}

	public void setOnConfirmDirectoryChooseListener(
			@Nullable OnConfirmDirectoryChooseListener listener) {
		mListener = listener;
	}

	public interface OnConfirmDirectoryChooseListener {
		public void OnConfirmDirectoryChoose(@NonNull String path);
	}
}
