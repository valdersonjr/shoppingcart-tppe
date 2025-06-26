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

    List<CartItem> findByShoppingCartId(Long shoppingCartId);

    Optional<CartItem> findByShoppingCartIdAndProductId(Long shoppingCartId, Long productId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.shoppingCartId = :cartId")
    void deleteAllByShoppingCartId(@Param("cartId") Long cartId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.shoppingCartId = :cartId AND ci.productId = :productId")
    void deleteByShoppingCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}