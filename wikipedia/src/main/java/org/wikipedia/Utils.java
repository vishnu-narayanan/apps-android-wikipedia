package org.wikipedia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.InputType;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.interlanguage.LanguageUtil;
import org.wikipedia.settings.PrefKeys;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.ShareUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Contains utility methods that Java doesn't have because we can't make code look too good, can we?
 */
public final class Utils {

    private static final int KB16 = 16 * 1024;

    /**
     * Private constructor, so nobody can construct Utils.
     *
     * THEIR EVIL PLANS HAVE BEEN THWARTED!!!1
     */
    private Utils() { }

    /**
     * Compares two strings properly, even when one of them is null - without throwing up
     *
     * @param str1 The first string
     * @param str2 Guess?
     * @return true if they are both equal (even if both are null)
     */
    public static boolean compareStrings(String str1, String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }

    /**
     * Capitalise the first character of the description, for style
     *
     * @param orig original string
     * @return same string as orig, except the first letter is capitalized
     */
    public static String capitalizeFirstChar(String orig) {
        return orig.substring(0, 1).toUpperCase() + orig.substring(1);
    }

    /**
     * Remove leading and trailing whitespace from a CharSequence. This is useful after using
     * the fromHtml() function to convert HTML to a CharSequence.
     * @param str CharSequence to be trimmed.
     * @return The trimmed CharSequence.
     */
    public static CharSequence trim(CharSequence str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        int len = str.length();
        int start = 0;
        int end = len - 1;
        while (Character.isWhitespace(str.charAt(start)) && start < len) {
            start++;
        }
        while (Character.isWhitespace(str.charAt(end)) && end > 0) {
            end--;
        }
        if (end > start) {
            return str.subSequence(start, end + 1);
        }
        return "";
    }

    /**
     * Creates an MD5 hash of the provided string and returns its ASCII representation
     * @param s String to hash
     * @return ASCII MD5 representation of the string passed in
     */
    public static String md5string(String s) {
        StringBuilder hexStr = new StringBuilder();
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes("utf-8"));
            byte[] messageDigest = digest.digest();

            final int maxByteVal = 0xFF;
            for (byte b : messageDigest) {
                hexStr.append(Integer.toHexString(maxByteVal & b));
            }
        } catch (NoSuchAlgorithmException e) {
            // This will never happen, yes.
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            // This will never happen, yes.
            throw new RuntimeException(e);
        }
        return hexStr.toString();
    }

    /**
     * Decodes a URL-encoded string into its UTF-8 equivalent.
     * @param url The URL-encoded string that you wish to decode.
     * @return The decoded string, or the input string if the decoding failed.
     */
    public static String decodeURL(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Inexplicable decoding problem. This shouldn't happen. Return the input.
            Log.d("Wikipedia", "URL decoding failed. String was: " + url);
            return url;
        }
    }

    /**
     * Deletes a file or directory, with optional recursion.
     * @param path File or directory to delete.
     * @param recursive Whether to delete all subdirectories and files.
     */
    public static void delete(File path, boolean recursive) {
        if (recursive && path.isDirectory()) {
            String[] children = path.list();
            for (String child : children) {
                delete(new File(path, child), recursive);
            }
        }
        path.delete();
    }

    /**
     * Formats provided date relative to the current system time
     * @param date Date to format
     * @return String representing the relative time difference of the paramter from current time
     */
    public static String formatDateRelative(Date date) {
        return DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, 0).toString();
    }

    /**
     * Ensures that the calling method is on the main thread.
     */
    public static void ensureMainThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Method must be called from the Main Thread");
        }
    }

    /**
     * Attempt to hide the Android Keyboard.
     *
     * FIXME: This should not need to exist.
     * I do not know why Android does not handle this automatically.
     *
     * @param activity The current activity
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager keyboard = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        // Not using getCurrentFocus as that sometimes is null, but the keyboard is still up.
        keyboard.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
    }

    /**
     * Attempt to display the Android keyboard.
     *
     * FIXME: This should not need to exist.
     * Android should always show the keyboard at the appropriate time. This method allows you to display the keyboard
     * when Android fails to do so.
     *
     * @param activity The current activity
     * @param view The currently focused view that will receive the keyboard input
     */
    public static void showSoftKeyboard(Activity activity, View view) {
        InputMethodManager keyboard = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    public static void setupShowPasswordCheck(final CheckBox check, final EditText edit) {
        check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // EditText loses the cursor position when you change the InputType
                int curPos = edit.getSelectionStart();
                if (isChecked) {
                    edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                edit.setSelection(curPos);
            }
        });
    }

    /**
     * Set message for error popup on a TextView.
     * @param textView the TextView or EditText to pop the error message from
     * @param error the error message. Use null message to clear.
     * @see TextView#setError
     * @see <a href='http://stackoverflow.com/questions/14413575/how-to-write-style-to-error-text-of-edittext-in-android'>StackOverflow: How to write style to error text of EditText in Android?</a>
     */
    public static void setErrorPopup(TextView textView, String error) {
        if (ApiUtil.hasHoneyComb()) {
            textView.setError(error);
        } else {
            textView.setError(Html.fromHtml("<font color='red'>" + error + "</font>"));
        }
    }

    /**
     * List of wiki language codes for which the content is primarily RTL.
     *
     * Ensure that this is always sorted alphabetically.
     */
    private static final String[] RTL_LANGS = {
            "ar", "arc", "arz", "bcc", "bqi", "ckb", "dv", "fa", "glk", "he",
            "khw", "ks", "mzn", "pnb", "ps", "sd", "ug", "ur", "yi"
    };

    /**
     * Returns true if the given wiki language is to be displayed RTL.
     *
     * @param lang Wiki code for the language to check for directionality
     * @return true if it is RTL, false if LTR
     */
    public static boolean isLangRTL(String lang) {
        return Arrays.binarySearch(RTL_LANGS, lang, null) >= 0;
    }

    /**
     * Setup directionality for both UI and content elements in a webview.
     *
     * @param contentLang The Content language to use to set directionality. Wiki Language code.
     * @param uiLang The UI language to use to set directionality. Java language code.
     * @param bridge The CommunicationBridge to use to communicate with the WebView
     */
    public static void setupDirectionality(String contentLang, String uiLang, CommunicationBridge bridge) {
        JSONObject payload = new JSONObject();
        try {
            if (isLangRTL(contentLang)) {
                payload.put("contentDirection", "rtl");
            } else {
                payload.put("contentDirection", "ltr");
            }
            if (isLangRTL(LanguageUtil.languageCodeToWikiLanguageCode(uiLang))) {
                payload.put("uiDirection", "rtl");
            } else {
                payload.put("uiDirection", "ltr");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setDirectionality", payload);
    }

    /**
     * Sets text direction (RTL / LTR) for given view based on given lang.
     *
     * Doesn't do anything on pre Android 4.2, since their RTL support is terrible.
     *
     * @param view View to set direction of
     * @param lang Wiki code for the language based on which to set direction
     */
    public static void setTextDirection(View view, String lang) {
        if (ApiUtil.hasJellyBeanMr1()) {
            view.setTextDirection(Utils.isLangRTL(lang) ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR);
        }
    }

    /**
     * Returns db name for given site
     *
     * WARNING: HARDCODED TO WORK FOR WIKIPEDIA ONLY
     *
     * @param site Site object to get dbname for
     * @return dbname for given site object
     */
    public static String getDBNameForSite(Site site) {
        return site.getLanguageCode() + "wiki";
    }

    public static void handleExternalLink(final Context context, final Uri uri) {
        if (WikipediaApp.getInstance().getWikipediaZeroHandler().isZeroEnabled()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPref.getBoolean(PrefKeys.getZeroInterstitial(), true)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle(context.getString(R.string.zero_interstitial_title));
                alert.setMessage(context.getString(R.string.zero_interstitial_leave_app));
                alert.setPositiveButton(context.getString(R.string.zero_interstitial_continue), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        visitInExternalBrowser(context, uri);
                    }
                });
                alert.setNegativeButton(context.getString(R.string.zero_interstitial_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
                AlertDialog ad = alert.create();
                ad.show();
            } else {
                visitInExternalBrowser(context, uri);
            }
        } else {
            visitInExternalBrowser(context, uri);
        }
    }

    /**
     * Open the specified URI in an external browser (even if our app's intent filter
     * matches the given URI)
     *
     * @param context Context of the calling app
     * @param uri URI to open in an external browser
     */
    public static void visitInExternalBrowser(final Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);

        Intent chooserIntent = ShareUtils.createChooserIntent(intent, null, context);
        if (chooserIntent == null) {
            // This means that there was no way to handle this link.
            // We will just show a toast now. FIXME: Make this more visible?
            ShareUtils.showUnresolvableIntentMessage(context);
        } else {
            context.startActivity(chooserIntent);
        }
    }

    /**
     * Utility method to detect whether an Email app is installed,
     * for conditionally enabling/disabling email links.
     * @param context Context of the calling app.
     * @return True if an Email app exists, false otherwise.
     */
    public static boolean mailAppExists(Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:test@wikimedia.org"));
        List<ResolveInfo> resInfo = context.getPackageManager().queryIntentActivities(intent, 0);
        return resInfo.size() > 0;
    }

    /**
     * Utility method to copy a stream into another stream.
     *
     * Uses a 16KB buffer.
     *
     * @param in Stream to copy from.
     * @param out Stream to copy to.
     * @throws IOException
     */
    public static void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[KB16]; // 16kb buffer
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    /**
     * Write a JSON object to a file
     * @param file file to be written
     * @param jsonObject content of file
     * @throws IOException when writing failed
     */
    public static void writeToFile(File file, JSONObject jsonObject) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
        try {
            writer.write(jsonObject.toString());
        } finally {
            writer.close();
        }
    }

    /**
     * Write the contents of a String to a stream.
     * @param outputStream Stream to which the contents will be written.
     * @param contents String with the contents to be written.
     * @throws IOException
     */
    public static void writeToStream(OutputStream outputStream, String contents) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        try {
            writer.write(contents);
        } finally {
            writer.close();
        }
    }

    /**
     * Reads the contents of this page from storage.
     * @return Page object with the contents of the page.
     * @throws IOException
     * @throws JSONException
     */
    public static JSONObject readJSONFile(File f) throws IOException, JSONException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String readStr;
            while ((readStr = reader.readLine()) != null) {
                stringBuilder.append(readStr);
            }
            return new JSONObject(stringBuilder.toString());
        } finally {
            reader.close();
        }
    }

    /**
     * Reads the contents of a file, preserving line breaks.
     * @return contents of the given file as a String.
     * @throws IOException
     */
    public static String readFile(final InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            StringBuilder stringBuilder = new StringBuilder();
            String readStr;
            while ((readStr = reader.readLine()) != null) {
                stringBuilder.append(readStr).append('\n');
            }
            return stringBuilder.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Format for formatting/parsing dates to/from the ISO 8601 standard
     */
    private static final String ISO8601_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Parse a date formatted in ISO8601 format.
     *
     * @param dateString Date String to parse
     * @return Parsed Date object.
     * @throws ParseException
     */
    public static Date parseISO8601(String dateString) throws ParseException {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(ISO8601_FORMAT_STRING, Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        date.setTime(sdf.parse(dateString).getTime());
        return date;
    }

    /**
     * Format a date to an ISO8601 formatted string.
     *
     * @param date Date to format.
     * @return The given date formatted in ISO8601 format.
     */
    public static String formatISO8601(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO8601_FORMAT_STRING, Locale.ROOT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Convert a JSONArray object to a String Array.
     *
     * @param array a JSONArray containing only Strings
     * @return a String[] with all the items in the JSONArray
     */
    public static String[] jsonArrayToStringArray(JSONArray array) {
        if (array == null) {
            return null;
        }
        String[] stringArray = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            stringArray[i] = array.optString(i);
        }
        return stringArray;
    }

    /**
     *
     * @param actual The exception object
     * @param expected The class you're trying to find, usually tossed by ExceptionImpl.class, for example.
     * @return boolean true if the Throwable type was found in the nested exception change, else false.
     */
    public static boolean throwableContainsSpecificType(Throwable actual, Class expected) {
        if (actual == null) {
            return false;
        } else if (actual.getClass() == expected) {
            return true;
        } else {
            return throwableContainsSpecificType(actual.getCause(), expected);
        }
    }

    /**
     * Calculates the actual font size for the current device, based on an "sp" measurement.
     * @param window The window on which the font will be rendered.
     * @param fontSp Measurement in "sp" units of the font.
     * @return Actual font size for the given sp amount.
     */
    public static float getFontSizeFromSp(Window window, float fontSp) {
        final DisplayMetrics metrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return fontSp / metrics.scaledDensity;
    }

    /**
     * Resolves the resource ID of a theme-dependent attribute (for example, a color value
     * that changes based on the selected theme)
     * @param activity The activity whose theme contains the attribute.
     * @param id Theme-dependent attribute ID to be resolved.
     * @return The actual resource ID of the requested theme-dependent attribute.
     */
    public static int getThemedAttributeId(Activity activity, int id) {
        TypedValue tv = new TypedValue();
        activity.getTheme().resolveAttribute(id, tv, true);
        return tv.resourceId;
    }

    /**
     * Returns the height of the ActionBar in the current activity. The system controls the
     * height of the ActionBar, which may be slightly different depending on screen orientation,
     * and device version.
     * @param context Context used for retrieving the height attribute.
     * @return Height of the ActionBar.
     */
    public static int getActionBarSize(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(new int[] {
                android.support.v7.appcompat.R.attr.actionBarSize
        });
        int size = (int)styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return size;
    }

    /**
     * Returns the distribution channel for the app from AndroidManifest.xml
     * @return The channel (the empty string if not defined)
     */
    private static String getChannelDescriptor(Context ctx) {
        try {
            ApplicationInfo a = ctx.getPackageManager()
                    .getApplicationInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA);
            String channel = a.metaData.getString(PrefKeys.getChannel());
            return channel != null ? channel : "";
        } catch (Throwable t) {
            // oops
            return "";
        }
    }

    /**
     * Sets the distribution channel for the app into SharedPreferences
     */
    private static void setChannel(Context ctx, String channel) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString(PrefKeys.getChannel(), channel).apply();
    }

    /**
     * Gets the distribution channel for the app from SharedPreferences
     */
    public static String getChannel(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String channel = prefs.getString(PrefKeys.getChannel(), null);
        if (channel == null) {
            channel = getChannelDescriptor(ctx);
            setChannel(ctx, channel);
        }
        return channel;
    }
}
