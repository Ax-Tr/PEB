package com.paywithease.ledger.domain;

/** The five fundamental account types and their normal (increasing) side. */
public enum AccountType {
  ASSET(NormalSide.DEBIT),
  LIABILITY(NormalSide.CREDIT),
  EQUITY(NormalSide.CREDIT),
  INCOME(NormalSide.CREDIT),
  EXPENSE(NormalSide.DEBIT);

  private final NormalSide normalSide;

  AccountType(NormalSide normalSide) {
    this.normalSide = normalSide;
  }

  public NormalSide normalSide() {
    return normalSide;
  }
}
