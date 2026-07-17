package com.hanserdev.interview.model.vo.category;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CategoryDetailRspVO {

    /**
     * 行业/岗位ID
     */
    private Long id;
    
    /**
     * 行业/岗位名称
     */
    private String name;
    
    /**
     * 行业ID
     */
    private Long parentId;
    
    /**
     * 层级，1-行业，2-岗位
     */
    private Integer level;
    
    /**
     * 路径，如果分类结构是 A (1) -> B (4) -> C (10)，那么 B 的 path 可能是 {1, 4}，C 的 path 可能是 {1, 4, 10}。
     */
    private List<Integer> path;
    
    /**
     * 额外补充数据JSON格式
     */
    private Map<String, Object> metadata;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
