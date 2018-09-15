package com.io.wallet.blockchain.account;

import android.text.TextUtils;

import com.io.wallet.bean.Account;
import com.io.wallet.bean.ImageSlice;
import com.io.wallet.crypto.Wallet;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.List;

import static com.io.wallet.utils.Constant.ACCOUNT;
import static com.io.wallet.utils.Constant.ACCOUNTALIAS;
import static com.io.wallet.utils.Constant.ACCOUNTCACHE;
import static com.io.wallet.utils.Constant.ACCOUNTINDEX;

/**
 * Created by hwj on 2018/9/15.
 */

public class AccountCache {

    private static ArrayList<Account> accountCache = new ArrayList<>();

    public AccountCache() {
        accountCache = Hawk.get(ACCOUNTCACHE, new ArrayList<Account>());
    }

    public static void addAccount(Account account) {
        accountCache.add(account);
        Hawk.put(ACCOUNTCACHE, accountCache);
        Hawk.put(ACCOUNT + account.getId(), account);
        Hawk.put(ACCOUNTALIAS + account.getAlias(), account.getId());
    }

    public static boolean hasAlias(String alias) {
        if (TextUtils.isEmpty(alias)) return false;
        for (Account item : accountCache) {
            if (item.getAlias().equals(alias)) {
                return true;
            }
        }
        return false;
    }

    public static String getAccoutnId(String alias) {
        return Hawk.get(ACCOUNTALIAS + alias.trim().toLowerCase(), "");
    }

    public static Account getAccountById(String id) {
        return Hawk.get(ACCOUNT + id, null);
    }

    public static int getNextAccountIndex() {
        int nextIndex = Hawk.get(ACCOUNTINDEX, 1);
        Hawk.put(ACCOUNTINDEX, nextIndex + 1);
        return nextIndex;
    }

    public static ArrayList<Account> getAllAccount() {
        return accountCache;
    }

    public static List<ImageSlice> backup() {
        ArrayList<ImageSlice> imageSlice = new ArrayList<>();
        for (Account account : accountCache) {
            imageSlice.add(new ImageSlice(account, getNextContractIndex(account.getId())));
        }
        return imageSlice;
    }

    public static int getNextContractIndex(String accountID) {
        int nextIndex = Hawk.get("ContractIndex" + accountID, 1);
        Hawk.put("ContractIndex" + accountID, nextIndex + 1);
        return nextIndex;
    }

    public static void restore(List<ImageSlice> imageSlice) throws Exception {
        int maxAccountIndex = 0;
        for (ImageSlice slice : imageSlice) {
            if (null != Hawk.get(ACCOUNT + slice.account.getId(), null)) {
                System.out.println("skip restore account due to already existed");
                continue;
            }
            if (hasAlias(slice.account.getAlias())) {
                throw new Exception("duplicate account alias");
            }
            if (slice.account.getKey_index() > maxAccountIndex) {
                maxAccountIndex = slice.account.getKey_index();
            }
            addAccount(slice.account);
        }
        Hawk.put(ACCOUNTINDEX, maxAccountIndex);
        for (ImageSlice slice : imageSlice) {
            for (int i = 1; i < slice.contract_index; i++) {
                Wallet.createAddress(slice.account.getId(), slice.account.getAlias());
            }
        }
    }
}
