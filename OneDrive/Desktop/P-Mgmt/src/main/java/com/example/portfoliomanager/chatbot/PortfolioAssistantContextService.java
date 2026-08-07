package com.example.portfoliomanager.chatbot;

import com.example.portfoliomanager.domain.Holding;
import com.example.portfoliomanager.domain.Portfolio;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.HoldingRepository;
import com.example.portfoliomanager.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class PortfolioAssistantContextService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final HoldingRepository holdingRepository;
    private final PortfolioRepository portfolioRepository;
    private final GroqProperties properties;

    public PortfolioAssistantContextService(
            HoldingRepository holdingRepository,
            PortfolioRepository portfolioRepository,
            GroqProperties properties) {
        this.holdingRepository = holdingRepository;
        this.portfolioRepository = portfolioRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PortfolioAssistantContext build(Long portfolioId) {
        String scopeLabel;
        List<Holding> holdings;

        if (portfolioId == null) {
            scopeLabel = "all portfolios";
            holdings = holdingRepository.findAll();
        } else {
            Portfolio portfolio = portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
            scopeLabel = "portfolio '" + portfolio.getName() + "'";
            holdings = holdingRepository.findByPortfolioId(portfolioId);
        }

        if (holdings.isEmpty()) {
            String empty = "No holdings are available for " + scopeLabel + ".";
            return new PortfolioAssistantContext(empty, empty, scopeLabel);
        }

        List<HoldingSnapshot> snapshots = holdings.stream().map(this::snapshot).toList();

        BigDecimal totalInvested = snapshots.stream()
                .map(HoldingSnapshot::investedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCurrent = snapshots.stream()
                .map(HoldingSnapshot::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPnl = totalCurrent.subtract(totalInvested);
        BigDecimal totalPnlPct = percentage(totalPnl, totalInvested);

        List<HoldingSnapshot> withPrices = snapshots.stream()
                .filter(s -> s.currentPrice() != null)
                .toList();

        List<HoldingSnapshot> topGainers = withPrices.stream()
                .filter(s -> s.pnlPercent() != null)
                .sorted(Comparator.comparing(HoldingSnapshot::pnlPercent).reversed())
                .limit(3)
                .toList();

        List<HoldingSnapshot> topFallers = withPrices.stream()
                .filter(s -> s.pnlPercent() != null)
                .sorted(Comparator.comparing(HoldingSnapshot::pnlPercent))
                .limit(3)
                .toList();

        int maxRows = Math.max(1, properties.getMaxHoldingsInContext());
        List<HoldingSnapshot> rowsForModel = snapshots.stream()
                .sorted(Comparator.comparing(HoldingSnapshot::currentValue).reversed())
                .limit(maxRows)
                .toList();

        String llmContext = buildModelContext(scopeLabel, snapshots.size(), totalInvested, totalCurrent, totalPnl,
                totalPnlPct, topGainers, topFallers, rowsForModel, snapshots.size() - rowsForModel.size());
        String highlights = buildHighlights(scopeLabel, totalPnl, totalPnlPct, topGainers, topFallers);

        return new PortfolioAssistantContext(llmContext, highlights, scopeLabel);
    }

    private String buildModelContext(
            String scopeLabel,
            int totalHoldings,
            BigDecimal totalInvested,
            BigDecimal totalCurrent,
            BigDecimal totalPnl,
            BigDecimal totalPnlPct,
            List<HoldingSnapshot> topGainers,
            List<HoldingSnapshot> topFallers,
            List<HoldingSnapshot> rowsForModel,
            int omittedCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Portfolio scope: ").append(scopeLabel).append("\n");
        sb.append("Total holdings: ").append(totalHoldings).append("\n");
        sb.append("Total invested value: ").append(money(totalInvested)).append("\n");
        sb.append("Total current value: ").append(money(totalCurrent)).append("\n");
        sb.append("Total gain/loss: ").append(money(totalPnl)).append(" (")
                .append(percent(totalPnlPct)).append(")\n\n");

        sb.append("Top gainers by return (%):\n");
        appendMovers(sb, topGainers);

        sb.append("Top fallers by return (%):\n");
        appendMovers(sb, topFallers);

        sb.append("\nHoldings detail rows:\n");
        for (HoldingSnapshot row : rowsForModel) {
            sb.append("- Symbol: ").append(row.symbol())
                    .append(", Company: ").append(row.companyName())
                    .append(", Portfolio: ").append(row.portfolioName())
                    .append(", Qty: ").append(row.quantity())
                    .append(", Avg Buy: ").append(money(row.averagePurchasePrice()))
                    .append(", Current: ").append(row.currentPrice() == null ? "N/A" : money(row.currentPrice()))
                    .append(", Invested: ").append(money(row.investedValue()))
                    .append(", Current Value: ").append(money(row.currentValue()))
                    .append(", Gain/Loss: ").append(money(row.pnlAbsolute()))
                    .append(" (").append(percent(row.pnlPercent())).append(")")
                    .append("\n");
        }

        if (omittedCount > 0) {
            sb.append("Rows omitted from details due to context size: ").append(omittedCount).append("\n");
        }

        return sb.toString();
    }

    private String buildHighlights(
            String scopeLabel,
            BigDecimal totalPnl,
            BigDecimal totalPnlPct,
            List<HoldingSnapshot> topGainers,
            List<HoldingSnapshot> topFallers) {
        StringBuilder sb = new StringBuilder();
        sb.append("This answer covers ").append(scopeLabel).append(". ");
        sb.append("Overall performance is ").append(money(totalPnl)).append(" (").append(percent(totalPnlPct)).append("). ");

        if (!topGainers.isEmpty()) {
            HoldingSnapshot best = topGainers.get(0);
            sb.append("Top performer: ").append(best.symbol())
                    .append(" at ").append(percent(best.pnlPercent())).append(". ");
        }

        if (!topFallers.isEmpty()) {
            HoldingSnapshot worst = topFallers.get(0);
            sb.append("Largest decline: ").append(worst.symbol())
                    .append(" at ").append(percent(worst.pnlPercent())).append(".");
        }

        return sb.toString().trim();
    }

    private void appendMovers(StringBuilder sb, List<HoldingSnapshot> movers) {
        if (movers.isEmpty()) {
            sb.append("- No priced holdings available\n");
            return;
        }
        for (HoldingSnapshot mover : movers) {
            sb.append("- ").append(mover.symbol())
                    .append(" (portfolio ").append(mover.portfolioName()).append(")")
                    .append(": ").append(percent(mover.pnlPercent()))
                    .append(" | ").append(money(mover.pnlAbsolute()))
                    .append("\n");
        }
    }

    private HoldingSnapshot snapshot(Holding holding) {
        BigDecimal invested = holding.getAveragePurchasePrice().multiply(holding.getQuantity());
        BigDecimal currentPrice = holding.getCurrentPrice();
        BigDecimal currentValue = currentPrice == null ? invested : currentPrice.multiply(holding.getQuantity());
        BigDecimal pnl = currentValue.subtract(invested);
        BigDecimal pnlPct = percentage(pnl, invested);

        return new HoldingSnapshot(
                holding.getSymbol(),
                holding.getCompanyName() == null || holding.getCompanyName().isBlank()
                        ? "Unknown"
                        : holding.getCompanyName(),
                holding.getPortfolio().getName(),
                holding.getQuantity(),
                holding.getAveragePurchasePrice(),
                currentPrice,
                invested,
                currentValue,
                pnl,
                pnlPct);
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return value == null ? "N/A" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String percent(BigDecimal value) {
        return value == null ? "N/A" : value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public record PortfolioAssistantContext(String llmContext, String highlights, String scopeLabel) {
    }

    private record HoldingSnapshot(
            String symbol,
            String companyName,
            String portfolioName,
            BigDecimal quantity,
            BigDecimal averagePurchasePrice,
            BigDecimal currentPrice,
            BigDecimal investedValue,
            BigDecimal currentValue,
            BigDecimal pnlAbsolute,
            BigDecimal pnlPercent) {
    }
}
