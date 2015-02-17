package com.manvir.SnapColors;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.manvir.logger.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Random;

public class Util {
    public Context con;

    public Util(Context con){
        this.con = con;
    }
    public Util(){}

    /**Soft reboots the device <strong>(Development only)</strong>*/
    public static void sofReboot(){
        try{
            Process su = Runtime.getRuntime().exec("su");

            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("pkill zygote");
            outputStream.flush();

        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**Allows the {@link android.widget.EditText} to have multiple lines
     * @param editText The EditText to enable multiline on*/
    public static void doMultiLine(EditText editText){
        editText.setSingleLine(false);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
    }

    /**Converts px to dpi
     * @param dips The dpi to convert
     * */
    public int px(float dips){
        float DP = con.getResources().getDisplayMetrics().density;
        return Math.round(dips * DP);
    }

    /**Sets the EditText background, and the text color to a random color.
     * @param textsBox The {@link android.widget.EditText} to set a random background color and text color on.
     * */
    public void random(EditText textsBox) {
        Random random = new Random();
        int colorBG = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        int colorText = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
        textsBox.setBackgroundColor(colorBG);
        textsBox.setTextColor(colorText);
    }

    /**Used by {@link #doFonts(android.content.Context, android.app.ProgressDialog, android.os.Handler, android.graphics.Typeface, android.widget.RelativeLayout, android.widget.HorizontalScrollView, android.widget.ImageButton) doFonts}
     * to copy the all assets(fonts) to sdcard/Android/data/com.snapchat.android/files
     * @param res The {@link android.content.res.Resources} object of the application to extract assets from.
     * */
    public void copyAssets(Resources res){
        AssetManager assetManager = res.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for(String filename : files) {
            InputStream in;
            OutputStream out;
            try {
                in = assetManager.open(filename);
                File outFile = new File(con.getExternalFilesDir(null), filename);
                out = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int read;
                while((read = in.read(buffer)) != -1){
                    out.write(buffer, 0, read);
                }
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch(IOException e) {
                Log.e("SnapColors", "Failed to copy asset file: " + filename, e);
            }
        }
    }

    /**Checks to see if the user has the fonts apk installed, if it is copy the fonts(.ttf) to snapchats data directory.
     * @param con Snapchats {@link android.content.Context}.
     * @param pro The {@link android.app.ProgressDialog} to show while copying the .ttf's or when the view is done loading.
     * @param handler Used to run stuff on the uithread.
     * @param defTypeFace The captions default {@link com.manvir.SnapColors.Typefaces}.
     * @param SnapChatLayout Snapchats main layout to witch we add the preview view to.
     * @param f SnapColors outer view.
     * @param SnapColorsBtn SnapColors main options panel button(The colored "T").
     * */
    public static void doFonts(final Context con, final ProgressDialog pro, final Handler handler, final Typeface defTypeFace, final RelativeLayout SnapChatLayout, final HorizontalScrollView f, final ImageButton SnapColorsBtn){
        new Thread(){
            @Override
            public void run() {
                try {
                    Resources res = con.getPackageManager().getResourcesForApplication("com.manvir.snapcolorsfonts");
                    new Util(con).copyAssets(res);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            SnapChatLayout.addView(new FontsListView(con, defTypeFace, f, SnapColorsBtn), App.optionsViewLayoutParams);
                            pro.dismiss();
                        }
                    });
                } catch (PackageManager.NameNotFoundException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            final AlertDialog.Builder al = new AlertDialog.Builder(con);
                            al.setTitle("SnapColors");
                            al.setMessage("You need to download fonts, they are not included. To download just tap \"Download & Install\". (Note no icon will be added)");
                            al.setNegativeButton("Download & Install", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new Util(con).downloadFontsApk();
                                }
                            });
                            al.setNeutralButton("Why", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String whyText = "The reason why fonts are not included with the tweak are:\n1. People may not have the space for fonts on there phone.\n2. Its easier for me to manage.\n3. You can move the apk to your SDCARD with out moving the tweak to the SDCARD.\n4. This way I can have different font packs with different sizes.";
                                    final AlertDialog alertDialog = new AlertDialog.Builder(con).create();
                                    alertDialog.setTitle("SnapColors");
                                    alertDialog.setMessage(whyText);
                                    alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            alertDialog.dismiss();
                                        }
                                    });
                                    alertDialog.show();
                                }
                            });
                            al.show();
                            pro.dismiss();
                        }
                    });
                }
            }
        }.start();
    }

    /**Downloads the fonts apk*/
    public void downloadFontsApk() {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"SnapColorsFonts.apk";
        String url = "http://forum.xda-developers.com/devdb/project/dl/?id=5916&task=get";
        new DownloadFileAsync().execute(url, path);
    }

    /**
     * Downloads a file from the given url then writes it to the given path.
     * */
    public class DownloadFileAsync extends AsyncTask<String, String, String> {
        ProgressDialog pro;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pro = ProgressDialog.show(con, "", "Downloading");
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            pro.dismiss();
        }

        @Override
        protected String doInBackground(String... aurl) {
            int count;
            try {
                URL url = new URL(aurl[0]);
                URLConnection conexion = url.openConnection();
                conexion.connect();

                int lenghtOfFile = conexion.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(aurl[1]);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                install.setDataAndType(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"SnapColorsFonts.apk")),
                        "application/vnd.android.package-archive");
                con.startActivity(install);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
    * Checks to see if the user just updated. If they did then show the donation message
    * @param con The current {@link android.content.Context}
    * */
    public static void doDonationMsg(Context con){
        Logger.log("Doing donation stuff");
        try {
            con.createPackageContext("com.manvir.snapcolorsdonation", 0); //old donation package
        } catch (PackageManager.NameNotFoundException e) {
            try {
                con.createPackageContext("com.manvir.programming4lifedonate", 0); //new donation package
            } catch (PackageManager.NameNotFoundException e1) {
                try {
                    int SnapColorsVersionCode = con.getPackageManager().getPackageInfo("com.manvir.SnapColors", 0).versionCode;
                    File versionFile = new File(con.getCacheDir().getAbsolutePath()+"/version");

                    if(!versionFile.exists())
                        versionFile.createNewFile();

                    if(getStringFromFile(versionFile.getAbsolutePath()).equals("")){
                        new DonationDialog(con).show();
                        PrintWriter writer = new PrintWriter(versionFile);
                        writer.write(String.valueOf(SnapColorsVersionCode)+ "\n" + new Date().getTime());
                        writer.close();
                    }else if(Integer.parseInt(getStringFromFile(versionFile.getAbsolutePath())) != SnapColorsVersionCode){
                        new DonationDialog(con).show();
                        PrintWriter writer = new PrintWriter(versionFile);
                        writer.write(String.valueOf(SnapColorsVersionCode)+ "\n" + new Date().getTime());
                        writer.close();
                    }
                } catch (Exception ee) {
                    ee.printStackTrace();
                }
            }
        }
    }

    /**Reads a file into a single continues {@link java.lang.String}
     * <p>Used by {@link #doDonationMsg(android.content.Context) doDonationMsg}</p>
     * @param filePath The file you want to get data from as a {@link java.lang.String}
     * @return A {@link java.lang.String} containing the data from the file
     * */
    private static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        fin.close();
        return sb.toString();
    }

    /**Deletes a directory regardless of whats in it.
     * @param fileOrDirectory The directory or file you would like to delete.
     * @return true if the file was deleted, false otherwise.
     * */
    public static boolean DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        return fileOrDirectory.delete();
    }
}
