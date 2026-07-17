package com.hanserdev.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hanserdev.interview.domain.dataobject.CategoryDO;
import com.hanserdev.interview.domain.mapper.CategoryMapper;
import com.hanserdev.interview.enums.ResponseCodeEnum;
import com.hanserdev.interview.exception.ApiException;
import com.hanserdev.interview.model.convert.CategoryAssembler;
import com.hanserdev.interview.model.vo.category.CategoryCreateReqVO;
import com.hanserdev.interview.model.vo.category.CategoryDetailRspVO;
import com.hanserdev.interview.model.vo.category.CategoryTreeRspVO;
import com.hanserdev.interview.model.vo.category.CategoryUpdateReqVO;
import com.hanserdev.interview.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryCreateReqVO reqVO) {
        CategoryDO parent = null;
        if (reqVO.getParentId() != null) {
            parent = loadCategory(reqVO.getParentId());
        }
        validateLevel(reqVO.getLevel(), parent);
        assertNameUnique(reqVO.getName(), reqVO.getParentId(), null);

        CategoryDO categoryDO = new CategoryDO();
        categoryDO.setName(reqVO.getName());
        categoryDO.setParentId(reqVO.getParentId());
        categoryDO.setLevel(reqVO.getLevel());
        categoryDO.setMetadata(defaultMetadata(reqVO.getMetadata()));
        categoryDO.setPath(new Integer[0]);

        if (categoryMapper.insert(categoryDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }

        categoryDO.setPath(buildPath(categoryDO.getId(), parent));
        categoryMapper.updateById(categoryDO);
        return categoryDO.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryUpdateReqVO reqVO) {
        CategoryDO categoryDO = loadCategory(id);
        if (StringUtils.isNotBlank(reqVO.getName())) {
            assertNameUnique(reqVO.getName(), categoryDO.getParentId(), id);
            categoryDO.setName(reqVO.getName());
        }
        if (reqVO.getMetadata() != null) {
            categoryDO.setMetadata(defaultMetadata(reqVO.getMetadata()));
        }
        if (categoryMapper.updateById(categoryDO) != 1) {
            throw new ApiException(ResponseCodeEnum.SYSTEM_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        loadCategory(id);
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<CategoryDO> wrapper = new UpdateWrapper<>();
        wrapper.set("deleted_at", now);
        wrapper.isNull("deleted_at");
        wrapper.apply("path @> ARRAY[{0}]::integer[]", id);
        categoryMapper.update(null, wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDetailRspVO getCategory(Long id) {
        CategoryDO categoryDO = loadCategory(id);
        return CategoryAssembler.toDetailRspVO(categoryDO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeRspVO> listCategoryTree() {
        LambdaQueryWrapper<CategoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(CategoryDO::getDeletedAt);
        wrapper.orderByAsc(CategoryDO::getLevel, CategoryDO::getId);
        List<CategoryDO> categories = categoryMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(categories)) {
            return new ArrayList<>();
        }
        categories.sort(Comparator.comparingInt(c -> c.getPath() == null ? 0 : c.getPath().length));
        Map<Long, CategoryTreeRspVO> nodeMap = new HashMap<>();
        List<CategoryTreeRspVO> roots = new ArrayList<>();
        for (CategoryDO category : categories) {
            CategoryTreeRspVO node = CategoryAssembler.toTreeNode(category);
            nodeMap.put(category.getId(), node);
            if (category.getParentId() == null) {
                roots.add(node);
            } else {
                CategoryTreeRspVO parentNode = nodeMap.get(category.getParentId());
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    private CategoryDO loadCategory(Long id) {
        CategoryDO categoryDO = categoryMapper.selectById(id);
        if (categoryDO == null || categoryDO.getDeletedAt() != null) {
            throw new ApiException(ResponseCodeEnum.CATEGORY_NOT_FOUND);
        }
        return categoryDO;
    }

    private void assertNameUnique(String name, Long parentId, Long excludeId) {
        QueryWrapper<CategoryDO> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name);
        if (parentId == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", parentId);
        }
        wrapper.isNull("deleted_at");
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ApiException(ResponseCodeEnum.CATEGORY_NAME_DUPLICATE);
        }
    }

    private void validateLevel(Integer level, CategoryDO parent) {
        if (parent == null) {
            if (level == null || level != 1) {
                throw new ApiException(ResponseCodeEnum.CATEGORY_LEVEL_INVALID);
            }
        } else {
            int expected = parent.getLevel() + 1;
            if (level == null || !level.equals(expected)) {
                throw new ApiException(ResponseCodeEnum.CATEGORY_LEVEL_INVALID);
            }
        }
    }

    private Integer[] buildPath(Long categoryId, CategoryDO parent) {
        if (parent == null || parent.getPath() == null) {
            return new Integer[]{categoryId.intValue()};
        }
        Integer[] parentPath = parent.getPath();
        Integer[] path = new Integer[parentPath.length + 1];
        System.arraycopy(parentPath, 0, path, 0, parentPath.length);
        path[parentPath.length] = categoryId.intValue();
        return path;
    }

    private Map<String, Object> defaultMetadata(Map<String, Object> metadata) {
        return metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }
}
