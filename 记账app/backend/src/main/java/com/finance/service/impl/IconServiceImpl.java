package com.finance.service.impl;

import com.finance.dto.IconPresetDto;
import com.finance.service.IconService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图标管理服务实现
 */
@Service
public class IconServiceImpl implements IconService {
    
    // 预设图标数据
    private static final Map<String, List<IconPresetDto>> ICON_PRESETS = new HashMap<>();
    
    static {
        // 食物相关图标
        ICON_PRESETS.put("food", Arrays.asList(
            createIcon("restaurant", "餐厅", "食物", "餐厅图标"),
            createIcon("local_cafe", "咖啡", "食物", "咖啡厅图标"),
            createIcon("fastfood", "快餐", "食物", "快餐图标"),
            createIcon("restaurant_menu", "菜单", "食物", "菜单图标"),
            createIcon("local_dining", "用餐", "食物", "用餐图标"),
            createIcon("bakery_dining", "烘焙", "食物", "烘焙店图标")
        ));
        
        // 交通相关图标
        ICON_PRESETS.put("transportation", Arrays.asList(
            createIcon("directions_car", "汽车", "交通", "汽车图标"),
            createIcon("directions_bike", "自行车", "交通", "自行车图标"),
            createIcon("directions_bus", "公交", "交通", "公交车图标"),
            createIcon("directions_railway", "火车", "交通", "火车图标"),
            createIcon("flight", "飞机", "交通", "飞机图标"),
            createIcon("directions_boat", "船", "交通", "船只图标"),
            createIcon("local_taxi", "出租车", "交通", "出租车图标")
        ));
        
        // 购物相关图标
        ICON_PRESETS.put("shopping", Arrays.asList(
            createIcon("shopping_cart", "购物车", "购物", "购物车图标"),
            createIcon("store", "商店", "购物", "商店图标"),
            createIcon("local_mall", "商场", "购物", "商场图标"),
            createIcon("shopping_bag", "购物袋", "购物", "购物袋图标"),
            createIcon("credit_card", "信用卡", "购物", "信用卡图标"),
            createIcon("redeem", "礼品卡", "购物", "礼品卡图标")
        ));
        
        // 娱乐相关图标
        ICON_PRESETS.put("entertainment", Arrays.asList(
            createIcon("movie", "电影", "娱乐", "电影图标"),
            createIcon("music_note", "音乐", "娱乐", "音乐图标"),
            createIcon("sports_esports", "游戏", "娱乐", "游戏图标"),
            createIcon("fitness_center", "健身", "娱乐", "健身图标"),
            createIcon("beach_access", "海滩", "娱乐", "海滩图标"),
            createIcon("photo_camera", "摄影", "娱乐", "摄影图标"),
            createIcon("palette", "艺术", "娱乐", "艺术图标")
        ));
        
        // 医疗健康相关图标
        ICON_PRESETS.put("health", Arrays.asList(
            createIcon("local_hospital", "医院", "医疗", "医院图标"),
            createIcon("healing", "医疗", "医疗", "医疗图标"),
            createIcon("medication", "药物", "医疗", "药物图标"),
            createIcon("favorite", "健康", "医疗", "健康图标"),
            createIcon("accessibility", "无障碍", "医疗", "无障碍图标"),
            createIcon("psychology", "心理", "医疗", "心理图标")
        ));
        
        // 教育相关图标
        ICON_PRESETS.put("education", Arrays.asList(
            createIcon("school", "学校", "教育", "学校图标"),
            createIcon("book", "书籍", "教育", "书籍图标"),
            createIcon("menu_book", "教科书", "教育", "教科书图标"),
            createIcon("science", "科学", "教育", "科学图标"),
            createIcon("computer", "电脑", "教育", "电脑图标"),
            createIcon("language", "语言", "教育", "语言图标")
        ));
        
        // 收入相关图标
        ICON_PRESETS.put("income", Arrays.asList(
            createIcon("attach_money", "工资", "收入", "工资图标"),
            createIcon("business_center", "奖金", "收入", "奖金图标"),
            createIcon("savings", "投资", "收入", "投资图标"),
            createIcon("trending_up", "收益", "收入", "收益图标"),
            createIcon("account_balance", "理财", "收入", "理财图标"),
            createIcon("payments", "副业", "收入", "副业图标")
        ));
        
        // 其他通用图标
        ICON_PRESETS.put("general", Arrays.asList(
            createIcon("home", "家庭", "通用", "家庭图标"),
            createIcon("work", "工作", "通用", "工作图标"),
            createIcon("pets", "宠物", "通用", "宠物图标"),
            createIcon("child_friendly", "孩子", "通用", "孩子图标"),
            createIcon("elderly", "长辈", "通用", "长辈图标"),
            createIcon("public", "公共", "通用", "公共图标")
        ));
    }
    
    @Override
    public List<IconPresetDto> getIconPresets() {
        return ICON_PRESETS.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(IconPresetDto::getCategory)
                        .thenComparing(IconPresetDto::getName))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<IconPresetDto> getIconPresetsByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return getIconPresets();
        }
        
        List<IconPresetDto> result = ICON_PRESETS.get(category.toLowerCase());
        return result != null ? result : new ArrayList<>();
    }
    
    @Override
    public boolean isValidIcon(String iconName) {
        if (iconName == null || iconName.trim().isEmpty()) {
            return false;
        }
        
        // 检查是否为预设图标
        for (List<IconPresetDto> icons : ICON_PRESETS.values()) {
            for (IconPresetDto icon : icons) {
                if (icon.getIconName().equals(iconName)) {
                    return true;
                }
            }
        }
        
        // 允许自定义图标（如果包含特殊字符或驼峰命名）
        return iconName.matches("^[a-z][a-zA-Z0-9_]*$") || 
               iconName.contains("-") || 
               iconName.contains("_");
    }
    
    private static IconPresetDto createIcon(String iconName, String name, String category, String description) {
        IconPresetDto icon = new IconPresetDto();
        icon.setName(name);
        icon.setIconName(iconName);
        icon.setDisplayName(name);
        icon.setCategory(category);
        icon.setDescription(description);
        return icon;
    }
}