package com.hanserdev.interview.service;

import com.hanserdev.interview.model.vo.category.CategoryCreateReqVO;
import com.hanserdev.interview.model.vo.category.CategoryDetailRspVO;
import com.hanserdev.interview.model.vo.category.CategoryTreeRspVO;
import com.hanserdev.interview.model.vo.category.CategoryUpdateReqVO;

import java.util.List;

public interface CategoryService {

    /**
     * 创建分类
     * @param reqVO 分类创建请求参数
     * @return 分类ID
     */
    Long createCategory(CategoryCreateReqVO reqVO);

    /**
     * 更新分类
     * @param id 分类ID
     * @param reqVO 分类更新请求参数
     */
    void updateCategory(Long id, CategoryUpdateReqVO reqVO);

    /**
     * 删除分类
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 获取分类详情
     * @param id 分类ID
     * @return 分类详情响应数据
     */
    CategoryDetailRspVO getCategory(Long id);

    /**
     * 获取分类树列表
     * @return 分类树列表响应数据
     */
    List<CategoryTreeRspVO> listCategoryTree();
}
