<template>
  <div class="resume-preview-container">
    <div class="resume-preview">
      <component :is="currentTemplateComponent" :data="resumeStore.resumeData" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useResumeStore } from '../stores/resume'
import ModernTemplate from './templates/ModernTemplate.vue'
import ClassicTemplate from './templates/ClassicTemplate.vue'
import CreativeTemplate from './templates/CreativeTemplate.vue'

const resumeStore = useResumeStore()

const currentTemplateComponent = computed(() => {
  const templates = {
    modern: ModernTemplate,
    classic: ClassicTemplate,
    creative: CreativeTemplate
  }
  return templates[resumeStore.currentTemplate] || ModernTemplate
})
</script>

<style scoped>
.resume-preview-container {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 12px;
}

.resume-preview {
  width: 210mm;
  min-height: 297mm;
  background: white;
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
  padding: 20mm;
}

@media print {
  .resume-preview-container {
    width: 100%;
  }

  .resume-preview {
    box-shadow: none;
    padding: 15mm;
  }
}

@media (max-width: 768px) {
  .resume-preview-container {
    padding: 8px;
  }

  .resume-preview {
    width: 100%;
    min-height: auto;
    padding: 15mm;
    box-sizing: border-box;
  }
}

@media (max-width: 480px) {
  .resume-preview-container {
    padding: 4px;
  }

  .resume-preview {
    padding: 10mm;
  }
}
</style>
