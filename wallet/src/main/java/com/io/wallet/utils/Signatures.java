package com.io.wallet.utils;

import android.util.Log;

import com.io.wallet.bean.RawTransaction;
import com.io.wallet.bean.Template;
import com.io.wallet.crypto.ChainKd;
import com.io.wallet.crypto.NonHardenedChild;
import com.io.wallet.crypto.Signer;

import org.bouncycastle.util.encoders.Hex;

public class Signatures {

    public static Template generateSignatures(String[] privateKeys, Template template, RawTransaction decodedTx) {
        Template result = template;
        for (int i = 0; i < template.signingInstructions.size(); i++) {
            Template.SigningInstruction sigIns = template.signingInstructions.get(i);
            for (Template.WitnessComponent wc : sigIns.witnessComponents) {
                // Have two cases
                switch (wc.type) {
                    case "raw_tx_signature":
                        Log.d("", "=====raw_tx_signature");
                        Log.d("keys.length: ", String.valueOf(wc.keys.length));
                        if (wc.signatures == null || wc.signatures.length < wc.keys.length) {
                            wc.signatures = new String[wc.keys.length];
                        }
                        // 一个input对应一个Template.WitnessComponent
                        String input = decodedTx.inputs.get(sigIns.position).inputID;
                        String tx_id = decodedTx.txID;
                        byte[] message = decodedTx.hashFn(Hex.decode(input), Hex.decode(tx_id));
                        for (int j = 0; j < wc.keys.length; j++) {
                            if (wc.signatures[j] == null || wc.signatures[j].isEmpty()) {

                                byte[] sig = new byte[64];
                                try {
                                    String publicKey = wc.keys[j].xpub;
                                    // 多签情况下，找到xpub对应的private key的下标 dst
                                    int dst = ChainKd.find(privateKeys, publicKey);
                                    //一级私钥
                                    byte[] privateKey = Hex.decode(privateKeys[dst]);
                                    // 一级私钥推出二级私钥
                                    String[] hpaths = wc.keys[j].derivationPath;
                                    byte[] childXprv = NonHardenedChild.child(privateKey, hpaths);
                                    // 一级私钥推出公钥
                                    byte[] xpub = ChainKd.deriveXpub(privateKey);
                                    // 二级私钥得到扩展私钥
                                    byte[] expandedPrv = HDUtils.expandedPrivateKey(childXprv);
                                    Log.d("privateKey: ", Strings.byte2hex(privateKey));
                                    Log.d("childXpriv: ", Strings.byte2hex(childXprv));
                                    Log.d("xpub: ", Strings.byte2hex(xpub));
                                    Log.d("message: ", Strings.byte2hex(message));
                                    sig = Signer.Ed25519InnerSign(expandedPrv, message);
                                    Log.d("sig google: ", Strings.byte2hex(sig));

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Log.d("sig:", Hex.toHexString(sig));
                                wc.signatures[j] = Hex.toHexString(sig);
                                result.signingInstructions.get(i).witnessComponents[j].signatures = wc.signatures;
                            }

                        }
                        break;
                    case "":

                        break;
                    default:

                }
            }
        }
        return result;
    }
}
