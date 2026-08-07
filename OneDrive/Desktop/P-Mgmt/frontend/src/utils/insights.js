import { formatCurrency } from "./formatters";

const asNumber = (value) => (value == null ? null : Number(value));

const holdingMetrics = (holding) => {
  const qty = asNumber(holding.quantity) || 0;
  const avg = asNumber(holding.averagePurchasePrice) || 0;
  const current = asNumber(holding.currentPrice);
  const invested = qty * avg;
  const currentValue = qty * (current ?? avg);
  const pnl = currentValue - invested;
  const pnlPct = invested === 0 ? null : (pnl / invested) * 100;
  return { invested, currentValue, pnl, pnlPct };
};

export const computePortfolioInsights = ({ holdings = [], transactions = [] }) => {
  if (!holdings.length) {
    return {
      hasData: false,
      cards: [],
      allocation: [],
      timeline: [],
      holdingRows: [],
      summary: "No holdings yet. Add holdings to unlock live insights.",
    };
  }

  const rows = holdings.map((holding) => ({
    ...holding,
    ...holdingMetrics(holding),
  }));

  const totalInvestment = rows.reduce((sum, row) => sum + row.invested, 0);
  const totalValue = rows.reduce((sum, row) => sum + row.currentValue, 0);
  const totalProfit = totalValue - totalInvestment;
  const roi = totalInvestment === 0 ? null : (totalProfit / totalInvestment) * 100;

  const sortedByPnl = [...rows].sort((a, b) => (b.pnlPct ?? -Infinity) - (a.pnlPct ?? -Infinity));
  const sortedByValue = [...rows].sort((a, b) => b.currentValue - a.currentValue);

  const best = sortedByPnl[0];
  const worst = [...sortedByPnl].reverse()[0];
  const largest = sortedByValue[0];

  const allocation = rows.map((row) => ({
    name: row.symbol,
    value: Math.max(row.currentValue, 0),
  }));

  const timeline = [...transactions]
    .sort((a, b) => new Date(a.transactionDate).getTime() - new Date(b.transactionDate).getTime())
    .reduce((acc, tx) => {
      const prev = acc.length ? acc[acc.length - 1].value : 0;
      const signedQty = tx.type === "SELL" ? -Number(tx.quantity) : Number(tx.quantity);
      const delta = signedQty * Number(tx.pricePerShare);
      acc.push({
        date: tx.transactionDate,
        value: prev + delta,
      });
      return acc;
    }, []);

  const cards = [
    { label: "Portfolio Value", value: formatCurrency(totalValue) },
    { label: "Total Investment", value: formatCurrency(totalInvestment) },
    { label: "Total Profit", value: formatCurrency(totalProfit), numeric: totalProfit },
    { label: "ROI", value: roi == null ? "N/A" : `${roi.toFixed(2)}%`, numeric: roi },
    {
      label: "Today's Gain/Loss",
      value: "N/A",
      helper: "TODO: backend does not expose day delta endpoint yet",
    },
    { label: "Best Performer", value: best ? `${best.symbol} (${best.pnlPct?.toFixed(2) ?? "N/A"}%)` : "N/A" },
    { label: "Worst Performer", value: worst ? `${worst.symbol} (${worst.pnlPct?.toFixed(2) ?? "N/A"}%)` : "N/A" },
    { label: "Largest Holding", value: largest ? `${largest.symbol} (${formatCurrency(largest.currentValue)})` : "N/A" },
    {
      label: "Cash Balance",
      value: "N/A",
      helper: "TODO: backend has no cash balance endpoint yet",
    },
  ];

  return {
    hasData: true,
    cards,
    allocation,
    timeline,
    holdingRows: rows,
    summary: `Tracking ${rows.length} holdings with estimated total value ${formatCurrency(totalValue)}.`,
  };
};
