package com.cdstore.servlet;

import com.cdstore.payment.VnPayUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@WebServlet({
    "/vnpay/create",
    "/vnpay/return",
    "/vnpay/ipn"
})
public class VnPayServlet extends HttpServlet {


    private static final long AMOUNT =
            100_000L;


    private static final String ORDER_INFO =
            "Thanh toan don hang CD Store";


    /*
     * Demo chưa dùng database.
     */
    private static final Map<String, PaymentOrder> ORDERS =
            new ConcurrentHashMap<>();


    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {


        switch (req.getServletPath()) {

            case "/vnpay/return":
                handleReturn(
                    req,
                    resp
                );
                break;


            case "/vnpay/ipn":
                handleIpn(
                    req,
                    resp
                );
                break;


            default:
                resp.sendError(
                    HttpServletResponse.SC_NOT_FOUND
                );
        }
    }


    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {


        req.setCharacterEncoding(
            "UTF-8"
        );


        if ("/vnpay/create".equals(
                req.getServletPath())) {

            createPayment(
                req,
                resp
            );

            return;
        }


        resp.sendError(
            HttpServletResponse.SC_NOT_FOUND
        );
    }


    /**
     * Tạo giao dịch và redirect sang VNPAY.
     */
    private void createPayment(
            HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {


        String orderId = null;


        try {

            orderId =
                    VnPayUtil.generateTxnRef();


            ORDERS.put(
                orderId,
                new PaymentOrder(
                    orderId,
                    AMOUNT,
                    ORDER_INFO
                )
            );


            String paymentUrl =
                    VnPayUtil.createPaymentUrl(
                        AMOUNT,
                        orderId,
                        ORDER_INFO,
                        getClientIp(req)
                    );


            resp.sendRedirect(
                paymentUrl
            );


        } catch (Exception e) {

            e.printStackTrace();


            if (orderId != null) {
                ORDERS.remove(
                    orderId
                );
            }


            forwardResult(
                req,
                resp,
                "FAILED",
                false,
                "Khong the tao giao dich VNPAY: "
                    + e.getMessage(),
                orderId,
                "N/A",
                "N/A",
                "N/A",
                "N/A",
                "N/A",
                "N/A",
                "N/A"
            );
        }
    }


    /**
     * VNPAY redirect browser về website.
     *
     * Return URL chỉ dùng để hiển thị kết quả.
     * IPN mới là nơi cập nhật trạng thái backend.
     */
    private void handleReturn(
            HttpServletRequest req,
            HttpServletResponse resp)
            throws ServletException, IOException {


        Map<String, String> params =
                collect(req);


        String orderId =
                params.get(
                    "vnp_TxnRef"
                );


        PaymentOrder order =
                orderId == null
                    ? null
                    : ORDERS.get(
                        orderId
                    );


        boolean valid =
                false;


        String message =
                null;


        try {

            valid =
                    VnPayUtil.verifyResponse(
                        params
                    );


            if (!valid) {

                message =
                    "Chu ky VNPAY khong hop le.";
            }


        } catch (Exception e) {

            e.printStackTrace();


            message =
                "Khong the xac thuc chu ky VNPAY.";
        }


        if (valid &&
            !VnPayUtil.getTmnCode().equals(
                params.get("vnp_TmnCode")
            )) {


            valid = false;


            message =
                "Ma website VNPAY khong hop le.";
        }


        String code =
                value(
                    params.get("vnp_ResponseCode"),
                    "99"
                );


        String transactionStatus =
                value(
                    params.get("vnp_TransactionStatus"),
                    "99"
                );


        String transactionNo =
                value(
                    params.get("vnp_TransactionNo"),
                    "N/A"
                );


        String bankCode =
                value(
                    params.get("vnp_BankCode"),
                    "N/A"
                );


        String cardType =
                value(
                    params.get("vnp_CardType"),
                    "N/A"
                );


        String payDate =
                value(
                    params.get("vnp_PayDate"),
                    "N/A"
                );


        String amount =
                order == null
                    ? "N/A"
                    : String.valueOf(
                        order.amount
                    );


        String status;


        boolean ipnVerified =
                false;


        if (!valid) {

            status =
                "INVALID";


            if (message == null) {

                message =
                    "Khong the xac thuc ket qua thanh toan.";
            }


        } else if (order == null) {

            status =
                "UNKNOWN";


            message =
                "Khong tim thay don hang trong bo nho giao dich.";


        } else if (
                parseLong(
                    params.get("vnp_Amount"),
                    -1
                )
                != order.amount * 100L) {


            status =
                "FAILED";


            message =
                "So tien giao dich khong khop.";


        } else if (
                "SUCCESS".equals(
                    order.status
                )) {


            status =
                "SUCCESS";


            ipnVerified =
                order.ipnVerified;


            message =
                "Thanh toan thanh cong. "
                + "IPN da cap nhat trang thai giao dich.";


        } else if (
                "FAILED".equals(
                    order.status
                )) {


            status =
                "FAILED";


            ipnVerified =
                order.ipnVerified;


            message =
                "Giao dich khong thanh cong. "
                + "Ma phan hoi VNPAY: "
                + code;


        } else if (
                "00".equals(code) &&
                "00".equals(transactionStatus)
            ) {


            status =
                "PENDING";


            message =
                "VNPAY bao giao dich thanh cong, "
                + "nhung IPN chua cap nhat trang thai.";


        } else {


            status =
                "FAILED";


            message =
                "Giao dich khong thanh cong. "
                + "Ma phan hoi VNPAY: "
                + code;
        }


        forwardResult(
            req,
            resp,
            status,
            ipnVerified,
            message,
            value(
                orderId,
                "N/A"
            ),
            amount,
            transactionNo,
            code,
            transactionStatus,
            bankCode,
            cardType,
            payDate
        );
    }


    /**
     * IPN server-to-server từ VNPAY.
     *
     * VNPAY yêu cầu GET + JSON response.
     */
    private void handleIpn(
            HttpServletRequest req,
            HttpServletResponse resp)
            throws IOException {


        Map<String, String> params =
                collect(req);


        try {


            /*
             * 1. Verify checksum.
             */
            if (!VnPayUtil.verifyResponse(
                    params
                )) {


                writeJson(
                    resp,
                    "97",
                    "Invalid Signature"
                );


                return;
            }


            /*
             * 2. Verify TmnCode.
             */
            if (!VnPayUtil.getTmnCode().equals(
                    params.get("vnp_TmnCode")
                )) {


                writeJson(
                    resp,
                    "97",
                    "Invalid TmnCode"
                );


                return;
            }


            String orderId =
                    params.get(
                        "vnp_TxnRef"
                    );


            PaymentOrder order =
                    orderId == null
                        ? null
                        : ORDERS.get(
                            orderId
                        );


            /*
             * 3. Check order.
             */
            if (order == null) {


                writeJson(
                    resp,
                    "01",
                    "Order not found"
                );


                return;
            }


            /*
             * 4. Check amount.
             */
            long returnedAmount =
                    parseLong(
                        params.get("vnp_Amount"),
                        -1
                    );


            if (returnedAmount
                    != order.amount * 100L) {


                writeJson(
                    resp,
                    "04",
                    "Invalid Amount"
                );


                return;
            }


            /*
             * 5. Check OrderInfo.
             */
            if (!order.orderInfo.equals(
                    params.get("vnp_OrderInfo")
                )) {


                writeJson(
                    resp,
                    "04",
                    "Invalid OrderInfo"
                );


                return;
            }


            synchronized (order) {


                /*
                 * 6. Idempotency.
                 */
                if (!"PENDING".equals(
                        order.status
                    )) {


                    writeJson(
                        resp,
                        "02",
                        "Order already confirmed"
                    );


                    return;
                }


                String code =
                        value(
                            params.get("vnp_ResponseCode"),
                            "99"
                        );


                String transactionStatus =
                        value(
                            params.get("vnp_TransactionStatus"),
                            "99"
                        );


                boolean success =
                        "00".equals(code) &&
                        "00".equals(transactionStatus);


                order.status =
                        success
                            ? "SUCCESS"
                            : "FAILED";


                order.ipnVerified =
                        true;


                order.resultCode =
                        code;


                order.transactionStatus =
                        transactionStatus;


                order.transactionNo =
                        value(
                            params.get("vnp_TransactionNo"),
                            ""
                        );


                order.bankCode =
                        value(
                            params.get("vnp_BankCode"),
                            ""
                        );


                order.cardType =
                        value(
                            params.get("vnp_CardType"),
                            ""
                        );


                order.payDate =
                        value(
                            params.get("vnp_PayDate"),
                            ""
                        );
            }


            writeJson(
                resp,
                "00",
                "Confirm Success"
            );


        } catch (Exception e) {

            e.printStackTrace();


            writeJson(
                resp,
                "99",
                "Unknown error"
            );
        }
    }


    private Map<String, String> collect(
            HttpServletRequest req) {


        Map<String, String> params =
                new LinkedHashMap<>();


        Enumeration<String> names =
                req.getParameterNames();


        while (names.hasMoreElements()) {


            String name =
                    names.nextElement();


            if (name.startsWith("vnp_")) {


                params.put(
                    name,
                    value(
                        req.getParameter(name),
                        ""
                    )
                );
            }
        }


        return params;
    }


    private void forwardResult(
            HttpServletRequest req,
            HttpServletResponse resp,
            String status,
            boolean ipnVerified,
            String message,
            String orderId,
            String amount,
            String transactionNo,
            String resultCode,
            String transactionStatus,
            String bankCode,
            String cardType,
            String payDate)
            throws ServletException, IOException {


        req.setAttribute(
            "success",
            "SUCCESS".equals(status)
        );


        req.setAttribute(
            "paymentStatus",
            status
        );


        req.setAttribute(
            "verifiedByIpn",
            ipnVerified
        );


        req.setAttribute(
            "message",
            value(
                message,
                "Khong co thong tin phan hoi."
            )
        );


        req.setAttribute(
            "orderId",
            orderId
        );


        req.setAttribute(
            "amount",
            amount
        );


        req.setAttribute(
            "transId",
            transactionNo
        );


        req.setAttribute(
            "resultCode",
            resultCode
        );


        req.setAttribute(
            "transactionStatus",
            transactionStatus
        );


        req.setAttribute(
            "bankCode",
            bankCode
        );


        req.setAttribute(
            "cardType",
            cardType
        );


        req.setAttribute(
            "payDate",
            payDate
        );


        req.getRequestDispatcher(
            "/payment-result.jsp"
        ).forward(
            req,
            resp
        );
    }


    private void writeJson(
            HttpServletResponse resp,
            String code,
            String message)
            throws IOException {


        resp.setStatus(
            HttpServletResponse.SC_OK
        );


        resp.setContentType(
            "application/json; charset=UTF-8"
        );


        resp.getWriter().write(
            "{\"RspCode\":\""
                + escapeJson(code)
                + "\",\"Message\":\""
                + escapeJson(message)
                + "\"}"
        );
    }


    private String getClientIp(
            HttpServletRequest req) {


        String forwarded =
                req.getHeader(
                    "X-Forwarded-For"
                );


        if (forwarded != null &&
            !forwarded.isBlank()) {


            return forwarded
                    .split(",")[0]
                    .trim();
        }


        String realIp =
                req.getHeader(
                    "X-Real-IP"
                );


        if (realIp != null &&
            !realIp.isBlank()) {


            return realIp.trim();
        }


        return req.getRemoteAddr();
    }


    private static long parseLong(
            String value,
            long fallback) {


        try {

            return value == null ||
                   value.isBlank()

                ? fallback

                : Long.parseLong(value);

        } catch (NumberFormatException e) {

            return fallback;
        }
    }


    private static String value(
            String value,
            String fallback) {


        return value == null ||
               value.isBlank()

            ? fallback

            : value;
    }


    private static String escapeJson(
            String value) {


        return value(
                value,
                ""
            )
            .replace(
                "\\",
                "\\\\"
            )
            .replace(
                "\"",
                "\\\""
            )
            .replace(
                "\r",
                "\\r"
            )
            .replace(
                "\n",
                "\\n"
            );
    }


    private static class PaymentOrder {

        private final String orderId;

        private final long amount;

        private final String orderInfo;


        private volatile String status =
                "PENDING";


        private volatile boolean ipnVerified;


        private volatile String resultCode;

        private volatile String transactionStatus;

        private volatile String transactionNo;

        private volatile String bankCode;

        private volatile String cardType;

        private volatile String payDate;


        private PaymentOrder(
                String orderId,
                long amount,
                String orderInfo) {


            this.orderId =
                    orderId;


            this.amount =
                    amount;


            this.orderInfo =
                    orderInfo;
        }
    }
}