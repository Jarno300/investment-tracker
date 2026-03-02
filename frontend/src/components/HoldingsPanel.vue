<template>
  <article class="panel">
    <h2>Holdings</h2>
    <div v-if="holdings.length" class="table holdings-table">
      <div class="row header-row holdings-row holdings-header-row">
        <span>Asset</span>
        <span>Qty</span>
        <span>Investment Value</span>
        <span>Total Costs</span>
        <span>Total Investment</span>
        <span>Current Value</span>
        <span>P/L %</span>
        <span>Action</span>
      </div>
      <div
        v-for="holding in holdings"
        :key="holding.id"
        class="row holdings-row"
      >
        <span>{{ holding.asset?.name || "Unknown" }}</span>
        <span>{{ holding.quantity }}</span>
        <span>{{ formatCurrency(holding.marketValue) }}</span>
        <span>{{ formatCurrency(holding.totalCosts) }}</span>
        <span>{{ formatCurrency(holding.totalInvestment) }}</span>
        <span>{{ formatCurrency(holding.currentValue) }}</span>
        <span :class="plClass(holding.profitLossPercent)">
          {{ formatPercent(holding.profitLossPercent) }}
        </span>
        <span>
          <button
            class="action secondary sell-button"
            type="button"
            @click="$emit('sell', holding)"
          >
            Sell
          </button>
        </span>
      </div>
    </div>
    <p v-else class="muted">
      No holdings yet. Import positions to see them here.
    </p>
  </article>
</template>

<script setup>
defineProps({
  holdings: {
    type: Array,
    default: () => [],
  },
  formatCurrency: {
    type: Function,
    required: true,
  },
});

defineEmits(["sell"]);

function formatPercent(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return "—";
  const sign = numeric > 0 ? "+" : "";
  return `${sign}${numeric.toFixed(2)}%`;
}

function plClass(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric === 0) return "";
  return numeric > 0 ? "positive" : "negative";
}
</script>

<style scoped>
.holdings-table {
  max-height: 200px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(138, 147, 166, 0.35) transparent;
}

.holdings-table::-webkit-scrollbar {
  width: 8px;
}

.holdings-table::-webkit-scrollbar-track {
  background: transparent;
}

.holdings-table::-webkit-scrollbar-thumb {
  background: rgba(138, 147, 166, 0.35);
  border-radius: 999px;
}

.holdings-table::-webkit-scrollbar-thumb:hover {
  background: rgba(138, 147, 166, 0.55);
}

.holdings-header-row {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #151a22;
}

.holdings-row {
  grid-template-columns:
    minmax(0, 1.5fr)
    minmax(0, 0.5fr)
    minmax(0, 1fr)
    minmax(0, 1fr)
    minmax(0, 1fr)
    minmax(0, 1fr)
    minmax(0, 0.75fr)
    minmax(0, 0.75fr);
}

.holdings-row span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.sell-button {
  padding: 6px 12px;
  font-size: 12px;
}

.positive {
  color: #22c55e;
}

.negative {
  color: #f87171;
}
</style>
