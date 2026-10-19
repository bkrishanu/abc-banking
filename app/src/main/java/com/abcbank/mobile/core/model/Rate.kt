package com.abcbank.mobile.core.model

/**
 * An interest rate (APY or APR) in basis points: 310 is 3.10%. Whole numbers, so rates never pick
 * up floating-point error before they reach a fee or EMI rule.
 */
@JvmInline
value class Rate(val basisPoints: Int) {
    init {
        require(basisPoints >= 0) { "A rate cannot be negative" }
    }

    companion object {
        const val BASIS_POINTS_PER_PERCENT = 100
    }
}
