<script setup lang="ts">
import { computed } from 'vue'
import { presentBusinessHours, type BusinessHoursValue } from '@serenmeet/business-components'

const props = defineProps<{ value?: BusinessHoursValue | null }>()
const presentation = computed(() => presentBusinessHours(props.value))
</script>

<template>
  <div class="business-hours-view">
    <div class="business-hours-heading">
      <span>营业时间</span>
      <strong>{{ presentation.summary }}</strong>
    </div>
    <div class="business-hours-groups">
      <div v-for="group in presentation.groups" :key="group.key" class="business-hours-group" :class="{ closed: !group.open }">
        <strong>{{ group.dayLabels.join(' · ') }}</strong>
        <small>{{ group.open ? `${group.start || '--:--'}-${group.end || '--:--'}` : '休息' }}</small>
      </div>
    </div>
  </div>
</template>

<style scoped>
.business-hours-view {
  display: grid;
  gap: 12px;
  width: 100%;
}

.business-hours-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.business-hours-heading > span {
  margin: 0;
}

.business-hours-heading > strong {
  color: var(--ink);
  font-size: 13px;
  text-align: right;
}

.business-hours-groups {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
}

.business-hours-group {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  border-left: 3px solid var(--green);
  background: var(--gray-soft);
  padding: 8px 10px;
}

.business-hours-group > strong {
  overflow-wrap: anywhere;
  color: var(--ink);
  font-size: 12px;
  line-height: 1.4;
}

.business-hours-group > small {
  color: var(--green-dark);
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.business-hours-group.closed {
  border-left-color: #aeb9b3;
  background: #f0f3f1;
}

.business-hours-group.closed > small {
  color: var(--muted);
}

@media (max-width: 640px) {
  .business-hours-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }

  .business-hours-heading > strong {
    text-align: left;
  }

  .business-hours-groups {
    grid-template-columns: 1fr;
  }
}
</style>
