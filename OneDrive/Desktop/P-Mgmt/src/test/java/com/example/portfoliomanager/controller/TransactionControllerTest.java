package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.domain.TransactionType;
import com.example.portfoliomanager.dto.ApiDtos.TransactionRequest;
import com.example.portfoliomanager.dto.ApiDtos.TransactionResponse;
import com.example.portfoliomanager.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService service;

    @InjectMocks
    private TransactionController controller;

    @Test
    void findAllPassesOptionalPortfolioId() {
        List<TransactionResponse> expected = List.of();
        when(service.findAll(5L)).thenReturn(expected);

        List<TransactionResponse> actual = controller.findAll(5L);

        assertThat(actual).isSameAs(expected);
        verify(service).findAll(5L);
    }

    @Test
    void createDelegatesToService() {
        LocalDateTime now = LocalDateTime.now();
        TransactionRequest request = new TransactionRequest(
                TransactionType.BUY,
                "MSFT",
                new BigDecimal("4"),
                new BigDecimal("300.50"),
                now,
                "initial buy",
                1L);
        TransactionResponse expected = new TransactionResponse(
                21L,
                TransactionType.BUY,
                "MSFT",
                new BigDecimal("4"),
                new BigDecimal("300.50"),
                now,
                "initial buy",
                1L);
        when(service.create(request)).thenReturn(expected);

        TransactionResponse actual = controller.create(request);

        assertThat(actual).isEqualTo(expected);
        verify(service).create(request);
    }
}
