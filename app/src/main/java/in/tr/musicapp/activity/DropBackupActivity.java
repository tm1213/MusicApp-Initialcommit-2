package in.tr.musicapp.activity;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import in.tr.musicapp.R;

public class DropBackupActivity extends AppCompatActivity implements View.OnClickListener {

    private enum BackupAction {
        NONE,
        BACKUP_DROPBOX,
        RESTORE_DROPBOX,
    }

    private BackupAction mCurrentAction;
    private SharedPreferences mPref;
    private DropboxManager mDropboxManager;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        mPref = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentAction = BackupAction.NONE;

        ((Button) findViewById(R.id.backup_backup_dropbox)).setOnClickListener(this);
        ((Button) findViewById(R.id.backup_restore_dropbox)).setOnClickListener(this);
    }

    /**
     * Dropboxの認証画面から戻った際に呼ばれる。
     * バックアップ・復元を実行する。
     */
    @Override
    protected void onResume() {
        super.onResume();
        switch (mCurrentAction) {
            case NONE:
                break;
            case BACKUP_DROPBOX:
                mCurrentAction = BackupAction.NONE;

                try {
                    String token = mDropboxManager.getAccessToken();
                    if(token == null) {
                        // 認証失敗
                        break;
                    }
                    mDropboxManager = new DropboxManager(this, token);

                    // 次回以降に認証を省くため、トークンを保存する
                    SharedPreferences.Editor edit = mPref.edit();
                    edit.putString("access_token", token);
                    edit.apply();

                    // アップロードを実行
                    new AsyncHttpRequest(this, BackupAction.BACKUP_DROPBOX).execute();

                } catch (IllegalStateException e) {
                    // IllegalStateException
                }
                break;
            case RESTORE_DROPBOX:
                mCurrentAction = BackupAction.NONE;

                try {
                    String token = mDropboxManager.getAccessToken();
                    if(token == null) {
                        // 認証失敗
                        break;
                    }

                    mDropboxManager = new DropboxManager(this, token);

                    // 次回以降に認証を省くため、トークンを保存する
                    SharedPreferences.Editor edit = mPref.edit();
                    edit.putString("access_token", token);
                    edit.apply();

                    // ダウンロードを実行
                    new AsyncHttpRequest(this, BackupAction.RESTORE_DROPBOX).execute();
                } catch (IllegalStateException e) {
                    // IllegalStateException
                }
                break;
            default:
                break;
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backup_backup_dropbox:
                // 認証済みの場合はアップロードを実行、未認証の場合は認証処理を行う
                if (mPref.contains("access_token")) {

                    if(mDropboxManager == null) {
                        String token = mPref.getString("access_token", null);
                        mDropboxManager = new DropboxManager(this, token);
                    }

                    // アップロードを実行
                    new AsyncHttpRequest(this, BackupAction.BACKUP_DROPBOX).execute();

                } else {
                    mCurrentAction = BackupAction.BACKUP_DROPBOX;

                    if(mDropboxManager == null) {
                        mDropboxManager = new DropboxManager(this);
                    }
                    // 認証ページを開く
                    mDropboxManager.startAuthenticate();
                }
                break;

            case R.id.backup_restore_dropbox:
                // 認証済みの場合はダウンロードを実行、未認証の場合は認証処理を行う
                if (mPref.contains("access_token")) {

                    if(mDropboxManager == null) {
                        String token = mPref.getString("access_token", null);
                        mDropboxManager = new DropboxManager(this, token);
                    }

                    // ダウンロードを実行
                    new AsyncHttpRequest(this, BackupAction.RESTORE_DROPBOX).execute();
                } else {
                    mCurrentAction = BackupAction.RESTORE_DROPBOX;

                    if(mDropboxManager == null) {
                        mDropboxManager = new DropboxManager(this);
                    }
                    // 認証ページを開く
                    mDropboxManager.startAuthenticate();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 非同期処理終了後に呼び出される。
     * メッセージを表示する。
     */
    public void callback(BackupAction action, boolean result) {
        switch (action) {
            case BACKUP_DROPBOX:
                if (result) {
                    // success
                    Log.d("User", "Dropboxにアップロードされました");
                } else {
                    // failed
                    Log.d("User", "Dropboxにアップロード出来ませんでした");
                }
                break;
            case RESTORE_DROPBOX:
                if (result) {
                    // success
                    Log.d("User", "Dropboxからダウンロードされました");
                } else {
                    // failed
                    Log.d("User", "Dropboxからのダウンロードに失敗しました");
                }
                break;
            default:

                break;
        }
    }

    /**
     * アップロード・ダウンロードのHTTPリクエストを行う非同期処理タスク。
     */
    public class AsyncHttpRequest extends AsyncTask<Void, Void, Boolean> {

        private DropBackupActivity activity;
        private BackupAction action;
        private ProgressDialog progressDialog;

        private AsyncHttpRequest(DropBackupActivity activity, BackupAction action) {  //もともと、public だった
            this.activity = activity;
            this.action = action;
        }

        @Override
        protected void onPreExecute() {
            // プログレスダイアログの生成
            this.progressDialog = new ProgressDialog(activity);
            switch (action) {
                case BACKUP_DROPBOX:
                    this.progressDialog.setTitle("アップロード中...");
                    this.progressDialog.setMessage("アップロード中です。このまましばらくお待ちください");
                    break;
                case RESTORE_DROPBOX:
                    this.progressDialog.setTitle("ダウンロード中...");
                    this.progressDialog.setMessage("ダウンロード中です。このまましばらくお待ちください");
                    break;
                default:
                    break;
            }
            this.progressDialog.setCancelable(false); // キャンセル不可にする
            this.progressDialog.show();
          //  return;
        }

        @Override
        protected Boolean doInBackground(Void... builder) {
            boolean isSuccess = false;
            String srcFilePath = getFilesDir().getPath() + getPackageName() + "/databases/test.splite"; // Android端末内のSQLiteのデータ
            String dstFilePath = "db_data.bak"; // Dropboxに保存される際のファイル名

            switch (action) {
                case BACKUP_DROPBOX: {
                    isSuccess = mDropboxManager.backup(srcFilePath, dstFilePath);
                    break;
                }
                case RESTORE_DROPBOX: {
                    isSuccess = mDropboxManager.restore(dstFilePath, srcFilePath);
                    break;
                }
                default:
                    break;
            }
            return isSuccess;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // プログレスダイアログを閉じる
            if (this.progressDialog != null && this.progressDialog.isShowing()) {
                this.progressDialog.dismiss();
            }
            activity.callback(action, result);
        }
    }
}
