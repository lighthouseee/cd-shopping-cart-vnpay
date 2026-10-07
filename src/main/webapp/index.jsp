<%@ page contentType="text/html;charset=UTF-8" %>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>MoMo Payment Demo</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/style.css">
</head>

<body>

<div class="container">

    <div class="payment-card">

        <h1>MoMo Payment Demo</h1>

        <p class="subtitle">
            Thanh toán đơn hàng
        </p>


        <div class="order-info">

            <div class="info-row">
                <span>Sản phẩm</span>
                <strong>CD Store Demo</strong>
            </div>

            <div class="info-row">
                <span>Số tiền</span>
                <strong>100.000 VND</strong>
            </div>

        </div>


        <form action="${pageContext.request.contextPath}/momo/create"
              method="post">

            <button type="submit"
                    class="momo-button">
                Thanh toán bằng MoMo
            </button>

        </form>


        <p class="note">
            Đây là môi trường MoMo Sandbox.
        </p>

    </div>

</div>

</body>

</html>