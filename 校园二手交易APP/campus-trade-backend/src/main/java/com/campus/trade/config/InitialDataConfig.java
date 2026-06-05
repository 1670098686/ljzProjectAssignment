package com.campus.trade.config;

import com.campus.trade.model.entity.Category;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.repository.CategoryRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class InitialDataConfig implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting data initialization for dev environment...");
        
        // 初始化分类数据（仅在分类表为空时执行）
        initCategories();
        
        // 获取或创建测试用户
        User testUser = userRepository.findByUsername("test_user")
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername("test_user");
                    user.setPassword(passwordEncoder.encode("password123"));
                    user.setEmail("test@example.com");
                    user.setRole(UserRole.STUDENT);
                    return userRepository.save(user);
                });
        
        // 获取或创建买家用户
        User buyerUser = userRepository.findByUsername("buyer01")
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername("buyer01");
                    user.setPassword(passwordEncoder.encode("password123"));
                    user.setEmail("buyer01@example.com");
                    user.setPhone("13800138002");
                    user.setRole(UserRole.STUDENT);
                    return userRepository.save(user);
                });
        
        // 获取或创建管理员用户
        User adminUser = userRepository.findByUsername("admin")
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername("admin");
                    user.setPassword(passwordEncoder.encode("123456"));
                    user.setEmail("admin@example.com");
                    user.setRole(UserRole.ADMIN);
                    return userRepository.save(user);
                });

        // 创建已审核商品（仅在商品表为空时执行）
        if (productRepository.count() == 0) {
            createApprovedProducts(testUser);
            createPendingProducts(testUser);
            System.out.println("Data initialization completed successfully!");
        } else {
            System.out.println("Skipping data initialization as products already exist.");
        }
    }
    
    private void initCategories() {
        // 仅在分类表为空时执行初始化
        if (categoryRepository.count() == 0) {
            // 创建顶级分类
            List<Category> categories = List.of(
                    // 电子产品分类
                    createCategory("ELECTRONICS", "电子产品", null, 1, true),
                    // 书籍分类
                    createCategory("BOOKS", "书籍", null, 2, true),
                    // 服装分类
                    createCategory("CLOTHING", "服装", null, 3, true),
                    // 日常用品分类
                    createCategory("DAILY", "日用品", null, 4, true),
                    // 运动器材分类
                    createCategory("SPORTS", "运动器材", null, 5, true),
                    // 其他分类
                    createCategory("OTHER", "其他", null, 6, true)
            );
            
            categoryRepository.saveAll(categories);
            System.out.println("Successfully initialized " + categories.size() + " categories");
        } else {
            System.out.println("Skipping category initialization as categories already exist.");
        }
    }
    
    private Category createCategory(String code, String name, Long parentId, Integer sortOrder, Boolean enabled) {
        Category category = new Category();
        category.setCode(code);
        category.setName(name);
        category.setParentId(parentId);
        category.setSortOrder(sortOrder);
        category.setEnabled(enabled);
        return category;
    }

    private void createApprovedProducts(User seller) {
        // 获取所有分类
        List<Category> categories = categoryRepository.findAll();
        
        List<Product> approvedProducts = List.of(
                // 书籍分类 (BOOKS)
                createProduct("大学英语四级词汇", "全新，包含所有四级核心词汇", new BigDecimal(15), ProductCategory.BOOKS, findCategoryByCode(categories, "BOOKS"), AuditStatus.APPROVED, seller),
                createProduct("高等数学习题集", "包含详细解答，适合复习", new BigDecimal(25), ProductCategory.BOOKS, findCategoryByCode(categories, "BOOKS"), AuditStatus.APPROVED, seller),
                createProduct("计算机网络基础", "第二版，内容全面", new BigDecimal(35), ProductCategory.BOOKS, findCategoryByCode(categories, "BOOKS"), AuditStatus.APPROVED, seller),
                createProduct("概率论与数理统计", "考研必备，几乎全新", new BigDecimal(45), ProductCategory.BOOKS, findCategoryByCode(categories, "BOOKS"), AuditStatus.APPROVED, seller),
                createProduct("现代汉语词典", "第六版，正版", new BigDecimal(48), ProductCategory.BOOKS, findCategoryByCode(categories, "BOOKS"), AuditStatus.APPROVED, seller),
                
                // 电子产品分类 (ELECTRONICS)
                createProduct("USB转Type-C数据线", "2米长，快充支持", new BigDecimal(12), ProductCategory.ELECTRONICS, findCategoryByCode(categories, "ELECTRONICS"), AuditStatus.APPROVED, seller),
                createProduct("无线鼠标", "静音设计，电池续航长", new BigDecimal(28), ProductCategory.ELECTRONICS, findCategoryByCode(categories, "ELECTRONICS"), AuditStatus.APPROVED, seller),
                createProduct("手机支架", "可调节角度，稳固耐用", new BigDecimal(18), ProductCategory.ELECTRONICS, findCategoryByCode(categories, "ELECTRONICS"), AuditStatus.APPROVED, seller),
                createProduct("蓝牙耳机", "音质清晰，佩戴舒适", new BigDecimal(38), ProductCategory.ELECTRONICS, findCategoryByCode(categories, "ELECTRONICS"), AuditStatus.APPROVED, seller),
                createProduct("充电宝 10000mAh", "轻薄便携，双向快充", new BigDecimal(45), ProductCategory.ELECTRONICS, findCategoryByCode(categories, "ELECTRONICS"), AuditStatus.APPROVED, seller),
                
                // 服装分类 (CLOTHING)
                createProduct("纯棉T恤", "白色，M码，全新", new BigDecimal(19), ProductCategory.CLOTHING, findCategoryByCode(categories, "CLOTHING"), AuditStatus.APPROVED, seller),
                createProduct("运动短裤", "黑色，L码，几乎全新", new BigDecimal(25), ProductCategory.CLOTHING, findCategoryByCode(categories, "CLOTHING"), AuditStatus.APPROVED, seller),
                createProduct("帆布鞋", "经典款，39码，穿过一次", new BigDecimal(35), ProductCategory.CLOTHING, findCategoryByCode(categories, "CLOTHING"), AuditStatus.APPROVED, seller),
                createProduct("棒球帽", "黑色，可调节大小", new BigDecimal(15), ProductCategory.CLOTHING, findCategoryByCode(categories, "CLOTHING"), AuditStatus.APPROVED, seller),
                createProduct("围巾", "毛线材质，保暖舒适", new BigDecimal(28), ProductCategory.CLOTHING, findCategoryByCode(categories, "CLOTHING"), AuditStatus.APPROVED, seller),
                
                // 运动分类 (SPORTS)
                createProduct("羽毛球拍", "铝合金材质，送羽毛球", new BigDecimal(35), ProductCategory.SPORTS, findCategoryByCode(categories, "SPORTS"), AuditStatus.APPROVED, seller),
                createProduct("瑜伽垫", "加厚款，防滑设计", new BigDecimal(45), ProductCategory.SPORTS, findCategoryByCode(categories, "SPORTS"), AuditStatus.APPROVED, seller),
                createProduct("跳绳", "计数功能，长度可调节", new BigDecimal(12), ProductCategory.SPORTS, findCategoryByCode(categories, "SPORTS"), AuditStatus.APPROVED, seller),
                createProduct("篮球", "标准7号球，弹性好", new BigDecimal(48), ProductCategory.SPORTS, findCategoryByCode(categories, "SPORTS"), AuditStatus.APPROVED, seller),
                createProduct("运动水杯", "500ml，保温效果好", new BigDecimal(25), ProductCategory.SPORTS, findCategoryByCode(categories, "SPORTS"), AuditStatus.APPROVED, seller),
                
                // 日常用品分类 (DAILY)
                createProduct("笔记本", "A5大小，100页", new BigDecimal(10), ProductCategory.DAILY, findCategoryByCode(categories, "DAILY"), AuditStatus.APPROVED, seller),
                createProduct("笔袋", "大容量，防水材质", new BigDecimal(18), ProductCategory.DAILY, findCategoryByCode(categories, "DAILY"), AuditStatus.APPROVED, seller),
                createProduct("雨伞", "折叠式，防风设计", new BigDecimal(28), ProductCategory.DAILY, findCategoryByCode(categories, "DAILY"), AuditStatus.APPROVED, seller),
                createProduct("台灯", "可调节亮度，护眼", new BigDecimal(38), ProductCategory.DAILY, findCategoryByCode(categories, "DAILY"), AuditStatus.APPROVED, seller),
                createProduct("收纳盒", "塑料材质，带盖子", new BigDecimal(15), ProductCategory.DAILY, findCategoryByCode(categories, "DAILY"), AuditStatus.APPROVED, seller),
                
                // 其他分类 (OTHER)
                createProduct("十字绣套件", "简单图案，送工具", new BigDecimal(25), ProductCategory.OTHER, findCategoryByCode(categories, "OTHER"), AuditStatus.APPROVED, seller),
                createProduct("盆栽", "多肉植物，带花盆", new BigDecimal(18), ProductCategory.OTHER, findCategoryByCode(categories, "OTHER"), AuditStatus.APPROVED, seller),
                createProduct("手工编织袋", "环保材质，大容量", new BigDecimal(35), ProductCategory.OTHER, findCategoryByCode(categories, "OTHER"), AuditStatus.APPROVED, seller),
                createProduct("明信片套装", "30张不同图案", new BigDecimal(15), ProductCategory.OTHER, findCategoryByCode(categories, "OTHER"), AuditStatus.APPROVED, seller),
                createProduct("钥匙扣", "个性设计，金属材质", new BigDecimal(12), ProductCategory.OTHER, findCategoryByCode(categories, "OTHER"), AuditStatus.APPROVED, seller)
        );
        productRepository.saveAll(approvedProducts);
    }

    private void createPendingProducts(User seller) {
        // 获取所有分类
        List<Category> categories = categoryRepository.findAll();
        
        List<Product> pendingProducts = List.of(
                // 待审核商品
                createProduct("iPhone 15 Pro", "使用半年，95新，无划痕", new BigDecimal(7999), ProductCategory.ELECTRONICS, findCategoryByCode(categories, "ELECTRONICS"), AuditStatus.PENDING, seller),
                createProduct("Python编程从入门到精通", "第二版，几乎全新", new BigDecimal(89), ProductCategory.BOOKS, findCategoryByCode(categories, "BOOKS"), AuditStatus.PENDING, seller),
                createProduct("跑步机家用款", "折叠式，带显示屏", new BigDecimal(1499), ProductCategory.SPORTS, findCategoryByCode(categories, "SPORTS"), AuditStatus.PENDING, seller),
                createProduct("智能手表", "功能齐全，九成新", new BigDecimal(1299), ProductCategory.ELECTRONICS, findCategoryByCode(categories, "ELECTRONICS"), AuditStatus.PENDING, seller),
                createProduct("吉他", "民谣吉他，入门级", new BigDecimal(599), ProductCategory.OTHER, findCategoryByCode(categories, "OTHER"), AuditStatus.PENDING, seller)
        );
        productRepository.saveAll(pendingProducts);
    }

    private Product createProduct(String title, String description, BigDecimal price, ProductCategory category, Category categoryEntity, AuditStatus auditStatus, User seller) {
        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);
        product.setCategoryEntity(categoryEntity); // 关联分类实体
        product.setAuditStatus(auditStatus);
        product.setStatus(ProductStatus.ON_SALE);
        product.setSeller(seller);
        product.setImages(List.of("https://via.placeholder.com/300"));
        product.setTags(List.of(category.name().toLowerCase(), "二手", "校园"));
        return product;
    }
    
    private Category findCategoryByCode(List<Category> categories, String code) {
        return categories.stream()
            .filter(cat -> cat.getCode().equalsIgnoreCase(code))
            .findFirst()
            .orElse(null);
    }
}
