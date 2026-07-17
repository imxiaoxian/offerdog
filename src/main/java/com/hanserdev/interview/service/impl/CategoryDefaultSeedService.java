package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hanserdev.interview.domain.dataobject.CategoryDO;
import com.hanserdev.interview.domain.mapper.CategoryMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

/**
 * 若库中没有任何二级岗位分类，则写入最小默认树（后端/Java、前端/Vue），保证示例题库与前端分类接口可用。
 */
@Slf4j
@Service
public class CategoryDefaultSeedService {

    @Resource
    private CategoryMapper categoryMapper;

    @Transactional(rollbackFor = Exception.class)
    public void seedIfNoLevel2Categories() {
        long jobCount = categoryMapper.selectCount(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getLevel, 2)
                .isNull(CategoryDO::getDeletedAt));
        if (jobCount > 0) {
            return;
        }

        log.info("未检测到二级岗位分类，写入默认分类（后端/Java、前端/Vue）…");

        CategoryDO backendRoot = newRoot("后端");
        categoryMapper.insert(backendRoot);
        fixLevel1Path(backendRoot);

        CategoryDO frontendRoot = newRoot("前端");
        categoryMapper.insert(frontendRoot);
        fixLevel1Path(frontendRoot);

        CategoryDO javaJob = newJob(backendRoot.getId(), "Java");
        categoryMapper.insert(javaJob);
        fixLevel2Path(javaJob);

        CategoryDO vueJob = newJob(frontendRoot.getId(), "Vue");
        categoryMapper.insert(vueJob);
        fixLevel2Path(vueJob);
    }

    private static CategoryDO newRoot(String name) {
        CategoryDO c = new CategoryDO();
        c.setName(name);
        c.setLevel(1);
        c.setParentId(null);
        c.setPath(new Integer[]{0});
        c.setMetadata(new HashMap<>());
        return c;
    }

    private static CategoryDO newJob(Long parentId, String name) {
        CategoryDO c = new CategoryDO();
        c.setName(name);
        c.setLevel(2);
        c.setParentId(parentId);
        c.setPath(new Integer[]{parentId.intValue(), 0});
        c.setMetadata(new HashMap<>());
        return c;
    }

    private void fixLevel1Path(CategoryDO c) {
        c.setPath(new Integer[]{c.getId().intValue()});
        categoryMapper.updateById(c);
    }

    private void fixLevel2Path(CategoryDO c) {
        c.setPath(new Integer[]{c.getParentId().intValue(), c.getId().intValue()});
        categoryMapper.updateById(c);
    }
}
