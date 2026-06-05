package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.cart.AddCartItemRequest;
import com.campus.trade.dto.cart.CartItemResponse;
import com.campus.trade.dto.cart.CartSummaryResponse;
import com.campus.trade.dto.cart.UpdateCartItemRequest;
import com.campus.trade.exception.BusinessException;
import com.campus.trade.exception.ErrorCode;
import com.campus.trade.model.entity.CartItem;
import com.campus.trade.model.entity.Product;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.ProductStatus;
import com.campus.trade.repository.CartItemRepository;
import com.campus.trade.repository.ProductRepository;
import com.campus.trade.repository.UserRepository;
import com.campus.trade.util.CartMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository,
                       UserRepository userRepository,
                       ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CartItemResponse addItem(String username, AddCartItemRequest request) {
        User user = loadUser(username);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "商品已下架或售罄");
        }
        if (product.getSeller().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PRODUCT_STATUS_INVALID, "无法将自己的商品加入购物车");
        }
        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "数量必须大于 0");
        }
        // 检查商品是否已经在购物车中，如果存在则累加数量
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .map(existing -> {
                    // 如果商品已存在，累加数量
                    existing.setQuantity(existing.getQuantity() + quantity);
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> {
                    // 如果商品不存在，创建新记录
                    CartItem item = new CartItem();
                    item.setUser(user);
                    item.setProduct(product);
                    item.setQuantity(quantity);
                    return cartItemRepository.save(item);
                });
        return CartMapper.toResponse(cartItem);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CartItemResponse> listItems(String username, int page, int size) {
        User user = loadUser(username);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<CartItemResponse> pageResult = cartItemRepository.findByUserId(user.getId(), pageable)
                .map(CartMapper::toResponse);
        return PaginatedResponse.of(pageResult.getContent(), safePage, safeSize, pageResult.getTotalElements());
    }

    @Transactional
    public CartItemResponse updateQuantity(String username, Long itemId, UpdateCartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "数量必须大于 0");
        }
        User user = loadUser(username);
        CartItem cartItem = cartItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItem.setQuantity(request.getQuantity());
        CartItem saved = cartItemRepository.save(cartItem);
        return CartMapper.toResponse(saved);
    }

    @Transactional
    public void removeItem(String username, Long itemId) {
        User user = loadUser(username);
        CartItem cartItem = cartItemRepository.findByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void removeItemByProduct(String username, Long productId) {
        User user = loadUser(username);
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void clearCart(String username) {
        User user = loadUser(username);
        cartItemRepository.deleteAllByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public long countItems(String username) {
        User user = loadUser(username);
        return cartItemRepository.countByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public CartSummaryResponse getSummary(String username) {
        User user = loadUser(username);
        List<CartItem> items = cartItemRepository.findAllByUserId(user.getId());
        int totalQuantity = items.stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();
        BigDecimal totalAmount = items.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getPrice() != null)
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CartSummaryResponse summary = new CartSummaryResponse();
        summary.setTotalQuantity(totalQuantity);
        summary.setUniqueProducts(items.size());
        summary.setTotalAmount(totalAmount);
        return summary;
    }

    private User loadUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
