package com.campus.trade.repository.spec;

import com.campus.trade.dto.product.ProductSearchClause;
import com.campus.trade.model.entity.Category;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AuditStatus;
import com.campus.trade.model.enums.ProductCategory;
import com.campus.trade.model.enums.ProductStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> excludeStatus(ProductStatus status) {
        return (root, query, cb) -> status == null ? null : cb.notEqual(root.get("status"), status);
    }

    public static Specification<Product> hasCategory(ProductCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return null;
            }
            // 同时支持基于categoryEntity实体和基于category枚举的分类过滤
            Join<Product, Category> categoryJoin = root.join("categoryEntity", JoinType.LEFT);
            return cb.or(
                cb.equal(categoryJoin.get("id"), categoryId),
                // 这里需要根据实际的分类ID映射关系添加枚举值过滤
                // 例如，如果categoryId=1对应BOOKS，categoryId=2对应ELECTRONICS等
                // 但由于目前没有这个映射关系，我们只保留基于categoryEntity的过滤
                // 后续需要添加分类ID到枚举值的映射
                cb.equal(categoryJoin.get("id"), categoryId)
            );
        };
    }

    public static Specification<Product> hasCategoryIds(Collection<Long> categoryIds) {
        return (root, query, cb) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return null;
            }
            return root.join("categoryEntity", JoinType.LEFT).get("id").in(categoryIds);
        };
    }

    public static Specification<Product> hasCategories(Collection<ProductCategory> categories) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) {
                return null;
            }
            return root.get("category").in(categories);
        };
    }

    public static Specification<Product> hasAuditStatus(AuditStatus auditStatus) {
        return (root, query, cb) -> auditStatus == null ? null : cb.equal(root.get("auditStatus"), auditStatus);
    }

    public static Specification<Product> hasStatuses(Collection<ProductStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return null;
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Product> titleLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(root.get("title"), pattern),
                    cb.like(root.get("description"), pattern)
            );
        };
    }

    public static Specification<Product> matchKeywords(List<String> keywords) {
        return (root, query, cb) -> {
            if (keywords == null || keywords.isEmpty()) {
                return null;
            }
            Join<Product, User> sellerJoin = root.join("seller", JoinType.LEFT);
            List<Predicate> keywordPredicates = new ArrayList<>();
            for (String keyword : keywords) {
                if (!StringUtils.hasText(keyword)) {
                    continue;
                }
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descriptionLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate locationLike = cb.like(cb.lower(root.get("location")), pattern);
                Predicate sellerSchoolLike = cb.like(cb.lower(sellerJoin.get("school")), pattern);
                keywordPredicates.add(cb.or(titleLike, descriptionLike, locationLike, sellerSchoolLike));
            }
            if (keywordPredicates.isEmpty()) {
                return null;
            }
            return cb.and(keywordPredicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> locationLike(String location) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(location)) {
                return null;
            }
            String pattern = "%" + location.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("location")), pattern);
        };
    }

    public static Specification<Product> locationContainsAll(List<String> keywords) {
        return (root, query, cb) -> {
            if (keywords == null || keywords.isEmpty()) {
                return null;
            }
            Join<Product, User> sellerJoin = root.join("seller", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            for (String keyword : keywords) {
                if (!StringUtils.hasText(keyword)) {
                    continue;
                }
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ENGLISH) + "%";
                Predicate locationLike = cb.like(cb.lower(root.get("location")), pattern);
                Predicate sellerSchoolLike = cb.like(cb.lower(sellerJoin.get("school")), pattern);
                predicates.add(cb.or(locationLike, sellerSchoolLike));
            }
            if (predicates.isEmpty()) {
                return null;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> sellerSchoolLike(String school) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(school)) {
                return null;
            }
            String pattern = "%" + school.trim().toLowerCase() + "%";
            Join<Product, User> sellerJoin = root.join("seller", JoinType.LEFT);
            return cb.like(cb.lower(sellerJoin.get("school")), pattern);
        };
    }

    public static Specification<Product> sellerSchoolEquals(String school) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(school)) {
                return null;
            }
            Join<Product, User> sellerJoin = root.join("seller", JoinType.LEFT);
            return cb.equal(cb.lower(sellerJoin.get("school")), school.trim().toLowerCase());
        };
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) {
                return null;
            }
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("price"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> hasImages(Boolean onlyWithImages) {
        return (root, query, cb) -> Boolean.TRUE.equals(onlyWithImages) ? cb.isNotNull(root.get("images")) : null;
    }

    public static Specification<Product> createdAfter(LocalDateTime time) {
        return (root, query, cb) -> time == null ? null : cb.greaterThanOrEqualTo(root.get("createTime"), time);
    }

    public static Specification<Product> matchClauses(List<ProductSearchClause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return null;
        }
        Specification<Product> spec = null;
        for (ProductSearchClause clause : clauses) {
            Specification<Product> clauseSpec = buildClauseSpecification(clause);
            if (clauseSpec == null) {
                continue;
            }
            if (spec == null) {
                spec = clauseSpec;
                continue;
            }
            if (clause.getRelation() == ProductSearchClause.Relation.OR) {
                spec = spec.or(clauseSpec);
            } else {
                spec = spec.and(clauseSpec);
            }
        }
        return spec;
    }

    private static Specification<Product> buildClauseSpecification(ProductSearchClause clause) {
        if (clause == null || clause.getField() == null || clause.getOperator() == null) {
            return null;
        }
        return switch (clause.getField()) {
            case CATEGORY -> enumSpecification("category", clause, ProductCategory.class);
            case STATUS -> enumSpecification("status", clause, ProductStatus.class);
            case AUDIT_STATUS -> enumSpecification("auditStatus", clause, AuditStatus.class);
            case PRICE -> priceSpecification(clause);
            case LOCATION -> stringSpecification("location", clause);
            case SELLER_SCHOOL -> sellerSchoolSpecification(clause);
            case CREATED_TIME -> dateSpecification(clause);
        };
    }

    private static <E extends Enum<E>> Specification<Product> enumSpecification(String attribute,
                                                                                ProductSearchClause clause,
                                                                                Class<E> enumType) {
        List<E> values = clause.getValues() == null ? Collections.emptyList() : clause.getValues().stream()
                .map(value -> safeEnumValue(enumType, value))
                .filter(value -> value != null)
            .collect(Collectors.toList());
        if (values.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> switch (clause.getOperator()) {
            case EQ -> cb.equal(root.get(attribute), values.get(0));
            case NE -> cb.notEqual(root.get(attribute), values.get(0));
            case IN -> root.get(attribute).in(values);
            case NOT_IN -> cb.not(root.get(attribute).in(values));
            default -> null;
        };
    }

    private static Specification<Product> priceSpecification(ProductSearchClause clause) {
        List<BigDecimal> numbers = clause.getValues() == null ? Collections.emptyList() : clause.getValues().stream()
                .map(ProductSpecifications::safeBigDecimal)
                .filter(value -> value != null)
            .collect(Collectors.toList());
        if (numbers.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> switch (clause.getOperator()) {
            case GT -> cb.greaterThan(root.get("price"), numbers.get(0));
            case GTE -> cb.greaterThanOrEqualTo(root.get("price"), numbers.get(0));
            case LT -> cb.lessThan(root.get("price"), numbers.get(0));
            case LTE -> cb.lessThanOrEqualTo(root.get("price"), numbers.get(0));
            case BETWEEN -> numbers.size() < 2 ? null : cb.between(root.get("price"), numbers.get(0), numbers.get(1));
            case EQ -> cb.equal(root.get("price"), numbers.get(0));
            case NE -> cb.notEqual(root.get("price"), numbers.get(0));
            default -> null;
        };
    }

    private static Specification<Product> stringSpecification(String attribute, ProductSearchClause clause) {
        if (clause.getValues() == null || clause.getValues().isEmpty()) {
            return null;
        }
        String value = clause.getValues().get(0);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String pattern = value.trim().toLowerCase(Locale.ENGLISH);
        return (root, query, cb) -> {
            switch (clause.getOperator()) {
                case EQ:
                    return cb.equal(cb.lower(root.get(attribute)), pattern);
                case NE:
                    return cb.notEqual(cb.lower(root.get(attribute)), pattern);
                case CONTAINS:
                    return cb.like(cb.lower(root.get(attribute)), "%" + pattern + "%");
                case STARTS_WITH:
                    return cb.like(cb.lower(root.get(attribute)), pattern + "%");
                case ENDS_WITH:
                    return cb.like(cb.lower(root.get(attribute)), "%" + pattern);
                default:
                    return null;
            }
        };
    }

    private static Specification<Product> sellerSchoolSpecification(ProductSearchClause clause) {
        if (clause.getValues() == null || clause.getValues().isEmpty()) {
            return null;
        }
        String value = clause.getValues().get(0);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String pattern = value.trim().toLowerCase(Locale.ENGLISH);
        return (root, query, cb) -> {
            Join<Product, User> sellerJoin = root.join("seller", JoinType.LEFT);
            switch (clause.getOperator()) {
                case EQ:
                    return cb.equal(cb.lower(sellerJoin.get("school")), pattern);
                case NE:
                    return cb.notEqual(cb.lower(sellerJoin.get("school")), pattern);
                case CONTAINS:
                    return cb.like(cb.lower(sellerJoin.get("school")), "%" + pattern + "%");
                case STARTS_WITH:
                    return cb.like(cb.lower(sellerJoin.get("school")), pattern + "%");
                case ENDS_WITH:
                    return cb.like(cb.lower(sellerJoin.get("school")), "%" + pattern);
                default:
                    return null;
            }
        };
    }

    private static Specification<Product> dateSpecification(ProductSearchClause clause) {
        List<LocalDateTime> dates = clause.getValues() == null ? Collections.emptyList() : clause.getValues().stream()
                .map(ProductSpecifications::safeDateTime)
                .filter(value -> value != null)
            .collect(Collectors.toList());
        if (dates.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> switch (clause.getOperator()) {
            case GT -> cb.greaterThan(root.get("createTime"), dates.get(0));
            case GTE -> cb.greaterThanOrEqualTo(root.get("createTime"), dates.get(0));
            case LT -> cb.lessThan(root.get("createTime"), dates.get(0));
            case LTE -> cb.lessThanOrEqualTo(root.get("createTime"), dates.get(0));
            case BETWEEN -> dates.size() < 2 ? null : cb.between(root.get("createTime"), dates.get(0), dates.get(1));
            case EQ -> cb.equal(root.get("createTime"), dates.get(0));
            case NE -> cb.notEqual(root.get("createTime"), dates.get(0));
            default -> null;
        };
    }

    private static <E extends Enum<E>> E safeEnumValue(Class<E> enumType, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static BigDecimal safeBigDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDateTime safeDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
