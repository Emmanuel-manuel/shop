package emm.sys;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

//import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "Login.db";

    public DBHelper(Context context) {
        super(context, "Login.db", null, 1);
    }

    //    ............. The database name is called LoginDetails ..............
    @Override
    public void onCreate(SQLiteDatabase LoginDetails) {
        LoginDetails.execSQL("create Table users(role TEXT, email TEXT primary key, password TEXT)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase LoginDetails, int i, int i1) {
        LoginDetails.execSQL("drop Table if exists users");

    }

    public Boolean insertData(String role, String email, String password) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("role", role);
        contentValues.put("email", email);
        contentValues.put("password", password);
        long result = LoginDetails.insert("users", null, contentValues);
        if (result == -1) return false;
        else
            return true;

    }

    public Boolean checkusername(String email) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        Cursor cursor = LoginDetails.rawQuery("Select * from users where email = ?", new String[]{email});
        if (cursor.getCount() > 0)
            return true;
        else
            return false;
    }

    public Boolean checkusernamepassword(String email, String password) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        Cursor cursor = LoginDetails.rawQuery("Select * from users where email = ? and password = ?", new String[]{email, password});
        if (cursor.getCount() > 0)
            return true;
        else
            return false;
    }

    public Boolean checkRolePassword(String role, String password) {
        SQLiteDatabase LoginDetails = this.getWritableDatabase();
        Cursor cursor = LoginDetails.rawQuery(
                "SELECT * FROM users WHERE role = ? AND password = ?",
                new String[]{role, password}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
}
