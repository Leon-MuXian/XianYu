<script setup lang="ts">
import Taro from '@tarojs/taro'

const props = withDefaults(defineProps<{
  title: string
  back?: boolean
  interceptBack?: boolean
}>(), { back: false, interceptBack: false })

const emit = defineEmits<{
  back: []
}>()

const statusBarHeight = Taro.getWindowInfo().statusBarHeight || 20

function goBack() {
  if (props.interceptBack) {
    emit('back')
    return
  }
  if (!Taro.getCurrentPages().length || Taro.getCurrentPages().length === 1) return
  void Taro.navigateBack()
}
</script>

<template>
  <View class="custom-head">
    <View class="safe-status" :style="{ height: `${statusBarHeight}px` }" />
    <View class="topbar">
      <View class="topbar-title" @tap="props.back && goBack()">
        <View v-if="props.back" class="back-mark" aria-hidden="true" />
        <Text class="topbar-heading">{{ props.title }}</Text>
      </View>
    </View>
  </View>
</template>
