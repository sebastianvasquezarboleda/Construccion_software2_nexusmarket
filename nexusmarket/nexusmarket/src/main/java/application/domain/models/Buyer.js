class Buyer {
    constructor({
        id,
        userId,
        mainAddress,
        additionalAddresses = [],
        commercialStatus
    }) {
        this.id = id;
        this.userId = userId;
        this.mainAddress = mainAddress;
        this.additionalAddresses = additionalAddresses;
        this.commercialStatus = commercialStatus;
    }
}

module.exports = Buyer;