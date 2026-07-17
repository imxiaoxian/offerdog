package com.hanserdev.interview.controller;

import com.hanserdev.interview.common.aop.ApiOperationLog;
import com.hanserdev.interview.common.response.Response;
import com.hanserdev.interview.model.vo.category.CategoryCreateReqVO;
import com.hanserdev.interview.model.vo.category.CategoryDetailRspVO;
import com.hanserdev.interview.model.vo.category.CategoryTreeRspVO;
import com.hanserdev.interview.model.vo.category.CategoryUpdateReqVO;
import com.hanserdev.interview.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@Validated
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ApiOperationLog(description = "创建行业/岗位分类")
    public Response<Long> createCategory(@Valid @RequestBody CategoryCreateReqVO reqVO) {
        return Response.success(categoryService.createCategory(reqVO));
    }

    @PutMapping("/{id}")
    @ApiOperationLog(description = "更新行业/岗位分类")
    public Response<Void> updateCategory(@PathVariable("id") Long id,
                                         @Valid @RequestBody CategoryUpdateReqVO reqVO) {
        categoryService.updateCategory(id, reqVO);
        return Response.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperationLog(description = "删除行业/岗位分类")
    public Response<Void> deleteCategory(@PathVariable("id") Long id) {
        categoryService.deleteCategory(id);
        return Response.success();
    }

    @GetMapping("/{id}")
    @ApiOperationLog(description = "查询行业/岗位分类详情")
    public Response<CategoryDetailRspVO> getCategory(@PathVariable("id") Long id) {
        return Response.success(categoryService.getCategory(id));
    }

    @GetMapping("/tree")
    @ApiOperationLog(description = "获取行业/岗位树")
    public Response<List<CategoryTreeRspVO>> listCategoryTree() {
        return Response.success(categoryService.listCategoryTree());
    }
}
