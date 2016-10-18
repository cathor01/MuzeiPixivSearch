package cn.cathor.muzeipixivsearch.settings


import android.annotation.TargetApi
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.LinearLayout
import cn.cathor.muzeipixivsearch.Definition
import cn.cathor.muzeipixivsearch.R
import cn.cathor.muzeipixivsearch.controller.RequestController
import cn.cathor.muzeipixivsearch.debug
import cn.cathor.muzeipixivsearch.items.PixivItem
import com.orm.SugarRecord
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.find
import org.jetbrains.anko.toast

/**
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 *
 * See [
   * Android Design: Settings](http://developer.android.com/design/patterns/settings.html) for design guidelines and the [Settings
   * API Guide](http://developer.android.com/guide/topics/ui/settings.html) for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = findViewById(android.R.id.list).parent.parent.parent as LinearLayout
        val toolbar = Toolbar(this)
        toolbar.backgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)
        root.addView(toolbar, 0)
        setSupportActionBar(toolbar)
        setupActionBar()
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
                || DataSyncPreferenceFragment::class.java.name == fragmentName
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
            setHasOptionsMenu(true)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("pixiv_days"))
            bindPreferenceSummaryToValue(findPreference("pixiv_daily"))
            bindPreferenceSummaryToValue(findPreference("pixiv_tag"))
            bindPreferenceSummaryToValue(findPreference("pixiv_star"))
            findPreference("pixiv_login_cookies").onPreferenceClickListener = clickListener
            findPreference("pixiv_refresh_button").onPreferenceClickListener = clickListener
            val preference = PreferenceManager.getDefaultSharedPreferences(activity).getString("pixiv_login_cookies", null)
            if(preference != null){
                findPreference("pixiv_login_cookies").setSummary(R.string.pref_logined)
            }
            findPreference("pixiv_refresh_button").summary = getString(R.string.pref_refresh_summary, SugarRecord.count<PixivItem>(PixivItem::class.java).toInt())
        }

        val clickListener = Preference.OnPreferenceClickListener { preference ->
            when(preference.key){
                "pixiv_login_cookies" -> {
                    val view = LayoutInflater.from(activity).inflate(R.layout.login_fragment, null)
                    AlertDialog.Builder(activity).setView(view).setPositiveButton(R.string.record, { dialog, which ->
                        val id = view.find<TextInputLayout>(R.id.login_id).editText!!.text.toString()
                        val password = view.find<TextInputLayout>(R.id.login_password).editText!!.text.toString()
                        val editor = PreferenceManager.getDefaultSharedPreferences(activity).edit()
                        editor.putString("pixiv_login_cookies", "$id.#.$password")
                        editor.commit()
                        findPreference("pixiv_login_cookies").setSummary(R.string.pref_logined)
                    }).setTitle(getString(R.string.login_information)).show()

                    true
                }
                "pixiv_refresh_button" -> {
                    val pref = PreferenceManager.getDefaultSharedPreferences(activity)
                    val tag = pref.getString("pixiv_tag", null)
                    val page = pref.getString("pixiv_days", null)
                    val count = pref.getString("pixiv_daily", null)
                    val cookies = pref.getString("pixiv_login_cookies", null)
                    val star = pref.getString("pixiv_star", null)
                    val r18 = pref.getBoolean("pixiv_r18", true)
                    if(tag == null || page == null || count == null || cookies == null || star == null){
                        toast(R.string.unfinished)
                    }
                    else{
                        val id_pass = cookies.split(".#.")
                        val dialog = ProgressDialog(activity)
                        dialog.setCancelable(false)
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
                        dialog.setTitle(R.string.refresh_title)
                        dialog.show()
                        val controller = RequestController.getInstance()
                        controller.search(id_pass[0], id_pass[1], tag, page.toInt(), star.toInt(), count.toInt(), {
                            dialog.cancel()
                            toast(R.string.success)
                            findPreference("pixiv_refresh_button").summary = getString(R.string.pref_refresh_summary, SugarRecord.count<PixivItem>(PixivItem::class.java).toInt())
                        }, {
                            dialog.cancel()
                            toast(it.message!!)
                            if (Definition.DEBUG_LOG) debug(it.message)
                        }, {
                            dialog.setMessage(it)
                        }, r18)
                    }
                    true
                }
                else -> false
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }




    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class DataSyncPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_data_sync)
            setHasOptionsMenu(true)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("pixiv_change_frequency"))
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val index = preference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            preference.entries[index]
                        else
                            null)

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.context).getString(preference.key, ""))
        }
    }
}
