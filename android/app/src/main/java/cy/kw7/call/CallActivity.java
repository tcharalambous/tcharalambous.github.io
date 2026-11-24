package cy.kw7.call;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

/** Opened by KW7CRM's links. Shows nothing of its own.
 *  - A Call link (the number after the #, e.g. /call/#+35799123456): rings it straight away when phone calls are allowed,
 *    otherwise opens the dialler with the number ready.
 *  - The Refresh link (/call/contacts): saves the phone's contacts as Downloads/kw7-contacts.vcf for KW7CRM, then goes back. */
public class CallActivity extends Activity {
    private static final int ASK_CALL = 7, ASK_CONTACTS = 8;
    private String tel = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Uri u = getIntent() == null ? null : getIntent().getData();
        String path = u == null || u.getPath() == null ? "" : u.getPath();
        String frag = u == null || u.getFragment() == null ? "" : u.getFragment();
        if (path.startsWith("/call/contacts") || frag.equals("contacts")) { contacts(); return; }
        tel = frag.replaceAll("[^0-9+]", "");
        if (tel.replaceAll("\\D", "").length() < 3) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) call();
        else requestPermissions(new String[] { Manifest.permission.CALL_PHONE }, ASK_CALL);
    }

    // ---- calls ----
    private Uri telUri() { return Uri.parse("tel:" + Uri.encode(tel)); }

    private void call() {
        try { startActivity(new Intent(Intent.ACTION_CALL, telUri())); }
        catch (Exception e) { dial(); }
        finish();
    }

    private void dial() {
        try { startActivity(new Intent(Intent.ACTION_DIAL, telUri())); } catch (Exception ignored) { }
    }

    // ---- contacts for KW7CRM ----
    static String[] contactPerms() {
        return Build.VERSION.SDK_INT >= 29 ? new String[] { Manifest.permission.READ_CONTACTS }
            : new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    }

    static boolean contactsAllowed(Activity a) {
        for (String p : contactPerms()) if (a.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) return false;
        return true;
    }

    private void contacts() {
        if (contactsAllowed(this)) saveContacts();
        else requestPermissions(contactPerms(), ASK_CONTACTS);
    }

    private void saveContacts() {
        new Thread(() -> {
            String msg;
            try {
                int n = ContactsExport.save(getApplicationContext());
                msg = n + " contacts saved for KW7CRM. Back in KW7CRM, tap Pick the contacts file and choose " + ContactsExport.FILE + ".";
            } catch (Exception e) {
                msg = "KW7 Call couldn't save your contacts. Open KW7 Call and try Save contacts for KW7CRM.";
            }
            final String m = msg;
            runOnUiThread(() -> { Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show(); finish(); });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        boolean ok = results.length > 0;
        for (int r : results) ok &= r == PackageManager.PERMISSION_GRANTED;
        if (code == ASK_CALL) { if (ok) call(); else { dial(); finish(); } }
        else if (code == ASK_CONTACTS) {
            if (ok) saveContacts();
            else { Toast.makeText(getApplicationContext(), "KW7 Call needs to read your contacts to send them to KW7CRM.", Toast.LENGTH_LONG).show(); finish(); }
        }
    }
}
