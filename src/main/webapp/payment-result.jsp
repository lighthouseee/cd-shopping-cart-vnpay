<%@ page contentType="text/html;charset=UTF-8" %>

<%
    Boolean success =
        (Boolean) request.getAttribute("success");

    String message =
        (String) request.getAttribute("message");

    String orderId =
        (String) request.getAttribute("orderId");

    String amount =
        String.valueOf(
            request.getAttribute("amount")
        );

    String transId =
        (String) request.getAttribute("transId");

    String resultCode =
        (String) request.getAttribute("resultCode");

    if (success == null) {
        success = false;
    }

    if (message == null || message.isBlank()) {
        message = "Không có thông tin phản hồi.";
    }

    if (orderId == null) {
        orderId = "N/A";
    }

    if (amount == null ||
        amount.equals("null")) {
        amount = "N/A";
    }

    if (transId == null) {
        transId = "N/A";
    }

    if (resultCode == null) {
        resultCode = "N/A";
    }
%>

<!DOCTYPE html>
<html lang="vi">

<head>

    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>Payment Result</title>

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

        <% } else { %>

            <div class="result-icon failed-icon">
                ✕
            </div>

            <h1 class="failed-title">
                Thanh toán thất bại
            </h1>

        <% } %>


        <p class="result-message">
            <%= message %>
        </p>


        <div class="order-info">

            <div class="info-row">
                <span>Mã đơn hàng</span>
                <strong>
                    <%= orderId %>
                </strong>
            </div>

            <div class="info-row">
                <span>Số tiền</span>
                <strong>
                    <%= amount %> VND
                </strong>
            </div>

            <div class="info-row">
                <span>Mã giao dịch</span>
                <strong>
                    <%= transId %>
                </strong>
            </div>

            <div class="info-row">
                <span>Result Code</span>
                <strong>
                    <%= resultCode %>
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