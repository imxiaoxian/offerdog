package com.hanserdev.interview.model.convert;

import com.hanserdev.interview.domain.dataobject.CategoryDO;
import com.hanserdev.interview.model.vo.category.CategoryDetailRspVO;
import com.hanserdev.interview.model.vo.category.CategoryTreeRspVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class CategoryAssembler {

    private CategoryAssembler() {
    }

    public static CategoryDetailRspVO toDetailRspVO(CategoryDO categoryDO) {
        CategoryDetailRspVO rspVO = new CategoryDetailRspVO();
        rspVO.setId(categoryDO.getId());
        rspVO.setName(categoryDO.getName());
        rspVO.setParentId(categoryDO.getParentId());
        rspVO.setLevel(categoryDO.getLevel());
        rspVO.setPath(convertPath(categoryDO.getPath()));
        rspVO.setMetadata(defaultMetadata(categoryDO.getMetadata()));
        rspVO.setCreatedAt(categoryDO.getCreatedAt());
        rspVO.setUpdatedAt(categoryDO.getUpdatedAt());
        return rspVO;
    }

    public static CategoryTreeRspVO toTreeNode(CategoryDO categoryDO) {
        CategoryTreeRspVO node = new CategoryTreeRspVO();
        node.setId(categoryDO.getId());
        node.setName(categoryDO.getName());
        node.setLevel(categoryDO.getLevel());
        node.setMetadata(defaultMetadata(categoryDO.getMetadata()));
        return node;
    }

    private static List<Integer> convertPath(Integer[] path) {
        return path == null ? Collections.emptyList() : Arrays.asList(path);
    }

    private static Map<String, Object> defaultMetadata(Map<String, Object> metadata) {
        return metadata == null ? Collections.emptyMap() : metadata;
    }
}
