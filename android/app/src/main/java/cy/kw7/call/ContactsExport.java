package cy.kw7.call;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Saves the phone's contacts (names and numbers only, Google ones included) as Downloads/kw7-contacts.vcf,
 *  replacing the copy it saved before, for KW7CRM's dialler to read. Nothing leaves the phone. */
final class ContactsExport {
    static final String FILE = "kw7-contacts.vcf";

    private ContactsExport() { }

    /** Returns how many contacts with a number were saved. */
    static int save(Context c) throws Exception {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, List<String[]>> nums = new LinkedHashMap<>();
        String[] cols = { Phone.CONTACT_ID, Phone.DISPLAY_NAME_PRIMARY, Phone.NUMBER, Phone.TYPE };
        try (Cursor k = c.getContentResolver().query(Phone.CONTENT_URI, cols, null, null, Phone.DISPLAY_NAME_PRIMARY + " COLLATE NOCASE")) {
            if (k == null) throw new Exception("no contacts");
            while (k.moveToNext()) {
                long id = k.getLong(0);
                String name = k.isNull(1) ? "" : k.getString(1), num = k.isNull(2) ? "" : k.getString(2).trim();
                if (num.replaceAll("\\D", "").length() < 6) continue;
                names.put(id, name);
                List<String[]> l = nums.get(id);
                if (l == null) { l = new ArrayList<>(); nums.put(id, l); }
                l.add(new String[] { num, kind(k.getInt(3)) });
            }
        }
        StringBuilder v = new StringBuilder();
        int n = 0;
        for (Map.Entry<Long, List<String[]>> e : nums.entrySet()) {
            Set<String> seen = new HashSet<>();
            StringBuilder tels = new StringBuilder();
            for (String[] t : e.getValue()) {
                String d = t[0].replaceAll("\\D", "");
                String key = d.length() > 8 ? d.substring(d.length() - 8) : d;
                if (!seen.add(key)) continue;
                tels.append("TEL;TYPE=").append(t[1]).append(':').append(t[0].replaceAll("[\\r\\n]", " ")).append("\r\n");
            }
            if (tels.length() == 0) continue;
            v.append("BEGIN:VCARD\r\nVERSION:3.0\r\nFN:").append(esc(names.get(e.getKey()))).append("\r\n").append(tels).append("END:VCARD\r\n");
            n++;
        }
        write(c, v.toString().getBytes(StandardCharsets.UTF_8));
        return n;
    }

    private static String kind(int t) {
        switch (t) {
            case Phone.TYPE_MOBILE: case Phone.TYPE_WORK_MOBILE: return "CELL";
            case Phone.TYPE_HOME: return "HOME";
            case Phone.TYPE_WORK: return "WORK";
            case Phone.TYPE_MAIN: case Phone.TYPE_COMPANY_MAIN: return "MAIN";
            case Phone.TYPE_FAX_HOME: case Phone.TYPE_FAX_WORK: case Phone.TYPE_OTHER_FAX: return "FAX";
            default: return "VOICE";
        }
    }

    private static String esc(String s) {
        return (s == null ? "" : s).replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replaceAll("[\\r\\n]+", " ").trim();
    }

    private static void write(Context c, byte[] data) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver r = c.getContentResolver();
            Uri dl = MediaStore.Downloads.EXTERNAL_CONTENT_URI, uri = null;
            // the copy this app saved last time, if it's still there (an app sees its own files without asking)
            try (Cursor k = r.query(dl, new String[] { MediaStore.MediaColumns._ID }, MediaStore.MediaColumns.DISPLAY_NAME + "=?", new String[] { FILE }, null)) {
                if (k != null && k.moveToFirst()) uri = Uri.withAppendedPath(dl, String.valueOf(k.getLong(0)));
            } catch (Exception ignored) { }
            if (uri != null) {
                try (OutputStream o = r.openOutputStream(uri, "wt")) { o.write(data); return; }
                catch (Exception e) { try { r.delete(uri, null, null); } catch (Exception ignored) { } }
            }
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, FILE);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, "text/x-vcard");
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
            uri = r.insert(dl, cv);
            if (uri == null) throw new Exception("couldn't save");
            try (OutputStream o = r.openOutputStream(uri, "w")) { o.write(data); }
            cv.clear(); cv.put(MediaStore.MediaColumns.IS_PENDING, 0);
            r.update(uri, cv, null, null);
        } else {
            File d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            d.mkdirs();
            File f = new File(d, FILE);
            try (FileOutputStream o = new FileOutputStream(f, false)) { o.write(data); }
            MediaScannerConnection.scanFile(c, new String[] { f.getAbsolutePath() }, new String[] { "text/x-vcard" }, null);
        }
    }
}
