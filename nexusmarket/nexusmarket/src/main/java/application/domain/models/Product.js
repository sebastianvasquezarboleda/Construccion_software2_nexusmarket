class Product {
    constructor({
        id,
        name,
        description,
        type,
        variants = [],
        status
    }) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.variants = variants;
        this.status = status;
    }
}

module.exports = Product;