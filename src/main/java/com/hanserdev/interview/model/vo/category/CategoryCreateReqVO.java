package com.hanserdev.interview.model.vo.category;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class CategoryCreateReqVO {

    /**
     * 分类名称
     */
    @NotBlank
    @Size(max = 100)
    private String name;

    /**
     * 父级分类ID
     */
    private Long parentId;

    /**
     * 分类层级
     */
    @Min(1)
    @Max(2)
    private Integer level;

    /**
     * 额外补充数据JSON格式
     */
    private Map<String, Object> metadata;
}
