package com.cdstore.servlet;

import com.cdstore.payment.MomoUtil;
import com.cdstore.payment.MomoUtil.MomoResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;

@WebServlet({
    "/momo/create",
    "/momo/return",
    "/momo/ipn"
})
public class MomoServlet extends HttpServlet {

    /*
     * Demo chỉ có một sản phẩm/dịch vụ thanh toán cố định.
     * Sau này nếu cần có thể lấy số tiền từ database/cart.
     */
    private static final long PAYMENT_AMOUNT = 100_000L;

    private static final String ORDER_INFO =
            "Thanh toan don hang CD Store";


    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getServletPath();

        switch (path) {

            case "/momo/return":
                handleReturn(request, response);
                break;

            default:
                response.sendRedirect(
                    request.getContextPath() + "/"
                );
                break;
        }
    }


    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String path = request.getServletPath();

        switch (path) {

            case "/momo/create":
                createPayment(request, response);
                break;

            case "/momo/ipn":
                handleIpn(request, response);
                break;

            default:
                response.sendError(
                    HttpServletResponse.SC_NOT_FOUND
                );
                break;
        }
    }


    /**
     * Tạo giao dịch MoMo.
     */
    private void createPayment(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            String orderId =
                    MomoUtil.generateOrderId();

            String redirectUrl =
                    getRequiredEnv("MOMO_REDIRECT_URL");

            String ipnUrl =
                    getRequiredEnv("MOMO_IPN_URL");


            MomoResponse momoResponse =
                    MomoUtil.createPayment(
                        PAYMENT_AMOUNT,
                        orderId,
                        ORDER_INFO,
                        redirectUrl,
                        ipnUrl
                    );


            if (momoResponse.isSuccess()) {

                /*
                 * MoMo trả về payUrl.
                 *
                 * Chuyển người dùng sang trang
                 * thanh toán của MoMo.
                 */
                response.sendRedirect(
                    momoResponse.getPayUrl()
                );

                return;
            }


            /*
             * Nếu MoMo trả lỗi thì hiển thị
             * thông tin lỗi trên website.
             */
            request.setAttribute(
                "success",
                false
            );

            request.setAttribute(
                "message",
                momoResponse.getMessage()
            );

            request.setAttribute(
                "orderId",
                orderId
            );

            request.setAttribute(
                "amount",
                PAYMENT_AMOUNT
            );

            request.getRequestDispatcher(
                "/payment-result.jsp"
            ).forward(
                request,
                response
            );


        } catch (Exception e) {

            e.printStackTrace();

            request.setAttribute(
                "success",
                false
            );

            request.setAttribute(
                "message",
                "Không thể tạo giao dịch MoMo: "
                    + e.getMessage()
            );

            request.getRequestDispatcher(
                "/payment-result.jsp"
            ).forward(
                request,
                response
            );
        }
    }


    /**
     * Xử lý redirect từ MoMo về website.
     *
     * MoMo sử dụng GET cho redirectUrl.
     */
    private void handleReturn(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String resultCode =
                request.getParameter("resultCode");

        String message =
                request.getParameter("message");

        String orderId =
                request.getParameter("orderId");

        String amount =
                request.getParameter("amount");

        String transId =
                request.getParameter("transId");


        boolean success =
                "0".equals(resultCode);


        request.setAttribute(
            "success",
            success
        );

        request.setAttribute(
            "message",
            message
        );

        request.setAttribute(
            "orderId",
            orderId
        );

        request.setAttribute(
            "amount",
            amount
        );

        request.setAttribute(
            "transId",
            transId
        );

        request.setAttribute(
            "resultCode",
            resultCode
        );


        request.getRequestDispatcher(
            "/payment-result.jsp"
        ).forward(
            request,
            response
        );
    }


    /**
     * Xử lý IPN từ MoMo.
     *
     * MoMo gọi endpoint này bằng POST
     * với Content-Type: application/json.
     */
    private void handleIpn(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        StringBuilder body =
                new StringBuilder();

        try (BufferedReader reader =
                request.getReader()) {

            String line;

            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }


        /*
         * Tạm thời log IPN để kiểm tra
         * khi kết nối với MoMo Sandbox.
         */
        System.out.println(
            "========== MOMO IPN =========="
        );

        System.out.println(
            body.toString()
        );

        System.out.println(
            "=============================="
        );


        /*
         * Theo tài liệu MoMo:
         * IPN endpoint cần trả HTTP 204
         * và không gửi response body.
         */
        response.setStatus(
            HttpServletResponse.SC_NO_CONTENT
        );
    }


    /**
     * Lấy environment variable bắt buộc.
     */
    private String getRequiredEnv(
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
}