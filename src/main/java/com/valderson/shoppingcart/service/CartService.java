package com.valderson.shoppingcart.service;

import com.valderson.shoppingcart.dto.request.AddToCartRequest;
import com.valderson.shoppingcart.dto.response.CartItemResponse;
import com.valderson.shoppingcart.dto.response.CartResponse;
import com.valderson.shoppingcart.entity.CartItem;
import com.valderson.shoppingcart.entity.Product;
import com.valderson.shoppingcart.entity.ShoppingCart;
import com.valderson.shoppingcart.entity.User;
import com.valderson.shoppingcart.repository.CartItemRepository;
import com.valderson.shoppingcart.repository.ProductRepository;
import com.valderson.shoppingcart.repository.ShoppingCartRepository;
import com.valderson.shoppingcart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartResponse getCartByUserId(Long userId) {
        ShoppingCart cart = findOrCreateCart(userId);
        List<CartItem> items = cart.getCartItems();

        return buildCartResponse(cart, items);
    }

    public CartResponse addItemToCart(Long userId, AddToCartRequest request) {
        // Verificar se produto existe
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        ShoppingCart cart = findOrCreateCart(userId);

        // Verificar se item já existe no carrinho
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(request.getProductId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Atualizar quantidade
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            // Criar novo item
            CartItem newItem = CartItem.builder()
                    .shoppingCart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        // Retornar carrinho atualizado
        return getCartByUserId(userId);
    }

    public CartResponse removeItemFromCart(Long userId, Long productId) {
        ShoppingCart cart = findOrCreateCart(userId);

        cart.getCartItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);

        return getCartByUserId(userId);
    }

    public BigDecimal getCartTotal(Long userId) {
        ShoppingCart cart = findOrCreateCart(userId);

        List<CartItem> items = cartItemRepository.findByShoppingCartIdWithProduct(cart.getId());

        return items.stream()
                .map(this::calculateItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void clearCart(Long userId) {
        ShoppingCart cart = findOrCreateCart(userId);
        cartItemRepository.deleteAllByShoppingCartId(cart.getId());
    }

    private ShoppingCart findOrCreateCart(Long userId) {
        // Buscar usuário
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Se o usuário já tem carrinho, retorna ele
        if (user.getShoppingCart() != null) {
            return user.getShoppingCart();
        }

        // Senão, cria um novo carrinho
        ShoppingCart newCart = ShoppingCart.builder()
                .user(user)
                .build();

        ShoppingCart savedCart = cartRepository.save(newCart);

        // Atualiza o relacionamento bidirecional
        user.setShoppingCart(savedCart);
        userRepository.save(user);

        return savedCart;
    }

    private CartResponse buildCartResponse(ShoppingCart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalAmount(total)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        Product product = item.getProduct();
        BigDecimal subtotal = calculateItemSubtotal(item);

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }

    private BigDecimal calculateItemSubtotal(CartItem item) {
        Product product = item.getProduct();
        return product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}