package com.io.wallet.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.io.wallet.bean.WalletFile;
import com.io.wallet.bean.Xpub;
import com.io.wallet.utils.StoragePermission;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Storage {

    private ArrayList<WalletFile> mapWallet;
    private static Storage instance;
    private String KEYS_PATH;

    public static Storage getInstance() {
        if (instance == null)
            instance = new Storage();
        return instance;
    }

    private Storage() {
        try {
            load();
        } catch (Exception e) {
            e.printStackTrace();
            mapWallet = new ArrayList<>();
        }
    }

    public void init(String path) {
        this.KEYS_PATH = path;
    }

    public synchronized boolean add(WalletFile wallet) {
        for (int i = 0; i < mapWallet.size(); i++)
            if (mapWallet.get(i).getXpub().equalsIgnoreCase(wallet.getXpub())) return false;
        mapWallet.add(wallet);
        save();
        return true;
    }

    public boolean hasAlias(String alias) {
        if (null == mapWallet || 0 == mapWallet.size()) return false;
        for (int i = 0; i < mapWallet.size(); i++) {
            if (mapWallet.get(i).getAlias().equalsIgnoreCase(alias)) return true;
        }
        return false;
    }

    public synchronized ArrayList<WalletFile> get() {
        return mapWallet;
    }


    public void removeWallet(String address) {
        int position = -1;
        for (int i = 0; i < mapWallet.size(); i++) {
            if (mapWallet.get(i).getXpub().equalsIgnoreCase(address)) {
                position = i;
                break;
            }
        }
        if (position >= 0) {
            mapWallet.remove(position);
            save();
        }
    }

    public void importWallets(Context c, ArrayList<File> toImport) throws Exception {
        for (int i = 0; i < toImport.size(); i++) {
            String address = stripWalletName(toImport.get(i).getName());
            if (address.length() == 40) {
                copyFile(toImport.get(i), new File(c.getFilesDir(), address));
//                Storage.getInstance(c).add(new FullWallet("0x" + address, address), c);
//                AddressNameConverter.getInstance(c).put("0x" + address, "Wallet " + ("0x" + address).substring(0, 6), c);

                Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri fileContentUri = Uri.fromFile(toImport.get(i)); // With 'permFile' being the File object
                mediaScannerIntent.setData(fileContentUri);
                c.sendBroadcast(mediaScannerIntent); // With 'this' being the context, e.g. the activity

            }
        }
    }

    public static String stripWalletName(String s) {
        if (s.lastIndexOf("--") > 0)
            s = s.substring(s.lastIndexOf("--") + 2);
        if (s.endsWith(".json"))
            s = s.substring(0, s.indexOf(".json"));
        return s;
    }

    private boolean exportWallet(Activity c, boolean already, String walletToExport) {
        if (walletToExport == null) return false;
        if (walletToExport.startsWith("bm"))
            walletToExport = walletToExport.substring(2);

        if (StoragePermission.hasPermission(c)) {
            File folder = new File(Environment.getExternalStorageDirectory(), "bytom");
            if (!folder.exists()) folder.mkdirs();

            File storeFile = new File(folder, walletToExport + ".json");
            try {
                copyFile(new File(c.getFilesDir(), walletToExport), storeFile);
            } catch (IOException e) {
                return false;
            }

            Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri fileContentUri = Uri.fromFile(storeFile);
            mediaScannerIntent.setData(fileContentUri);
            c.sendBroadcast(mediaScannerIntent);
            return true;
        } else if (!already) {
            StoragePermission.askForPermission(c);
            return exportWallet(c, true, walletToExport);
        } else {
            return false;
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public synchronized void save() {
        save(mapWallet, "wallets.dat");
    }

    public synchronized String save(Object object, String name) {
        FileOutputStream fout;
        File file;
        try {
            file = new File(KEYS_PATH, name);
            if (!file.exists()) file.createNewFile();
            fout = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(object);
            oos.close();
            fout.close();
        } catch (Exception e) {
            return "";
        } finally {

        }
        return file.getAbsolutePath();
    }

    public String saveKey(WalletFile key, String name) {
        add(key);
        File file;
        PrintStream ps = null;
        try {
            file = new File(KEYS_PATH, name);
            if (!file.exists()) file.createNewFile();
            ps = new PrintStream(new FileOutputStream(file));
            ps.println(key.toJson());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (null != ps) {
                ps.close();
            }
        }
        return file.getAbsolutePath();
    }

    public synchronized void load() throws IOException, ClassNotFoundException {
        FileInputStream fout = new FileInputStream(new File(KEYS_PATH, "wallets.dat"));
        ObjectInputStream oos = new ObjectInputStream(new BufferedInputStream(fout));
        mapWallet = (ArrayList<WalletFile>) oos.readObject();
        oos.close();
        fout.close();
    }

    public List<Xpub> loadKeys() throws IOException {
        List<Xpub> list = new ArrayList<>();
        File baseFile = new File(KEYS_PATH);
        if (baseFile.isFile() || !baseFile.exists()) {
            return list;
        }
        File[] files = baseFile.listFiles();
        for (File file : files) {
            list.add(Xpub.getXpubObj(getFileString(file)));
        }
        return list;
    }

    public static String getFileString(File file) throws IOException {
        FileInputStream fis;
        InputStreamReader isr;
        fis = new FileInputStream(file);
        isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader bf = new BufferedReader(isr);
        String content = "";
        StringBuilder sb = new StringBuilder();
        while (content != null) {
            content = bf.readLine();
            if (content == null) {
                break;
            }
            sb.append(content.trim());
        }
        bf.close();
        return sb.toString();
    }

}
