package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.PortfolioRequest;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioResponse;
import com.example.portfoliomanager.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    @Mock
    private PortfolioService service;

    @InjectMocks
    private PortfolioController controller;

    @Test
    void findAllReturnsServiceResult() {
        List<PortfolioResponse> expected = List.of(new PortfolioResponse(
                1L,
                "Main Portfolio",
                "Primary",
                LocalDateTime.now(),
                LocalDateTime.now()));
        when(service.findAll()).thenReturn(expected);

        List<PortfolioResponse> actual = controller.findAll();

        assertThat(actual).isEqualTo(expected);
        verify(service).findAll();
    }

    @Test
    void createDelegatesToService() {
        PortfolioRequest request = new PortfolioRequest("Retirement", "Long term");
        PortfolioResponse expected = new PortfolioResponse(
                2L,
                "Retirement",
                "Long term",
                LocalDateTime.now(),
                LocalDateTime.now());
        when(service.create(request)).thenReturn(expected);

        PortfolioResponse actual = controller.create(request);

        assertThat(actual).isEqualTo(expected);
        verify(service).create(request);
    }

    @Test
    void deleteDelegatesToService() {
        controller.delete(9L);

        verify(service).delete(9L);
    }
}
