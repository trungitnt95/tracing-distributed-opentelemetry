package com.microservices.aa.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class CreateOrderCommand {
    @TargetAggregateIdentifier
    private String orderId;
    private String productId;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
