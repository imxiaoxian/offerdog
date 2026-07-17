<template>
  <div class="modern-template">
    <!-- 头部 -->
    <div class="modern-header">
      <h1 class="name">{{ data.personalInfo.name }}</h1>
      <div class="title">{{ data.personalInfo.title }}</div>
      <div class="contact">
        <span>{{ data.personalInfo.phone }}</span>
        <span class="separator">|</span>
        <span>{{ data.personalInfo.email }}</span>
        <span class="separator">|</span>
        <span>{{ data.personalInfo.location }}</span>
      </div>
    </div>

    <!-- 个人简介 -->
    <div v-if="data.personalInfo.summary" class="section">
      <h2 class="section-title">个人简介</h2>
      <p class="summary">{{ data.personalInfo.summary }}</p>
    </div>

    <!-- 工作经历 -->
    <div v-if="data.workExperience?.length" class="section">
      <h2 class="section-title">工作经历</h2>
      <div v-for="work in data.workExperience" :key="work.id" class="item">
        <div class="item-header">
          <div>
            <strong class="company">{{ work.company }}</strong>
            <span class="position"> - {{ work.position }}</span>
          </div>
          <div class="date">{{ work.startDate }} ~ {{ work.endDate }}</div>
        </div>
        <div class="description">
          <div v-for="(line, i) in work.description.split('\n')" :key="i">{{ line }}</div>
        </div>
      </div>
    </div>

    <!-- 项目经历 -->
    <div v-if="data.projects?.length" class="section">
      <h2 class="section-title">项目经历</h2>
      <div v-for="project in data.projects" :key="project.id" class="item">
        <div class="item-header">
          <div>
            <strong class="company">{{ project.name }}</strong>
            <span class="position"> - {{ project.role }}</span>
          </div>
          <div class="date">{{ project.startDate }} ~ {{ project.endDate }}</div>
        </div>
        <div class="description">
          <div v-for="(line, i) in project.description.split('\n')" :key="i">{{ line }}</div>
        </div>
      </div>
    </div>

    <!-- 教育经历 -->
    <div v-if="data.education?.length" class="section">
      <h2 class="section-title">教育经历</h2>
      <div v-for="edu in data.education" :key="edu.id" class="item">
        <div class="item-header">
          <div>
            <strong class="company">{{ edu.school }}</strong>
            <span class="position"> - {{ edu.major }} ({{ edu.degree }})</span>
          </div>
          <div class="date">{{ edu.startDate }} ~ {{ edu.endDate }}</div>
        </div>
        <div v-if="edu.description" class="description">
          <div v-for="(line, i) in edu.description.split('\n')" :key="i">{{ line }}</div>
        </div>
      </div>
    </div>

    <!-- 专业技能 -->
    <div v-if="data.skills?.length" class="section">
      <h2 class="section-title">专业技能</h2>
      <div class="skills">
        <div v-for="skill in data.skills" :key="skill.id" class="skill-item">
          <div class="skill-header">
            <span class="skill-name">{{ skill.name }}</span>
            <span class="skill-level">{{ skill.level }}%</span>
          </div>
          <div class="skill-bar">
            <div class="skill-bar-fill" :style="{ width: skill.level + '%' }"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  data: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.modern-template {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', sans-serif;
  color: #333;
  line-height: 1.6;
}

.modern-header {
  background: linear-gradient(135deg, #0f6dff 0%, #4facff 100%);
  color: white;
  padding: 30px;
  margin: -20mm -20mm 20px -20mm;
  text-align: center;
}

.name {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.title {
  font-size: 18px;
  margin-bottom: 12px;
  opacity: 0.95;
}

.contact {
  font-size: 13px;
  opacity: 0.9;
}

.separator {
  margin: 0 10px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #0f6dff;
  margin: 0 0 12px 0;
  padding-bottom: 6px;
  border-bottom: 2px solid #0f6dff;
}

.summary {
  line-height: 1.8;
  text-align: justify;
  margin: 0;
}

.item {
  margin-bottom: 18px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}

.company {
  font-size: 15px;
  color: #333;
}

.position {
  color: #666;
  font-size: 14px;
}

.date {
  color: #999;
  font-size: 13px;
  white-space: nowrap;
}

.description {
  color: #555;
  font-size: 13px;
  line-height: 1.8;
  padding-left: 18px;
}

.skills {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.skill-item {
  margin-bottom: 8px;
}

.skill-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 13px;
}

.skill-name {
  font-weight: 500;
}

.skill-level {
  color: #0f6dff;
  font-weight: 600;
}

.skill-bar {
  height: 7px;
  background: #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}

.skill-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #0f6dff 0%, #4facff 100%);
  border-radius: 4px;
  transition: width 0.3s;
}

@media print {
  .modern-header {
    margin: -15mm -15mm 15px -15mm;
    padding: 20px;
  }

  .name {
    font-size: 28px;
  }
}

@media (max-width: 768px) {
  .modern-header {
    padding: 20px;
    margin: -15mm -15mm 15px -15mm;
  }

  .name {
    font-size: 24px;
  }

  .title {
    font-size: 14px;
  }

  .contact {
    font-size: 11px;
  }

  .section-title {
    font-size: 16px;
  }

  .item-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .date {
    width: 100%;
  }

  .description {
    padding-left: 12px;
  }

  .skills {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .modern-header {
    padding: 16px;
    margin: -10mm -10mm 10mm -10mm;
  }

  .name {
    font-size: 20px;
  }

  .title {
    font-size: 13px;
  }

  .contact {
    font-size: 10px;
  }

  .section-title {
    font-size: 14px;
  }

  .item {
    margin-bottom: 12px;
  }

  .item-header {
    gap: 4px;
  }

  .company {
    font-size: 13px;
  }

  .position {
    font-size: 12px;
  }

  .date {
    font-size: 11px;
  }

  .description {
    font-size: 12px;
    padding-left: 8px;
  }

  .skill-header {
    font-size: 12px;
  }

  .skill-name {
    font-size: 12px;
  }

  .skill-level {
    font-size: 11px;
  }
}
</style>
