<script setup lang="ts">
import Taro from '@tarojs/taro'
import { ref } from 'vue'
import { createIdempotencyKey } from '@serenmeet/api-client'
import { api } from '../../api'
interface Invite { storeName:string; memberName:string; address:string; contactPhone:string; cards:Array<{cardName:string;cardType:string;remainCount:number|null;validUntil:string}> }
const code=ref(''); const preview=ref<Invite|null>(null); const loading=ref(false); const message=ref('')
async function check(){await run(async()=>{preview.value=await api.request('POST','/member/invites/check',{code:code.value})})}
async function bind(){await run(async()=>{await api.request('POST','/member/invites/bind',{code:code.value},createIdempotencyKey('invite-bind'));await Taro.reLaunch({url:'/pages/home/index'})})}
async function run(action:()=>Promise<void>){loading.value=true;message.value='';try{await action()}catch(error){message.value=error instanceof Error?error.message:'绑定失败'}finally{loading.value=false}}
</script>
<template><View class="page"><Text class="brand">会员绑定</Text><Text class="title">填写门店邀请码</Text><View v-if="!preview" class="panel"><View class="field"><Text class="label">邀请码</Text><input v-model="code" class="input" maxlength="8" /></View><Button class="button" :loading="loading" :disabled="loading || !code" @tap="check">查询门店信息</Button></View><View v-else class="panel"><Text class="panel-title">{{ preview.storeName }}</Text><Text class="subtitle">{{ preview.address }} · {{ preview.contactPhone }}</Text><View class="list-row"><Text class="row-title">{{ preview.memberName }}</Text><Text class="row-meta">请核对姓名和会员权益后确认绑定</Text></View><View v-for="card in preview.cards" :key="card.cardName" class="list-row"><Text class="row-title">{{ card.cardName }}</Text><Text class="row-meta">{{ card.cardType === 'count' ? `剩余 ${card.remainCount} 次` : '有效期内使用' }} · 至 {{ card.validUntil }}</Text></View><Button class="button" :disabled="loading" @tap="bind">确认绑定</Button><Button class="button secondary" :disabled="loading" @tap="preview=null">返回修改</Button></View><Text v-if="message" class="message">{{ message }}</Text></View></template>
