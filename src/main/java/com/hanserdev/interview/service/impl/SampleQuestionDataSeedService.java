package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserdev.interview.domain.dataobject.CategoryDO;
import com.hanserdev.interview.domain.dataobject.QuestionBankDO;
import com.hanserdev.interview.domain.dataobject.QuestionDO;
import com.hanserdev.interview.domain.mapper.CategoryMapper;
import com.hanserdev.interview.domain.mapper.QuestionBankMapper;
import com.hanserdev.interview.domain.mapper.QuestionMapper;
import com.hanserdev.interview.enums.QuestionDifficultyEnum;
import com.hanserdev.interview.enums.QuestionSourceEnum;
import com.hanserdev.interview.model.dto.question.QuestionStatsDTO;
import com.hanserdev.interview.service.QuestionVectorService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 预置示例题库与题目，便于前端展示与面试对话 RAG（题库向量 doc_type=question）。
 * 官方示例题 {@code created_by} 为空，无需先有注册用户即可写入。
 */
@Slf4j
@Service
public class SampleQuestionDataSeedService {

    private static final String SAMPLE_BANK_NAME = "示例题库";

    private static final List<SampleQuestionSpec> SPECS = List.of(
            new SampleQuestionSpec(
                    "Vue 3 的响应式原理与 Vue 2 有哪些主要区别？",
                    "Vue3 使用 Proxy 劫持整个对象，懒代理、嵌套对象按需；Vue2 使用 Object.defineProperty，需递归且无法检测数组下标等。Vue3 组合式 API、更好的 TS 支持。",
                    "可追问 ref/reactive、effect 与依赖收集。",
                    QuestionDifficultyEnum.MEDIUM,
                    new String[]{"Vue", "前端"},
                    "前端"),
            new SampleQuestionSpec(
                    "浏览器同源策略是什么？有哪些常见跨域方案？",
                    "协议/域名/端口一致为同源。方案：CORS、反向代理、JSONP（不推荐新场景）、postMessage 等。",
                    "结合 credentials、预检请求追问。",
                    QuestionDifficultyEnum.EASY,
                    new String[]{"浏览器", "跨域"},
                    "前端"),
            new SampleQuestionSpec(
                    "如何设计一个高并发下的接口幂等？",
                    "业务唯一键、Token、去重表、状态机；分布式锁或数据库唯一约束；考虑超时与重试风暴。",
                    "可结合支付场景。",
                    QuestionDifficultyEnum.HARD,
                    new String[]{"幂等", "并发"},
                    "后端"),
            new SampleQuestionSpec(
                    "Redis 持久化 RDB 与 AOF 的区别与选型？",
                    "RDB 快照恢复快、可能丢数据；AOF 日志更持久、文件更大；混合持久化折中。",
                    "追问 fsync 策略。",
                    QuestionDifficultyEnum.MEDIUM,
                    new String[]{"Redis", "持久化"},
                    "后端"),
            new SampleQuestionSpec(
                    "Kubernetes 中 Pod、Deployment、Service 的关系？",
                    "Pod 是最小调度单元；Deployment 管理副本与滚动升级；Service 提供稳定访问与负载均衡。",
                    "可问探针与 HPA。",
                    QuestionDifficultyEnum.MEDIUM,
                    new String[]{"K8s", "运维"},
                    "运维"),
            new SampleQuestionSpec(
                    "Docker 与虚拟机的大致区别？",
                    "容器共享宿主机内核、更轻量；虚拟机带完整 OS、隔离更强、资源占用更大。",
                    "追问 namespace/cgroup。",
                    QuestionDifficultyEnum.EASY,
                    new String[]{"Docker", "容器"},
                    "运维"),
            new SampleQuestionSpec(
                    "接口测试与单元测试的关注点有何不同？",
                    "单测关注函数/模块内部逻辑与边界；接口测试关注契约、状态码、字段与集成行为。",
                    "可提契约测试。",
                    QuestionDifficultyEnum.EASY,
                    new String[]{"测试", "接口"},
                    "测试"),
            new SampleQuestionSpec(
                    "简述自动化测试金字塔。",
                    "底层大量单元测试、中层服务/接口测试、上层少量 UI/E2E；成本与稳定性权衡。",
                    "结合团队实践追问。",
                    QuestionDifficultyEnum.MEDIUM,
                    new String[]{"自动化", "测试"},
                    "测试"),
            new SampleQuestionSpec(
                    "过拟合与欠拟合分别是什么？如何缓解过拟合？",
                    "过拟合：训练好泛化差；欠拟合：模型容量不足。缓解：正则、Dropout、更多数据、早停、交叉验证等。",
                    "可追问偏差方差分解。",
                    QuestionDifficultyEnum.MEDIUM,
                    new String[]{"机器学习", "算法"},
                    "算法"),
            new SampleQuestionSpec(
                    "技术方案评审时你会关注哪些维度？",
                    "需求与边界、架构与扩展性、风险与回滚、监控与运维成本、安全与合规、排期与里程碑。",
                    "管理向追问。",
                    QuestionDifficultyEnum.MEDIUM,
                    new String[]{"方案", "管理"},
                    "管理"),
            new SampleQuestionSpec(
                    "如何推动跨团队技术债务治理？",
                    "盘点与分级、与业务目标对齐、OKR/专项、度量（缺陷率/交付周期）、小步重构与 feature toggle。",
                    "可追问冲突处理。",
                    QuestionDifficultyEnum.HARD,
                    new String[]{"技术债务", "管理"},
                    "管理"),
            new SampleQuestionSpec(
                    "RESTful 设计里 PUT 与 PATCH 的常见区别？",
                    "PUT 常表示整体替换；PATCH 表示部分更新（语义依实现）。需说明幂等与版本控制。",
                    "可结合 HTTP 幂等性。",
                    QuestionDifficultyEnum.EASY,
                    new String[]{"HTTP", "REST"},
                    "后端")
    );

    @Resource
    private CategoryDefaultSeedService categoryDefaultSeedService;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private QuestionBankMapper questionBankMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionVectorService questionVectorService;

    @Transactional(rollbackFor = Exception.class)
    public void seedIfNeeded() {
        // 与 CategoryDefaultSeedRunner 顺序无关：空库时先保证有二级岗位，否则下方直接 return 导致题库一直为空
        categoryDefaultSeedService.seedIfNoLevel2Categories();

        List<CategoryDO> jobs = categoryMapper.selectList(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getLevel, 2)
                .isNull(CategoryDO::getDeletedAt));
        if (jobs.isEmpty()) {
            log.warn("无二级岗位分类，无法创建示例题库（请先初始化 categories）");
            return;
        }

        QuestionBankDO existingBank = questionBankMapper.selectOne(new LambdaQueryWrapper<QuestionBankDO>()
                .eq(QuestionBankDO::getName, SAMPLE_BANK_NAME)
                .isNull(QuestionBankDO::getDeletedAt)
                .last("LIMIT 1"));
        final Long bankId;
        if (existingBank == null) {
            bankId = insertBank(jobs.getFirst().getId());
            log.info("已创建官方示例题库 bankId={}（created_by 为空，所有登录用户可见）", bankId);
        } else {
            bankId = existingBank.getId();
        }

        long qCount = questionMapper.selectCount(new LambdaQueryWrapper<QuestionDO>()
                .eq(QuestionDO::getBankId, bankId)
                .isNull(QuestionDO::getDeletedAt));
        if (qCount > 0) {
            log.info("示例题库「{}」已有 {} 道题目，跳过示例题写入", SAMPLE_BANK_NAME, qCount);
            return;
        }

        int indexed = 0;
        for (SampleQuestionSpec spec : SPECS) {
            Long categoryId = pickCategoryId(jobs, spec.bucketHint());
            QuestionDO q = new QuestionDO();
            q.setBankId(bankId);
            q.setCategoryId(categoryId);
            q.setContent(spec.content());
            q.setAnswer(spec.answer());
            q.setTips(spec.tips());
            q.setDifficulty(spec.difficulty());
            q.setSource(QuestionSourceEnum.OFFICIAL);
            q.setTags(spec.tags());
            QuestionStatsDTO stats = new QuestionStatsDTO();
            q.setStats(stats);
            q.setCreatedBy(null);
            q.setRemark("sample_seed");
            questionMapper.insert(q);
            try {
                questionVectorService.addQuestion(q.getId());
                indexed++;
            } catch (Exception e) {
                log.warn("示例题目向量化失败 questionId={}: {}", q.getId(), e.toString());
            }
        }
        log.info("示例题库已创建 bankId={}，共 {} 道题，向量化成功 {} 道", bankId, SPECS.size(), indexed);
    }

    private Long insertBank(Long anyJobCategoryId) {
        QuestionBankDO bank = new QuestionBankDO();
        bank.setName(SAMPLE_BANK_NAME);
        bank.setDescription("系统预置示例题目：列表展示、PgVector 题库检索与面试 RAG。");
        bank.setCategoryId(anyJobCategoryId);
        bank.setCreatedBy(null);
        questionBankMapper.insert(bank);
        return bank.getId();
    }

    /**
     * 按大类提示匹配岗位名称；无法匹配时取第一个二级岗位。
     */
    private static Long pickCategoryId(List<CategoryDO> jobs, String bucketHint) {
        String hint = bucketHint == null ? "" : bucketHint;
        return switch (hint) {
            case "前端" -> findFirstNameContaining(jobs, "前端")
                    .or(() -> findFirstNameContaining(jobs, "Web"))
                    .orElse(jobs.getFirst().getId());
            case "后端" -> findFirstNameContaining(jobs, "后端")
                    .or(() -> findFirstNameContaining(jobs, "Java"))
                    .orElse(jobs.getFirst().getId());
            case "运维" -> findFirstNameContaining(jobs, "运维")
                    .or(() -> findFirstNameContaining(jobs, "DevOps"))
                    .orElse(jobs.getFirst().getId());
            case "测试" -> findFirstNameContaining(jobs, "测试")
                    .or(() -> findFirstNameContaining(jobs, "QA"))
                    .orElse(jobs.getFirst().getId());
            case "算法" -> findFirstNameContaining(jobs, "算法")
                    .or(() -> findFirstNameContaining(jobs, "数据"))
                    .orElse(jobs.getFirst().getId());
            case "管理" -> findFirstNameContaining(jobs, "管理")
                    .or(() -> findFirstNameContaining(jobs, "总监"))
                    .orElse(jobs.getFirst().getId());
            default -> jobs.getFirst().getId();
        };
    }

    private static java.util.Optional<Long> findFirstNameContaining(List<CategoryDO> jobs, String sub) {
        return jobs.stream()
                .filter(c -> c.getName() != null && c.getName().contains(sub))
                .map(CategoryDO::getId)
                .findFirst();
    }

    private record SampleQuestionSpec(
            String content,
            String answer,
            String tips,
            QuestionDifficultyEnum difficulty,
            String[] tags,
            String bucketHint
    ) {
    }
}
