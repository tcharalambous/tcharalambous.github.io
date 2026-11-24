package cy.kw7.call;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Map;

/** The app's one screen: what it does, and the two switches it needs (phone calls, and KW7CRM's call links). */
public class MainActivity extends Activity {
    static final String HOST = "tcharalambous.github.io";

    @Override protected void onResume() { super.onResume(); show(); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(0, 1.15f);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(8), 0, dp(8));
        return t;
    }

    private Button button(String s, View.OnClickListener go) {
        Button b = new Button(this);
        b.setText(s); b.setAllCaps(false); b.setOnClickListener(go);
        return b;
    }

    private void show() {
        int ink = Color.rgb(28, 28, 28), mute = Color.rgb(95, 99, 104);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(24), dp(40), dp(24), dp(24));
        l.addView(text("KW7 Call", 28, true, Color.rgb(179, 0, 0)));
        l.addView(text("Press Call in KW7CRM, even inside the Claude app, and this opens your phone's dialler with the number. You don't need to open it again once both switches below are on.", 16, false, ink));

        boolean calls = checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
        l.addView(text((calls ? "✓ " : "1. ") + "Phone calls: " + (calls ? "allowed. Call rings straight away." : "not allowed yet. The dialler opens with the number and you press call."), 16, true, ink));
        if (!calls) l.addView(button("Allow phone calls", v -> requestPermissions(new String[] { Manifest.permission.CALL_PHONE }, 1)));

        boolean links = linksOn();
        l.addView(text((links ? "✓ " : "2. ") + "KW7CRM's Call links: " + (links ? "open in KW7 Call." : "not on yet. Tap below, then Add link and tick " + HOST + "."), 16, true, ink));
        if (!links) l.addView(button("Turn on KW7CRM's Call links", v -> openLinkSettings()));

        l.addView(text("This app keeps nothing and sends nothing anywhere. KW7CRM records the call itself.", 14, false, mute));
        ScrollView s = new ScrollView(this);
        s.setBackgroundColor(Color.rgb(247, 247, 247));
        s.addView(l);
        setContentView(s);
    }

    private boolean linksOn() {
        if (Build.VERSION.SDK_INT < 31) return true; // older Android asks which app to use the first time
        try {
            DomainVerificationManager m = getSystemService(DomainVerificationManager.class);
            DomainVerificationUserState st = m.getDomainVerificationUserState(getPackageName());
            if (st == null) return false;
            Map<String, Integer> map = st.getHostToStateMap();
            Integer s = map.get(HOST);
            return s != null && s != DomainVerificationUserState.DOMAIN_STATE_NONE;
        } catch (Exception e) { return false; }
    }

    private void openLinkSettings() {
        Uri pkg = Uri.parse("package:" + getPackageName());
        try { startActivity(new Intent("android.settings.APP_OPEN_BY_DEFAULT_SETTINGS", pkg)); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)); }
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) { show(); }
}
