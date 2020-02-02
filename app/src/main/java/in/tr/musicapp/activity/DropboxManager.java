package in.tr.musicapp.activity;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.SearchMatch;
import com.dropbox.core.v2.files.SearchResult;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

public class DropboxManager {

    private Context mContext;
    private DbxClientV2 mClient;

    private final static String DROP_BOX_KEY = "o9dn4omovycs8rw";


    /**
     * コンストラクタ。
     * @param context コンテキスト
     */
    public DropboxManager(Context context) {
        mContext = context;
    }

    /**
     * コンストラクタ。認証済みの場合はこちらを使用する。
     * @param context コンテキスト
     * @param token 認証済みトークン
     */
    public DropboxManager(Context context, String token) {
        mContext = context;
        DbxRequestConfig config = new DbxRequestConfig("Name/Version");
        mClient = new DbxClientV2(config, token);
    }

    /**
     * 認証処理を開始する。
     * 認証ページが開く。
     */
    public void startAuthenticate() {
        Auth.startOAuth2Authentication(mContext, DROP_BOX_KEY);
    }

    /**
     * 認証トークンを取得する。
     * @return 認証トークン。
     */
    public String getAccessToken() {
        return Auth.getOAuth2Token();
    }

    /**
     * ファイルをバックアップする。
     * @param srcFilePath 保存元のファイルパス(Android)
     * @param dstFilePath 保存先のファイルパス(Dropbox)
     * @return 成功した場合はtrue,失敗した場合はfalseを返す
     */
    public boolean backup(String srcFilePath, String dstFilePath) {
        try {
            File file = new File(srcFilePath);
            InputStream input = new FileInputStream(file);
            byte[] bytes = convertFileToBytes(file);
            // Dropboxのファイルパスは先頭に"/"と指定する必要があるため、ファイルパスの先頭に"/"をつける
            mClient.files().uploadBuilder("/" + dstFilePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(input);

        } catch (Exception e) {
            Log.e("tag", "Upload Error: " + e);
            return false;
        }
        return true;
    }

    /**
     * ファイルを復元する。
     * @param srcFilePath 復元元のファイルパス(Dropbox)
     * @param dstFilePath 復元先のファイルパス(Android)
     * @return 成功した場合はtrue,失敗した場合はfalseを返す
     */
    public boolean restore(String srcFilePath, String dstFilePath) {
        try {
            // ファイルを検索する
            SearchResult result = mClient.files().search("", srcFilePath);
            List<SearchMatch> matches = result.getMatches();
            //LogUtil.d("matches.size(): " + matches.size());// 見つからないのでコメントアウト

            Metadata metadata = null;
            for (SearchMatch match : matches) {
                metadata = match.getMetadata();
                break;
            }

            if(metadata == null) {
                // ファイルが見つからない場合
                Log.d("tag", "metadata not found");
                return false;
            } else {
                Log.d("tag", "metadata.getPathLower(): " + metadata.getPathLower());
            }

            // ダウンロードし、ファイルを置き換える
            File file = new File(dstFilePath);
            OutputStream os = new FileOutputStream(file);
            mClient.files().download(metadata.getPathLower()).download(os);

        } catch (Exception e) {
            Log.e("tag", "Download Error: " +  e);
            return false;
        }
        return true;
    }

    /**
     * Fileをbytes[]に変換する。
     * @param file ファイル
     * @return bytes
     * @throws IOException
     */
    private byte[] convertFileToBytes(File file) throws IOException {
        final long fileSize = file.length();
        final int byteSize = (int) fileSize;
        byte[] bytes = new byte[byteSize];
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            try {
                raf.readFully(bytes);
            } finally {
                raf.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
