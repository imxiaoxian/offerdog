<template>
  <div class="classic-template">
    <!-- 头部 -->
    <div class="classic-header">
      <h1 class="name">{{ data.personalInfo.name }}</h1>
      <div class="title">{{ data.personalInfo.title }}</div>
      <div class="contact">
        {{ data.personalInfo.phone }} • {{ data.personalInfo.email }} • {{ data.personalInfo.location }}
      </div>
    </div>

    <div class="divider"></div>

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
          <div class="item-left">
            <strong>{{ work.position }}</strong>
            <span class="company"> @ {{ work.company }}</span>
          </div>
          <div class="date">{{ work.startDate }} - {{ work.endDate }}</div>
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
          <div class="item-left">
            <strong>{{ project.name }}</strong>
            <span class="company"> - {{ project.role }}</span>
          </div>
          <div class="date">{{ project.startDate }} - {{ project.endDate }}</div>
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
          <div class="item-left">
            <strong>{{ edu.school }}</strong>
            <span class="company"> - {{ edu.major }} ({{ edu.degree }})</span>
          </div>
          <div class="date">{{ edu.startDate }} - {{ edu.endDate }}</div>
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
        <span v-for="skill in data.skills" :key="skill.id" class="skill-tag">
          {{ skill.name }}
        </span>
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
.classic-template {
  font-family: Georgia, 'Times New Roman', serif;
  color: #2c3e50;
  line-height: 1.7;
}

.classic-header {
  text-align: center;
  margin-bottom: 18px;
}

.name {
  font-size: 30px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 6px 0;
  letter-spacing: 1px;
}

.title {
  font-size: 17px;
  color: #555;
  margin-bottom: 8px;
  font-style: italic;
}

.contact {
  font-size: 12px;
  color: #666;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.divider {
  height: 2px;
  background: #333;
  margin: 18px 0;
}

.section {
  margin-bottom: 22px;
}

.section-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 10px 0;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  border-bottom: 1px solid #333;
  padding-bottom: 4px;
}

.summary {
  text-align: justify;
  font-size: 13px;
  line-height: 1.8;
  color: #444;
  margin: 0;
}

.item {
  margin-bottom: 16px;
  page-break-inside: avoid;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 5px;
}

.item-left {
  flex: 1;
}

.item-left strong {
  font-size: 14px;
  color: #1a1a1a;
}

.company {
  color: #666;
  font-size: 13px;
  font-style: italic;
}

.date {
  color: #888;
  font-size: 12px;
  white-space: nowrap;
  margin-left: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.description {
  font-size: 12px;
  color: #555;
  line-height: 1.7;
  margin-left: 18px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  display: inline-block;
  padding: 5px 12px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  color: #333;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

@media print {
  .section {
    margin-bottom: 16px;
  }
}

@media (max-width: 768px) {
  .classic-header {
    margin-bottom: 12px;
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

  .divider {
    margin: 12px 0;
  }

  .section-title {
    font-size: 15px;
  }

  .item-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .date {
    width: 100%;
    margin-left: 0;
  }

  .description {
    margin-left: 12px;
  }

  .skill-tag {
    padding: 4px 10px;
    font-size: 11px;
  }
}

@media (max-width: 480px) {
  .classic-header {
    margin-bottom: 8px;
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

  .divider {
    margin: 8px 0;
  }

  .section-title {
    font-size: 14px;
  }

  .item {
    margin-bottom: 12px;
  }

  .item-header {
    gap: 3px;
  }

  .item-left strong {
    font-size: 13px;
  }

  .company {
    font-size: 12px;
  }

  .date {
    font-size: 11px;
  }

  .description {
    font-size: 11px;
    margin-left: 8px;
  }

  .skill-tag {
    padding: 3px 8px;
    font-size: 10px;
  }
}
</style>
