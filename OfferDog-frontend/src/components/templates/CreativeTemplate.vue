<template>
  <div class="creative-template">
    <div class="layout">
      <!-- 左侧栏 -->
      <div class="sidebar">
        <!-- 个人信息 -->
        <div class="profile">
          <div class="avatar">{{ data.personalInfo.name.charAt(0) }}</div>
          <h1 class="name">{{ data.personalInfo.name }}</h1>
          <div class="title">{{ data.personalInfo.title }}</div>
        </div>

        <!-- 联系方式 -->
        <div class="sidebar-section">
          <h3 class="sidebar-title">联系方式</h3>
          <div class="contact-item">
            <span class="icon">📱</span>
            <span>{{ data.personalInfo.phone }}</span>
          </div>
          <div class="contact-item">
            <span class="icon">✉️</span>
            <span>{{ data.personalInfo.email }}</span>
          </div>
          <div class="contact-item">
            <span class="icon">📍</span>
            <span>{{ data.personalInfo.location }}</span>
          </div>
        </div>

        <!-- 专业技能 -->
        <div v-if="data.skills?.length" class="sidebar-section">
          <h3 class="sidebar-title">专业技能</h3>
          <div v-for="skill in data.skills" :key="skill.id" class="skill">
            <div class="skill-name">{{ skill.name }}</div>
            <div class="skill-dots">
              <span
                v-for="i in 5"
                :key="i"
                class="dot"
                :class="{ filled: i <= Math.round(skill.level / 20) }"
              ></span>
            </div>
          </div>
        </div>

        <!-- 教育经历 -->
        <div v-if="data.education?.length" class="sidebar-section">
          <h3 class="sidebar-title">教育背景</h3>
          <div v-for="edu in data.education" :key="edu.id" class="edu-item">
            <div class="edu-degree">{{ edu.degree }}</div>
            <div class="edu-school">{{ edu.school }}</div>
            <div class="edu-major">{{ edu.major }}</div>
            <div class="edu-date">{{ edu.startDate }} - {{ edu.endDate }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧主内容 -->
      <div class="main">
        <!-- 个人简介 -->
        <div v-if="data.personalInfo.summary" class="section">
          <h2 class="section-title">
            <span class="title-icon">💡</span>
            个人简介
          </h2>
          <p class="summary">{{ data.personalInfo.summary }}</p>
        </div>

        <!-- 工作经历 -->
        <div v-if="data.workExperience?.length" class="section">
          <h2 class="section-title">
            <span class="title-icon">💼</span>
            工作经历
          </h2>
          <div v-for="work in data.workExperience" :key="work.id" class="item">
            <div class="item-header">
              <div>
                <div class="item-position">{{ work.position }}</div>
                <div class="item-company">{{ work.company }}</div>
              </div>
              <div class="item-date">{{ work.startDate }} ~ {{ work.endDate }}</div>
            </div>
            <div class="item-description">
              <div v-for="(line, i) in work.description.split('\n')" :key="i">{{ line }}</div>
            </div>
          </div>
        </div>

        <!-- 项目经历 -->
        <div v-if="data.projects?.length" class="section">
          <h2 class="section-title">
            <span class="title-icon">🚀</span>
            项目经历
          </h2>
          <div v-for="project in data.projects" :key="project.id" class="item">
            <div class="item-header">
              <div>
                <div class="item-position">{{ project.name }}</div>
                <div class="item-company">{{ project.role }}</div>
              </div>
              <div class="item-date">{{ project.startDate }} ~ {{ project.endDate }}</div>
            </div>
            <div class="item-description">
              <div v-for="(line, i) in project.description.split('\n')" :key="i">{{ line }}</div>
            </div>
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
.creative-template {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: #333;
}

.layout {
  display: flex;
  margin: -20mm;
  min-height: 257mm;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #2c3e50 0%, #34495e 100%);
  color: white;
  padding: 30px 20px;
  flex-shrink: 0;
}

.profile {
  text-align: center;
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 2px solid rgba(255, 255, 255, 0.2);
}

.avatar {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  margin: 0 auto 12px;
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.name {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px 0;
}

.title {
  font-size: 13px;
  opacity: 0.9;
}

.sidebar-section {
  margin-bottom: 25px;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 12px 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.contact-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  font-size: 12px;
}

.icon {
  margin-right: 8px;
  font-size: 14px;
}

.skill {
  margin-bottom: 12px;
}

.skill-name {
  font-size: 13px;
  margin-bottom: 5px;
  font-weight: 500;
}

.skill-dots {
  display: flex;
  gap: 5px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
}

.dot.filled {
  background: #667eea;
}

.edu-item {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.edu-item:last-child {
  border-bottom: none;
}

.edu-degree {
  font-weight: 600;
  font-size: 13px;
  margin-bottom: 3px;
}

.edu-school {
  font-size: 12px;
  opacity: 0.95;
  margin-bottom: 2px;
}

.edu-major {
  font-size: 12px;
  opacity: 0.85;
  margin-bottom: 3px;
}

.edu-date {
  font-size: 11px;
  opacity: 0.7;
}

.main {
  flex: 1;
  padding: 30px 35px;
  background: white;
}

.section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 8px;
  border-bottom: 3px solid #667eea;
}

.title-icon {
  font-size: 22px;
}

.summary {
  line-height: 1.8;
  color: #555;
  text-align: justify;
  margin: 0;
  font-size: 13px;
}

.item {
  margin-bottom: 20px;
  padding-left: 16px;
  border-left: 3px solid #e8e8e8;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.item-position {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 3px;
}

.item-company {
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
}

.item-date {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  margin-left: 15px;
}

.item-description {
  font-size: 13px;
  color: #666;
  line-height: 1.8;
}

@media print {
  .layout {
    margin: -15mm;
  }

  .sidebar {
    width: 220px;
    padding: 25px 16px;
  }

  .main {
    padding: 25px 30px;
  }

  .section {
    margin-bottom: 22px;
  }
}

@media (max-width: 768px) {
  .layout {
    flex-direction: column;
    margin: -15mm;
    min-height: auto;
  }

  .sidebar {
    width: 100%;
    padding: 20px 16px;
  }

  .profile {
    padding-bottom: 16px;
  }

  .avatar {
    width: 50px;
    height: 50px;
    font-size: 24px;
    margin-bottom: 8px;
  }

  .name {
    font-size: 18px;
  }

  .title {
    font-size: 12px;
  }

  .sidebar-section {
    margin-bottom: 20px;
  }

  .sidebar-title {
    font-size: 14px;
  }

  .contact-item {
    font-size: 11px;
  }

  .skill-name {
    font-size: 12px;
  }

  .edu-item {
    margin-bottom: 12px;
    padding-bottom: 8px;
  }

  .edu-degree {
    font-size: 12px;
  }

  .edu-school {
    font-size: 11px;
  }

  .edu-major {
    font-size: 11px;
  }

  .edu-date {
    font-size: 10px;
  }

  .main {
    padding: 20px 16px;
  }

  .section-title {
    font-size: 18px;
  }

  .title-icon {
    font-size: 20px;
  }

  .item {
    margin-bottom: 16px;
    padding-left: 12px;
    border-left: 2px solid #e8e8e8;
  }

  .item-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .item-position {
    font-size: 14px;
  }

  .item-company {
    font-size: 13px;
  }

  .item-date {
    font-size: 11px;
    margin-left: 0;
    width: 100%;
  }

  .item-description {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .layout {
    margin: -10mm;
  }

  .sidebar {
    padding: 16px 12px;
  }

  .profile {
    padding-bottom: 12px;
  }

  .avatar {
    width: 40px;
    height: 40px;
    font-size: 20px;
    margin-bottom: 6px;
  }

  .name {
    font-size: 16px;
  }

  .title {
    font-size: 11px;
  }

  .sidebar-section {
    margin-bottom: 16px;
  }

  .sidebar-title {
    font-size: 13px;
  }

  .contact-item {
    font-size: 10px;
  }

  .skill-name {
    font-size: 11px;
  }

  .skill-dots {
    gap: 3px;
  }

  .dot {
    width: 8px;
    height: 8px;
  }

  .edu-item {
    margin-bottom: 8px;
    padding-bottom: 6px;
  }

  .edu-degree {
    font-size: 11px;
  }

  .edu-school {
    font-size: 10px;
  }

  .edu-major {
    font-size: 10px;
  }

  .edu-date {
    font-size: 9px;
  }

  .main {
    padding: 16px 12px;
  }

  .section-title {
    font-size: 16px;
  }

  .title-icon {
    font-size: 18px;
  }

  .item {
    padding-left: 8px;
    border-left: 2px solid #e8e8e8;
  }

  .item-header {
    gap: 4px;
  }

  .item-position {
    font-size: 13px;
  }

  .item-company {
    font-size: 12px;
  }

  .item-date {
    font-size: 10px;
  }

  .item-description {
    font-size: 11px;
  }
}
</style>
