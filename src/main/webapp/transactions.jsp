<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<%@ taglib uri="jakarta.tags.fmt" prefix="fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>2025年1月交易记录</title>
    <style>
        table {
            border-collapse: collapse;
            width: 80%;
            margin: 20px auto;
        }

        th, td {
            border: 1px solid #333;
            padding: 10px;
            text-align: left;
        }

        th {
            background: #2c3e50;
            color: #fff;
        }

        .transactionAmount {
            text-align: right;
        }

        .modal-overlay {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.5);
            z-index: 999;
        }

        .modal {
            display: none;
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: #fff;
            padding: 24px;
            border-radius: 8px;
            z-index: 1000;
            min-width: 400px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        }

        .modal.active, .modal-overlay.active {
            display: block;
        }

        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
        }

        .modal-close {
            font-size: 24px;
            cursor: pointer;
            line-height: 1;
        }

        .form-group {
            margin-bottom: 12px;
        }

        .form-group label {
            display: block;
            margin-bottom: 4px;
            font-weight: bold;
        }

        .form-group input, .form-group select {
            width: 100%;
            padding: 8px;
            box-sizing: border-box;
            border: 1px solid #ccc;
            border-radius: 4px;
        }

        .modal-footer {
            margin-top: 20px;
            text-align: right;
        }

        .modal-footer button {
            padding: 8px 20px;
            margin-left: 8px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }

        .modal-footer button[type="submit"] {
            background: #007bff;
            color: #fff;
        }

        .btn-add {
            padding: 8px 16px;
            background: #28a745;
            color: #fff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            margin-bottom: 10px;
        }

        .btn-edit {
            padding: 4px 12px;
            background: #ffc107;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            margin-right: 4px;
        }

        .btn-delete {
            padding: 4px 12px;
            background: #dc3545;
            color: #fff;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        .record-summary {
            text-align: right;
            padding: 10px 20px;
            color: #555;
            font-size: 14px;
        }
        .record-summary strong {
            color: #2c3e50;
            font-size: 16px;
        }
        .pagination {
            text-align: center;
            margin: 20px 0;
        }
        .pagination a, .pagination span {
            display: inline-block;
            padding: 6px 12px;
            margin: 0 3px;
            border: 1px solid #ccc;
            border-radius: 4px;
            color: #333;
            text-decoration: none;
        }
        .pagination a:hover {
            background: #f0f0f0;
        }
        .pagination .current {
            background: #007bff;
            color: #fff;
            border-color: #007bff;
        }
        .pagination .disabled {
            color: #bbb;
            border-color: #eee;
            cursor: not-allowed;
        }
    </style>
</head>
<!-- 弹窗遮罩层 -->
<div id="modalOverlay" class="modal-overlay" onclick="closeModal()"></div>
<!-- 弹窗主体 -->
<div id="transactionModal" class="modal">
    <div class="modal-header">
        <h3 id="modalTitle">Add Transaction</h3>
        <span class="modal-close" onclick="closeModal()">&times;</span>
    </div>
    <form id="modalForm" action="${pageContext.request.contextPath}/transaction-servlet" method="post">
        <input type="hidden" name="action" id="modalAction" value="add"/>
        <input type="hidden" name="id" id="m_id"/>
        <div class="form-group">
            <label>Customer ID:</label>
            <input type="text" name="customerId" id="m_customerId" required/>
        </div>
        <div class="form-group">
            <label>Name:</label>
            <input type="text" name="name" id="m_name" required/>
        </div>
        <div class="form-group">
            <label>Transaction Type:</label>
            <select name="transactionType" id="m_type" required>
                <option value="Cash">Cash</option>
                <option value="Cheque">Cheque</option>
                <option value="Gift Card">Gift Card</option>
            </select>
        </div>
        <div class="form-group">
            <label>Datetime:</label>
            <input type="date" name="transactionDatetime" id="m_date" required/>
        </div>
        <div class="form-group">
            <label>Amount:</label>
            <input type="number" step="0.01" name="transactionAmount" id="m_amount" required/>
        </div>

        <div class="modal-footer">
            <button type="button" onclick="closeModal()">Cancel</button>
            <button type="submit" id="modalSubmitBtn">Add</button>
        </div>
    </form>
</div>
<body>
<h2 style="text-align:center;">交易记录</h2>
<!-- 查询功能 -->
<div style="margin-bottom: 20px; padding: 15px; background: #f5f5f5; border-radius: 4px;">
    <form action="${pageContext.request.contextPath}/transaction-servlet" method="get">
        <input type="hidden" id="edit-id" name="id" />
        <label>Name: <input type="text" name="name" value="${param.name}"/></label>
        <label>Type:
            <select name="type">
                <option value="">All</option>
                <option value="Cash" ${param.type == 'Cash' ? 'selected' : ''}>Cash</option>
                <option value="Cheque" ${param.type == 'Cheque' ? 'selected' : ''}>Cheque</option>
                <option value="Gift Card" ${param.type == 'Gift Card' ? 'selected' : ''}>Gift Card</option>
            </select>
        </label>
        <label>Date From: <input type="date" name="dateFrom" value="${param.dateFrom}"/></label>
        <label>Date To: <input type="date" name="dateTo" value="${param.dateTo}"/></label>
        <label>Min Amount: <input type="number" step="0.01" name="minAmount" value="${param.minAmount}"/></label>
        <label>Max Amount: <input type="number" step="0.01" name="maxAmount" value="${param.maxAmount}"/></label>
        <button type="submit">Search</button>
        <a href="${pageContext.request.contextPath}/transaction-servlet">Reset</a>
    </form>
</div>
<div style="margin: 15px 0; display: flex; justify-content: space-between; align-items: center;">
    <button type="button" class="btn-add" onclick="openAddModal()">+ Add New</button>
</div>
<table>
    <tr>
        <th>Customer ID</th>
        <th>Name</th>
        <th>Transaction Datetime</th>
        <th>Transaction Type</th>
        <th style="text-align:right">Transaction Amount</th>
        <th style="text-align:center">Actions</th>
    </tr>
    <c:forEach var="r" items="${transactions}">
        <tr>
            <td>${r.customerId}</td>
            <td>${r.name}</td>
            <td><fmt:formatDate value="${r.transactionDatetime}" pattern="yyyy-MM-dd"/></td>
            <td>${r.transactionType}</td>
            <td class="transactionAmount">
                HK$ <fmt:formatNumber value="${r.transactionAmount}" minFractionDigits="2" maxFractionDigits="2"/>
            </td>
            <td style="text-align:center; white-space:nowrap;">
                <button type="button" class="btn-edit"
                        data-id="${r.id}"
                        data-customer-id="${r.customerId}"
                        data-name="${r.name}"
                        data-type="${r.transactionType}"
                        data-amount="${r.transactionAmount}"
                        data-datetime="<fmt:formatDate value='${r.transactionDatetime}' pattern='yyyy-MM-dd'/>"
                        onclick="openEditModal(this)">Edit
                </button>
                <form action="${pageContext.request.contextPath}/transaction-servlet" method="post"
                      style="display:inline;" onsubmit="return confirm('确定删除？');">
                    <input type="hidden" name="action" value="delete"/>
                    <input type="hidden" name="id" value="${r.id}"/>
                    <button type="submit" class="btn-delete">Delete</button>
                </form>
            </td>
        </tr>
    </c:forEach>
</table>
<div class="record-summary">
    <p>共 <strong>${totalRecords}</strong> 条交易记录<c:if test="${totalPages > 0}"> · 第 <strong>${currentPage}</strong> / ${totalPages} 页</c:if></p>
</div>

<c:if test="${totalPages > 1}">
    <div class="pagination">
        <c:choose>
            <c:when test="${currentPage > 1}">
                <c:url var="prevUrl" value="/transaction-servlet">
                    <c:param name="page" value="${currentPage - 1}"/>
                    <c:param name="name" value="${param.name}"/>
                    <c:param name="type" value="${param.type}"/>
                    <c:param name="dateFrom" value="${param.dateFrom}"/>
                    <c:param name="dateTo" value="${param.dateTo}"/>
                    <c:param name="minAmount" value="${param.minAmount}"/>
                    <c:param name="maxAmount" value="${param.maxAmount}"/>
                </c:url>
                <a href="${prevUrl}">&laquo; 上一页</a>
            </c:when>
            <c:otherwise><span class="disabled">&laquo; 上一页</span></c:otherwise>
        </c:choose>

        <c:forEach begin="1" end="${totalPages}" var="p">
            <c:url var="pageUrl" value="/transaction-servlet">
                <c:param name="page" value="${p}"/>
                <c:param name="name" value="${param.name}"/>
                <c:param name="type" value="${param.type}"/>
                <c:param name="dateFrom" value="${param.dateFrom}"/>
                <c:param name="dateTo" value="${param.dateTo}"/>
                <c:param name="minAmount" value="${param.minAmount}"/>
                <c:param name="maxAmount" value="${param.maxAmount}"/>
            </c:url>
            <c:choose>
                <c:when test="${p == currentPage}"><span class="current">${p}</span></c:when>
                <c:otherwise><a href="${pageUrl}">${p}</a></c:otherwise>
            </c:choose>
        </c:forEach>

        <c:choose>
            <c:when test="${currentPage < totalPages}">
                <c:url var="nextUrl" value="/transaction-servlet">
                    <c:param name="page" value="${currentPage + 1}"/>
                    <c:param name="name" value="${param.name}"/>
                    <c:param name="type" value="${param.type}"/>
                    <c:param name="dateFrom" value="${param.dateFrom}"/>
                    <c:param name="dateTo" value="${param.dateTo}"/>
                    <c:param name="minAmount" value="${param.minAmount}"/>
                    <c:param name="maxAmount" value="${param.maxAmount}"/>
                </c:url>
                <a href="${nextUrl}">下一页 &raquo;</a>
            </c:when>
            <c:otherwise><span class="disabled">下一页 &raquo;</span></c:otherwise>
        </c:choose>
    </div>
</c:if>
<script>
    // 打开新增弹窗
    function openAddModal() {
        document.getElementById('modalTitle').innerText = 'Add Transaction';
        document.getElementById('modalAction').value = 'add';
        document.getElementById('modalSubmitBtn').innerText = 'Add';
        document.getElementById('m_name').value = '';
        document.getElementById('m_type').selectedIndex = 0;
        document.getElementById('m_date').value = '';
        document.getElementById('m_amount').value = '';
        showModal();
    }

    // 打开修改弹窗
    function openEditModal(btn) {
        const d = btn.dataset;
        document.getElementById('modalTitle').innerText = 'Edit Transaction';
        document.getElementById('modalAction').value = 'update';
        document.getElementById('modalSubmitBtn').innerText = 'Update';
        document.getElementById('m_id').value = d.id;
        document.getElementById('m_customerId').value = d.customerId;
        document.getElementById('m_customerId').readOnly = true;
        document.getElementById('m_name').value = d.name;
        document.getElementById('m_type').value = d.type;
        document.getElementById('m_date').value = d.date;
        document.getElementById('m_amount').value = d.amount;
        showModal();
    }

    function showModal() {
        document.getElementById('modalOverlay').classList.add('active');
        document.getElementById('transactionModal').classList.add('active');
    }

    function closeModal() {
        document.getElementById('modalOverlay').classList.remove('active');
        document.getElementById('transactionModal').classList.remove('active');
    }

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') closeModal();
    });
</script>
</body>
</html>