package org.wikipedia.interlanguage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.R;
import org.wikipedia.settings.PreferenceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.wikipedia.util.StringUtil.defaultIfNull;
import static org.wikipedia.util.StringUtil.emptyIfNull;

/** Language lookup and state management for the application language and most recently used article
 * and application languages. */
public class AppLanguageState {
    public static final String SYSTEM_LANGUAGE_CODE = null;

    @NonNull
    private final Context context;

    @NonNull
    private final AppLanguageLookUpTable appLanguageLookUpTable;

    // The language code used by the app when the article language is unspecified. It's possible for
    // this code to be unsupported if the languages supported changes. Null is a special value that
    // indicates the system language should used.
    @Nullable
    private String appLanguageCode;

    // Language codes that have been explicitly chosen by the user in most recently used order. This
    // list includes both app and article languages.
    @NonNull
    private final List<String> mruLanguageCodes;

    public AppLanguageState(@NonNull Context context) {
        this.context = context;
        appLanguageLookUpTable = new AppLanguageLookUpTable(context);
        appLanguageCode = PreferenceUtil.getAppLanguageCode();
        mruLanguageCodes = unmarshalMruLanguageCodes();
    }

    @Nullable
    public String getAppLanguageCode() {
        return appLanguageCode;
    }

    @NonNull
    public String getAppOrSystemLanguageCode() {
        return isSystemLanguageEnabled() ? getSystemLanguageCode() : appLanguageCode;
    }

    public void setAppLanguageCode(@Nullable String code) {
        appLanguageCode = code;
        PreferenceUtil.setAppLanguageCode(code);
    }

    public boolean isSystemLanguageEnabled() {
        return emptyIfNull(getAppLanguageCode()).equals(emptyIfNull(SYSTEM_LANGUAGE_CODE));
    }

    public void setSystemLanguageEnabled() {
        setAppLanguageCode(SYSTEM_LANGUAGE_CODE);
    }

    @NonNull
    public String getSystemLanguageCode() {
        String code = LanguageUtil.languageCodeToWikiLanguageCode(Locale.getDefault().getLanguage());
        return appLanguageLookUpTable.isSupportedCode(code)
                ? code
                : AppLanguageLookUpTable.FALLBACK_LANGUAGE_CODE;
    }

    public Locale getAppLocale() {
        return isSystemLanguageEnabled() ? Locale.getDefault() : new Locale(getAppLanguageCode());
    }

    /** Note: returned codes may include languages offered by articles but not the app. */
    @NonNull
    public List<String> getMruLanguageCodes() {
        return mruLanguageCodes;
    }

    public void setMruLanguageCode(@Nullable String code) {
        List<String> codes = getMruLanguageCodes();
        codes.remove(code);
        codes.add(0, code);
        PreferenceUtil.setMruLanguageCodes(listToCsv(codes));
    }

    /** @return All app supported languages in MRU order. */
    public List<String> getAppMruLanguageCodes() {
        List<String> codes = new ArrayList<>(appLanguageLookUpTable.getCodes());
        int insertIndex = 0;
        for (String code : getMruLanguageCodes()) {
            if (codes.contains(code)) {
                codes.remove(code);
                codes.add(insertIndex, code);
                ++insertIndex;
            }
        }
        return codes;
    }

    @Nullable
    public String getAppLanguageCanonicalName() {
        return isSystemLanguageEnabled()
                ? getString(R.string.preference_system_language_summary)
                : getAppLanguageCanonicalName(getAppLanguageCode());
    }

    /** @return English name if app language is supported. */
    @Nullable
    public String getAppLanguageCanonicalName(String code) {
        return appLanguageLookUpTable.getCanonicalName(code);
    }

    @Nullable
    public String getAppLanguageLocalizedName() {
        return isSystemLanguageEnabled()
                ? getString(R.string.preference_system_language_summary)
                : getAppLanguageLocalizedName(getAppLanguageCode());
    }

    /** @return Native name if app language is supported. */
    @Nullable
    public String getAppLanguageLocalizedName(String code) {
        return appLanguageLookUpTable.getLocalizedName(code);
    }

    @NonNull
    private List<String> unmarshalMruLanguageCodes() {
        // Null value is used to indicate that system language should be used.
        String systemLanguageCodeString = String.valueOf(SYSTEM_LANGUAGE_CODE);

        String csv = defaultIfNull(PreferenceUtil.getMruLanguageCodes(), systemLanguageCodeString);

        List<String> list = new ArrayList<>(csvToList(csv));

        Collections.replaceAll(list, systemLanguageCodeString, SYSTEM_LANGUAGE_CODE);

        return list;
    }

    @NonNull
    private String listToCsv(@NonNull List<String> list) {
        return TextUtils.join(",", list);
    }

    /** @return Nonnull immutable list. */
    @NonNull
    private List<String> csvToList(@NonNull String csv) {
        return Arrays.asList(csv.split(","));
    }

    @Nullable
    private String getString(int id, @Nullable Object... formatArgs) {
        return context.getString(id, formatArgs);
    }
}