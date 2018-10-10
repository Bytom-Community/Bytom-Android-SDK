package com.io.wallet.bean;

import com.google.gson.annotations.SerializedName;
import com.io.wallet.utils.Strings;

import java.util.List;

public class Account {
  private String id;
  private String alias;
  private String type;
  private List<String> xpubs;
  private int quorum;

  @SerializedName("keyIndex")
  private int key_index;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<String> getXpubs() {
    return xpubs;
  }

  public void setXpubs(List<String> xpubs) {
    this.xpubs = xpubs;
  }

  public int getQuorum() {
    return quorum;
  }

  public void setQuorum(int quorum) {
    this.quorum = quorum;
  }

  public int getKey_index() {
    return key_index;
  }

  public void setKey_index(int key_index) {
    this.key_index = key_index;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String toJson() {
    return Strings.serializer.toJson(this);
  }

  public static Account getAccount(String account) {
    return Strings.serializer.fromJson(account, Account.class);
  }
}
