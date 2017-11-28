package org.functionalrefactoring

import org.functionalrefactoring.Option.None
import org.functionalrefactoring.Option.Some
import org.functionalrefactoring.models.*
import java.math.BigDecimal

sealed class Option<out A> {

    object None : Option<Nothing>()

    class Some<out A>(val value: A) : Option<A>()

    inline infix fun <B> map(f: (A) -> B): Option<B> = when (this) {
        is None -> this
        is Some -> Some(f(value))
    }

    inline fun <B, C> map2(option: Option<C>, f: (A,C) -> B): Option<B> = when (this) {
        is None -> this
        is Some -> option.flatMap {  v -> Some(f(value, v)) }
    }

    inline infix fun <B> flatMap(f: (A) -> Option<B>): Option<B> = when (this) {
        is None -> this
        is Some -> f(value)
    }

    fun <B> apply(f: Option<(A) -> B>) : Option<B> = when (this) {
        is None -> this
        is Some -> f.flatMap { function -> Some(function(value)) }
    }

}

object App {
    fun applyDiscount(cartId: CartId, storage: Storage<Cart>) {
        val cart = loadCart(cartId)
        val discountRuleOption: Option<(Cart) -> Amount> = cart.flatMap { c -> lookupDiscountRule(c.customerId) }
        val amountOption: Option<Amount> = cart.apply(discountRuleOption)
        val updatedCartOption: Option<Cart> = cart.map2(amountOption, ::updateAmount)
        updatedCartOption.map { uc -> save(uc, storage) }
    }
}

private fun loadCart(id: CartId): Option<Cart> {
    if (id.value.contains("gold"))
        return Some(Cart(id, CustomerId("gold-customer"), Amount(BigDecimal(100))))
    return if (id.value.contains("normal")) Some(Cart(id, CustomerId("normal-customer"), Amount(BigDecimal(100)))) else None
}

private fun lookupDiscountRule(id: CustomerId): Option<(Cart) -> Amount> {
    return if (id.value.contains("gold")) Some({ cart -> half(cart) })
    else None
}

private fun updateAmount(cart: Cart, discount: Amount): Cart {
    return Cart(cart.id, cart.customerId, Amount(cart.amount.value.subtract(discount.value)))
}

private fun save(cart: Cart, storage: Storage<Cart>) {
    storage.flush(cart)
}

private fun half(cart: Cart): Amount {
    return Amount(cart.amount.value.divide(BigDecimal(2)))
}