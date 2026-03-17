<template>
  <div class="dashboard-content">
    <SummaryCards
      :summary="summary"
      :total-value="totalValue"
      :asset-type-breakdown="assetTypeBreakdown"
      :holdings="holdings"
      :transactions="transactions"
      :format-type="formatType"
      :format-currency="formatCurrency"
    />

    <section class="grid dashboard-grid">
      <HoldingsPanel
        class="holdings-panel-wide"
        :holdings="holdings"
        :format-currency="formatCurrency"
        @sell="emit('sell', $event)"
      />
      <AssetTypePanel
        class="asset-type-panel-third"
        :items="assetTypeBreakdown"
        :format-type="formatType"
      />
      <ActivityPanel
        class="activity-panel-two-thirds"
        :transactions="recentTransactions"
        :format-date="formatDate"
        :format-currency="formatCurrency"
      />
    </section>
  </div>
</template>

<script setup>
import { computed } from "vue";
import ActivityPanel from "./ActivityPanel.vue";
import AssetTypePanel from "./AssetTypePanel.vue";
import HoldingsPanel from "./HoldingsPanel.vue";
import SummaryCards from "./SummaryCards.vue";

const props = defineProps({
  summary: { type: Object, required: true },
  totalValue: { type: String, required: true },
  assetTypeBreakdown: { type: Array, required: true },
  holdings: { type: Array, required: true },
  transactions: { type: Array, required: true },
  formatType: { type: Function, required: true },
  formatCurrency: { type: Function, required: true },
  formatDate: { type: Function, required: true },
});

const emit = defineEmits(["sell"]);

const recentTransactions = computed(() => props.transactions.slice(0, 5));
</script>

<style scoped>
.dashboard-content {
  display: grid;
  gap: 16px;
}

.dashboard-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.holdings-panel-wide {
  grid-column: 1 / -1;
}

.asset-type-panel-third {
  grid-column: span 1;
}

.activity-panel-two-thirds {
  grid-column: span 2;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .asset-type-panel-third,
  .activity-panel-two-thirds,
  .holdings-panel-wide {
    grid-column: 1 / -1;
  }
}
</style>
