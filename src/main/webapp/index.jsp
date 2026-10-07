<%@ page contentType="text/html;charset=UTF-8" %>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>VNPAY Payment Demo</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/style.css">
</head>

<body>

<div class="container">

    <div class="payment-card">

        <div class="brand-badge">
            VNPAY SANDBOX
        </div>


        <h1>
            Thanh toán đơn hàng
        </h1>


        <p class="subtitle">
            CD Store Payment Demo
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


        <form action="${pageContext.request.contextPath}/vnpay/create"
              method="post">

            <button type="submit"
                    class="vnpay-button">

                Thanh toán bằng VNPAY

            </button>

        </form>


        <p class="note">
            Đây là môi trường VNPAY Sandbox dùng để kiểm thử.
        </p>

    </div>

</div>

</body>

</html>