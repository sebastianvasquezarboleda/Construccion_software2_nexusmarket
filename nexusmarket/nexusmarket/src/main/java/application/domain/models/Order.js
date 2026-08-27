class Order {
    constructor({
        id,
        buyerId,
        items = [],
        status,
        total
    }) {
        this.id = id;
        this.buyerId = buyerId;
        this.items = items;
        this.status = status;
        this.total = total;
    }
}

module.exports = Order;