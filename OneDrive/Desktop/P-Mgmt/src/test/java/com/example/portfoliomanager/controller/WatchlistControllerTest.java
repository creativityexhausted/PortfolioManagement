package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.WatchlistRequest;
import com.example.portfoliomanager.dto.ApiDtos.WatchlistResponse;
import com.example.portfoliomanager.service.WatchlistService;
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
class WatchlistControllerTest {

    @Mock
    private WatchlistService service;

    @InjectMocks
    private WatchlistController controller;

    @Test
    void findAllPassesOptionalPortfolioId() {
        List<WatchlistResponse> expected = List.of();
        when(service.findAll(4L)).thenReturn(expected);

        List<WatchlistResponse> actual = controller.findAll(4L);

        assertThat(actual).isSameAs(expected);
        verify(service).findAll(4L);
    }

    @Test
    void createDelegatesToService() {
        WatchlistRequest request = new WatchlistRequest(
                "NVDA",
                "NVIDIA",
                new BigDecimal("1200"),
                2L);
        WatchlistResponse expected = new WatchlistResponse(
                31L,
                "NVDA",
                "NVIDIA",
                new BigDecimal("1200"),
                new BigDecimal("1150"),
                LocalDateTime.now(),
                LocalDateTime.now(),
                2L);
        when(service.create(request)).thenReturn(expected);

        WatchlistResponse actual = controller.create(request);

        assertThat(actual).isEqualTo(expected);
        verify(service).create(request);
    }
}
