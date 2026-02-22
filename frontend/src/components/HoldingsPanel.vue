<template>
  <article class="panel">
    <h2>Holdings</h2>
    <div v-if="holdings.length" class="table">
      <div class="row header-row">
        <span>Asset</span>
        <span>Qty</span>
        <span>Market value</span>
      </div>
      <div v-for="holding in holdings" :key="holding.id" class="row">
        <span>{{ holding.asset?.name || "Unknown" }}</span>
        <span>{{ holding.quantity }}</span>
        <span>{{ formatCurrency(holding.marketValue) }}</span>
      </div>
    </div>
    <p v-else class="muted">No holdings yet. Import positions to see them here.</p>
  </article>
</template>

<script setup>
defineProps({
  holdings: {
    type: Array,
    default: () => []
  },
  formatCurrency: {
    type: Function,
    required: true
  }
});
</script>
