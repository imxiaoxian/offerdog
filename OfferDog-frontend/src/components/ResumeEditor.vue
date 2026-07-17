<template>
  <div class="resume-editor">
    <a-card class="template-card" :bordered="false">
      <template #title>
        <div class="card-header">
          <AppstoreOutlined />
          <span>选择模板</span>
        </div>
      </template>
      <a-radio-group v-model:value="resumeStore.currentTemplate" button-style="solid" size="large">
        <a-radio-button value="modern">现代简洁</a-radio-button>
        <a-radio-button value="classic">经典传统</a-radio-button>
        <a-radio-button value="creative">创意时尚</a-radio-button>
      </a-radio-group>
    </a-card>

    <a-card class="section-card" :bordered="false">
      <template #title>
        <div class="card-header">
          <UserOutlined />
          <span>个人信息</span>
        </div>
      </template>
      <a-form layout="vertical" :model="resumeStore.resumeData.personalInfo">
        <a-form-item label="姓名">
          <a-input v-model:value="resumeStore.resumeData.personalInfo.name" placeholder="请输入姓名" allow-clear />
        </a-form-item>
        <a-form-item label="职位">
          <a-input v-model:value="resumeStore.resumeData.personalInfo.title" placeholder="请输入求职职位" allow-clear />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="电话">
              <a-input v-model:value="resumeStore.resumeData.personalInfo.phone" placeholder="联系电话" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="resumeStore.resumeData.personalInfo.email" placeholder="电子邮箱" allow-clear />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="地址">
          <a-input v-model:value="resumeStore.resumeData.personalInfo.location" placeholder="所在城市" allow-clear />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="resumeStore.resumeData.personalInfo.summary"
            :rows="4"
            placeholder="请输入个人简介"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="section-card" :bordered="false">
      <template #title>
        <div class="card-header">
          <ReadOutlined />
          <span>教育经历</span>
          <a-button type="primary" size="small" @click="addEducation" class="add-btn">
            <template #icon>
              <PlusOutlined />
            </template>
            添加
          </a-button>
        </div>
      </template>
      <div v-for="(edu, index) in resumeStore.resumeData.education" :key="edu.id" class="list-item">
        <div class="item-header">
          <span class="item-title">教育经历 {{ index + 1 }}</span>
          <a-button type="text" danger size="small" @click="deleteEducation(edu.id)">
            <template #icon>
              <DeleteOutlined />
            </template>
            删除
          </a-button>
        </div>
        <a-form layout="vertical" :model="edu">
          <a-form-item label="学校">
            <a-input v-model:value="edu.school" placeholder="学校名称" allow-clear />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="专业">
                <a-input v-model:value="edu.major" placeholder="专业" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="学历">
                <a-select v-model:value="edu.degree" placeholder="选择学历">
                  <a-select-option value="专科">专科</a-select-option>
                  <a-select-option value="本科">本科</a-select-option>
                  <a-select-option value="硕士">硕士</a-select-option>
                  <a-select-option value="博士">博士</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="开始时间">
                <a-input v-model:value="resumeStore.resumeData.education[index].startDate" placeholder="2019-09" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="结束时间">
                <a-input v-model:value="resumeStore.resumeData.education[index].endDate" placeholder="2026-06" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="描述">
            <a-textarea v-model:value="resumeStore.resumeData.education[index].description" :rows="3" placeholder="主要课程、成绩等" allow-clear />
          </a-form-item>
        </a-form>
      </div>
    </a-card>

    <a-card class="section-card" :bordered="false">
      <template #title>
        <div class="card-header">
          <BuildOutlined />
          <span>工作经历</span>
          <a-button type="primary" size="small" @click="addWork" class="add-btn">
            <template #icon>
              <PlusOutlined />
            </template>
            添加
          </a-button>
        </div>
      </template>
      <div v-for="(work, index) in resumeStore.resumeData.workExperience" :key="work.id" class="list-item">
        <div class="item-header">
          <span class="item-title">工作经历 {{ index + 1 }}</span>
          <a-button type="text" danger size="small" @click="deleteWork(work.id)">
            <template #icon>
              <DeleteOutlined />
            </template>
            删除
          </a-button>
        </div>
        <a-form layout="vertical" :model="work">
          <a-form-item label="公司">
            <a-input v-model:value="work.company" placeholder="公司名称" allow-clear />
          </a-form-item>
          <a-form-item label="职位">
            <a-input v-model:value="work.position" placeholder="职位名称" allow-clear />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="开始时间">
                <a-input v-model:value="resumeStore.resumeData.workExperience[index].startDate" placeholder="2024-07" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="结束时间">
                <a-input v-model:value="resumeStore.resumeData.workExperience[index].endDate" placeholder="至今" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="工作描述">
            <a-textarea v-model:value="resumeStore.resumeData.workExperience[index].description" :rows="5" placeholder="工作内容、业绩等，每行一条" allow-clear />
          </a-form-item>
        </a-form>
      </div>
    </a-card>

    <a-card class="section-card" :bordered="false">
      <template #title>
        <div class="card-header">
          <ProjectOutlined />
          <span>项目经历</span>
          <a-button type="primary" size="small" @click="addProject" class="add-btn">
            <template #icon>
              <PlusOutlined />
            </template>
            添加
          </a-button>
        </div>
      </template>
      <div v-for="(project, index) in resumeStore.resumeData.projects" :key="project.id" class="list-item">
        <div class="item-header">
          <span class="item-title">项目经历 {{ index + 1 }}</span>
          <a-button type="text" danger size="small" @click="deleteProject(project.id)">
            <template #icon>
              <DeleteOutlined />
            </template>
            删除
          </a-button>
        </div>
        <a-form layout="vertical" :model="project">
          <a-form-item label="项目名称">
            <a-input v-model:value="project.name" placeholder="项目名称" allow-clear />
          </a-form-item>
          <a-form-item label="项目角色">
            <a-input v-model:value="project.role" placeholder="你的角色" allow-clear />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="开始时间">
                <a-input v-model:value="resumeStore.resumeData.projects[index].startDate" placeholder="2026-03" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="结束时间">
                <a-input v-model:value="resumeStore.resumeData.projects[index].endDate" placeholder="2026-12" allow-clear />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="项目描述">
            <a-textarea v-model:value="resumeStore.resumeData.projects[index].description" :rows="5" placeholder="项目职责、成果等" allow-clear />
          </a-form-item>
        </a-form>
      </div>
    </a-card>

    <a-card class="section-card" :bordered="false">
      <template #title>
        <div class="card-header">
          <ToolOutlined />
          <span>专业技能</span>
          <a-button type="primary" size="small" @click="addSkill" class="add-btn">
            <template #icon>
              <PlusOutlined />
            </template>
            添加
          </a-button>
        </div>
      </template>
      <div v-for="(skill, index) in resumeStore.resumeData.skills" :key="skill.id" class="list-item">
        <div class="item-header">
          <span class="item-title">技能 {{ index + 1 }}</span>
          <a-button type="text" danger size="small" @click="deleteSkill(skill.id)">
            <template #icon>
              <DeleteOutlined />
            </template>
            删除
          </a-button>
        </div>
        <a-form layout="vertical" :model="skill">
          <a-form-item label="技能名称">
            <a-input v-model:value="resumeStore.resumeData.skills[index].name" placeholder="技能名称" allow-clear />
          </a-form-item>
          <a-form-item :label="`熟练度${resumeStore.resumeData.skills[index].level}%`">
            <a-slider v-model:value="resumeStore.resumeData.skills[index].level" :min="0" :max="100" />
          </a-form-item>
        </a-form>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { AppstoreOutlined, UserOutlined, ReadOutlined, BuildOutlined, ProjectOutlined, ToolOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useResumeStore } from '../stores/resume'

const resumeStore = useResumeStore()

const addEducation = () => {
  resumeStore.resumeData.education.push({
    id: Date.now().toString(),
    school: '',
    major: '',
    degree: '本科',
    startDate: '',
    endDate: '',
    description: ''
  })
}

const deleteEducation = (id: string) => {
  const index = resumeStore.resumeData.education.findIndex(item => item.id === id)
  if (index > -1) {
    resumeStore.resumeData.education.splice(index, 1)
  }
}

const addWork = () => {
  resumeStore.resumeData.workExperience.push({
    id: Date.now().toString(),
    company: '',
    position: '',
    startDate: '',
    endDate: '',
    description: ''
  })
}

const deleteWork = (id: string) => {
  const index = resumeStore.resumeData.workExperience.findIndex(item => item.id === id)
  if (index > -1) {
    resumeStore.resumeData.workExperience.splice(index, 1)
  }
}

const addProject = () => {
  resumeStore.resumeData.projects.push({
    id: Date.now().toString(),
    name: '',
    role: '',
    startDate: '',
    endDate: '',
    description: ''
  })
}

const deleteProject = (id: string) => {
  const index = resumeStore.resumeData.projects.findIndex(item => item.id === id)
  if (index > -1) {
    resumeStore.resumeData.projects.splice(index, 1)
  }
}

const addSkill = () => {
  resumeStore.resumeData.skills.push({
    id: Date.now().toString(),
    name: '',
    level: 70
  })
}

const deleteSkill = (id: string) => {
  const index = resumeStore.resumeData.skills.findIndex(item => item.id === id)
  if (index > -1) {
    resumeStore.resumeData.skills.splice(index, 1)
  }
}
</script>

<style scoped>
.resume-editor {
  width: 100%;
}

.template-card,
.section-card {
  margin-bottom: 20px;
  border-radius: 18px;
  box-shadow: 0 16px 40px rgba(15, 65, 140, 0.08);
}

.section-card:last-of-type {
  margin-bottom: 0;
}

.template-card :deep(.ant-card-body),
.section-card :deep(.ant-card-body) {
  padding: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #0b1a3c;
}

.card-header :deep(.anticon) {
  color: #1677ff;
}

.add-btn {
  margin-left: auto;
}

.list-item {
  padding: 20px;
  background: #f7fbff;
  border-radius: 12px;
  margin-bottom: 16px;
  border: 1px solid #e3edff;
}

.list-item:last-child {
  margin-bottom: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e3edff;
}

.item-title {
  font-weight: 600;
  color: #1f2a44;
}

:deep(.ant-form-item) {
  margin-bottom: 16px;
}

:deep(.ant-radio-group-solid .ant-radio-button-checked:not(.ant-radio-button-disabled)) {
  background: #1677ff;
  border-color: #1677ff;
  color: #fff;
}

:deep(.ant-radio-button-wrapper) {
  min-width: 120px;
  text-align: center;
}

@media (max-width: 768px) {
  .template-card,
  .section-card {
    margin-bottom: 16px;
    border-radius: 12px;
    box-shadow: 0 8px 20px rgba(15, 65, 140, 0.06);
  }

  .template-card :deep(.ant-card-body),
  .section-card :deep(.ant-card-body) {
    padding: 16px;
  }

  .card-header {
    font-size: 14px;
  }

  :deep(.ant-form-item) {
    margin-bottom: 12px;
  }

  :deep(.ant-form-item-label) {
    font-size: 13px;
  }

  :deep(.ant-input),
  :deep(.ant-select),
  :deep(.ant-textarea) {
    font-size: 14px;
  }

  :deep(.ant-btn) {
    font-size: 12px;
    padding: 2px 8px;
  }

  .list-item {
    padding: 12px;
    margin-bottom: 12px;
    border-radius: 8px;
  }

  .item-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .add-btn {
    width: 100%;
  }

  :deep(.ant-radio-group-solid .ant-radio-button-wrapper) {
    min-width: 80px;
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .template-card,
  .section-card {
    margin-bottom: 12px;
    border-radius: 8px;
  }

  .template-card :deep(.ant-card-body),
  .section-card :deep(.ant-card-body) {
    padding: 12px;
  }

  .card-header {
    font-size: 13px;
  }

  :deep(.ant-form-item) {
    margin-bottom: 8px;
  }

  :deep(.ant-input),
  :deep(.ant-select),
  :deep(.ant-textarea) {
    font-size: 13px;
  }

  .list-item {
    padding: 8px;
    margin-bottom: 8px;
  }

  .item-header {
    gap: 6px;
  }

  :deep(.ant-radio-group-solid .ant-radio-button-wrapper) {
    min-width: 70px;
    font-size: 11px;
    padding: 0 4px;
  }
}
</style>
