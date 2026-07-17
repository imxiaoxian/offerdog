<script setup lang="ts">
import { ref } from 'vue'

import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale('zh-cn')
const locale = ref(zhCN)
</script>

<template>
  <a-config-provider 
    :locale="locale"
    :theme="{
      token: {
        colorPrimary: '#1976d2',
        colorPrimaryHover: '#42a5f5',
        colorPrimaryActive: '#1565c0',
        colorPrimaryBorder: '#42a5f5',
        colorTextBase: '#000000',
        colorText: '#333333',
        colorTextSecondary: '#666666',
        colorTextQuaternary: '#999999',
        colorBgBase: '#ffffff',
        colorBgContainer: '#ffffff',
        colorBgLayout: '#fafafa',
        colorBorder: '#e0e0e0',
        colorSplit: '#e8e8e8',
        borderRadius: 8,
        borderRadiusLG: 12,
        borderRadiusSM: 4,
        borderRadiusXS: 2,
        lineWidth: 1,
        lineWidthFocus: 2,
        opacityImage: 1,
        colorBgSpotlight: 'rgba(0, 0, 0, 0.6)',
        colorBgMask: 'rgba(0, 0, 0, 0.4)',
        colorWhite: '#ffffff',
      },
      components: {
        Button: {
          colorPrimary: '#1976d2',
          colorPrimaryHover: '#42a5f5',
          colorPrimaryActive: '#1565c0',
          defaultShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
          primaryShadow: '0 4px 12px rgba(25, 118, 210, 0.3)',
          defaultHoverShadow: '0 4px 12px rgba(0, 0, 0, 0.12)',
          defaultActiveShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
        },
        Card: {
          colorBgContainer: '#ffffff',
          colorBorder: '#e0e0e0',
          shadowLevel1: '0 2px 8px rgba(0, 0, 0, 0.08)',
          shadowLevel2: '0 4px 12px rgba(0, 0, 0, 0.12)',
          shadowLevel3: '0 8px 16px rgba(0, 0, 0, 0.14)',
          shadowLevel4: '0 12px 20px rgba(0, 0, 0, 0.16)',
          shadowLevel5: '0 16px 24px rgba(0, 0, 0, 0.18)',
        },
        Input: {
          colorBorder: '#d9d9d9',
          colorBorderHover: '#bfbfbf',
          colorBgContainer: '#ffffff',
        },
        Select: {
          colorBorder: '#d9d9d9',
          colorBgContainer: '#ffffff',
        },
        Pagination: {
          itemActiveBg: '#1976d2',
          itemBg: '#ffffff',
          itemLinkBg: '#ffffff',
          itemDisabledBg: '#ffffff',
          itemHoverBg: 'rgba(25, 118, 210, 0.08)',
          colorTextLightSolid: '#ffffff',
        },
      },
    }"
  >
    <router-view />
  </a-config-provider>
</template>

<style scoped></style>
