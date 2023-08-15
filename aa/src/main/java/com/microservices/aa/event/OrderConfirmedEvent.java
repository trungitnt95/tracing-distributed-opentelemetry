package com.microservices.aa.event;

public class OrderConfirmedEvent {
    private String orderId;

    public OrderConfirmedEvent() {
    }

    public OrderConfirmedEvent(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
