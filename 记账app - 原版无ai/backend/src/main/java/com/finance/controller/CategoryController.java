package com.finance.controller;

import com.finance.annotation.OperationLog;
import com.finance.dto.ApiResponse;
import com.finance.dto.CategoryDto;
import com.finance.dto.CreateCategoryRequest;
import com.finance.dto.IconPresetDto;
import com.finance.dto.UpdateCategoryRequest;
import com.finance.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "分类管理", description = "收支分类管理接口，支持收入支出分类的增删改查操作")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(
        summary = "获取分类列表", 
        description = "获取所有收支分类列表，包含收入类型和支出类型的分类"
    )
    @OperationLog(value = "GET_CATEGORIES", description = "获取分类列表", recordParams = false, recordResult = false)
    public ApiResponse<List<CategoryDto>> getCategories(
            @Parameter(description = "分类类型：1=收入，2=支出，null=全部") @RequestParam(required = false) Integer type) {
        return ApiResponse.success(categoryService.listCategories(type));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "获取分类详情", 
        description = "根据ID获取分类详细信息，包括分类名称、图标、类型等完整信息"
    )
    @OperationLog(value = "GET_CATEGORY", description = "获取分类详情", recordParams = false, recordResult = false)
    public ApiResponse<CategoryDto> getCategory(
            @Parameter(description = "分类ID，示例：1") @PathVariable Long id) {
        return ApiResponse.success(categoryService.getCategory(id));
    }

    @PostMapping
    @Operation(
        summary = "创建分类", 
        description = "创建新的收支分类，支持设置分类名称、图标和类型（收入/支出）"
    )
    @OperationLog(value = "CREATE_CATEGORY", description = "创建分类", businessType = "CATEGORY")
    public ApiResponse<CategoryDto> createCategory(
            @Parameter(description = "分类创建请求体，包含分类基本信息") @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新分类", 
        description = "更新指定ID的分类信息，支持修改分类名称、图标和类型"
    )
    @OperationLog(value = "UPDATE_CATEGORY", description = "更新分类", businessType = "CATEGORY")
    public ApiResponse<CategoryDto> updateCategory(
            @Parameter(description = "分类ID，示例：1") @PathVariable Long id,
            @Parameter(description = "分类更新请求体") @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除分类", 
        description = "删除指定ID的分类，删除前会检查该分类是否被交易记录使用"
    )
    @OperationLog(value = "DELETE_CATEGORY", description = "删除分类", businessType = "CATEGORY")
    public ApiResponse<Void> deleteCategory(
            @Parameter(description = "分类ID，示例：1") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.successMessage("Category deleted");
    }
    
    @GetMapping("/icons")
    @Operation(
        summary = "获取图标预设列表", 
        description = "获取所有可用的分类图标预设列表，按分类组织"
    )
    @OperationLog(value = "GET_ICONS", description = "获取图标预设列表", recordParams = false, recordResult = false)
    public ApiResponse<List<IconPresetDto>> getAvailableIcons() {
        return ApiResponse.success(categoryService.getAvailableIcons());
    }
    
    @GetMapping("/icons/{category}")
    @Operation(
        summary = "获取指定分类的图标预设列表", 
        description = "根据分类名称获取该分类下的图标预设列表"
    )
    @OperationLog(value = "GET_ICONS_BY_CATEGORY", description = "获取指定分类的图标", recordParams = false, recordResult = false)
    public ApiResponse<List<IconPresetDto>> getIconsByCategory(
            @Parameter(description = "图标分类", required = true) 
            @PathVariable String category) {
        
        List<IconPresetDto> icons = categoryService.getIconsByCategory(category);
        return ApiResponse.success(icons);
    }
    
    @PostMapping("/reorder")
    @Operation(
        summary = "重新排序分类", 
        description = "批量更新分类的排序顺序，支持收入和支出分类的重新排列"
    )
    @OperationLog(value = "REORDER_CATEGORIES", description = "重新排序分类", businessType = "CATEGORY")
    public ApiResponse<Void> reorderCategories(
            @Parameter(description = "分类ID列表，按新顺序排列", required = true) 
            @RequestBody List<Long> categoryIds) {
        categoryService.reorderCategories(categoryIds);
        return ApiResponse.successMessage("分类排序更新成功");
    }
    
    @PutMapping("/{id}/sort-order")
    @Operation(
        summary = "更新分类排序", 
        description = "更新单个分类的排序值，支持精细化控制分类显示顺序"
    )
    @OperationLog(value = "UPDATE_CATEGORY_SORT_ORDER", description = "更新分类排序", businessType = "CATEGORY")
    public ApiResponse<CategoryDto> updateCategorySortOrder(
            @Parameter(description = "分类ID", required = true) 
            @PathVariable Long id,
            @Parameter(description = "新的排序值", required = true) 
            @RequestParam Integer sortOrder) {
        return ApiResponse.success(categoryService.updateCategorySortOrder(id, sortOrder));
    }
}
