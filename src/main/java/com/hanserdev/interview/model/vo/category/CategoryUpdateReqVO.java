package com.hanserdev.interview.model.vo.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class CategoryUpdateReqVO {

    /**
     * 行业/岗位名称
     */
    @NotBlank
    @Size(max = 100)
    private String name;

    /**
     * 行业/岗位元数据
     */
    private Map<String, Object> metadata;
}
