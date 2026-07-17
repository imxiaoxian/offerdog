// AI面试相关的类型定义

export interface InterviewTemplate {
  id: string
  templateName: string
  description: string
  difficultyLevel: 'junior' | 'mid' | 'senior' | 'high_senior'
  estimatedDurationMinutes: number
  category: string
  systemPrompt: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface YearMonth {
  year?: number
  month?: number
}

export interface ResumeLocation {
  province?: string
  city?: string
}

export interface ResumeEducation {
  level?: string
  school?: string
  major?: string
  graduationDate?: YearMonth
}

export interface ResumeContact {
  phone?: string
  email?: string
  homepage?: string
}

export interface ResumeExperience {
  company?: string
  title?: string
  startDate?: YearMonth
  endDate?: YearMonth
  description?: string
}

export interface ResumeProject {
  name?: string
  role?: string
  description?: string
  highlights?: string[]
}

export interface ResumeWork {
  firstEmployment?: YearMonth
  experience?: ResumeExperience[]
  projects?: ResumeProject[]
}

export interface UserResume {
  name?: string
  birth?: string
  gender?: string
  location?: ResumeLocation
  education?: ResumeEducation
  contact?: ResumeContact
  work?: ResumeWork
  skills?: string[]
  jobIntention?: string
  selfEvaluation?: string
  resumeFileUrl?: string
}

export interface QuestionInfo {
  question_id: string
  topic: string
  initial_question: string
  candidate_project?: string
  candidate_role?: string
  difficulty?: string
  status: 'pending' | 'current' | 'completed'
  depth_level: number
  max_depth: number
  follow_up_count: number
  start_time?: string
  end_time?: string
}

export interface InterviewPlan {
  total_questions: number
  current_question_index: number
  questions: QuestionInfo[]
}

export interface SessionMetadata {
  resume_name?: string
  resume_level?: string
  work_years?: number
  technical_stack?: string[]
  focus_points?: string[]
}

export interface InterviewSession {
  sessionId: string
  userId: number
  templateId: string
  planTemplateId: string
  status: 'in_progress' | 'completed' | 'cancelled'
  interviewPlan: InterviewPlan
  metadata?: SessionMetadata
  currentQuestionIndex: number
  questionsCompleted: number
  totalQuestions: number
  totalMessagesCount: number
  startTime: string
  endTime?: string
  expectedEndTime: string
  durationMinutes?: number
  createdAt?: string
}

export interface ConversationMessage {
  id?: number
  sessionId: string
  role: 'interviewer' | 'candidate'
  content: string
  relatedQuestionId: string
  sequenceNumber: number
  tokenCount?: number
  timestamp: string
}

export enum ActionType {
  CONTINUE = 'CONTINUE',
  FOLLOW_UP = 'FOLLOW_UP',
  NEXT_QUESTION = 'NEXT_QUESTION',
  END_INTERVIEW = 'END_INTERVIEW',
}

export interface AIResponse {
  content: string
  action: ActionType
  currentQuestionIndex: number
  completed: boolean
  sequenceNumber?: number
}

export interface CreateSessionRequest {
  templateId: string
  userId: number
}

export interface AnswerRequest {
  sessionId: string
  questionId: string
  answer: string
}

export interface DimensionScores {
  technical_knowledge: number
  problem_solving: number
  system_design: number
  communication: number
  practical_experience: number
  learning_ability: number
}

export interface QuestionAnalysis {
  question_id: string
  question: string
  answer_summary: string
  score: number
  time_spent_minutes: number
  follow_up_count: number
  feedback: string
  key_points_covered: string[]
  key_points_missed: string[]
  resume_consistency: string
}

export interface SkillAssessment {
  [skillName: string]: number
}

export interface SkillGapAnalysis {
  [skillName: string]: string
}

export interface ResumeVerification {
  overall_consistency: string
  verified_claims: string[]
  questionable_claims: string[]
  skill_gap_analysis: SkillGapAnalysis
}

export interface InterviewLevelMatch {
  applied_level: string
  evaluated_level: string
  match: boolean
  suggested_level: string
  reasoning: string
}

export interface HiringRecommendation {
  decision: string
  confidence: string
  suitable_positions: string[]
  compensation_suggestion: string
  onboarding_focus: string[]
}

/** 大模型对回答内容的结构化评价 */
export interface ContentAnalysis {
  technical_correctness: number
  knowledge_depth: number
  logical_rigor: number
  job_fit: number
  highlights?: string
  gaps?: string
}

/** 基于文本与消息统计的表达推断（非声学实测） */
export interface ExpressionAnalysis {
  pace_score: number
  clarity_score: number
  confidence_score: number
  emotional_stability_score: number
  evidence_from_text?: string
  caveat?: string
}

export interface PracticePlan {
  focus_summary: string
  weekly_schedule: string[]
  drills: string[]
}

export interface InterviewReportContent {
  overall_score: number
  pass_status: boolean
  confidence_level: string
  summary: string
  strengths: string[]
  weaknesses: string[]
  dimension_scores: DimensionScores
  /** 新版本报告字段，旧报告可能缺失 */
  content_analysis?: ContentAnalysis
  expression_analysis?: ExpressionAnalysis
  practice_plan?: PracticePlan
  question_analysis: QuestionAnalysis[]
  skill_assessment: SkillAssessment
  resume_verification: ResumeVerification
  recommendations: string[]
  interview_level_match: InterviewLevelMatch
  hiring_recommendation: HiringRecommendation
}

export interface InterviewReport {
  id: number
  sessionId: string
  overallScore: number
  passStatus: boolean
  confidenceLevel: string
  reportContent: InterviewReportContent
  reportText?: string
  reportHtml?: string
  aiModel: string
  generationTokensUsed: number
  generationDurationMs: number
  reviewStatus: string
  reviewerId?: number
  reviewNotes?: string
  createdAt: string
  updatedAt: string
}

export interface SessionStatus {
  sessionId: string
  status: string
  currentQuestionIndex: number
  totalQuestions: number
  questionsCompleted: number
  elapsedMinutes: number
  expectedEndTime: string
}

export interface WebSocketMessage {
  type: 'progress' | 'ai_response' | 'ai_complete' | 'report_ready' | 'error'
  data: unknown
}

export type ReportGenerationStatus = 'QUEUED' | 'PROCESSING' | 'SUCCESS' | 'FAILED'

export interface ReportGenerationEvent {
  sessionId: string
  reportId?: string
  userId?: number
  status: ReportGenerationStatus
  message?: string
  timestamp?: number
}

export type AudioEventType = 'START' | 'CHUNK' | 'COMPLETE' | 'ERROR'

export interface AudioStreamEvent {
  sessionId: string
  sequenceNumber: number
  type: AudioEventType
  voice?: string
  audioFormat?: string
  text?: string
  message?: string
  timestamp: number
  chunkIndex?: number
  base64Audio?: string
}

export interface RecommendedQuestion {
  id: number
  content: string
  difficulty: string
  tags: string[]
  matchedPoint?: string
  similarityScore?: number
}

export interface QuestionRecommendationRsp {
  sessionId: string
  reason: string
  weakPoints: string[]
  recommendedQuestions: RecommendedQuestion[]
  generatedAt: string
}

/** 单次已生成报告的快照，用于成长曲线 */
export interface InterviewGrowthPoint {
  sessionId: string
  reportCreatedAt: string
  overallScore: number
  technicalKnowledge: number | null
  communication: number | null
  problemSolving: number | null
  systemDesign?: number | null
  practicalExperience?: number | null
  learningAbility?: number | null
}

export interface KnowledgeSnippet {
  title: string
  kbCategory: string
  excerpt: string
  sourceHint: string
  matchedWeakPoint: string
  relevanceScore: number | null
}

export interface ExternalLearningLink {
  title: string
  url: string
  description: string
  topicTag: string
}

export interface PracticePlanDayGenerated {
  day: number
  theme: string
  tasks: string[]
}

export interface PracticePlanGenerated {
  goal: string
  days: PracticePlanDayGenerated[]
}

/** 后端聚合：知识片段 + 外链 + AI 周计划 + 报告内练习计划 + 推荐题 */
export interface InterviewLearningPack {
  sessionId: string
  weakPoints: string[]
  summaryReason: string
  reportPracticePlan?: PracticePlan | Record<string, unknown> | null
  generatedPlan: PracticePlanGenerated
  knowledgeSnippets: KnowledgeSnippet[]
  /** 面试题库向量检索片段（与 knowledgeSnippets 结构相同） */
  questionSnippets?: KnowledgeSnippet[]
  externalLinks: ExternalLearningLink[]
  recommendedQuestions: RecommendedQuestion[]
  generatedAt: string
}
