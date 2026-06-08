<script setup lang="ts">
import type { TenantStatus } from '@/types/admin'

defineProps<{
  status: TenantStatus | string
  text?: string
}>()

function klass(status: string) {
  return {
    TRIALING: 'trialing',
    EXPIRING: 'expiring',
    FROZEN: 'frozen',
    EXTENDED: 'extended'
  }[status] || 'trialing'
}
</script>

<template>
  <span class="status-pill" :class="klass(status)">{{ text || status }}</span>
</template>
