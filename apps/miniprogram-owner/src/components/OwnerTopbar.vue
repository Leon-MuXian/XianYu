<script setup lang="ts">
import Taro from '@tarojs/taro'

withDefaults(defineProps<{
  title: string
  back?: boolean
}>(), { back: false })

const statusBarHeight = Taro.getWindowInfo().statusBarHeight || 20

function goBack() {
  if (!Taro.getCurrentPages().length || Taro.getCurrentPages().length === 1) return
  void Taro.navigateBack()
}
</script>

<template>
  <View class="custom-head">
    <View class="safe-status" :style="{ height: `${statusBarHeight}px` }" />
    <View class="topbar">
      <View class="topbar-title" @tap="back && goBack()">
        <View v-if="back" class="back-mark" aria-hidden="true" />
        <Text class="topbar-heading">{{ title }}</Text>
      </View>
    </View>
  </View>
</template>
