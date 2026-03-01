<template>
  <div v-if="open && holding" class="modal-backdrop" @click.self="close">
    <div class="modal sell-modal">
      <header class="modal-header">
        <div>
          <p class="eyebrow">Sell holding</p>
          <h2>Sell {{ holding.asset?.symbol || holding.asset?.name }}</h2>
        </div>
        <button class="icon-button" type="button" @click="close">✕</button>
      </header>

      <div class="modal-body">
        <p class="muted">
          Owned quantity: <strong>{{ holding.quantity }}</strong>
        </p>
        <div class="form-grid">
          <label class="field">
            Amount to sell
            <input v-model.number="quantity" type="number" min="0" step="0.0001" />
          </label>
          <label class="field">
            Price sold at ({{ holding.asset?.currency || "USD" }})
            <input v-model.number="price" type="number" min="0" step="0.01" />
          </label>
          <label class="field">
            Transaction costs ({{ holding.asset?.currency || "USD" }})
            <input v-model.number="costs" type="number" min="0" step="0.01" />
          </label>
        </div>
        <button class="action" type="button" :disabled="saving" @click="submit">
          {{ saving ? "Selling..." : "Confirm Sell" }}
        </button>
        <p v-if="error" class="quote-error">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { sellHolding } from "../api";

const props = defineProps({
  open: { type: Boolean, default: false },
  holding: { type: Object, default: null },
});

const emit = defineEmits(["close", "sold"]);

const quantity = ref(0);
const price = ref(0);
const costs = ref(0);
const saving = ref(false);
const error = ref("");

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      quantity.value = 0;
      price.value = 0;
      costs.value = 0;
      error.value = "";
      saving.value = false;
    }
  },
);

function close() {
  emit("close");
}

async function submit() {
  if (!props.holding) return;
  const owned = Number(props.holding.quantity || 0);
  const qty = Number(quantity.value || 0);
  const soldPrice = Number(price.value || 0);
  const txCosts = Number(costs.value || 0);
  if (qty <= 0 || soldPrice <= 0 || txCosts < 0) {
    error.value =
      "Amount and price must be greater than zero, and costs cannot be negative.";
    return;
  }
  if (qty > owned) {
    error.value = "Amount to sell cannot exceed owned quantity.";
    return;
  }

  saving.value = true;
  error.value = "";
  try {
    await sellHolding({
      holdingId: props.holding.id,
      quantity: qty,
      price: soldPrice,
      costs: txCosts,
    });
    emit("sold");
    close();
  } catch (err) {
    error.value = err.message || "Failed to sell holding.";
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped src="../styles/components/sell-holding-modal.css"></style>
