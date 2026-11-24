package cy.kw7.call;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

/** Opened by a KW7CRM Call link: the number is after the # (e.g. #+35799123456). Rings it straight away when phone calls
 *  are allowed, otherwise opens the dialler with the number ready. Shows nothing of its own. */
public class CallActivity extends Activity {
    private static final int ASK = 7;
    private String tel = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Uri u = getIntent() == null ? null : getIntent().getData();
        String raw = u == null || u.getFragment() == null ? "" : u.getFragment();
        tel = raw.replaceAll("[^0-9+]", "");
        if (tel.replaceAll("\\D", "").length() < 3) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) call();
        else requestPermissions(new String[] { Manifest.permission.CALL_PHONE }, ASK);
    }

    private Uri telUri() { return Uri.parse("tel:" + Uri.encode(tel)); }

    private void call() {
        try { startActivity(new Intent(Intent.ACTION_CALL, telUri())); }
        catch (Exception e) { dial(); }
        finish();
    }

    private void dial() {
        try { startActivity(new Intent(Intent.ACTION_DIAL, telUri())); } catch (Exception ignored) { }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        if (code != ASK) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) call();
        else { dial(); finish(); }
    }
}
