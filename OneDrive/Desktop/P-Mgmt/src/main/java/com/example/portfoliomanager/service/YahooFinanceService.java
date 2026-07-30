package com.example.portfoliomanager.service;

import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ExternalApiException;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.util.Locale;

@Service
public class YahooFinanceService {

    public StockPriceResponse getQuote(String rawSymbol) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        try {
            Stock stock = YahooFinance.get(symbol);
            if (stock == null || stock.getQuote() == null || stock.getQuote().getPrice() == null) {
                throw new ExternalApiException("No Yahoo Finance quote found for " + symbol, null);
            }
            return new StockPriceResponse(
                    symbol,
                    stock.getName(),
                    stock.getQuote().getPrice(),
                    stock.getCurrency());
        } catch (IOException exception) {
            throw new ExternalApiException("Yahoo Finance request failed for " + symbol, exception);
        }
    }
}
