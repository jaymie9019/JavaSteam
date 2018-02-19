package in.dragonbra.javasteam.enums;

import java.util.Arrays;

public enum ECurrencyCode {

    Invalid(0),
    USD(1),
    GBP(2),
    EUR(3),
    CHF(4),
    RUB(5),
    PLN(6),
    BRL(7),
    JPY(8),
    NOK(9),
    IDR(10),
    MYR(11),
    PHP(12),
    SGD(13),
    THB(14),
    VND(15),
    KRW(16),
    TRY(17),
    UAH(18),
    MXN(19),
    CAD(20),
    AUD(21),
    NZD(22),
    CNY(23),
    INR(24),
    CLP(25),
    PEN(26),
    COP(27),
    ZAR(28),
    HKD(29),
    TWD(30),
    SAR(31),
    AED(32),
    ARS(34),
    ILS(35),
    BYN(36),
    KZT(37),
    KWD(38),
    QAR(39),
    CRC(40),
    UYU(41),
    Max(42),

    ;

    private final int code;

    ECurrencyCode(int code) {
        this.code = code;
    }

    public int code() {
        return this.code;
    }

    public static ECurrencyCode from(int code) {
        return Arrays.stream(ECurrencyCode.values()).filter(x -> x.code == code).findFirst().orElse(null);
    }
}
