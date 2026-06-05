package com.finance.service;

import com.finance.dto.CategoryDto;
import com.finance.dto.CreateCategoryRequest;
import com.finance.dto.IconPresetDto;
import com.finance.dto.UpdateCategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> listCategories(Integer type);

    CategoryDto getCategory(Long id);

    CategoryDto createCategory(CreateCategoryRequest request);

    CategoryDto updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);
    
    /**
     * 获取可用的图标预设列表
     * 
     * @return 图标预设列表
     */
    List<IconPresetDto> getAvailableIcons();
    
    /**
     * 根据分类获取图标预设列表
     * 
     * @param category 图标分类
     * @return 该分类下的图标预设列表
     */
    List<IconPresetDto> getIconsByCategory(String category);
    
    /**
     * 重新排序分类
     * 
     * @param categoryIds 分类ID列表，按新顺序排列
     */
    void reorderCategories(List<Long> categoryIds);
    
    /**
     * 更新分类排序
     * 
     * @param id 分类ID
     * @param sortOrder 新的排序值
     * @return 更新后的分类DTO
     */
    CategoryDto updateCategorySortOrder(Long id, Integer sortOrder);
}
