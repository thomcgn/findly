package dev.thomcgn.findly.price;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DealScoreService {

    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public String classify(BigDecimal listingPrice, BigDecimal marketPrice) {
        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) == 0) {
            return "FAIR";
        }

        BigDecimal differencePercent = listingPrice.subtract(marketPrice)
                .divide(marketPrice, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED);

        if (differencePercent.compareTo(BigDecimal.valueOf(-30)) <= 0) {
            return "VERY_GOOD";
        }
        if (differencePercent.compareTo(BigDecimal.valueOf(-10)) < 0) {
            return "GOOD";
        }
        if (differencePercent.compareTo(BigDecimal.valueOf(10)) <= 0) {
            return "FAIR";
        }
        if (differencePercent.compareTo(BigDecimal.valueOf(30)) <= 0) {
            return "EXPENSIVE";
        }
        return "VERY_EXPENSIVE";
    }

    public BigDecimal calculateDifferencePercent(BigDecimal listingPrice, BigDecimal marketPrice) {
        if (marketPrice == null || marketPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return listingPrice.subtract(marketPrice)
                .divide(marketPrice, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
