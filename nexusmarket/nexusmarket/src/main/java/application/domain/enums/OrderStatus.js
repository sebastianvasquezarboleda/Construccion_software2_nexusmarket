const OrderStatus = Object.freeze({
    CART: 'CART',
    PENDING_PAYMENT: 'PENDING_PAYMENT',
    PAID: 'PAID',
    DISPATCHED: 'DISPATCHED',
    FINALIZED: 'FINALIZED'
});

module.exports = OrderStatus;