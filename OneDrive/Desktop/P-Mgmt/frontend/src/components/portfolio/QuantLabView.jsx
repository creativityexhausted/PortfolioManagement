import React, { useState, useMemo } from 'react';
import { portfolioApi } from '../../services/portfolioApi';
import { formatCurrency } from '../../utils/formatters';
import { useToast } from '../common/ToastProvider';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as RechartsTooltip,
} from 'recharts';
import { Sparkles, BarChart2, TrendingUp, AlertTriangle, Info } from 'lucide-react';

const CHART_PALETTE = ["#4be277", "#38bdf8", "#818cf8", "#fbbf24", "#f43f5e", "#a78bfa", "#34d399"];

// A custom Tooltip component using Tailwind group-hover
const InfoTooltip = ({ text }) => (
  <div className="relative group flex items-center justify-center ml-1">
    <Info className="h-4 w-4 text-on-surface-variant hover:text-primary transition-colors cursor-help" />
    <div className="absolute bottom-full mb-2 hidden group-hover:block w-56 p-sm bg-surface border border-outline-variant/60 rounded-lg shadow-xl text-xs text-on-surface-variant z-50 normal-case font-normal">
      {text}
      <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 border-t-4 border-t-outline-variant/60 border-l-4 border-l-transparent border-r-4 border-r-transparent"></div>
    </div>
  </div>
);

export function QuantLabView({ portfolioId, holdings }) {
  const { pushToast } = useToast();
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [budget, setBudget] = useState(3);
  const [riskFactor, setRiskFactor] = useState(0.5);

  // Extract unique symbols from holdings to use as the universe. Strictly use real holdings.
  const universe = useMemo(() => {
    const symbols = new Set(holdings.map(h => h.symbol));
    return Array.from(symbols);
  }, [holdings]);

  const handleOptimize = async () => {
    if (!portfolioId) {
      pushToast("Please select or create a portfolio first.", "error");
      return;
    }
    if (universe.length === 0) {
      pushToast("Your portfolio is empty. Add some holdings first.", "error");
      return;
    }
    if (budget > universe.length) {
      pushToast("Budget cannot exceed the number of assets in your portfolio.", "error");
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

  // Generate 5-year projection data based on Expected Return and Risk
  const projectionData = useMemo(() => {
    if (!result) return [];
    const data = [];
    let currentCapital = 10000; // Starting with $10k
    const returnRate = result.expectedReturn; 
    const volatility = result.risk;

    for (let year = 0; year <= 5; year++) {
      if (year === 0) {
        data.push({ year: `Year ${year}`, expected: currentCapital, optimistic: currentCapital, pessimistic: currentCapital });
      } else {
        const expected = currentCapital * Math.pow(1 + returnRate, year);
        // Simple visual scaling for the risk band
        const riskBand = expected * (volatility * year * 0.5); 
        data.push({
          year: `Year ${year}`,
          expected: Number(expected.toFixed(0)),
          optimistic: Number((expected + riskBand).toFixed(0)),
          pessimistic: Number(Math.max(currentCapital * 0.5, expected - riskBand).toFixed(0)), // Floor at 50% loss for realism
        });
      }
    }
    return data;
  }, [result]);

  return (
    <div className="relative space-y-lg pb-xl">
      {/* AMBIENT GLOW BACKDROP (Matched to Dashboard) */}
      <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute -top-32 -left-24 h-[420px] w-[420px] rounded-full bg-primary/25 blur-[120px]" />
        <div className="absolute top-1/3 -right-24 h-[380px] w-[380px] rounded-full bg-sky-500/20 blur-[130px]" />
      </div>

      <div className="flex justify-between items-center mb-md">
        <div>
          <h2 className="font-headline-md text-headline-md font-bold flex items-center gap-sm">
            <Sparkles className="text-primary h-6 w-6" /> Quant Lab
          </h2>
          <p className="text-body-sm text-on-surface-variant">Quantum Approximate Optimization Algorithm (QAOA)</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-gutter">
        {/* Controls - Left Side */}
        <div className="col-span-1 lg:col-span-4 glass-surface p-md rounded-xl flex flex-col gap-lg border border-outline-variant/30">
          <div className="flex items-center justify-between border-b border-outline-variant/30 pb-xs">
            <h3 className="font-headline-md text-headline-md font-bold">Model Parameters</h3>
          </div>
          
          <div className="space-y-sm">
            <div className="flex items-center">
              <label className="text-xs font-label-caps text-on-surface-variant">Asset Universe</label>
              <InfoTooltip text="The pool of assets the algorithm is allowed to select from. Drawn directly from your current holdings." />
            </div>
            <div className="text-xs text-on-surface-variant bg-surface-dim p-sm rounded-lg border border-outline-variant/30 break-words opacity-80">
              {universe.length === 0 ? "No assets in portfolio" : 
               universe.length <= 10 ? universe.join(', ') : `${universe.slice(0, 10).join(', ')}... (+${universe.length - 10} more)`}
            </div>
          </div>

          <div className="space-y-sm">
             <div className="flex items-center">
              <label className="text-xs font-label-caps text-on-surface-variant">Budget constraint</label>
              <InfoTooltip text="The exact number of assets the algorithm is allowed to include in the optimal portfolio." />
            </div>
            <div className="flex items-center gap-md">
              <input
                type="range"
                min="1"
                max={Math.max(1, universe.length)}
                value={Math.min(budget, Math.max(1, universe.length))}
                onChange={(e) => setBudget(e.target.value)}
                disabled={universe.length === 0}
                className="flex-1 accent-primary"
              />
              <span className="font-bold text-primary text-lg w-6 text-right">
                {universe.length === 0 ? 0 : Math.min(budget, Math.max(1, universe.length))}
              </span>
            </div>
          </div>

          <div className="space-y-sm">
            <div className="flex items-center">
              <label className="text-xs font-label-caps text-on-surface-variant">
                Risk Aversion Factor
              </label>
              <InfoTooltip text="Controls how safe you want to play it. Higher numbers force the algorithm to pick safer, more stable stocks. Lower numbers allow it to chase higher returns despite wild price swings." />
            </div>
            <input
              type="range"
              min="0.1"
              max="2.0"
              step="0.1"
              value={riskFactor}
              onChange={(e) => setRiskFactor(e.target.value)}
              className="w-full accent-primary"
            />
            <div className="flex justify-between text-[10px] text-on-surface-variant px-1">
              <span>Risk Seeking (0.1)</span>
              <span className="font-bold text-on-surface">{riskFactor}</span>
              <span>Risk Averse (2.0)</span>
            </div>
          </div>

          <button
            onClick={handleOptimize}
            disabled={loading || universe.length === 0}
            className="mt-auto w-full py-md rounded-xl font-bold text-sm bg-primary text-on-primary hover:brightness-110 shadow-lg shadow-primary/20 transition-all flex justify-center items-center gap-sm disabled:opacity-50"
          >
            {loading ? (
              <span className="animate-pulse flex items-center gap-xs"><Sparkles className="h-4 w-4 animate-spin-slow" /> Executing QAOA...</span>
            ) : (
              <>Run Quantum Optimization</>
            )}
          </button>
        </div>

        {/* Results - Right Side */}
        <div className="col-span-1 lg:col-span-8 flex flex-col gap-gutter">
          
          {/* Top Panel: Projection Chart */}
          <div className="glass-surface p-md rounded-xl border border-outline-variant/30 min-h-[300px] flex flex-col">
            <div className="flex items-center justify-between mb-md">
              <h3 className="font-headline-md text-headline-md font-bold">5-Year Growth Projection</h3>
              {result && (
                <div className="flex gap-sm">
                  <div className="flex items-center gap-xs text-[10px] text-on-surface-variant">
                    <div className="w-2 h-2 rounded-full bg-primary/80"></div> Expected
                  </div>
                  <div className="flex items-center gap-xs text-[10px] text-on-surface-variant">
                    <div className="w-2 h-2 rounded-full bg-surface-bright border border-primary/30"></div> Risk Band
                  </div>
                </div>
              )}
            </div>

            {!result && !loading && (
              <div className="flex-1 flex flex-col items-center justify-center text-on-surface-variant opacity-50">
                <BarChart2 className="h-16 w-16 mb-sm" />
                <p className="text-sm">Run the optimizer to generate a 5-year projection.</p>
              </div>
            )}

            {loading && (
              <div className="flex-1 flex flex-col items-center justify-center text-primary">
                <Sparkles className="h-12 w-12 mb-sm animate-spin-slow" />
                <p className="animate-pulse font-bold">Solving QUBO constraints...</p>
              </div>
            )}

            {result && !loading && (
              <div className="flex-1 w-full relative pt-2">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={projectionData} margin={{ top: 10, right: 10, left: -15, bottom: 0 }}>
                    <defs>
                      <linearGradient id="colorExpected" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#4be277" stopOpacity={0.4}/>
                        <stop offset="95%" stopColor="#4be277" stopOpacity={0}/>
                      </linearGradient>
                      <linearGradient id="colorRisk" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#4be277" stopOpacity={0.1}/>
                        <stop offset="95%" stopColor="#4be277" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="#334155" vertical={false} />
                    <XAxis dataKey="year" stroke="#94a3b8" fontSize={11} tickLine={false} axisLine={false} />
                    <YAxis 
                      stroke="#94a3b8" 
                      fontSize={11} 
                      tickLine={false} 
                      axisLine={false}
                      tickFormatter={(val) => `$${val >= 1000 ? (val / 1000).toFixed(0) + "k" : val}`}
                    />
                    <RechartsTooltip 
                      formatter={(value, name) => [formatCurrency(value, "USD"), name.charAt(0).toUpperCase() + name.slice(1)]}
                      contentStyle={{ backgroundColor: "#1e293b", borderColor: "#334155", borderRadius: "8px", color: "#f8fafc", fontSize: "12px" }}
                    />
                    {/* Risk Band Area (Optimistic to Pessimistic) */}
                    <Area type="monotone" dataKey="optimistic" stroke="none" fill="url(#colorRisk)" fillOpacity={1} />
                    <Area type="monotone" dataKey="pessimistic" stroke="none" fill="#0f172a" fillOpacity={1} /> 
                    {/* Main Expected Line */}
                    <Area type="monotone" dataKey="expected" stroke="#4be277" strokeWidth={3} fill="url(#colorExpected)" fillOpacity={1} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>

          {/* Bottom Panel: Selection & Metrics */}
          {result && !loading && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter">
              
              {/* Selected Assets */}
              <div className="glass-surface p-md rounded-xl border border-outline-variant/30 flex flex-col items-center justify-center relative min-h-[220px]">
                <h3 className="absolute top-4 left-4 font-headline-md text-headline-md font-bold">Optimal Selection</h3>
                <div className="w-full h-44 relative flex items-center justify-center mt-6">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={chartData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={55}
                        outerRadius={78}
                        paddingAngle={5}
                        stroke="none"
                      >
                        {chartData.map((entry, idx) => (
                          <Cell key={entry.name} fill={CHART_PALETTE[idx % CHART_PALETTE.length]} />
                        ))}
                      </Pie>
                      <RechartsTooltip
                        formatter={(val, name) => ["Included", name]}
                        contentStyle={{ backgroundColor: "#1e293b", borderColor: "#334155", borderRadius: "8px", color: "#f8fafc", fontSize: "12px" }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                  <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
                    <p className="font-bold text-2xl text-on-surface">{chartData.length}</p>
                    <p className="font-label-caps text-[10px] text-on-surface-variant tracking-wider uppercase">Assets</p>
                  </div>
                </div>
              </div>

              {/* Metrics */}
              <div className="glass-surface p-md rounded-xl border border-outline-variant/30 flex flex-col justify-center gap-sm">
                
                <div className="bg-surface-dim p-md rounded-xl border border-outline-variant/30 group hover:border-primary/50 transition-colors">
                  <div className="flex items-center mb-1">
                    <p className="font-label-caps text-[10px] text-on-surface-variant flex items-center gap-1 uppercase tracking-wider">
                      <TrendingUp className="h-3 w-3"/> Expected Return
                    </p>
                    <InfoTooltip text="The estimated overall profit percentage these stocks typically generate." />
                  </div>
                  <p className="text-2xl font-bold text-primary">{(result.expectedReturn * 100).toFixed(2)}%</p>
                </div>
                
                <div className="bg-surface-dim p-md rounded-xl border border-outline-variant/30 group hover:border-amber-500/50 transition-colors">
                  <div className="flex items-center mb-1">
                    <p className="font-label-caps text-[10px] text-on-surface-variant flex items-center gap-1 uppercase tracking-wider">
                      <AlertTriangle className="h-3 w-3"/> Portfolio Variance (Risk)
                    </p>
                    <InfoTooltip text="The 'bounciness' of your selected stocks. A high percentage means these stocks frequently experience huge price jumps and drops." />
                  </div>
                  <p className="text-2xl font-bold text-amber-500">{(result.risk * 100).toFixed(2)}%</p>
                </div>

                <div className="bg-surface-dim p-md rounded-xl border border-outline-variant/30 group hover:border-sky-500/50 transition-colors">
                  <div className="flex items-center mb-1">
                    <p className="font-label-caps text-[10px] text-on-surface-variant flex items-center gap-1 uppercase tracking-wider">
                      <Sparkles className="h-3 w-3"/> Minimum Energy (fval)
                    </p>
                    <InfoTooltip text="The algorithm's internal score. It searches for the lowest possible score (energy) to find the absolute best combination of stocks." />
                  </div>
                  <p className="text-lg font-bold text-on-surface">{result.fval.toFixed(4)}</p>
                </div>

              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
