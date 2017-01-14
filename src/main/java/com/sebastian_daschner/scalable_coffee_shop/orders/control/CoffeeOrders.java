package com.sebastian_daschner.scalable_coffee_shop.orders.control;

import com.sebastian_daschner.scalable_coffee_shop.events.entity.AbstractEvent;
import com.sebastian_daschner.scalable_coffee_shop.orders.entity.*;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Contains the {@link CoffeeOrder} aggregates.
 * Handles, dispatches & applies internal events.
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CoffeeOrders {

    private Map<UUID, CoffeeOrder> coffeeOrders = new ConcurrentHashMap<>();

    @Inject
    OrderEventStore eventStore;

    @Inject
    Event<AbstractEvent> replayEvents;

    @PostConstruct
    private void init() {
        eventStore.getEvents().forEach(replayEvents::fire);
    }

    public CoffeeOrder get(final UUID orderId) {
        return coffeeOrders.get(orderId);
    }

    public void apply(@Observes OrderPlaced event) {
        coffeeOrders.putIfAbsent(event.getOrderInfo().getOrderId(), new CoffeeOrder());
        applyFor(event.getOrderInfo().getOrderId(), o -> o.place(event.getOrderInfo()));
    }

    public void apply(@Observes OrderCancelled event) {
        applyFor(event.getOrderId(), CoffeeOrder::cancel);
    }

    public void apply(@Observes OrderAccepted event) {
        applyFor(event.getOrderInfo().getOrderId(), CoffeeOrder::accept);
    }

    public void apply(@Observes OrderStarted event) {
        applyFor(event.getOrderId(), CoffeeOrder::start);
    }

    public void apply(@Observes OrderFinished event) {
        applyFor(event.getOrderId(), CoffeeOrder::finish);
    }

    public void apply(@Observes OrderDelivered event) {
        applyFor(event.getOrderId(), CoffeeOrder::deliver);
    }

    private void applyFor(final UUID orderId, final Consumer<CoffeeOrder> consumer) {
        final CoffeeOrder coffeeOrder = coffeeOrders.get(orderId);
        if (coffeeOrder != null)
            consumer.accept(coffeeOrder);
    }

}
