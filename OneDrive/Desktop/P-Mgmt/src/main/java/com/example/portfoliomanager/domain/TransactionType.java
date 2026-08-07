package com.example.portfoliomanager.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transaction type: BUY, SELL, or DIVIDEND")
public enum TransactionType {
    BUY,
    SELL,
    DIVIDEND
}
