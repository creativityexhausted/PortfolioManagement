import React, { useState, useMemo } from 'react';
import { portfolioApi } from '../../services/portfolioApi';
import { formatCurrency } from '../../utils/formatters';
import { useToast } from '../common/ToastProvider';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip as RechartsTooltip,
} from 'recharts';
import { Sparkles, BarChart2, TrendingUp, AlertTriangle } from 'lucide-react';

const CHART_PALETTE = ["#4be277", "#38bdf8", "#818cf8", "#fbbf24", "#f43f5e", "#a78bfa", "#34d399"];

export function QuantLabView({ portfolioId, holdings }) {
  const { pushToast } = useToast();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [budget, setBudget] = useState(3);
  const [riskFactor, setRiskFactor] = useState(0.5);

  // Extract unique symbols from holdings to use as the universe
  const universe = useMemo(() => {
    const symbols = new Set(holdings.map(h => h.symbol));
    // Add some defaults if the portfolio is empty
    if (symbols.size < 5) {
      ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'TSLA', 'NVDA', 'META'].forEach(s => symbols.add(s));
    }
    return Array.from(symbols);
  }, [holdings]);

  const handleOptimize = async () => {
    if (!portfolioId) {
      pushToast("Please select or create a portfolio first.", "error");
      return;
    }
    setLoading(true);
    setResult(null);
    try {
      const payload = {
        assets: universe,
        budget: Number(budget),
        riskFactor: Number(riskFactor),
      };
      const data = await portfolioApi.optimizePortfolio(portfolioId, payload);
      setResult(data);
      pushToast("Quantum optimization complete!", "success");
    } catch (error) {
      pushToast(error?.response?.data?.error || "Optimization failed.", "error");
    } finally {
      setLoading(false);
    }
  };

  const chartData = useMemo(() => {
    if (!result || !result.optimalSelection) return [];
    const selected = [];
    result.optimalSelection.forEach((isSelected, idx) => {
      if (isSelected === 1) {
        selected.push({ name: result.assets[idx], value: 1 });
      }
    });
    return selected;
  }, [result]);

  return (
    <div className="relative space-y-lg">
      <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute -top-32 -left-24 h-[420px] w-[420px] rounded-full bg-primary/25 blur-[120px]" />
        <div className="absolute top-1/3 -right-24 h-[380px] w-[380px] rounded-full bg-fuchsia-500/20 blur-[130px]" />
      </div>

      <div className="flex justify-between items-center mb-md">
        <div>
          <h2 className="font-headline-md text-headline-md font-bold flex items-center gap-sm">
            <Sparkles className="text-fuchsia-400 h-6 w-6" /> Quant Lab
          </h2>
          <p className="text-body-sm text-on-surface-variant">Quantum Approximate Optimization Algorithm (QAOA) Portfolio Selection</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-gutter">
        {/* Controls */}
        <div className="col-span-1 glass-surface p-md rounded-xl flex flex-col gap-md">
          <h3 className="font-bold text-on-surface text-base border-b border-outline-variant/30 pb-xs">Parameters</h3>
          
          <div>
            <label className="text-xs font-label-caps text-on-surface-variant mb-1 block">Asset Universe</label>
            <div className="text-xs text-on-surface-variant bg-surface-dim p-sm rounded-lg border border-outline-variant/30 break-words">
              {universe.join(', ')}
            </div>
            <p className="text-[10px] mt-1 text-on-surface-variant">({universe.length} assets available for selection)</p>
          </div>

          <div>
            <label className="text-xs font-label-caps text-on-surface-variant mb-1 block">
              Budget (Number of assets to pick)
            </label>
            <input
              type="number"
              min="1"
              max={universe.length}
              value={budget}
              onChange={(e) => setBudget(e.target.value)}
              className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface focus:ring-1 focus:ring-fuchsia-500 outline-none"
            />
          </div>

          <div>
            <label className="text-xs font-label-caps text-on-surface-variant mb-1 block">
              Risk Aversion Factor: {riskFactor}
            </label>
            <input
              type="range"
              min="0.1"
              max="2.0"
              step="0.1"
              value={riskFactor}
              onChange={(e) => setRiskFactor(e.target.value)}
              className="w-full accent-fuchsia-500"
            />
            <div className="flex justify-between text-[10px] text-on-surface-variant px-1 mt-1">
              <span>Risk Seeking</span>
              <span>Risk Averse</span>
            </div>
          </div>

          <button
            onClick={handleOptimize}
            disabled={loading}
            className="mt-auto w-full py-sm rounded-lg font-bold text-sm bg-fuchsia-600 hover:bg-fuchsia-500 text-white transition-colors flex justify-center items-center gap-xs disabled:opacity-50"
          >
            {loading ? (
              <span className="animate-pulse flex items-center gap-xs"><Sparkles className="h-4 w-4" /> Simulating...</span>
            ) : (
              <>Run QAOA Optimizer</>
            )}
          </button>
        </div>

        {/* Results */}
        <div className="col-span-1 lg:col-span-2 glass-surface p-md rounded-xl min-h-[400px] flex flex-col">
          <h3 className="font-bold text-on-surface text-base border-b border-outline-variant/30 pb-xs mb-md">Optimization Results</h3>
          
          {!result && !loading && (
            <div className="flex-1 flex flex-col items-center justify-center text-on-surface-variant opacity-60">
              <BarChart2 className="h-12 w-12 mb-sm" />
              <p>Configure parameters and run the optimizer.</p>
            </div>
          )}

          {loading && (
            <div className="flex-1 flex flex-col items-center justify-center text-fuchsia-400">
              <Sparkles className="h-12 w-12 mb-sm animate-spin-slow" />
              <p className="animate-pulse font-bold">Constructing QUBO and executing QAOA...</p>
            </div>
          )}

          {result && !loading && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-md h-full">
              <div className="flex flex-col items-center justify-center relative min-h-[250px]">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={chartData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={90}
                      paddingAngle={5}
                      stroke="none"
                    >
                      {chartData.map((entry, idx) => (
                        <Cell key={entry.name} fill={CHART_PALETTE[idx % CHART_PALETTE.length]} />
                      ))}
                    </Pie>
                    <RechartsTooltip
                      formatter={(val, name) => ["Included", name]}
                      contentStyle={{
                        backgroundColor: "#1e293b",
                        borderColor: "#334155",
                        borderRadius: "8px",
                        color: "#f8fafc",
                      }}
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
                  <p className="font-label-caps text-[10px] text-on-surface-variant tracking-wider">Selected</p>
                  <p className="font-bold text-xl text-fuchsia-400">{chartData.length}</p>
                </div>
              </div>

              <div className="flex flex-col justify-center gap-md">
                <div className="bg-surface-dim p-md rounded-xl border border-outline-variant/30 border-l-4 border-l-fuchsia-500">
                  <p className="font-label-caps text-xs text-on-surface-variant mb-1 flex items-center gap-1"><TrendingUp className="h-3.5 w-3.5"/> Expected Return (Score)</p>
                  <p className="text-2xl font-bold text-on-surface">{result.expectedReturn.toFixed(4)}</p>
                </div>
                
                <div className="bg-surface-dim p-md rounded-xl border border-outline-variant/30 border-l-4 border-l-amber-500">
                  <p className="font-label-caps text-xs text-on-surface-variant mb-1 flex items-center gap-1"><AlertTriangle className="h-3.5 w-3.5"/> Risk (Variance)</p>
                  <p className="text-2xl font-bold text-on-surface">{result.risk.toFixed(4)}</p>
                </div>

                <div className="text-xs text-on-surface-variant p-sm mt-xs">
                  <p><strong>Energy (fval):</strong> {result.fval}</p>
                  <p className="mt-1">The quantum algorithm translates the portfolio constraints into a Hamiltonian and minimizes its energy state to find the optimal binary selection.</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
