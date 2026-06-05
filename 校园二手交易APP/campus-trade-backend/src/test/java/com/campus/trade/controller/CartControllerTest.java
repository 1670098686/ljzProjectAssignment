package com.campus.trade.controller;

import com.campus.trade.common.PageMeta;
import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.cart.AddCartItemRequest;
import com.campus.trade.dto.cart.CartItemResponse;
import com.campus.trade.dto.cart.CartSummaryResponse;
import com.campus.trade.dto.cart.UpdateCartItemRequest;
import com.campus.trade.security.AdminUserDetailsService;
import com.campus.trade.security.CustomUserDetailsService;
import com.campus.trade.security.JwtTokenProvider;
import com.campus.trade.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AdminUserDetailsService adminUserDetailsService;

        private static RequestPostProcessor buyer() {
        return user("buyer").roles("STUDENT");
    }

    @Test
    void addItem_shouldReturnCreatedItem() throws Exception {
        CartItemResponse response = new CartItemResponse();
        response.setId(10L);
        response.setQuantity(2);
        when(cartService.addItem(eq("buyer"), any(AddCartItemRequest.class))).thenReturn(response);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setProductId(5L);
        request.setQuantity(2);

        mockMvc.perform(post("/api/v1/cart/items")
                .with(csrf())
                .with(buyer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    void listItems_shouldReturnPaginatedPayload() throws Exception {
        CartItemResponse item = new CartItemResponse();
        item.setId(1L);
        PaginatedResponse<CartItemResponse> paginatedResponse = new PaginatedResponse<>(
                List.of(item), new PageMeta(1, 5, 1)
        );
        when(cartService.listItems(eq("buyer"), eq(1), eq(5))).thenReturn(paginatedResponse);

        mockMvc.perform(get("/api/v1/cart/items")
                .with(buyer())
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(1L))
                .andExpect(jsonPath("$.data.meta.page").value(1));
    }

    @Test
    void summary_shouldExposeTotals() throws Exception {
        CartSummaryResponse summaryResponse = new CartSummaryResponse();
        summaryResponse.setUniqueProducts(2);
        summaryResponse.setTotalQuantity(3);
        summaryResponse.setTotalAmount(new BigDecimal("88.00"));
        when(cartService.getSummary("buyer")).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/v1/cart/items/summary").with(buyer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uniqueProducts").value(2))
                .andExpect(jsonPath("$.data.totalAmount").value(88.00));
    }

    @Test
    void updateQuantity_shouldDelegateToService() throws Exception {
        CartItemResponse response = new CartItemResponse();
        response.setId(1L);
        response.setQuantity(5);
        when(cartService.updateQuantity(eq("buyer"), eq(1L), any(UpdateCartItemRequest.class))).thenReturn(response);

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(5);

        mockMvc.perform(patch("/api/v1/cart/items/1")
                .with(csrf())
                .with(buyer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    void clearCart_shouldReturnSuccess() throws Exception {
        doNothing().when(cartService).clearCart("buyer");

        mockMvc.perform(delete("/api/v1/cart/items")
                .with(csrf())
                .with(buyer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void removeItem_shouldDelegateToService() throws Exception {
        doNothing().when(cartService).removeItem("buyer", 10L);

        mockMvc.perform(delete("/api/v1/cart/items/10")
                .with(csrf())
                .with(buyer()))
                .andExpect(status().isOk());

        verify(cartService, times(1)).removeItem("buyer", 10L);
    }

    @Test
    void removeItemByProduct_shouldDelegateToService() throws Exception {
        doNothing().when(cartService).removeItemByProduct("buyer", 8L);

        mockMvc.perform(delete("/api/v1/cart/items/product/8")
                .with(csrf())
                .with(buyer()))
                .andExpect(status().isOk());

        verify(cartService, times(1)).removeItemByProduct("buyer", 8L);
    }

    @Test
    void count_shouldReturnTotalItems() throws Exception {
        when(cartService.countItems("buyer")).thenReturn(4L);

        mockMvc.perform(get("/api/v1/cart/items/count").with(buyer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(4));
    }
}
