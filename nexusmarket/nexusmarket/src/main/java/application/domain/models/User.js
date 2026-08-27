class User {
    constructor({
        id,
        fullName,
        email,
        role,
        status
    }) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
    }
}

module.exports = User;