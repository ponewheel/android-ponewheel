package net.kwatts.powtools;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.content.*;
import java.util.*;
/**
 * Created by kwatts on 6/15/16.
 */
public class Util {


    public static String getUsername(Context c) {
        AccountManager manager = AccountManager.get(c);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            // TODO: Check possibleEmail against an email regex or treat
            // account.name as an email address only for certain account.type values.
            possibleEmails.add(account.name);
        }

        if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");

            if (parts.length > 1)
                return parts[0];
        }
        return null;
    }
}
