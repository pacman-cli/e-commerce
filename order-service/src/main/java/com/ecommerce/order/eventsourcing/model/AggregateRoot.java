package com.ecommerce.order.eventsourcing.model;

import java.util.List;

/**
 * Aggregate Root Base Class for Event Sourcing
 * 
 * An aggregate is a consistency boundary containing entities and value objects.
 * In event sourcing, aggregates are rebuilt by replaying their events.
 * 
 * Key Principles:
 * 1. State changes only through events
 * 2. Aggregate is the transaction boundary
 * 3. Apply events to mutate state
 * 4. Uncommitted events pending persistence
 * 
 * Example Order Aggregate:
 * ```java
 * public class Order extends AggregateRoot {
 *     private OrderStatus status;
 *     private List<OrderItem> items;
 *     
 *     public static Order create(String userId, List<Item> items) {
 *         Order order = new Order();
 *         order.apply(new OrderCreatedEvent(userId, items));
 *         return order;
 *     }
 *     
 *     public void pay() {
 *         if (status != OrderStatus.CREATED) {
 *             throw new IllegalStateException("Order must be created");
 *         }
 *         apply(new OrderPaidEvent(id));
 *     }
 *     
 *     private void on(OrderCreatedEvent event) {
 *         this.status = OrderStatus.CREATED;
 *         this.items = event.getItems();
 *     }
 *     
 *     private void on(OrderPaidEvent event) {
 *         this.status = OrderStatus.PAID;
 *     }
 * }
 * ```
 */
public abstract class AggregateRoot {
    
    private String id;
    private long version = 0;
    private List<Event> uncommittedEvents = new java.util.ArrayList<>();
    private boolean isReplaying = false;
    
    /**
     * Apply a new event to the aggregate
     * 1. Append to uncommitted events
     * 2. Invoke event handler
     * 3. Increment version
     */
    protected void apply(Event event) {
        uncommittedEvents.add(event);
        handleEvent(event);
        
        if (!isReplaying) {
            version++;
        }
    }
    
    /**
     * Handle event by invoking appropriate "on" method
     * Uses reflection to call on(EventType) methods
     */
    @SuppressWarnings("unchecked")
    protected void handleEvent(Event event) {
        try {
            String methodName = "on";
            Class<?> eventClass = event.getClass();
            
            java.lang.reflect.Method method = findEventHandler(eventClass);
            if (method != null) {
                method.setAccessible(true);
                method.invoke(this, event);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle event: " + event.getEventType(), e);
        }
    }
    
    /**
     * Find event handler method for the given event type
     */
    private java.lang.reflect.Method findEventHandler(Class<?> eventClass) {
        String methodName = "on";
        
        // Look for method "on(EventClass)"
        for (java.lang.reflect.Method method : this.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName) && 
                method.getParameterCount() == 1 &&
                method.getParameterTypes()[0].equals(eventClass)) {
                return method;
            }
        }
        
        // Look for method "on(Event)" as fallback
        for (java.lang.reflect.Method method : this.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName) && 
                method.getParameterCount() == 1 &&
                method.getParameterTypes()[0].equals(Event.class)) {
                return method;
            }
        }
        
        return null;
    }
    
    /**
     * Rehydrate aggregate from historical events
     * Used to rebuild aggregate state from event store
     */
    public void rehydrate(List<Event> events) {
        isReplaying = true;
        
        for (Event event : events) {
            handleEvent(event);
            version = event.getVersion();
        }
        
        isReplaying = false;
    }
    
    /**
     * Mark events as committed (clear uncommitted list)
     * Call this after successfully persisting events
     */
    public void markCommitted() {
        uncommittedEvents.clear();
    }
    
    /**
     * Get uncommitted events that need to be persisted
     */
    public List<Event> getUncommittedEvents() {
        return new java.util.ArrayList<>(uncommittedEvents);
    }
    
    /**
     * Check if there are uncommitted events
     */
    public boolean hasUncommittedEvents() {
        return !uncommittedEvents.isEmpty();
    }
    
    /**
     * Get current version of the aggregate
     */
    public long getVersion() {
        return version;
    }
    
    /**
     * Get aggregate ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Set aggregate ID (typically called during creation)
     */
    protected void setId(String id) {
        this.id = id;
    }
    
    /**
     * Get aggregate type
     */
    public abstract String getAggregateType();
    
    @Override
    public String toString() {
        return String.format("%s{id=%s, version=%d, uncommitted=%d}",
            getClass().getSimpleName(), id, version, uncommittedEvents.size());
    }
}
