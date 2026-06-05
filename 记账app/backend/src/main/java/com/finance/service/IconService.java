package com.finance.service;

import com.finance.dto.IconPresetDto;

import java.util.List;

/**
 * 图标管理服务接口
 */
public interface IconService {
    
    /**
     * 获取所有预设图标列表
     * 
     * @return 图标预设列表
     */
    List<IconPresetDto> getIconPresets();
    
    /**
     * 根据分类获取预设图标列表
     * 
     * @param category 图标分类
     * @return 该分类下的图标预设列表
     */
    List<IconPresetDto> getIconPresetsByCategory(String category);
    
    /**
     * 验证图标名称是否有效
     * 
     * @param iconName 图标名称
     * @return 是否有效
     */
    boolean isValidIcon(String iconName);
}