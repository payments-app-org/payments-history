package com.paymentsapp.history.ingest;

import com.paymentsapp.history.operation.CategoryCode;
import java.util.Arrays;
import java.util.Optional;

// Категория операции по типу платежа; оплата поставщику (BILL) — по группе поставщика
final class PaymentCategories {

    private PaymentCategories() {
    }

    // PaymentType в payment
    enum PaymentType {
        SBP_PHONE(CategoryCode.SBP_TRANSFER),
        CARD2CARD(CategoryCode.CARD_TRANSFER),
        OWN_TRANSFER(CategoryCode.OWN_TRANSFER),
        ABROAD(CategoryCode.ABROAD_TRANSFER),
        WALLET_TRANSFER(CategoryCode.WALLET_TRANSFER),
        REQUISITES(CategoryCode.REQUISITES),
        REQUISITES_OUR(CategoryCode.REQUISITES),
        BILL(null);

        private final CategoryCode category;

        PaymentType(CategoryCode category) {
            this.category = category;
        }
    }

    // Группа поставщика в biller
    enum ProviderGroup {
        MOBILE(CategoryCode.MOBILE),
        FOREIGN_MOBILE(CategoryCode.MOBILE),
        GOV(CategoryCode.GOV),
        TRANSPORT(CategoryCode.TRANSPORT),
        GAMES(CategoryCode.GAMES),
        EMONEY(CategoryCode.EMONEY),
        EDUCATION(CategoryCode.EDUCATION),
        SHOPS(CategoryCode.MARKETPLACES),
        TRAVEL(CategoryCode.TRAVEL),
        INSURANCE(CategoryCode.INSURANCE),
        SECURITY(CategoryCode.SECURITY),
        DONATIONS(CategoryCode.DONATIONS),
        UTILITIES(CategoryCode.UTILITIES),
        INTERNET(CategoryCode.INTERNET);

        private final CategoryCode category;

        ProviderGroup(CategoryCode category) {
            this.category = category;
        }
    }

    static CategoryCode of(String paymentType, String providerGroup) {
        Optional<PaymentType> type = find(PaymentType.class, paymentType);
        if (type.isEmpty()) {
            return CategoryCode.OTHER;
        }
        if (type.get() == PaymentType.BILL) {
            return find(ProviderGroup.class, providerGroup).map(g -> g.category).orElse(CategoryCode.OTHER);
        }
        return type.get().category;
    }

    private static <E extends Enum<E>> Optional<E> find(Class<E> type, String name) {
        return Arrays.stream(type.getEnumConstants()).filter(c -> c.name().equals(name)).findFirst();
    }
}
