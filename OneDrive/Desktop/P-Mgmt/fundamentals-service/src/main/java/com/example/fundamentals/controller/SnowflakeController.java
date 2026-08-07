package com.example.fundamentals.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fundamentals.dto.SnowflakeDtos.SnowflakeScore;
import com.example.fundamentals.service.SnowflakeService;

@RestController
@RequestMapping("/api/snowflake")
public class SnowflakeController {

    private final SnowflakeService snowflakeService;

    public SnowflakeController(SnowflakeService snowflakeService) {
        this.snowflakeService = snowflakeService;
    }

    /**
     * Returns the 5-axis Snowflake fundamentals score for a symbol, computed from Finnhub
     * fundamental data and cached for 24h. This is the only endpoint this microservice
     * exposes - deliberately small and focused (single responsibility).
     */
    @GetMapping("/{symbol}")
    public SnowflakeScore getSnowflake(@PathVariable String symbol) {
        return snowflakeService.getScore(symbol);
    }
}
