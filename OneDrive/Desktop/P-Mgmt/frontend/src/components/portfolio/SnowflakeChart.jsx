import {
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  ResponsiveContainer,
  Tooltip as RechartsTooltip,
} from "recharts";

// Overall score (0-6 average) -> fill color, mirroring the green/yellow/red convention
// used by Simply Wall St's Snowflake chart.
function colorForScore(score) {
  if (score >= 3.8) return "#4be277"; // strong / green
  if (score >= 2.5) return "#facc15"; // average / yellow
  return "#f87171"; // weak / red
}

/**
 * Renders the 5-axis (Value / Future / Past / Health / Dividend) Snowflake radar chart
 * for a single symbol. Pure presentational component - takes an already-computed
 * SnowflakeScore object (see fundamentals-service's SnowflakeDtos.SnowflakeScore) as a prop.
 */
export function SnowflakeChart({ snowflake }) {
  if (!snowflake) return null;

  const fill = colorForScore(snowflake.overallScore);

  const data = [
    { axis: "Value", score: snowflake.value?.score ?? 0, fullMark: 6 },
    { axis: "Future", score: snowflake.future?.score ?? 0, fullMark: 6 },
    { axis: "Past", score: snowflake.past?.score ?? 0, fullMark: 6 },
    { axis: "Health", score: snowflake.health?.score ?? 0, fullMark: 6 },
    { axis: "Dividend", score: snowflake.dividend?.score ?? 0, fullMark: 6 },
  ];

  return (
    <div className="w-full h-80">
      <ResponsiveContainer width="100%" height="100%">
        <RadarChart data={data} outerRadius="75%">
          <PolarGrid stroke="rgba(255,255,255,0.15)" />
          <PolarAngleAxis dataKey="axis" tick={{ fontSize: 12, fill: "currentColor" }} />
          <PolarRadiusAxis angle={90} domain={[0, 6]} tick={{ fontSize: 9 }} tickCount={4} />
          <Radar
            name="Score"
            dataKey="score"
            stroke={fill}
            fill={fill}
            fillOpacity={0.45}
            strokeWidth={2}
          />
          <RechartsTooltip
            formatter={(value) => [`${value} / 6`, "Score"]}
          />
        </RadarChart>
      </ResponsiveContainer>
    </div>
  );
}
