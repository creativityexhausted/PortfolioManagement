package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.HoldingRequest;
import com.example.portfoliomanager.dto.ApiDtos.HoldingResponse;
import com.example.portfoliomanager.service.HoldingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoldingControllerTest {

    @Mock
    private HoldingService service;

    @InjectMocks
    private HoldingController controller;

    @Test
    void findAllPassesOptionalPortfolioId() {
        List<HoldingResponse> expected = List.of();
        when(service.findAll(3L)).thenReturn(expected);

        List<HoldingResponse> actual = controller.findAll(3L);

        assertThat(actual).isSameAs(expected);
        verify(service).findAll(3L);
    }

    @Test
    void createDelegatesToService() {
        HoldingRequest request = new HoldingRequest(
                "AAPL",
                "Apple Inc.",
                new BigDecimal("10"),
                new BigDecimal("190.00"),
                LocalDate.of(2026, 1, 1),
                1L);
        HoldingResponse expected = new HoldingResponse(
                11L,
                "AAPL",
                "Apple Inc.",
                new BigDecimal("10"),
                new BigDecimal("190.00"),
                new BigDecimal("200.00"),
                LocalDateTime.now(),
                LocalDate.of(2026, 1, 1),
                1L,
                "FINNHUB",
                List.of("ok"));
        when(service.create(request)).thenReturn(expected);

        HoldingResponse actual = controller.create(request);

        assertThat(actual).isEqualTo(expected);
        verify(service).create(request);
    }
}
