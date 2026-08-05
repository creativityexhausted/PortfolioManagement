package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.AppUser;
import com.example.portfoliomanager.domain.Portfolio;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioRequest;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.AppUserRepository;
import com.example.portfoliomanager.repository.PortfolioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository repository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private PortfolioService service;

    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("demo", "n/a"));

        currentUser = new AppUser();
        currentUser.setUsername("demo");
        when(appUserRepository.findByUsername("demo")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAllCreatesDefaultPortfolioWhenUserHasNone() {
        Portfolio saved = portfolio(1L, "Main Portfolio", "Primary Investment Account");
        saved.setUser(currentUser);

        when(repository.findByUser(currentUser)).thenReturn(List.of());
        when(repository.save(any(Portfolio.class))).thenReturn(saved);

        List<PortfolioResponse> responses = service.findAll();

        ArgumentCaptor<Portfolio> portfolioCaptor = ArgumentCaptor.forClass(Portfolio.class);
        verify(repository).save(portfolioCaptor.capture());
        Portfolio created = portfolioCaptor.getValue();
        assertThat(created.getName()).isEqualTo("Main Portfolio");
        assertThat(created.getDescription()).isEqualTo("Primary Investment Account");
        assertThat(created.getUser()).isSameAs(currentUser);
        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.name()).isEqualTo("Main Portfolio");
        });
    }

    @Test
    void createTrimsNameAndAssignsCurrentUser() {
        PortfolioRequest request = new PortfolioRequest("  Retirement Fund  ", "Long term holdings");
        Portfolio saved = portfolio(5L, "Retirement Fund", "Long term holdings");
        saved.setUser(currentUser);
        when(repository.save(any(Portfolio.class))).thenReturn(saved);

        PortfolioResponse response = service.create(request);

        ArgumentCaptor<Portfolio> portfolioCaptor = ArgumentCaptor.forClass(Portfolio.class);
        verify(repository).save(portfolioCaptor.capture());
        Portfolio persisted = portfolioCaptor.getValue();
        assertThat(persisted.getName()).isEqualTo("Retirement Fund");
        assertThat(persisted.getDescription()).isEqualTo("Long term holdings");
        assertThat(persisted.getUser()).isSameAs(currentUser);
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Retirement Fund");
    }

    @Test
    void updateUsesOwnedPortfolioAndPersistsChanges() {
        Portfolio existing = portfolio(7L, "Old Name", "Old description");
        existing.setUser(currentUser);
        when(repository.findByIdAndUser(7L, currentUser)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        PortfolioResponse response = service.update(7L, new PortfolioRequest("  New Name ", "New description"));

        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDescription()).isEqualTo("New description");
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("New Name");
        verify(repository).save(existing);
    }

    @Test
    void findByIdRejectsPortfolioOutsideCurrentUserScope() {
        when(repository.findByIdAndUser(99L, currentUser)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Portfolio")
                .hasMessageContaining("99");
    }

    private Portfolio portfolio(Long id, String name, String description) {
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setDescription(description);
        try {
            java.lang.reflect.Field idField = Portfolio.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(portfolio, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return portfolio;
    }
}