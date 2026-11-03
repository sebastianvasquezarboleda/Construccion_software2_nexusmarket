package application.api;

import application.domain.enums.OrderStatus;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.Role;
import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserStatus;
import application.domain.enums.WarehouseType;
import application.domain.models.AdministrativeSummary;
import application.domain.models.Buyer;
import application.domain.models.Invoice;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.PageResult;
import application.domain.models.ReturnRequest;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.in.BuyerInputPort;
import application.domain.ports.in.InvoiceInputPort;
import application.domain.ports.in.InventoryInputPort;
import application.domain.ports.in.OrderInputPort;
import application.domain.ports.in.ProductInputPort;
import application.domain.ports.in.ReturnInputPort;
import application.domain.ports.in.SellerInputPort;
import application.domain.ports.in.ShipmentInputPort;
import application.domain.ports.in.UserInputPort;
import application.domain.ports.in.WarehouseInputPort;
import application.services.reports.AdministrativeReportService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class NexusmarketApiController {
    private final UserInputPort userService;
    private final BuyerInputPort buyerService;
    private final SellerInputPort sellerService;
    private final WarehouseInputPort warehouseService;
    private final ProductInputPort productService;
    private final InventoryInputPort inventoryService;
    private final OrderInputPort orderService;
    private final InvoiceInputPort invoiceService;
    private final ShipmentInputPort shipmentService;
    private final ReturnInputPort returnService;
    private final AdministrativeReportService reportService;

    public NexusmarketApiController(
            UserInputPort userService,
            BuyerInputPort buyerService,
            SellerInputPort sellerService,
            WarehouseInputPort warehouseService,
            ProductInputPort productService,
            InventoryInputPort inventoryService,
            OrderInputPort orderService,
            InvoiceInputPort invoiceService,
            ShipmentInputPort shipmentService,
            ReturnInputPort returnService,
            AdministrativeReportService reportService) {
        this.userService = userService;
        this.buyerService = buyerService;
        this.sellerService = sellerService;
        this.warehouseService = warehouseService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.shipmentService = shipmentService;
        this.returnService = returnService;
        this.reportService = reportService;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrfToken(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName());
    }

    @GetMapping("/users")
    public PageResult<User> listUsers(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
        return userService.findPage(page, size);
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id, Authentication authentication) {
        User user = userService.findById(id);
        User current = currentUser(authentication);
        if (current.getRole() != Role.ADMINISTRATOR && current.getRole() != Role.SUPERVISOR
                && !current.getId().equals(user.getId())) {
            throw new AccessDeniedException("Users can only access their own account");
        }
        return user;
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
        User user = new User(request.id(), request.fullName(), request.email(), request.documentNumber(),
                request.role(), request.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(user, request.password()));
    }

    @PatchMapping("/users/{id}/activate")
    public User activateUser(@PathVariable Long id) {
        userService.activate(id);
        return userService.findById(id);
    }

    @PatchMapping("/users/{id}/block")
    public User blockUser(@PathVariable Long id) {
        userService.block(id);
        return userService.findById(id);
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<Void> changeUserPassword(@PathVariable Long id, @RequestBody PasswordRequest request) {
        userService.changePassword(id, request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buyers")
    public PageResult<Buyer> listBuyers(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return buyerService.findPage(page, size);
    }

    @GetMapping("/buyers/{id}")
    public Buyer getBuyer(@PathVariable Long id, Authentication authentication) {
        requireBuyerAccess(id, authentication);
        return buyerService.findById(id);
    }

    @PostMapping("/buyers")
    public ResponseEntity<Buyer> createBuyer(@RequestBody BuyerRequest request, Authentication authentication) {
        User user = userService.findById(request.userId());
        if (user.getRole() != Role.BUYER) throw new IllegalArgumentException("The selected user is not a buyer");
        if (currentUser(authentication).getRole() != Role.ADMINISTRATOR && !user.getId().equals(currentUser(authentication).getId())) {
            throw new AccessDeniedException("Buyers can only register their own profile");
        }
        Buyer buyer = new Buyer(request.id(), request.userId(), request.mainAddress(),
                request.additionalAddresses(), request.commercialStatus());
        return ResponseEntity.status(HttpStatus.CREATED).body(buyerService.register(buyer));
    }

    @PostMapping("/buyers/{id}/addresses")
    public Buyer addBuyerAddress(@PathVariable Long id, @RequestBody AddressRequest request, Authentication authentication) {
        requireBuyerAccess(id, authentication);
        buyerService.addAddress(id, request.address());
        return buyerService.findById(id);
    }

    @DeleteMapping("/buyers/{id}/addresses")
    public Buyer removeBuyerAddress(@PathVariable Long id, @RequestParam String address, Authentication authentication) {
        requireBuyerAccess(id, authentication);
        buyerService.removeAddress(id, address);
        return buyerService.findById(id);
    }

    @GetMapping("/sellers")
    public PageResult<Seller> listSellers(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return sellerService.findPage(page, size);
    }

    @GetMapping("/sellers/{id}")
    public Seller getSeller(@PathVariable Long id, Authentication authentication) {
        Seller seller = sellerService.findById(id);
        User current = currentUser(authentication);
        if (current.getRole() != Role.ADMINISTRATOR && current.getRole() != Role.SUPERVISOR
                && (current.getRole() != Role.SELLER || !seller.userId().equals(current.getId()))) {
            throw new AccessDeniedException("Sellers can only access their own seller profile");
        }
        return seller;
    }

    @PostMapping("/sellers")
    public ResponseEntity<Seller> createSeller(@RequestBody SellerRequest request) {
        User user = userService.findById(request.userId());
        if (user.getRole() != Role.SELLER) throw new IllegalArgumentException("The selected user is not a seller");
        Seller seller = new Seller(request.id(), request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerService.register(seller));
    }

    @GetMapping("/warehouses")
    public PageResult<Warehouse> listWarehouses(Authentication authentication,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        User current = currentUser(authentication);
        if (current.getRole() != Role.SELLER) return warehouseService.findPage(page, size);
        Long sellerId = sellerService.findByUserId(current.getId()).id();
        return warehouseService.findPageForSeller(sellerId, page, size);
    }

    @GetMapping("/warehouses/{id}")
    public Warehouse getWarehouse(@PathVariable Long id, Authentication authentication) {
        Warehouse warehouse = warehouseService.findById(id);
        User current = currentUser(authentication);
        if (current.getRole() == Role.SELLER && warehouse.type() == WarehouseType.SELLER
                && !sellerService.findByUserId(current.getId()).id().equals(warehouse.sellerId())) {
            throw new AccessDeniedException("Sellers can only access their own warehouses");
        }
        return warehouse;
    }

    @PostMapping("/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody WarehouseRequest request,
                                                      Authentication authentication) {
        User current = currentUser(authentication);
        if (request.type() == WarehouseType.SELLER) {
            Seller seller = sellerService.findById(request.sellerId());
            if (current.getRole() == Role.SELLER && !seller.userId().equals(current.getId())) {
                throw new AccessDeniedException("Sellers can only create their own warehouses");
            }
        } else if (current.getRole() != Role.ADMINISTRATOR) {
            throw new AccessDeniedException("Only administrators can create Marketplace warehouses");
        }
        Warehouse warehouse = new Warehouse(request.id(), request.name(), request.type(), request.sellerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.register(warehouse));
    }

    @GetMapping("/products")
    public PageResult<Product> listProducts(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return productService.findPage(page, size);
    }

    @GetMapping("/products/{id}")
    public Product getProduct(@PathVariable Long id) { return productService.findById(id); }

    @PostMapping("/products")
    public ResponseEntity<Product> createProduct(@RequestBody ProductRequest request, Authentication authentication) {
        Seller seller = sellerService.findById(request.sellerId());
        User current = currentUser(authentication);
        if (current.getRole() == Role.SELLER && !seller.userId().equals(current.getId())) {
            throw new AccessDeniedException("Sellers can only manage their own products");
        }
        Product product = new Product(request.id(), request.sellerId(), request.name(), request.description(),
                request.type(), request.variants(), request.status(), request.price());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.register(product));
    }

    @PatchMapping("/products/{id}/price")
    public Product updateProductPrice(@PathVariable Long id, @RequestBody PriceRequest request,
                                      Authentication authentication) {
        requireProductAccess(id, authentication);
        productService.updatePrice(id, request.price());
        return productService.findById(id);
    }

    @PatchMapping("/products/{id}/publish")
    public Product publishProduct(@PathVariable Long id, Authentication authentication) {
        requireProductAccess(id, authentication);
        productService.publish(id);
        return productService.findById(id);
    }

    @PatchMapping("/products/{id}/suspend")
    public Product suspendProduct(@PathVariable Long id, Authentication authentication) {
        requireProductAccess(id, authentication);
        productService.suspend(id);
        return productService.findById(id);
    }

    @PatchMapping("/products/{id}/discontinue")
    public Product discontinueProduct(@PathVariable Long id, Authentication authentication) {
        requireProductAccess(id, authentication);
        productService.discontinue(id);
        return productService.findById(id);
    }

    @PostMapping("/products/{id}/variants")
    public Product addVariant(@PathVariable Long id, @RequestBody VariantRequest request, Authentication authentication) {
        requireProductAccess(id, authentication);
        productService.addVariant(id, request.variant());
        return productService.findById(id);
    }

    @GetMapping("/inventories")
    public PageResult<Inventory> listInventories(Authentication authentication,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        User current = currentUser(authentication);
        if (current.getRole() != Role.SELLER) return inventoryService.findPage(page, size);
        Long sellerId = sellerService.findByUserId(current.getId()).id();
        List<Long> productIds = productService.findBySellerId(sellerId).stream()
                .map(Product::getId)
                .toList();
        return inventoryService.findPageForProducts(productIds, page, size);
    }

    @GetMapping("/inventories/{id}")
    public Inventory getInventory(@PathVariable Long id, Authentication authentication) {
        Inventory inventory = inventoryService.findById(id);
        requireInventoryAccess(inventory, authentication);
        return inventory;
    }

    @PostMapping("/inventories")
    public ResponseEntity<Inventory> createInventory(@RequestBody InventoryRequest request,
                                                      Authentication authentication) {
        Product product = productService.findById(request.productId());
        Warehouse warehouse = warehouseService.findById(request.warehouseId());
        if (!product.isPhysical()) {
            throw new IllegalArgumentException("Digital products do not have physical inventory");
        }
        requireInventoryAssociation(product, warehouse, authentication);
        Inventory inventory = new Inventory(request.id(), request.productId(), request.warehouseId(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.register(inventory));
    }

    @PatchMapping("/inventories/{id}/add")
    public Inventory addInventory(@PathVariable Long id, @RequestParam int amount, Authentication authentication) {
        requireInventoryAccess(inventoryService.findById(id), authentication);
        inventoryService.add(id, amount);
        return inventoryService.findById(id);
    }

    @PatchMapping("/inventories/{id}/reserve")
    public Inventory reserveInventory(@PathVariable Long id, @RequestParam int amount, Authentication authentication) {
        requireInventoryAccess(inventoryService.findById(id), authentication);
        inventoryService.reserve(id, amount);
        return inventoryService.findById(id);
    }

    @PatchMapping("/inventories/{id}/remove")
    public Inventory removeInventory(@PathVariable Long id, @RequestParam int amount, Authentication authentication) {
        requireInventoryAccess(inventoryService.findById(id), authentication);
        inventoryService.remove(id, amount);
        return inventoryService.findById(id);
    }

    @PatchMapping("/inventories/{id}/return")
    public Inventory returnInventory(@PathVariable Long id, @RequestParam int amount, Authentication authentication) {
        requireInventoryAccess(inventoryService.findById(id), authentication);
        inventoryService.returnInventory(id, amount);
        return inventoryService.findById(id);
    }

    @PatchMapping("/inventories/{id}/adjust")
    public Inventory adjustInventory(@PathVariable Long id, @RequestParam int quantity, Authentication authentication) {
        requireInventoryAccess(inventoryService.findById(id), authentication);
        inventoryService.adjust(id, quantity);
        return inventoryService.findById(id);
    }

    @PatchMapping("/inventories/{id}/damage")
    public Inventory markInventoryDamaged(@PathVariable Long id, @RequestParam int amount, Authentication authentication) {
        requireInventoryAccess(inventoryService.findById(id), authentication);
        inventoryService.markDamaged(id, amount);
        return inventoryService.findById(id);
    }

    @GetMapping("/orders")
    public PageResult<Order> listOrders(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return orderService.findPage(page, size);
    }

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id, Authentication authentication) {
        requireOrderAccess(id, authentication);
        return orderService.findById(id);
    }

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request, Authentication authentication) {
        User current = currentUser(authentication);
        if (current.getRole() == Role.BUYER && !buyerService.findByUserId(current.getId()).getId().equals(request.buyerId())) {
            throw new AccessDeniedException("Buyers can only create orders for themselves");
        }
        buyerService.findById(request.buyerId());
        Order order = orderService.create(request.id(), request.buyerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/orders/{id}/products")
    public Order addProductToOrder(@PathVariable Long id, @RequestBody OrderItemRequest request, Authentication authentication) {
        requireOrderAccess(id, authentication);
        orderService.addProduct(id, request.productId(), request.quantity());
        return orderService.findById(id);
    }

    @DeleteMapping("/orders/{id}/products/{productId}")
    public Order removeProductFromOrder(@PathVariable Long id, @PathVariable Long productId, Authentication authentication) {
        requireOrderAccess(id, authentication);
        orderService.removeProduct(id, productId);
        return orderService.findById(id);
    }

    @PatchMapping("/orders/{id}/payment")
    public Order sendToPayment(@PathVariable Long id) {
        orderService.sendToPayment(id);
        return orderService.findById(id);
    }

    @PatchMapping("/orders/{id}/paid")
    @Transactional
    public Order markAsPaid(@PathVariable Long id) {
        Order order = orderService.findById(id);
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            orderService.markAsPaid(id);
            order = orderService.findById(id);
        } else if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DISPATCHED
                && order.getStatus() != OrderStatus.FINALIZED) {
            throw new IllegalStateException("Only pending payment orders can be marked as paid");
        }
        inventoryService.confirmSaleAllocations(order.getId(), order.getItems().stream()
                .filter(item -> productService.findById(item.productId()).isPhysical())
                .flatMap(item -> item.allocations().stream())
                .toList());
        invoiceService.createForPaidOrder(order);
        if (order.getStatus() == OrderStatus.PAID && !hasPhysicalItems(order)) {
            orderService.dispatch(id);
            orderService.finalizeOrder(id);
            return orderService.findById(id);
        }
        return order;
    }

    @PatchMapping("/orders/{id}/dispatch")
    @Transactional
    public Order dispatchOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        if (hasPhysicalItems(order)) {
            Shipment shipment = shipmentService.findByOrderId(id);
            if (shipment.getStatus() != ShipmentStatus.PACKED) {
                throw new IllegalStateException("The shipment must be packed before dispatch");
            }
            orderService.dispatch(id);
            shipmentService.dispatch(shipment.getId());
            return orderService.findById(id);
        }
        orderService.dispatch(id);
        return orderService.findById(id);
    }

    @PatchMapping("/orders/{id}/finalize")
    @Transactional
    public Order finalizeOrder(@PathVariable Long id) {
        Order order = orderService.findById(id);
        if (hasPhysicalItems(order)) {
            Shipment shipment = shipmentService.findByOrderId(id);
            if (shipment.getStatus() != ShipmentStatus.DISPATCHED) {
                throw new IllegalStateException("The shipment must be dispatched before delivery confirmation");
            }
            orderService.finalizeOrder(id);
            shipmentService.deliver(shipment.getId());
            return orderService.findById(id);
        }
        orderService.finalizeOrder(id);
        return orderService.findById(id);
    }

    @PatchMapping("/orders/{id}/cancel")
    public Order cancelOrder(@PathVariable Long id, Authentication authentication) {
        requireOrderAccess(id, authentication);
        orderService.cancel(id);
        return orderService.findById(id);
    }

    @GetMapping("/orders/{id}/invoice")
    public Invoice getOrderInvoice(@PathVariable Long id, Authentication authentication) {
        requireOrderAccess(id, authentication);
        return invoiceService.findByOrderId(id);
    }

    @GetMapping("/invoices")
    public PageResult<Invoice> listInvoices(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return invoiceService.findPage(page, size);
    }

    @GetMapping("/invoices/{id}")
    public Invoice getInvoice(@PathVariable String id, Authentication authentication) {
        Invoice invoice = invoiceService.findById(id);
        requireBuyerAccess(invoice.buyerId(), authentication);
        return invoice;
    }

    @PostMapping("/shipments")
    public ResponseEntity<Shipment> createShipment(@RequestBody ShipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shipmentService.create(request.orderId(), request.deliveryAddress()));
    }

    @GetMapping("/shipments")
    public PageResult<Shipment> listShipments(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return shipmentService.findPage(page, size);
    }

    @GetMapping("/shipments/{id}")
    public Shipment getShipment(@PathVariable String id) { return shipmentService.findById(id); }

    @GetMapping("/orders/{id}/shipment")
    public Shipment getOrderShipment(@PathVariable Long id, Authentication authentication) {
        requireOrderAccess(id, authentication);
        return shipmentService.findByOrderId(id);
    }

    @PatchMapping("/shipments/{id}/pack")
    public Shipment packShipment(@PathVariable String id) {
        shipmentService.pack(id);
        return shipmentService.findById(id);
    }

    @PatchMapping("/shipments/{id}/dispatch")
    @Transactional
    public Shipment dispatchShipment(@PathVariable String id) {
        Shipment shipment = shipmentService.findById(id);
        dispatchOrder(shipment.getOrderId());
        return shipmentService.findById(id);
    }

    @PatchMapping("/shipments/{id}/deliver")
    @Transactional
    public Shipment deliverShipment(@PathVariable String id) {
        Shipment shipment = shipmentService.findById(id);
        finalizeOrder(shipment.getOrderId());
        return shipmentService.findById(id);
    }

    @PostMapping("/returns")
    public ResponseEntity<ReturnRequest> requestReturn(@RequestBody ReturnRequestBody request,
                                                       Authentication authentication) {
        requireOrderAccess(request.orderId(), authentication);
        if (!productService.findById(request.productId()).isPhysical()) {
            throw new IllegalArgumentException("Digital products cannot be returned as physical inventory");
        }
        ReturnRequest result = returnService.request(request.orderId(), request.productId(), request.quantity(), request.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/returns")
    public PageResult<ReturnRequest> listReturns(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return returnService.findPage(page, size);
    }

    @GetMapping("/returns/{id}")
    public ReturnRequest getReturn(@PathVariable String id, Authentication authentication) {
        ReturnRequest request = returnService.findById(id);
        requireBuyerAccess(request.getBuyerId(), authentication);
        return request;
    }

    @PatchMapping("/returns/{id}/approve")
    public ReturnRequest approveReturn(@PathVariable String id) {
        returnService.approve(id);
        return returnService.findById(id);
    }

    @PatchMapping("/returns/{id}/reject")
    public ReturnRequest rejectReturn(@PathVariable String id) {
        returnService.reject(id);
        return returnService.findById(id);
    }

    @PatchMapping("/returns/{id}/refund")
    public ReturnRequest recordRefund(@PathVariable String id) {
        returnService.recordRefund(id);
        return returnService.findById(id);
    }

    @GetMapping("/reports/summary")
    public AdministrativeSummary getAdministrativeSummary() { return reportService.summary(); }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return userService.findByEmail(authentication.getName());
    }

    private void requireInventoryAccess(Inventory inventory, Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() == Role.ADMINISTRATOR || user.getRole() == Role.LOGISTICS_OPERATOR
                || user.getRole() == Role.SUPERVISOR) return;
        if (user.getRole() != Role.SELLER) {
            throw new AccessDeniedException("The user cannot access inventory");
        }
        Long sellerId = sellerService.findByUserId(user.getId()).id();
        if (!productService.findById(inventory.getProductId()).getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("Sellers can only access inventory for their products");
        }
    }

    private void requireInventoryAssociation(Product product, Warehouse warehouse, Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() != Role.SELLER) return;
        Long sellerId = sellerService.findByUserId(user.getId()).id();
        if (!product.getSellerId().equals(sellerId)
                || (warehouse.type() == WarehouseType.SELLER && !sellerId.equals(warehouse.sellerId()))) {
            throw new AccessDeniedException("Sellers can only stock their products in authorized warehouses");
        }
    }

    private void requireBuyerAccess(Long buyerId, Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() == Role.ADMINISTRATOR || user.getRole() == Role.SUPERVISOR) return;
        if (user.getRole() != Role.BUYER || !buyerService.findById(buyerId).getUserId().equals(user.getId())) {
            throw new AccessDeniedException("Buyers can only access their own information");
        }
    }

    private void requireOrderAccess(Long orderId, Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() == Role.ADMINISTRATOR || user.getRole() == Role.SUPERVISOR) return;
        Order order = orderService.findById(orderId);
        if (user.getRole() != Role.BUYER || !buyerService.findById(order.getBuyerId()).getUserId().equals(user.getId())) {
            throw new AccessDeniedException("Buyers can only access their own orders");
        }
    }

    private void requireProductAccess(Long productId, Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() == Role.ADMINISTRATOR) return;
        Product product = productService.findById(productId);
        if (user.getRole() != Role.SELLER
                || !sellerService.findByUserId(user.getId()).id().equals(product.getSellerId())) {
            throw new AccessDeniedException("Sellers can only manage their own products");
        }
    }

    private boolean hasPhysicalItems(Order order) {
        return order.getItems().stream().anyMatch(item -> productService.findById(item.productId()).isPhysical());
    }

    public record UserRequest(Long id, String fullName, String email, String documentNumber,
                              Role role, UserStatus status, String password) {}

    public record CsrfTokenResponse(String token, String headerName) {}

    public record PasswordRequest(String password) {}

    public record BuyerRequest(Long id, Long userId, String mainAddress,
                               List<String> additionalAddresses, String commercialStatus) {}

    public record SellerRequest(Long id, Long userId) {}

    public record WarehouseRequest(Long id, String name, WarehouseType type, Long sellerId) {}

    public record ProductRequest(Long id, Long sellerId, String name, String description, ProductType type,
                                List<String> variants, ProductStatus status, BigDecimal price) {}

    public record PriceRequest(BigDecimal price) {}

    public record InventoryRequest(Long id, Long productId, Long warehouseId, int quantity) {}

    public record OrderRequest(Long id, Long buyerId) {}

    public record OrderItemRequest(Long productId, int quantity) {}

    public record AddressRequest(String address) {}

    public record VariantRequest(String variant) {}

    public record ShipmentRequest(Long orderId, String deliveryAddress) {}

    public record ReturnRequestBody(Long orderId, Long productId, int quantity, String reason) {}
}
