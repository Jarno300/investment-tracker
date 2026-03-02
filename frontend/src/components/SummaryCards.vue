<template>
  <section class="summary">
    <div class="card summary-main-card">
      <div class="summary-metrics">
        <div>
          <p class="label">Total Holdings Value</p>
          <p class="value">{{ holdingsValueDisplay }}</p>
        </div>
        <div>
          <p class="label">Profit / Loss</p>
          <p class="value" :class="profitLossClass">{{ profitLossDisplay }}</p>
        </div>
      </div>
      <div class="history-wrap">
        <canvas
          ref="historyChartRef"
          width="560"
          height="200"
          v-show="historySeries.labels.length"
        ></canvas>
        <p v-show="!historySeries.labels.length" class="muted">
          Not enough transaction history yet
        </p>
      </div>
    </div>
    <div class="card chart-card">
      <p class="label">Assets by type</p>
      <div class="chart-wrap">
        <canvas
          ref="assetsChartRef"
          width="280"
          height="180"
          v-show="assetTypeBreakdown.length"
        ></canvas>
        <p v-show="!assetTypeBreakdown.length" class="muted">No assets yet</p>
      </div>
    </div>
    <div class="card chart-card">
      <p class="label">Portfolio allocation</p>
      <div class="chart-wrap">
        <canvas
          ref="holdingsChartRef"
          width="280"
          height="180"
          v-show="holdingsChartData.length"
        ></canvas>
        <p v-show="!holdingsChartData.length" class="muted">No holdings yet</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue";
import Chart from "chart.js/auto";

const props = defineProps({
  summary: {
    type: Object,
    required: true,
  },
  totalValue: {
    type: String,
    required: true,
  },
  assetTypeBreakdown: {
    type: Array,
    default: () => [],
  },
  holdings: {
    type: Array,
    default: () => [],
  },
  transactions: {
    type: Array,
    default: () => [],
  },
  formatType: {
    type: Function,
    default: (t) => t,
  },
  formatCurrency: {
    type: Function,
    required: true,
  },
});

const historyChartRef = ref(null);
const assetsChartRef = ref(null);
const holdingsChartRef = ref(null);
let historyChart = null;
let assetsChart = null;
let holdingsChart = null;

const holdingsValueDisplay = computed(() =>
  props.formatCurrency(
    props.summary.portfolioValue ?? props.summary.totalValue ?? 0,
  ),
);

const profitLossDisplay = computed(() =>
  props.formatCurrency(props.summary.profitLoss ?? 0),
);

const profitLossClass = computed(() => {
  const n = Number(props.summary.profitLoss ?? 0);
  if (!Number.isFinite(n) || n === 0) return "";
  return n > 0 ? "positive" : "negative";
});

const historySeries = computed(() => {
  const txs = [...props.transactions]
    .filter((t) => t?.tradedAt)
    .sort((a, b) => new Date(a.tradedAt) - new Date(b.tradedAt));
  if (!txs.length) {
    return { labels: [], holdingsValues: [], profitLossValues: [] };
  }
  const quantityBySymbol = new Map();
  const priceBySymbol = new Map();
  const labels = [];
  const holdingsValues = [];
  const profitLossValues = [];
  let netInvested = 0;

  for (const tx of txs) {
    const symbol = tx.asset?.symbol || tx.asset?.name || "UNKNOWN";
    const qty = Number(tx.quantity || 0);
    const price = Number(tx.price || 0);
    const costs = Number(tx.costs || 0);
    const prevQty = quantityBySymbol.get(symbol) || 0;

    if (tx.type === "BUY") {
      quantityBySymbol.set(symbol, prevQty + qty);
      netInvested += qty * price + costs;
    } else if (tx.type === "SELL") {
      quantityBySymbol.set(symbol, Math.max(0, prevQty - qty));
      netInvested -= qty * price - costs;
    }

    if (price > 0) {
      priceBySymbol.set(symbol, price);
    }

    let holdingsValue = 0;
    for (const [s, heldQty] of quantityBySymbol.entries()) {
      const lastPrice = priceBySymbol.get(s) || 0;
      holdingsValue += heldQty * lastPrice;
    }

    labels.push(
      new Date(tx.tradedAt).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
      }),
    );
    holdingsValues.push(holdingsValue);
    profitLossValues.push(holdingsValue - netInvested);
  }

  return { labels, holdingsValues, profitLossValues };
});

const holdingsChartData = computed(() => {
  const total =
    Number(props.summary.portfolioValue ?? props.summary.totalValue) || 0;
  if (total <= 0) return [];
  return props.holdings
    .filter((h) => Number(h.currentValue ?? h.marketValue) > 0)
    .map((h) => ({
      label: h.asset?.name || "Unknown",
      value: Number(h.currentValue ?? h.marketValue),
    }))
    .sort((a, b) => b.value - a.value);
});

const CHART_COLORS = [
  "#6366f1",
  "#8b5cf6",
  "#a855f7",
  "#d946ef",
  "#ec4899",
  "#f43f5e",
  "#f97316",
  "#eab308",
  "#22c55e",
  "#14b8a6",
];

function buildHistoryChart() {
  if (!historyChartRef.value || !historySeries.value.labels.length) return;
  if (historyChart) historyChart.destroy();
  historyChart = new Chart(historyChartRef.value, {
    type: "line",
    data: {
      labels: historySeries.value.labels,
      datasets: [
        {
          label: "Holdings value",
          data: historySeries.value.holdingsValues,
          borderColor: "#6366f1",
          backgroundColor: "rgba(99,102,241,0.18)",
          tension: 0.28,
          pointRadius: 2,
          fill: false,
        },
        {
          label: "Profit / Loss",
          data: historySeries.value.profitLossValues,
          borderColor: "#22c55e",
          backgroundColor: "rgba(34,197,94,0.15)",
          tension: 0.28,
          pointRadius: 2,
          fill: false,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: "bottom",
          labels: { color: "#eef2f6", padding: 12 },
        },
      },
      scales: {
        x: {
          ticks: { color: "#8a93a6" },
          grid: { color: "rgba(138,147,166,0.15)" },
        },
        y: {
          ticks: { color: "#8a93a6" },
          grid: { color: "rgba(138,147,166,0.15)" },
        },
      },
    },
  });
}

function buildAssetsChart() {
  if (!assetsChartRef.value || !props.assetTypeBreakdown.length) return;
  if (assetsChart) assetsChart.destroy();
  assetsChart = new Chart(assetsChartRef.value, {
    type: "pie",
    data: {
      labels: props.assetTypeBreakdown.map((d) => props.formatType(d.type)),
      datasets: [
        {
          data: props.assetTypeBreakdown.map((d) => d.count),
          backgroundColor: CHART_COLORS.slice(
            0,
            props.assetTypeBreakdown.length,
          ),
          borderColor: "#151a22",
          borderWidth: 2,
          radius: "75%",
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: "bottom",
          labels: { color: "#eef2f6", padding: 12 },
        },
      },
    },
  });
}

function buildHoldingsChart() {
  if (!holdingsChartRef.value || !holdingsChartData.value.length) return;
  if (holdingsChart) holdingsChart.destroy();
  holdingsChart = new Chart(holdingsChartRef.value, {
    type: "bar",
    data: {
      labels: holdingsChartData.value.map((d) => d.label),
      datasets: [
        {
          data: holdingsChartData.value.map((d) => d.value),
          backgroundColor: CHART_COLORS.slice(
            0,
            holdingsChartData.value.length,
          ),
          borderRadius: 6,
          borderSkipped: false,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      indexAxis: "y",
      plugins: {
        legend: {
          display: false,
        },
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const pct = (
                (ctx.raw /
                  (Number(
                    props.summary.portfolioValue ?? props.summary.totalValue,
                  ) || 1)) *
                100
              ).toFixed(1);
              return `${props.formatCurrency(ctx.raw)} (${pct}%)`;
            },
          },
        },
      },
      scales: {
        x: {
          ticks: { color: "#8a93a6" },
          grid: { color: "rgba(138,147,166,0.15)" },
        },
        y: {
          ticks: { color: "#8a93a6" },
          grid: { display: false },
        },
      },
    },
  });
}

function refreshCharts() {
  nextTick(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        buildHistoryChart();
        buildAssetsChart();
        buildHoldingsChart();
      });
    });
  });
}

onMounted(refreshCharts);
watch(
  () => [
    props.assetTypeBreakdown,
    props.holdings,
    props.transactions,
    props.summary.totalValue,
    props.summary.portfolioValue,
    props.summary.profitLoss,
  ],
  refreshCharts,
  { deep: true },
);
</script>

<style scoped>
.summary-main-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
  min-height: 256px;
}

.summary-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(160px, 1fr));
  gap: 16px;
  min-width: 0;
}

.history-wrap {
  flex: 1;
  width: 100%;
  min-height: 0;
  margin-top: 0;
  position: relative;
  min-width: 0;
  overflow: hidden;
}

.chart-card {
  min-height: 256px;
  display: flex;
  flex-direction: column;
}
.chart-wrap {
  flex: 1;
  width: 100%;
  min-height: 0;
  margin-top: 8px;
  position: relative;
  min-width: 0;
  overflow: hidden;
}
.muted {
  color: #8a93a6;
  margin: 24px 0 0;
  font-size: 14px;
}

.positive {
  color: #22c55e;
}

.negative {
  color: #f87171;
}

canvas {
  max-width: 100% !important;
  width: 100% !important;
  height: 100% !important;
}
</style>
