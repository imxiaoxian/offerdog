<template>
  <div class="resume-page">
    <a-layout class="resume-layout">
      <a-layout-header class="resume-header">
        <div class="header-content">
        <div class="header-left">
          <FileTextOutlined class="header-icon" />
          <div class="header-text">
            <div class="header-title">编辑简历</div>
            <div class="header-sub">OfferDog · 智能助手</div>
          </div>
        </div>
          <a-space :size="8">
            <a-button size="small" @click="handleReset">
              <template #icon>
                <ReloadOutlined />
              </template>
              <span class="btn-text">重置</span>
            </a-button>
            <a-button size="small" type="primary" ghost @click="handlePrint">
              <template #icon>
                <PrinterOutlined />
              </template>
              <span class="btn-text">打印</span>
            </a-button>
            <a-upload
              :before-upload="handleResumeUpload"
              :show-upload-list="false"
              :accept="resumeUploadAccept"
            >
              <a-button size="small" :loading="parsingResume">
                <template #icon>
                  <UploadOutlined />
                </template>
                <span class="btn-text">上传</span>
              </a-button>
            </a-upload>
            <a-button type="primary" size="small" @click="handleSaveResume" :loading="resumeStore.loading">
              <template #icon>
                <SaveOutlined />
              </template>
              <span class="btn-text">保存</span>
            </a-button>
            <a-button type="primary" size="small" @click="handleExportPDF" :loading="exporting">
              <template #icon>
                <DownloadOutlined />
              </template>
              <span class="btn-text">PDF</span>
            </a-button>
          </a-space>
        </div>
      </a-layout-header>
      <a-layout-content class="resume-body">
        <a-row :gutter="[8, 8]">
          <a-col :xs="24" :lg="12">
            <a-card class="panel" :bordered="false">
              <a-spin :spinning="resumeStore.loading">
                <div class="panel-scroll">
                  <ResumeEditor />
                </div>
              </a-spin>
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-card class="panel preview-panel" :bordered="false">
              <a-spin :spinning="resumeStore.loading">
                <div class="panel-scroll preview-scroll">
                  <ResumePreview />
                </div>
              </a-spin>
            </a-card>
          </a-col>
        </a-row>
      </a-layout-content>
    </a-layout>
    
    <!-- 移动端浮动操作按钮 -->
    <div class="mobile-fab" v-if="isMobile">
      <a-space direction="vertical" :size="8">
        <a-tooltip title="重置">
          <a-button shape="circle" @click="handleReset">
            <template #icon>
              <ReloadOutlined />
            </template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="打印">
          <a-button shape="circle" type="primary" ghost @click="handlePrint">
            <template #icon>
              <PrinterOutlined />
            </template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="上传">
          <a-upload
            :before-upload="handleResumeUpload"
            :show-upload-list="false"
            :accept="resumeUploadAccept"
          >
            <a-button shape="circle" :loading="parsingResume">
              <template #icon>
                <UploadOutlined />
              </template>
            </a-button>
          </a-upload>
        </a-tooltip>
        <a-tooltip title="保存">
          <a-button shape="circle" type="primary" @click="handleSaveResume" :loading="resumeStore.loading">
            <template #icon>
              <SaveOutlined />
            </template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="导出PDF">
          <a-button shape="circle" type="primary" @click="handleExportPDF" :loading="exporting">
            <template #icon>
              <DownloadOutlined />
            </template>
          </a-button>
        </a-tooltip>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message, Modal, Upload, type UploadProps } from 'ant-design-vue'
import { FileTextOutlined, DownloadOutlined, PrinterOutlined, ReloadOutlined, SaveOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'
import ResumeEditor from '../components/ResumeEditor.vue'
import ResumePreview from '../components/ResumePreview.vue'
import { useResumeStore } from '@/stores/resume'
import { useUserStore } from '../stores/user'

const resumeStore = useResumeStore()
const userStore = useUserStore()
const { currentUser } = storeToRefs(userStore)
const exporting = ref(false)
const parsingResume = ref(false)
const resumeUploadAccept = '.pdf,.doc,.docx,.txt'

const isMobile = computed(() => {
  return window.innerWidth <= 768
})

onMounted(async () => {
  if (currentUser.value?.id) {
    const success = await resumeStore.loadUserResume(currentUser.value.id)
    if (success) {
      message.success('已加载您的简历信息')
    }
  } else {
    message.warning('未登录，使用默认模板')
  }
})

const handleReset = () => {
  Modal.confirm({
    title: '提示',
    content: '确定要重置为示例数据吗？当前数据将被清除。',
    okText: '确定',
    cancelText: '取消',
    centered: true,
    onOk: () => {
      resumeStore.resetData()
      message.success('已重置为示例数据')
    }
  })
}

const handlePrint = () => {
  window.print()
}

const handleResumeUpload: UploadProps['beforeUpload'] = async (file) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  const allowed = ['pdf', 'doc', 'docx', 'txt']
  if (!ext || !allowed.includes(ext)) {
    message.error('仅支持上传 pdf、doc、docx、txt 格式的简历文件')
    return Upload.LIST_IGNORE
  }

  if (!currentUser.value?.id) {
    message.warning('请先登录再上传简历解析')
    return Upload.LIST_IGNORE
  }

  parsingResume.value = true
  try {
    await resumeStore.parseResumeFromFile(currentUser.value.id, file as File)
  } finally {
    parsingResume.value = false
  }
  return false
}

const handleExportPDF = async () => {
  try {
    exporting.value = true
    const html2canvas = (await import('html2canvas')).default
    const jsPDF = (await import('jspdf')).default

    const element = document.querySelector('.resume-preview') as HTMLElement
    if (!element) {
      message.error('未找到简历内容区域')
      return
    }
    // 保证从顶部完整截取
    const originalScrollTop = element.scrollTop
    element.scrollTop = 0
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff'
    })
    element.scrollTop = originalScrollTop

    const imgData = canvas.toDataURL('image/png')
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    })

    const imgWidth = 210 // A4 宽度(mm)
    const pageHeight = 297 // A4 高度(mm)
    const imgHeight = (canvas.height * imgWidth) / canvas.width
    let heightLeft = imgHeight
    let position = 0

    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight

    while (heightLeft > 0) {
      position = heightLeft - imgHeight
      pdf.addPage()
      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
      heightLeft -= pageHeight
    }
    pdf.save(`${resumeStore.resumeData.personalInfo.name}-简历.pdf`)

    message.success('PDF导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出PDF失败，请重试')
  } finally {
    exporting.value = false
  }
}

const handleSaveResume = async () => {
  if (currentUser.value?.id) {
    await resumeStore.saveUserResume(currentUser.value.id)
  } else {
    message.warning('请先登录再保存简历')
  }
}
</script>

<style scoped>
.resume-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f4f8ff 0%, #fefefe 100%);
  position: relative;
}

.resume-layout {
  min-height: 100vh;
  background: transparent;
}

.resume-header {
  background: linear-gradient(120deg, #0f6dff, #69b1ff);
  padding: 0 20px;
  height: 70px;
  display: flex;
  align-items: center;
  box-shadow: 0 8px 20px rgba(22, 119, 255, 0.25);
  flex-wrap: wrap;
  gap: 12px;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.header-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.2;
  overflow: hidden;
}

.header-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-sub {
  font-size: 11px;
  color: #e5f0ff;
  white-space: nowrap;
}

.resume-body {
  padding: 16px;
}

.panel {
  border-radius: 16px;
  box-shadow: 0 12px 30px rgba(15, 65, 140, 0.12);
}

.panel :deep(.ant-card-body) {
  padding: 0;
}

.panel-scroll {
  padding: 16px;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  background: #fff;
  border-radius: 16px;
}

.preview-panel {
  background: #f5f8ff;
}

.preview-scroll {
  background: linear-gradient(180deg, #fff, #f8fbff);
}

/* 移动端浮动操作按钮 */
.mobile-fab {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mobile-fab .ant-btn {
  width: 56px;
  height: 56px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

@media print {
  .resume-header,
  .panel:not(.preview-panel),
  .mobile-fab {
    display: none !important;
  }

  .resume-body {
    padding: 0;
  }

  .preview-panel,
  .preview-scroll {
    box-shadow: none;
    background: #fff;
  }
}

@media (max-width: 768px) {
  .resume-header {
    padding: 0 12px;
    height: 60px;
  }

  .header-left {
    gap: 8px;
  }

  .header-icon {
    font-size: 24px;
  }

  .header-title {
    font-size: 14px;
  }

  .header-sub {
    font-size: 10px;
  }

  .header-content {
    flex-direction: row;
    flex-wrap: nowrap;
    justify-content: flex-start;
    align-items: center;
    gap: 10px;
  }

  .btn-text {
    display: none;
  }

  .resume-body {
    padding: 8px;
  }

  .panel-scroll,
  .preview-scroll {
    padding: 12px;
    max-height: calc(100vh - 150px);
  }

  .panel {
    border-radius: 12px;
  }

  :deep(.ant-btn) {
    font-size: 12px;
    padding: 4px 8px;
    min-width: auto;
  }

  :deep(.ant-btn-icon) {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .resume-header {
    height: 56px;
    padding: 0 8px;
  }

  .header-title {
    font-size: 13px;
  }

  .header-icon {
    font-size: 20px;
  }

  .resume-body {
    padding: 4px;
  }

  .panel-scroll,
  .preview-scroll {
    padding: 8px;
    max-height: calc(100vh - 140px);
  }

  :deep(.ant-btn) {
    font-size: 11px;
    padding: 3px 6px;
  }

  :deep(.ant-btn-icon) {
    font-size: 12px;
  }

  .mobile-fab {
    bottom: 16px;
    right: 16px;
  }

  .mobile-fab .ant-btn {
    width: 48px;
    height: 48px;
  }
}
</style>
