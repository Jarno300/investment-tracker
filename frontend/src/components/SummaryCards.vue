<template>
  <section class="summary">
    <div class="card">
      <p class="label">Total value</p>
      <p class="value">{{ totalValue }}</p>
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
  formatType: {
    type: Function,
    default: (t) => t,
  },
});

const assetsChartRef = ref(null);
const holdingsChartRef = ref(null);
let assetsChart = null;
let holdingsChart = null;

const holdingsChartData = computed(() => {
  const total = Number(props.summary.totalValue) || 0;
  if (total <= 0) return [];
  return props.holdings
    .filter((h) => Number(h.marketValue) > 0)
    .map((h) => ({
      label: h.asset?.name || "Unknown",
      value: Number(h.marketValue),
    }));
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
    type: "pie",
    data: {
      labels: holdingsChartData.value.map((d) => d.label),
      datasets: [
        {
          data: holdingsChartData.value.map((d) => d.value),
          backgroundColor: CHART_COLORS.slice(
            0,
            holdingsChartData.value.length,
          ),
          borderColor: "#151a22",
          borderWidth: 2,
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
        tooltip: {
          callbacks: {
            label: (ctx) => {
              const pct = (
                (ctx.raw / (Number(props.summary.totalValue) || 1)) *
                100
              ).toFixed(1);
              return `${ctx.label}: ${pct}%`;
            },
          },
        },
      },
    },
  });
}

function refreshCharts() {
  nextTick(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        buildAssetsChart();
        buildHoldingsChart();
      });
    });
  });
}

onMounted(refreshCharts);
watch(
  () => [props.assetTypeBreakdown, props.holdings, props.summary.totalValue],
  refreshCharts,
  { deep: true },
);
</script>

<style scoped>
.chart-card {
  min-height: 200px;
}
.chart-wrap {
  height: 180px;
  width: 100%;
  min-height: 180px;
  min-width: 150px;
  margin-top: 8px;
  position: relative;
}
.muted {
  color: #8a93a6;
  margin: 24px 0 0;
  font-size: 14px;
}
</style>
