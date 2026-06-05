package com.campus.trade.dto.product;

import com.campus.trade.model.enums.RecommendationEventType;
import com.campus.trade.model.enums.RecommendationScene;
import jakarta.validation.constraints.NotNull;

public class RecommendationEventRequest {

    @NotNull
    private Long productId;

    @NotNull
    private RecommendationEventType eventType;

    private RecommendationScene scene = RecommendationScene.HOME;

    private String metadata;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public RecommendationEventType getEventType() {
        return eventType;
    }

    public void setEventType(RecommendationEventType eventType) {
        this.eventType = eventType;
    }

    public RecommendationScene getScene() {
        return scene;
    }

    public void setScene(RecommendationScene scene) {
        this.scene = scene;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
