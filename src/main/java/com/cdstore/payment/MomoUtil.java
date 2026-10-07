package com.cdstore.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MomoUtil {

    private static final String ENDPOINT =
            getEnv(
                "MOMO_ENDPOINT",
                "https://test-payment.momo.vn/v2/gateway/api/create"
            );

    private static final String PARTNER_CODE =
            getRequiredEnv("MOMO_PARTNER_CODE");

    private static final String ACCESS_KEY =
            getRequiredEnv("MOMO_ACCESS_KEY");

    private static final String SECRET_KEY =
            getRequiredEnv("MOMO_SECRET_KEY");

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper();

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();


    /**
     * Tạo mã orderId duy nhất cho mỗi giao dịch.
     */
    public static String generateOrderId() {

        return "CD_" +
                System.currentTimeMillis() +
                "_" +
                UUID.randomUUID()
                   .toString()
                   .substring(0, 8);
    }


    /**
     * Tạo request thanh toán và gửi tới MoMo.
     */
    public static MomoResponse createPayment(
            long amount,
            String orderId,
            String orderInfo,
            String redirectUrl,
            String ipnUrl)
            throws Exception {

        if (amount < 1000) {
            throw new IllegalArgumentException(
                "MoMo payment amount must be at least 1000 VND."
            );
        }

        String requestId =
                UUID.randomUUID().toString();

        String requestType = "captureWallet";

        String extraData = "";

        /*
         * Chuỗi dùng để tạo HMAC-SHA256 signature.
         *
         * Thứ tự tham số phải đúng theo MoMo API.
         */
        String rawSignature =
                "accessKey=" + ACCESS_KEY +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + PARTNER_CODE +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature =
                generateSignature(
                    rawSignature,
                    SECRET_KEY
                );


        /*
         * Tạo JSON request body.
         */
        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        requestBody.put(
            "partnerCode",
            PARTNER_CODE
        );

        requestBody.put(
            "requestId",
            requestId
        );

        requestBody.put(
            "amount",
            amount
        );

        requestBody.put(
            "orderId",
            orderId
        );

        requestBody.put(
            "orderInfo",
            orderInfo
        );

        requestBody.put(
            "redirectUrl",
            redirectUrl
        );

        requestBody.put(
            "ipnUrl",
            ipnUrl
        );

        requestBody.put(
            "requestType",
            requestType
        );

        requestBody.put(
            "extraData",
            extraData
        );

        requestBody.put(
            "lang",
            "vi"
        );

        requestBody.put(
            "signature",
            signature
        );


        String json =
                OBJECT_MAPPER.writeValueAsString(
                    requestBody
                );


        /*
         * Gửi HTTP POST tới MoMo.
         */
        HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            json
                        )
                    )
                    .build();


        HttpResponse<String> response =
                HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );


        /*
         * Parse JSON response.
         */
        JsonNode responseJson =
                OBJECT_MAPPER.readTree(
                    response.body()
                );


        int resultCode =
                responseJson
                    .path("resultCode")
                    .asInt(-1);

        String message =
                responseJson
                    .path("message")
                    .asText("");

        String payUrl =
                responseJson
                    .path("payUrl")
                    .asText("");


        return new MomoResponse(
            resultCode,
            message,
            payUrl,
            response.body()
        );
    }


    /**
     * Tạo HMAC-SHA256 signature.
     */
    private static String generateSignature(
            String data,
            String secretKey)
            throws Exception {

        Mac mac =
                Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKeySpec =
                new SecretKeySpec(
                    secretKey.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                    ),
                    "HmacSHA256"
                );

        mac.init(secretKeySpec);

        byte[] hash =
                mac.doFinal(
                    data.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                    )
                );

        return java.util.HexFormat
                .of()
                .formatHex(hash);
    }


    /**
     * Lấy environment variable.
     */
    private static String getRequiredEnv(
            String name) {

        String value =
                System.getenv(name);

        if (value == null ||
            value.isBlank()) {

            throw new IllegalStateException(
                "Missing environment variable: " + name
            );
        }

        return value;
    }


    /**
     * Lấy environment variable,
     * nếu không có thì dùng giá trị mặc định.
     */
    private static String getEnv(
            String name,
            String defaultValue) {

        String value =
                System.getenv(name);

        if (value == null ||
            value.isBlank()) {

            return defaultValue;
        }

        return value;
    }


    /**
     * Kết quả trả về từ MoMo.
     */
    public static class MomoResponse {

        private final int resultCode;
        private final String message;
        private final String payUrl;
        private final String rawResponse;


        public MomoResponse(
                int resultCode,
                String message,
                String payUrl,
                String rawResponse) {

            this.resultCode = resultCode;
            this.message = message;
            this.payUrl = payUrl;
            this.rawResponse = rawResponse;
        }


        public int getResultCode() {
            return resultCode;
        }


        public String getMessage() {
            return message;
        }


        public String getPayUrl() {
            return payUrl;
        }


        public String getRawResponse() {
            return rawResponse;
        }


        public boolean isSuccess() {
            return resultCode == 0 &&
                   payUrl != null &&
                   !payUrl.isBlank();
        }
    }
}