<template>
  <article class="panel">
    <h2>Holdings</h2>
    <div v-if="holdings.length" class="table holdings-table">
      <div class="row header-row holdings-row holdings-header-row">
        <span>Asset</span>
        <span>Qty</span>
        <span>Investment Value</span>
        <span>Current Value</span>
        <span>Action</span>
      </div>
      <div v-for="holding in holdings" :key="holding.id" class="row holdings-row">
        <span>{{ holding.asset?.name || "Unknown" }}</span>
        <span>{{ holding.quantity }}</span>
        <span>{{ formatCurrency(holding.marketValue) }}</span>
        <span>{{ formatCurrency(holding.currentValue) }}</span>
        <span>
          <button class="action secondary sell-button" type="button" @click="$emit('sell', holding)">
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
</script>

<style scoped>
.holdings-row {
  grid-template-columns: minmax(120px, 1.4fr) minmax(70px, 0.5fr) minmax(120px, 1fr) minmax(120px, 1fr) minmax(100px, 0.8fr);
}

.holdings-row span {
  white-space: nowrap;
}

.sell-button {
  padding: 6px 12px;
  font-size: 12px;
}
</style>
