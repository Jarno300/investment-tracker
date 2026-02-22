<template>
  <article class="panel">
    <h2>Asset types</h2>
    <ul class="list" v-if="items.length">
      <li v-for="item in items" :key="item.type">
        <span>{{ formatType(item.type) }}</span>
        <span class="pill">{{ item.count }}</span>
      </li>
    </ul>
    <p v-else class="muted">No assets yet. Add your first asset.</p>
  </article>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  },
  formatType: {
    type: Function,
    required: true
  }
});
</script>
