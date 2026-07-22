<script setup lang="ts">
import Taro from '@tarojs/taro'
import { ref } from 'vue'
import { setSessionToken } from '@serenmeet/api-client/taro'
import { api } from '../../api'
const loading=ref(false); const message=ref('')
async function login(){loading.value=true;message.value='';try{const{code}=await Taro.login();const result=await api.request<{token:string;session:{nextPath:string}}>('POST','/auth/member/wechat-login',{code});setSessionToken(result.token);const target=result.session.nextPath.includes('frozen')?'/pages/frozen/index':result.session.nextPath.includes('bind')?'/pages/bind/index':'/pages/home/index';await Taro.reLaunch({url:target})}catch(error){message.value=error instanceof Error?error.message:'登录失败'}finally{loading.value=false}}
</script>
<template><View class="page"><Text class="brand">闲遇 · 会员端</Text><Text class="title">查看权益，预约下一次服务</Text><Text class="subtitle">使用门店提供的邀请码绑定会员资料。</Text><View class="panel"><Text class="panel-title">会员登录</Text><Button class="button" :loading="loading" :disabled="loading" @tap="login">微信登录</Button><Text v-if="message" class="message">{{ message }}</Text></View></View></template>
