package com.pyjudge.dao;

import com.pyjudge.db.Database;
import java.util.LinkedHashMap;
import java.util.Map;

/** Key/value rows behind the admin's system settings. */
public class SettingDao extends BaseDao {
    public SettingDao(Database db) {
        super(db);
    }

    public Map<String, String> all() {
        Map<String, String> out = new LinkedHashMap<>();
        db.query("SELECT setting_key, setting_value FROM settings", rs -> out.put(rs.getString(1), rs.getString(2)));
        return out;
    }

    /** Insert-or-update for every entry, as one transaction. */
    public void saveAll(Map<String, String> values) {
        db.inTransaction(() -> values.forEach((key, value) -> {
            int changed = db.update("UPDATE settings SET setting_value = ? WHERE setting_key = ?", value, key);
            if (changed == 0) {
                db.update("INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)", key, value);
            }
        }));
    }
}
