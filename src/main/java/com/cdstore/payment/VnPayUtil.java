package com.cdstore.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VnPayUtil {

    private static final ZoneId VNPAY_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    private VnPayUtil() {
    }


    public static String getTmnCode() {

        return getRequiredEnv(
            "VNPAY_TMN_CODE"
        );
    }


    public static String getReturnUrl() {

        return getRequiredEnv(
            "VNPAY_RETURN_URL"
        );
    }


    /**
     * Tạo mã giao dịch duy nhất.
     */
    public static String generateTxnRef() {

        String timestamp =
                ZonedDateTime.now(VNPAY_ZONE)
                        .format(
                            DateTimeFormatter.ofPattern(
                                "yyyyMMddHHmmssSSS"
                            )
                        );


        String random =
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8);


        return "CD"
                + timestamp
                + random;
    }


    /**
     * Tạo URL thanh toán VNPAY.
     *
     * VNPAY yêu cầu:
     * - Version 2.1.0
     * - HMAC-SHA512
     * - amount * 100
     * - tham số sort alphabet
     */
    public static String createPaymentUrl(
            long amount,
            String orderId,
            String orderInfo,
            String clientIp)
            throws Exception {


        if (amount < 5_000) {

            throw new IllegalArgumentException(
                "VNPAY payment amount must be at least 5,000 VND."
            );
        }


        long vnpAmount =
                Math.multiplyExact(
                    amount,
                    100L
                );


        ZonedDateTime now =
                ZonedDateTime.now(
                    VNPAY_ZONE
                );


        String createDate =
                now.format(
                    VNPAY_DATE_FORMAT
                );


        String expireDate =
                now.plusMinutes(15)
                   .format(
                       VNPAY_DATE_FORMAT
                   );


        Map<String, String> params =
                new TreeMap<>();


        params.put(
            "vnp_Version",
            "2.1.0"
        );


        params.put(
            "vnp_Command",
            "pay"
        );


        params.put(
            "vnp_TmnCode",
            getTmnCode()
        );


        params.put(
            "vnp_Amount",
            String.valueOf(
                vnpAmount
            )
        );


        params.put(
            "vnp_CurrCode",
            "VND"
        );


        params.put(
            "vnp_TxnRef",
            orderId
        );


        params.put(
            "vnp_OrderInfo",
            orderInfo
        );


        params.put(
            "vnp_OrderType",
            "other"
        );


        params.put(
            "vnp_Locale",
            "vn"
        );


        params.put(
            "vnp_ReturnUrl",
            getReturnUrl()
        );


        params.put(
            "vnp_IpAddr",
            clientIp
        );


        params.put(
            "vnp_CreateDate",
            createDate
        );


        params.put(
            "vnp_ExpireDate",
            expireDate
        );


        String queryString =
                buildQueryString(
                    params
                );


        String secureHash =
                hmacSha512(
                    queryString,
                    getRequiredEnv(
                        "VNPAY_HASH_SECRET"
                    )
                );


        return getRequiredEnv(
                    "VNPAY_PAYMENT_URL"
                )
                + "?"
                + queryString
                + "&vnp_SecureHash="
                + secureHash;
    }


    /**
     * Verify checksum từ VNPAY.
     */
    public static boolean verifyResponse(
            Map<String, String> responseParams)
            throws Exception {


        String receivedHash =
                responseParams.get(
                    "vnp_SecureHash"
                );


        if (receivedHash == null ||
            receivedHash.isBlank()) {

            return false;
        }


        Map<String, String> data =
                new TreeMap<>();


        for (Map.Entry<String, String> entry :
                responseParams.entrySet()) {


            String key =
                    entry.getKey();


            if (!key.startsWith("vnp_")) {
                continue;
            }


            if ("vnp_SecureHash".equals(key) ||
                "vnp_SecureHashType".equals(key)) {

                continue;
            }


            data.put(
                key,
                entry.getValue()
            );
        }


        String signedData =
                buildQueryString(
                    data
                );


        String expectedHash =
                hmacSha512(
                    signedData,
                    getRequiredEnv(
                        "VNPAY_HASH_SECRET"
                    )
                );


        return MessageDigest.isEqual(

            expectedHash.getBytes(
                StandardCharsets.UTF_8
            ),

            receivedHash.getBytes(
                StandardCharsets.UTF_8
            )
        );
    }


    /**
     * Sort + URL encode parameters.
     */
    private static String buildQueryString(
            Map<String, String> params) {


        StringBuilder query =
                new StringBuilder();


        for (Map.Entry<String, String> entry :
                params.entrySet()) {


            if (query.length() > 0) {
                query.append('&');
            }


            query.append(
                urlEncode(
                    entry.getKey()
                )
            );


            query.append('=');


            query.append(
                urlEncode(
                    entry.getValue()
                )
            );
        }


        return query.toString();
    }


    private static String urlEncode(
            String value) {

        return URLEncoder.encode(
            value == null ? "" : value,
            StandardCharsets.UTF_8
        );
    }


    /**
     * HMAC-SHA512.
     */
    private static String hmacSha512(
            String data,
            String secretKey)
            throws Exception {


        Mac mac =
                Mac.getInstance(
                    "HmacSHA512"
                );


        SecretKeySpec key =
                new SecretKeySpec(
                    secretKey.getBytes(
                        StandardCharsets.UTF_8
                    ),
                    "HmacSHA512"
                );


        mac.init(key);


        byte[] hash =
                mac.doFinal(
                    data.getBytes(
                        StandardCharsets.UTF_8
                    )
                );


        StringBuilder result =
                new StringBuilder(
                    hash.length * 2
                );


        for (byte b : hash) {

            result.append(
                String.format(
                    "%02x",
                    b & 0xff
                )
            );
        }


        return result.toString();
    }


    private static String getRequiredEnv(
            String name) {


        String value =
                System.getenv(name);


        if (value == null ||
            value.isBlank()) {

            throw new IllegalStateException(
                "Missing environment variable: "
                    + name
            );
        }


        return value;
    }
}