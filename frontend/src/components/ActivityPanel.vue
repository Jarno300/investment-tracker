<template>
  <article class="panel">
    <h2>Recent activity</h2>
    <ul v-if="transactions.length" class="list">
      <li v-for="transaction in transactions" :key="transaction.id">
        <span>{{ transaction.type }} {{ transaction.asset?.symbol || transaction.asset?.name }}</span>
        <span class="muted">{{ formatDate(transaction.tradedAt) }}</span>
      </li>
    </ul>
    <p v-else class="muted">No trades yet. Connect your first asset.</p>
  </article>
</template>

<script setup>
defineProps({
  transactions: {
    type: Array,
    default: () => []
  },
  formatDate: {
    type: Function,
    required: true
  }
});
</script>
