<%@ page contentType="text/html;charset=UTF-8" %>

<%
    String paymentStatus =
        (String) request.getAttribute("paymentStatus");

    String message =
        (String) request.getAttribute("message");

    String orderId =
        (String) request.getAttribute("orderId");

    String amount =
        (String) request.getAttribute("amount");

    String transId =
        (String) request.getAttribute("transId");

    String resultCode =
        (String) request.getAttribute("resultCode");

    String transactionStatus =
        (String) request.getAttribute("transactionStatus");

    String bankCode =
        (String) request.getAttribute("bankCode");

    String cardType =
        (String) request.getAttribute("cardType");

    String payDate =
        (String) request.getAttribute("payDate");

    Boolean verifiedByIpn =
        (Boolean) request.getAttribute("verifiedByIpn");


    if (paymentStatus == null)
        paymentStatus = "UNKNOWN";

    if (message == null)
        message = "Không có thông tin phản hồi.";

    if (orderId == null)
        orderId = "N/A";

    if (amount == null)
        amount = "N/A";

    if (transId == null)
        transId = "N/A";

    if (resultCode == null)
        resultCode = "N/A";

    if (transactionStatus == null)
        transactionStatus = "N/A";

    if (bankCode == null)
        bankCode = "N/A";

    if (cardType == null)
        cardType = "N/A";

    if (payDate == null)
        payDate = "N/A";

    if (verifiedByIpn == null)
        verifiedByIpn = false;


    boolean success =
        "SUCCESS".equals(paymentStatus);

    boolean pending =
        "PENDING".equals(paymentStatus);

    boolean invalid =
        "INVALID".equals(paymentStatus);
%>


<!DOCTYPE html>

<html lang="vi">

<head>

    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>Kết quả thanh toán VNPAY</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/style.css">

</head>


<body>

<div class="container">

    <div class="payment-card">


        <% if (success) { %>

            <div class="result-icon success-icon">
                ✓
            </div>

            <h1 class="success-title">
                Thanh toán thành công
            </h1>

            <p class="result-message">
                Giao dịch đã được VNPAY xác nhận
                và IPN đã cập nhật trạng thái.
            </p>


        <% } else if (pending) { %>

            <div class="result-icon pending-icon">
                !
            </div>

            <h1 class="pending-title">
                Đang chờ xác nhận
            </h1>

            <p class="result-message">
                VNPAY đã trả kết quả thành công
                nhưng IPN chưa cập nhật giao dịch.
                Bạn có thể chờ vài giây rồi tải lại trang.
            </p>


        <% } else if (invalid) { %>

            <div class="result-icon failed-icon">
                !
            </div>

            <h1 class="failed-title">
                Dữ liệu không hợp lệ
            </h1>

            <p class="result-message">
                Không thể xác thực dữ liệu trả về từ VNPAY.
            </p>


        <% } else { %>

            <div class="result-icon failed-icon">
                ✕
            </div>

            <h1 class="failed-title">
                Thanh toán thất bại
            </h1>

            <p class="result-message">
                Giao dịch không được thực hiện thành công.
            </p>

        <% } %>


        <div class="result-message-box">

            <strong>
                Phản hồi
            </strong>

            <p class="result-message">
                <%= message %>
            </p>

        </div>


        <div class="order-info">


            <div class="info-row">
                <span>Mã đơn hàng</span>
                <strong><%= orderId %></strong>
            </div>


            <div class="info-row">
                <span>Số tiền</span>
                <strong><%= amount %> VND</strong>
            </div>


            <div class="info-row">
                <span>Mã giao dịch VNPAY</span>
                <strong><%= transId %></strong>
            </div>


            <div class="info-row">
                <span>Response Code</span>
                <strong><%= resultCode %></strong>
            </div>


            <div class="info-row">
                <span>Transaction Status</span>
                <strong><%= transactionStatus %></strong>
            </div>


            <div class="info-row">
                <span>Ngân hàng</span>
                <strong><%= bankCode %></strong>
            </div>


            <div class="info-row">
                <span>Loại thẻ</span>
                <strong><%= cardType %></strong>
            </div>


            <div class="info-row">
                <span>Thời gian thanh toán</span>
                <strong><%= payDate %></strong>
            </div>


            <div class="info-row">
                <span>IPN đã xác thực</span>

                <strong>
                    <%= verifiedByIpn ? "YES" : "NO" %>
                </strong>
            </div>


        </div>


        <a href="${pageContext.request.contextPath}/"
           class="back-button">

            Quay lại trang thanh toán

        </a>


    </div>

</div>

</body>

</html>