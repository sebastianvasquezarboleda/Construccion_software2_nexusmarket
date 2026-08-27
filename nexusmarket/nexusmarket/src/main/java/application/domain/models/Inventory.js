class Inventory {
    constructor({
        id,
        productId,
        warehouseId,
        quantity
    }) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }
}

module.exports = Inventory;