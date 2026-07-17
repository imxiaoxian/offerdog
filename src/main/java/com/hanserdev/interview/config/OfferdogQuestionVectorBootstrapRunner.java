package com.hanserdev.interview.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserdev.interview.domain.dataobject.QuestionDO;
import com.hanserdev.interview.domain.mapper.QuestionMapper;
import com.hanserdev.interview.service.QuestionVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时将未向量化的题目补齐到向量库（用于 RAG topK 检索）。
 * 默认开启，可通过 INTERVIEW_BOOTSTRAP_QUESTION_VECTORS=false 关闭。
 */
@Slf4j
@Component
@Order(210)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "interview.bootstrap.question-vectors", havingValue = "true", matchIfMissing = true)
@Profile("!question-vector-cli")
public class OfferdogQuestionVectorBootstrapRunner implements CommandLineRunner {

    private final QuestionMapper questionMapper;
    private final QuestionVectorService questionVectorService;

    @Override
    public void run(String... args) {
        List<QuestionDO> pending = questionMapper.selectList(new LambdaQueryWrapper<QuestionDO>()
                .isNull(QuestionDO::getDeletedAt)
                .isNull(QuestionDO::getVectorAt)
                .orderByAsc(QuestionDO::getId)
                .last("LIMIT 2000"));

        if (pending.isEmpty()) {
            log.info("题目向量化补齐：无待处理题目");
            return;
        }

        int ok = 0;
        for (QuestionDO q : pending) {
            try {
                questionVectorService.addQuestion(q.getId());
                ok++;
            } catch (Exception e) {
                log.warn("题目向量化补齐失败 questionId={}: {}", q.getId(), e.getMessage());
            }
        }
        log.info("题目向量化补齐完成：待处理 {} 道，成功 {} 道", pending.size(), ok);
    }
}

