package com.hanserdev.interview.constants;

/**
 * AI提示词常量类
 * 集中管理所有AI相关的提示词模板
 */
public class AIPromptConstants {

    /**
     * 简历分析提示词模板
     */
    public static final String RESUME_ANALYSIS_PROMPT = """
            请分析以下候选人简历,提取关键信息用于定制化面试。
            
            候选人信息:
            姓名: %s
            教育背景: %s - %s - %s
            工作年限: %d年
            当前/最近职位: %s @ %s
            技能栈: %s
            求职意向: %s
            
            工作经历:
            %s
            
            项目经历:
            %s
            
            请以JSON格式返回:
            {
                "workYears": 5,
                "currentLevel": "high_senior",
                "technicalStack": ["Python", "GoLang", "Kafka"],
                "strongAreas": ["分布式系统", "高并发"],
                "weakAreas": ["前端技术", "算法竞赛"],
                "interviewFocusPoints": [
                    "实时推荐系统的架构设计",
                    "Flink流处理实战经验",
                    "大规模分布式系统挑战"
                ],
                "suggestedQuestions": [
                    "请详细介绍实时推荐系统的技术架构",
                    "如何优化Flink任务的性能"
                ]
            }
            
            注意:
            1. currentLevel从以下选择: junior, mid, senior, high_senior
            2. 工作年限根据firstEmployment计算
            3. 重点关注候选人的核心优势和技术深度
            4. 建议的问题要具体且针对其项目经验
            """;

    /**
     * 个性化开场白提示词模板
     */
    public static final String OPENING_PROMPT_TEMPLATE = """
            %s
            
            ## 候选人信息
            - 姓名: %s
            - 工作年限: %d年
            - 技术水平: %s
            - 技术栈: %s
            - 重点考察方向: %s
            
            ## 面试说明
            - 总问题数: %d
            - 预计时长: %d分钟
            - 第一个问题: %s
            
            ## 开场要求
            1. 称呼候选人的名字,让对方感觉亲切
            2. 简单提及你看到了他的简历(如"我看到您在某某公司有丰富的XX经验")
            3. 介绍面试流程和时长
            4. 自然地引出第一个问题
            5. 保持专业但友好的语气
            6. 控制在3-4句话以内
            
            请直接生成开场白,不要有任何前缀或标记。
            """;

    /**
     * 对话响应提示词模板
     */
    public static final String CONVERSATION_PROMPT_TEMPLATE = """
            %s
            
            ## 候选人背景
            - 姓名: %s
            - 工作年限: %d年
            - 技术水平: %s
            - 核心技能: %s
            %s
            
            ## 知识库与题库参考（RAG）
            %s
            
            ## 当前面试状态
            - 进度: 第%d题 / 共%d题
            - 已完成: %d题
            - 面试时长: %d分钟
            
            ## 当前问题详情
            - 问题ID: %s
            - 主题: %s
            - 初始问题: %s
            - 当前深度: %d / %d
            - 已追问次数: %d
            
            ## 你需要做的判断
            
            请根据候选人的最新回答,评估以下几点:
            
            1. **回答质量评估**
               - 是否直接回答了问题?
               - 技术深度是否足够?(考虑候选人是%s级别)
               - 是否结合了实际项目经验?
               - 逻辑是否清晰?
               - 有没有明显的知识盲区?
            
            2. **决定下一步动作**(必须在回复开头用标记明确指出)
            
               **[ACTION:FOLLOW_UP]** - 继续深挖当前问题
               条件:
               - 回答不够深入 或 缺少关键细节
               - 当前深度 < 最大深度(%d)
               - 候选人有潜力回答得更好
            
               动作: 提出一个更深入的追问,可以是:
               - 要求举实际例子
               - 询问实现细节
               - 探讨极端场景的处理
               - 追问性能优化方案
            
               **[ACTION:NEXT_QUESTION]** - 切换到下一个问题
               条件:
               - 回答已经充分,涵盖了关键点
               - 或者追问已达上限
               - 候选人在这个问题上已展现实力
            
               动作: 简短点评当前回答(1-2句),然后提出下一题
               下一题信息: %s
            
               **[ACTION:END_INTERVIEW]** - 结束面试
               条件:
               - 所有问题已完成
               - 或距离预计结束时间不足5分钟
            
               动作: 感谢候选人,告知面试结束
            
            3. **保持个性化和自然**
               - 记住候选人叫"%s",偶尔称呼名字
               - 根据候选人的技术栈调整语言(比如他熟悉%s)
               - 如果候选人提到了简历中的项目,可以追问细节
               - 保持专业但友好的态度
               - 若回答中出现可深挖的关键词（技术名词、指标数字、组件/项目名），优先择其一追问，使对话像真人面试官一样跟随学生叙述
            
            ## 历史对话
            %s
            
            现在,请根据候选人的最新回答,做出判断并响应。
            记得在回复的最开头加上动作标记(如 [ACTION:FOLLOW_UP])!
            """;

    /**
     * 候选人项目背景模板（用于插入对话提示词）
     */
    public static final String PROJECT_CONTEXT_TEMPLATE = """
            
            ## 候选人相关经验
            - 项目: %s
            - 角色: %s
            """;

    /**
     * 报告生成提示词模板
     */
    public static final String REPORT_GENERATION_PROMPT_TEMPLATE = """
            你是一位资深的技术面试评估专家。请基于以下面试记录和候选人简历,生成一份专业、客观的评估报告。
            
            ## 面试基本信息
            - 面试类型: %s
            - 难度级别: %s
            - 面试时长: %d分钟
            - 总问题数: %d
            - 完成问题数: %d
            - 候选人背景: %s级别, %d年经验
            
            %s
            
            ## 候选人表达与互动统计（系统根据消息计算，供 expression_analysis 参考；非声学测量）
            %s
            
            ## 完整面试对话
            %s
            
            ## 问题列表
            %s
            
            ## 评估要求
            
            请综合考虑:
            1. 候选人简历中声称的技能和经验
            2. 面试中实际展现的技术深度和广度
            3. 简历真实性(是否言行一致)
            4. 问题解决能力和思维方式
            5. 沟通表达能力
            6. **内容分析**：分别评价技术正确性、知识深度、逻辑严谨性、与应聘岗位匹配度，写入 JSON 的 content_analysis（每项 0-10 分并附简短说明）
            7. **表达分析**：结合对话文本与上方统计数据，评价信息密度（近似「语速感」）、表达清晰度、自信度、情绪稳定倾向，写入 expression_analysis；必须在 caveat 中声明：评分基于文本与统计推断，非语音声学/情感计算模型实测
            8. **练习计划**：根据薄弱点给出可执行的 practice_plan（周安排与具体 drill）
            
            ## 输出格式
            
            **必须返回严格的JSON格式,不要包含markdown标记(如```json)。**
            
            JSON结构如下:
            
            {
                "overall_score": 8.5,
                "pass_status": true,
                "confidence_level": "high",
            
                "summary": "候选人展现出扎实的分布式系统开发能力,与简历描述基本一致。在推荐系统架构设计方面有深入理解,能够清晰阐述技术选型的理由。面试表现略高于简历预期,特别是在性能优化和问题排查方面展现了丰富的实战经验。",
            
                "strengths": [
                    "分布式系统架构设计能力强,能够从业务需求出发做技术选型",
                    "对核心技术有深入理解,实际项目经验丰富",
                    "性能优化意识强,能够举出多个实际优化案例",
                    "思维逻辑清晰,表达有条理",
                    "主动思考极端场景和边界情况"
                ],
            
                "weaknesses": [
                    "对某些理论基础略显薄弱",
                    "在算法优化方面的经验相对不足",
                    "回答时偶尔过于细节化,可以更注重整体架构思路",
                    "对新技术的关注度有待提升"
                ],
            
                "dimension_scores": {
                    "technical_knowledge": 8.5,
                    "problem_solving": 8.0,
                    "system_design": 9.0,
                    "communication": 7.5,
                    "practical_experience": 8.5,
                    "learning_ability": 7.5
                },
            
                "content_analysis": {
                    "technical_correctness": 8.5,
                    "knowledge_depth": 8.0,
                    "logical_rigor": 8.0,
                    "job_fit": 8.5,
                    "highlights": "技术判断总体准确，能结合生产场景举例。",
                    "gaps": "个别分布式概念边界可表述更严谨。"
                },
            
                "expression_analysis": {
                    "pace_score": 7.0,
                    "clarity_score": 7.5,
                    "confidence_score": 7.0,
                    "emotional_stability_score": 7.5,
                    "evidence_from_text": "回答多能分点展开；部分回答偏短但切题。",
                    "caveat": "基于对话文本与系统统计推断，非声学信号或专用情感模型评分。"
                },
            
                "practice_plan": {
                    "focus_summary": "一周内强化系统设计与一致性表述",
                    "weekly_schedule": [
                        "第1-2天：整理2个项目的架构图与数据流",
                        "第3-4天：各完成1道场景题口述录音",
                        "第5-7天：复习报告薄弱点并完成推荐题库练习"
                    ],
                    "drills": [
                        "限时8分钟口述「读多写少场景下的缓存设计」",
                        "写出三种缓存一致性方案并各举适用场景"
                    ]
                },
            
                "question_analysis": [
                    {
                        "question_id": "q1",
                        "question": "请介绍一下你负责的项目架构",
                        "answer_summary": "候选人详细介绍了架构设计和技术选型,展现了对分布式系统的深入理解。",
                        "score": 9.0,
                        "time_spent_minutes": 8.5,
                        "follow_up_count": 2,
                        "feedback": "回答非常出色,不仅讲清了架构设计,还主动分析了技术选型的trade-off。",
                        "key_points_covered": [
                            "系统整体架构",
                            "技术选型理由",
                            "性能优化方案",
                            "生产环境挑战"
                        ],
                        "key_points_missed": [
                            "灾难恢复方案",
                            "监控告警体系"
                        ],
                        "resume_consistency": "高度一致,简历中提到的项目在面试中得到了详细技术解释"
                    }
                ],
            
                "skill_assessment": {
                    "Java": 8.0,
                    "Spring": 8.5,
                    "MySQL": 8.0,
                    "Redis": 8.5,
                    "分布式系统": 8.5,
                    "系统设计": 9.0
                },
            
                "resume_verification": {
                    "overall_consistency": "高",
                    "verified_claims": [
                        "项目经验真实,技术细节可信",
                        "技术能力与简历描述匹配",
                        "工作经历真实可信"
                    ],
                    "questionable_claims": [
                        "简历提到的某些技术在面试中未深入展开"
                    ],
                    "skill_gap_analysis": {
                        "Java": "简历声称精通,实际表现为熟练使用(8/10)",
                        "分布式系统": "与简历描述一致,有丰富实战经验(8.5/10)"
                    }
                },
            
                "recommendations": [
                    "建议系统学习某某理论",
                    "可以深入研究某某技术",
                    "建议关注新技术栈",
                    "面试时可以更注重先讲整体思路,再展开技术细节"
                ],
            
                "interview_level_match": {
                    "applied_level": "高级工程师",
                    "evaluated_level": "高级工程师",
                    "match": true,
                    "suggested_level": "高级工程师",
                    "reasoning": "候选人技术能力扎实,符合高级工程师要求。"
                },
            
                "hiring_recommendation": {
                    "decision": "推荐录用",
                    "confidence": "high",
                    "suitable_positions": [
                        "高级后端工程师",
                        "分布式系统工程师"
                    ],
                    "compensation_suggestion": "根据市场行情,建议年薪范围: 40-60万",
                    "onboarding_focus": [
                        "补充相关理论知识",
                        "参与核心架构设计评审"
                    ]
                }
            }
            
            ## 评分标准
            - 0-4分: 不合格,基础知识缺失
            - 5-6分: 基本合格,但有明显短板
            - 7-8分: 良好,符合岗位要求
            - 8.5-9分: 优秀,超出预期
            - 9.5-10分: 卓越,行业顶尖水平
            
            ## 注意事项
            1. 评分要客观公正,避免过高或过低
            2. 优缺点要具体,举出面试中的实际例子
            3. 简历验证要基于事实,不要主观臆断
            4. 建议要有针对性和可操作性
            5. 整体评价要平衡候选人的强项和弱项
            
            现在,请生成评估报告(纯JSON,无markdown标记):
            """;

    /**
     * 简历信息模板（用于报告生成）
     */
    public static final String RESUME_INFO_TEMPLATE = """
            ## 候选人简历信息
            
            ### 基本信息
            - 姓名: %s
            - 工作年限: %d年
            - 教育背景: %s - %s - %s
            - 求职意向: %s
            
            ### 工作经历
            %s
            
            ### 项目经历
            %s
            
            ### 技能栈
            %s
            
            ### 自我评价
            %s
            """;

    /**
     * 根据薄弱点生成 7 日个性化练习计划（JSON）
     */
    public static final String PRACTICE_PLAN_GENERATION_PROMPT = """
            你是面试与职业辅导教练。根据候选人薄弱点，生成**7 天**可执行的练习计划。
            输出**仅**严格 JSON，不要 markdown 围栏，不要任何前缀或后缀说明文字。
            
            ## 薄弱点列表
            %s
            
            ## 面试报告摘要（可空）
            %s
            
            ## JSON 格式（必须可解析）
            {
              "goal": "一句话总目标",
              "days": [
                { "day": 1, "theme": "当天主题", "tasks": ["具体可执行任务1", "任务2"] }
              ]
            }
            
            ## 要求
            1. 必须恰好 7 个元素在 days 数组中，day 从 1 连续到 7
            2. 每天 tasks 2~4 条，动词开头，尽量可验证（如「写出」「口述」「刷题」「录音」）
            3. 合理分配：基础巩固、项目深挖、场景/系统设计、刷题、模拟面试、复盘等
            """;

    public static final String RESUME_PARSE_PROMPT_TEMPLATE = """
            你是一个专业的简历解析助手。请仔细阅读简历内容，提取关键信息并按照指定的JSON格式输出。
            
            要求：
            1. 严格按照JSON格式输出，不要添加任何额外的文字说明
            2. 如果某个字段信息不存在，使用null
            3. 日期格式：YYYY-MM-DD（如：1995-08-20）
            4. 年月格式：YYYY-MM（如：2020-07）
            5. 技能列表：提取所有技术栈、工具、语言等
            6. 最终必须输出**仅含一个元素的 JSON 数组**，形如 [ { ... } ]，与下方系统要求的数组格式一致
            
            JSON格式示例（注意外层是数组）：
            [{
              "name": "张三",
              "birth": "1995-08-20",
              "gender": "男",
              "location": {
                "province": "广东省",
                "city": "深圳市"
              },
              "education":[
              {
                "level": "本科",
                "school": "清华大学",
                "major": "计算机科学与技术",
                "graduationDate": "2020-07"
              },
              {
                "level": "硕士",
                "school": "北京大学",
                "major": "计算机科学与技术",
                "graduationDate": "2023-07"
              }
              ],
              "contact": {
                "phone": "+86-138-0013-8000",
                "email": "zhangsan@example.com",
                "homepage": "https://github.com/zhangsan"
              },
              "work": {
                "firstEmployment": "2020-08",
                "experience": [
                  {
                    "company": "字节跳动",
                    "title": "高级Java开发工程师",
                    "startDate": "2022-01",
                    "endDate": "2025-10",
                    "description": "负责推荐系统开发"
                  }
                ],
                "projects": [
                  {
                    "name": "推荐系统升级",
                    "role": "主要开发人员",
                    "description": "实现毫秒级推荐延迟",
                    "highlights": ["性能提升30%", "用户转化率提升5%"]
                  }
                ]
              },
              "skills": ["Java", "Python", "MySQL", "Redis"],
              "jobIntention": "高级Java开发工程师",
              "selfEvaluation": "5年后端开发经验，精通微服务架构"
            }]
            
            请直接输出 JSON 数组（如上仅含一个对象），不要包含```json```等标记：
            """;

    private AIPromptConstants() {
        // 工具类,禁止实例化
    }
}

