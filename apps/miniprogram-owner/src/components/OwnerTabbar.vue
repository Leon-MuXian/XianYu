<script setup lang="ts">
import { ownerRoutes, switchOwnerTab } from '../owner'

defineProps<{ active: 'dashboard' | 'schedules' | 'members' | 'report' | 'my' }>()

const items = [
  { key: 'dashboard', glyph: '台', label: '工作台', url: ownerRoutes.dashboard },
  { key: 'schedules', glyph: '排', label: '排期', url: ownerRoutes.schedules },
  { key: 'members', glyph: '会', label: '会员', url: ownerRoutes.members },
  { key: 'report', glyph: '报', label: '报表', url: ownerRoutes.report },
  { key: 'my', glyph: '我', label: '我的', url: ownerRoutes.my }
] as const
</script>

<template>
  <View class="bottom-nav">
    <View v-for="item in items" :key="item.key" :class="{ active: item.key === active }" @tap="switchOwnerTab(item.url)">
      <Text class="nav-glyph">{{ item.glyph }}</Text>
      <Text>{{ item.label }}</Text>
    </View>
  </View>
</template>
