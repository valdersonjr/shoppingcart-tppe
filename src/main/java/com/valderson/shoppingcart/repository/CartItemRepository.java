package com.valderson.shoppingcart.repository;

import com.valderson.shoppingcart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product WHERE ci.shoppingCart.id = :cartId")
    List<CartItem> findByShoppingCartIdWithProduct(@Param("cartId") Long cartId);

    // Consulta todos os itens de um carrinho específico
    List<CartItem> findAllByShoppingCartId(Long shoppingCartId);

    // Consulta usando navegação por atributos
    List<CartItem> findByShoppingCart_Id(Long shoppingCartId);

    Optional<CartItem> findByShoppingCart_IdAndProduct_Id(Long shoppingCartId, Long productId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.shoppingCart.id = :cartId")
    void deleteAllByShoppingCartId(@Param("cartId") Long cartId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.shoppingCart.id = :cartId AND ci.product.id = :productId")
    void deleteByShoppingCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}