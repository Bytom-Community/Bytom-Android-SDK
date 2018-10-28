## Bytom-Android-SDK

![](https://img.shields.io/badge/minSdk-15-yellow.svg) ![](https://img.shields.io/badge/version-1.0.1-orange.svg)
![](https://img.shields.io/badge/license-Apache%202-blue.svg) ![](https://img.shields.io/badge/build-passing-green.svg)

It is a project for Bytom Android SDK,this SDK contains methods for easily interacting with the Bytom wallet  at local .

## Installation

you can relevant dependency to your project:

- maven:

        <dependency>
            <groupId>com.io.bytom</groupId>
            <artifactId>wallet</artifactId>
            <version>1.0.1</version>
            <type>pom</type>
        </dependency>

- android:

         implementation  'com.io.bytom:wallet:1.0.1'
         
## Guide

This guide will walk you through the basic functions of Bytom-Android-Sdk：

#### Init the SDK

    BytomWallet.initWallet(getApplication());

> when initializing application, you need to apply the following permission at the same time.

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
#### Create Key
- `string`：alias , name of the key
- `string`：password , passowrld of the key

```
BytomWallet.createKey(alias, "123");
```
    
#### List all keys

```
BytomWallet.listKeys();
```

#### Create account
- `string`：alias , name of the key
- `Integer`: quorum, the default value is 1, threshold of keys that must sign a transaction to spend asset units controlled by the account.
- `string`：xpub, pubkey of the key.

```
BytomWallet.createAccount(alias, 1, xpub);
```
    
#### List all account

```
BytomWallet.listAccounts();
```

#### Create address 
- `string`：account_id, id of account.
- `string`：account_alias, alias of account.

```
BytomWallet.createAccountReceiver(accountId, accountAlias);
```
    
#### List all address
- `string`：account_id, id of account.
- `string`：account_alias, alias of account.

```
BytomWallet.listAddress(accountId, accountAlias);
```
    
#### Backup Wallet

```
BytomWallet.backupWallet();
```
    
#### Restore wallet image
- `string`：walletImage, string of walletImage.

```
BytomWallet.restoreWallet("")
```

#### For more
    
You find more examples at [examples](https://github.com/Bytom-Community/Bytom-Android-SDK/blob/dev/app/src/main/java/com/io/bytom/MainActivity.java) . If you find a bug, please submit the [issue](https://github.com/Bytom-Community/Bytom-Android-SDK/issues) in Github directly.
    
    
