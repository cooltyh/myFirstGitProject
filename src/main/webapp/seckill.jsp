<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>秒杀示例 - 防超卖</title>
    <style>
        body { font-family: Arial, sans-serif; text-align: center; margin-top: 60px; }
        .stock { font-size: 48px; font-weight: bold; color: #e74c3c; }
        button { padding: 10px 20px; margin: 8px; font-size: 16px; cursor: pointer; }
        input { padding: 8px; font-size: 16px; width: 90px; }
        .result { margin-top: 20px; font-size: 16px; color: #333; min-height: 24px; }
        .tip { color: #999; font-size: 13px; margin-top: 30px; max-width: 640px; margin-left: auto; margin-right: auto; line-height: 1.6; }
    </style>
</head>
<body>
<h2>秒杀示例（Redis 原子扣减防超卖）</h2>
<p>商品 ID：<strong>${productId}</strong></p>

<p>当前剩余库存：<span id="stock" class="stock">${stock}</span></p>

<div>
    <input id="initStock" type="number" value="100" min="1"/>
    <button onclick="initStock()">初始化库存</button>
</div>
<div>
    <button onclick="seckill()">抢购 1 件</button>
    <button onclick="seckillConcurrent(150)">模拟 150 个并发请求</button>
</div>

<div id="result" class="result"></div>

<div class="tip">
    说明：先把库存初始化成 100，再点「模拟 150 个并发请求」，会同时发出 150 个抢购请求。
    结果是恰好 100 次成功、50 次售罄，库存精确扣到 0，不会出现负数——这就是 Redis Lua 原子扣减防超卖的效果。
</div>

<script>
    const ctx = '${pageContext.request.contextPath}';

    function post(params) {
        return fetch(ctx + '/seckill', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams(params)
        }).then(r => r.json());
    }

    function render(json) {
        if (json.stock !== undefined) {
            document.getElementById('stock').innerText = json.stock;
        }
        return json;
    }

    function initStock() {
        const stock = document.getElementById('initStock').value;
        post({ action: 'init', stock: stock }).then(json => {
            render(json);
            document.getElementById('result').innerText = '已初始化库存为 ' + json.stock;
        });
    }

    function seckill() {
        post({ action: 'seckill' }).then(json => {
            render(json);
            document.getElementById('result').innerText =
                json.success ? '抢购成功！剩余 ' + json.stock : '已抢完（售罄）';
        });
    }

    function seckillConcurrent(n) {
        document.getElementById('result').innerText = '正在模拟 ' + n + ' 个并发请求...';
        const tasks = [];
        for (let i = 0; i < n; i++) {
            tasks.push(post({ action: 'seckill' }));
        }
        Promise.all(tasks).then(results => {
            let success = 0, soldOut = 0;
            results.forEach(r => r.success ? success++ : soldOut++);
            const last = results[results.length - 1];
            render(last);
            document.getElementById('result').innerText =
                '并发 ' + n + ' 次：成功 ' + success + ' 次，售罄 ' + soldOut + ' 次，剩余库存 ' + last.stock;
        });
    }
</script>
</body>
</html>
