<template>
  <article class="panel">
    <h2>Recent activity</h2>
    <ul v-if="transactions.length" class="list">
      <li v-for="transaction in transactions" :key="transaction.id">
        <div class="activity-main">
          <strong>{{ transaction.type }} {{ transaction.asset?.symbol || transaction.asset?.name }}</strong>
          <span class="muted">{{ formatDate(transaction.tradedAt) }}</span>
        </div>
        <div class="activity-sub">
          {{ formatQuantity(transaction.quantity) }} @
          {{ formatCurrency(transaction.price) }}
        </div>
        <div class="activity-sub" :class="plClass(transaction.profitLossAmount)">
          P/L:
          {{ formatProfitLoss(transaction.profitLossAmount, transaction.profitLossPercent) }}
        </div>
      </li>
    </ul>
    <p v-else class="muted">No trades yet. Connect your first asset.</p>
  </article>
</template>

<script setup>
const props = defineProps({
  transactions: {
    type: Array,
    default: () => [],
  },
  formatDate: {
    type: Function,
    required: true,
  },
  formatCurrency: {
    type: Function,
    required: true,
  },
});

function formatQuantity(value) {
  const n = Number(value || 0);
  if (!Number.isFinite(n)) return "0";
  return n.toFixed(4).replace(/\.?0+$/, "");
}

function formatProfitLoss(amount, percent) {
  const amountNum = Number(amount);
  const pctNum = Number(percent);
  if (!Number.isFinite(amountNum) || !Number.isFinite(pctNum)) {
    return "—";
  }
  const pctSign = pctNum > 0 ? "+" : "";
  return `${props.formatCurrency(amountNum)} (${pctSign}${pctNum.toFixed(2)}%)`;
}

function plClass(amount) {
  const n = Number(amount);
  if (!Number.isFinite(n) || n === 0) return "muted";
  return n > 0 ? "positive" : "negative";
}
</script>

<style scoped>
.activity-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.activity-main strong {
  min-width: 0;
  overflow-wrap: anywhere;
}

.activity-sub {
  font-size: 13px;
  overflow-wrap: anywhere;
}

.positive {
  color: #22c55e;
}

.negative {
  color: #f87171;
}
</style>
