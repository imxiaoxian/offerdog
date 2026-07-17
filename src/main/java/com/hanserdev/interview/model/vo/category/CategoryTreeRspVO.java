package com.hanserdev.interview.model.vo.category;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CategoryTreeRspVO {

    /**
     * 行业/岗位ID
     */
    private Long id;
    
    /**
     * 行业/岗位名称
     */
    private String name;
    
    /**
     * 行业/岗位层级
     */
    private Integer level;
    
    /**
     * 行业/岗位补充数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 岗位列表
     */
    private final List<CategoryTreeRspVO> children = new ArrayList<>();
}
