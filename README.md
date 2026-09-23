
## 1. Mo Ta Chi Tiet Trang Thai (States), Su Kien (Events) va Luong Chuyen Doi
### 1.1. Danh Sach Trang Thai (States)

- ORDER_PENDING: Don hang duoc tao va o trang thai cho khoi tao quy trinh Saga.
- PAYMENT_PROCESSING: Don hang dang trong qua trinh goi dich vu thanh toan.
- INVENTORY_PROCESSING: Thanh toan thanh cong, don hang dang trong qua trinh tru hang trong kho.
- SHIPPING_PROCESSING: Tru kho thanh cong, don hang dang trong qua trinh tao van don giao hang.
- ORDER_COMPLETED: Tat ca cac buoc (Thanh toan, Kho, Giao hang) deu thanh cong. Don hang hoan tat.
- ORDER_CANCELLED: Co it nhat mot buoc that bai hoac don hang bi huy. Cac buoc truoc do da duoc bu tru (rollback) thanh cong.

### 1.2. Danh Sach Su Kien (Events)

- START_ORDER: Bat dau quy trinh xu ly Saga cho don hang.
- PAYMENT_SUCCESS: Thanh toan duoc xu ly thanh cong tu PaymentService.
- PAYMENT_FAILED: Thanh toan that bai (vi du: the khong hop le, so du khong du).
- INVENTORY_SUCCESS: Tru kho thanh cong tu InventoryService.
- INVENTORY_FAILED: Tru kho that bai (vi du: het hang ton kho).
- SHIPPING_SUCCESS: Tao van don giao hang thanh cong tu ShippingService.
- SHIPPING_FAILED: Tao van don that bai (vi du: don vi van chuyen qua tai, dia chi khong hop le).
- CANCEL_ORDER: Lenh huy don hang tu he thong hoac nguoi dung.

### 1.3. Luong Chuyen Doi Trang Thai (State Transitions)

Bang quy dinh chuyen doi giua cac trang thai va su kien:

| Trang Thai Hien Tai | Su Kien (Event) | Trang Thai Tiep Theo | Hanh Dong / Y Nghia |
| :--- | :--- | :--- | :--- |
| ORDER_PENDING | START_ORDER | PAYMENT_PROCESSING | Khoi dong quy trinh, chuyen den buoc thanh toan |
| PAYMENT_PROCESSING | PAYMENT_SUCCESS | INVENTORY_PROCESSING | Thanh toan thanh cong, chuyen sang buoc tru kho |
| PAYMENT_PROCESSING | PAYMENT_FAILED | ORDER_CANCELLED | Thanh toan that bai, ket thuc luong khong can bu tru |
| INVENTORY_PROCESSING | INVENTORY_SUCCESS | SHIPPING_PROCESSING | Tru kho thanh cong, chuyen sang buoc giao hang |
| INVENTORY_PROCESSING | INVENTORY_FAILED | ORDER_CANCELLED | Tru kho that bai, kich hoat bu tru hoan tien |
| SHIPPING_PROCESSING | SHIPPING_SUCCESS | ORDER_COMPLETED | Giao hang thanh cong, hoan tat toan bo Saga |
| SHIPPING_PROCESSING | SHIPPING_FAILED | ORDER_CANCELLED | Giao hang that bai, kich hoat bu tru hoan kho va hoan tien |
| Bat ky trang thai | CANCEL_ORDER | ORDER_CANCELLED | Huy bo don hang va kich hoat bu tru cac buoc da chay |

---

## 2. Giai Thich Retry Policy va Co Che Bu Tru (Compensation)

### 2.1. Chinh Sach Thu Lai (Retry Policy)

Trong he thong phan tan su dung Saga Orchestration:
- Doi voi loi tam thoi (transient network errors, timeout mang): Orchestrator co the ap dung Retry voi co che Exponential Backoff (vi du: thu lai 3 lan cach nhau 1s, 2s, 4s).
- Doi voi loi nghiep vu (business errors nhu het hang ton kho, the bi khoa, tai khoan khong du so du): Khong thu lai ma phai lap tuc chuyen sang luong bu tru (Compensation) de tra lai trang thai nhat quan cho he thong.
- Trong pham vi bai toan hien tai: Cac loi gia lap la loi nghiep vu (Out of Stock, Payment Failed, Shipping Failed), do do Orchestrator lap tuc ngat luong thuan va chuyen huong xu ly sang triggerCompensation().

### 2.2. Co Che Bu Tru (Compensation Flow - LIFO)

Co che bu tru duoc thuc hien theo nguyen ly LIFO (Last In First Out - Buoc nao thanh cong sau cung se bi rollback dau tien):

1. Truong hop that bai tai buoc Thanh toan (PAYMENT_FAILED):
- Chua co buoc nao thanh cong phia truoc.
- Khong can thuc hien hanh dong rollback.
- Trang thai chuyen sang ORDER_CANCELLED.

2. Truong hop that bai tai buoc Tru kho (INVENTORY_FAILED):
- Buoc Thanh toan da thanh cong truoc do.
- Orchestrator goi PaymentService.refundPayment() de hoan tien cho khach hang.
- Trang thai chuyen sang ORDER_CANCELLED.

3. Truong hop that bai tai buoc Giao hang (SHIPPING_FAILED):
- Buoc Thanh toan va Tru kho da thanh cong truoc do.
- Orchestrator thuc hien bu tru theo thu tu nguoc lai:
  1. Goi InventoryService.compensateInventory() de hoan lai so luong hang vao kho.
  2. Goi PaymentService.refundPayment() de hoan tien cho khach hang.
- Trang thai chuyen sang ORDER_CANCELLED.

---

## 3. Huong Dan Cai Dat va Chay Du An

### 3.1. Yeu Cau Moi Truong

- Java Development Kit (JDK): Phien ban 17 tro len (hoac JDK 21/24).
- Gradle: Co the dung truc tiep Gradle Wrapper (gradlew) di kem trong project.

### 3.2. Cac Buoc Thuc Hien

1. Mo terminal tai thu muc goc cua du an:
```bash
cd d:/RA_IT214/bai4_it214_ss15
```

2. Kiem tra va bien dich ma nguon:
```bash
./gradlew build
```

3. Chay cac bai kiem thu tu dong (Unit Tests):
```bash
./gradlew test
```

4. Khoi chay ung dung Spring Boot:
```bash
./gradlew bootRun
```
Ung dung se khoi chay tai dia chi: http://localhost:8080.

---

## 4. Ket Qua Chay Thu Voi Du Lieu Dau Vao

### 4.1. Kich Ban 1: Don Hang Thanh Cong Hoan Toan (ORD-001)

#### Du lieu gui den (POST /api/orders/saga/start):
```bash
curl -X POST http://localhost:8080/api/orders/saga/start \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-001",
    "userId": "USER-101",
    "amount": 250.0
  }'
```

#### Log xu ly he thong:
```text
[Saga] Starting Order: ORD-001 ...
[Saga] Event: START_ORDER -> State: PAYMENT_PROCESSING
[PaymentService] Processing payment for ORD-001 ... Success
[Saga] Event: PAYMENT_SUCCESS -> State: INVENTORY_PROCESSING
[InventoryService] Deducting inventory for ORD-001 ... Success
[Saga] Event: INVENTORY_SUCCESS -> State: SHIPPING_PROCESSING
[ShippingService] Creating shipment for ORD-001 ... Success
[Saga] Event: SHIPPING_SUCCESS -> State: ORDER_COMPLETED
[Saga] Order ORD-001 completed successfully!
```

#### Ket qua tra ve:
- currentState: ORDER_COMPLETED
- paymentSuccessful: true
- inventorySuccessful: true
- shippingSuccessful: true

---

### 4.2. Kich Ban 2: Don Hang That Bai Tai Buoc Tru Kho - Bu Tru Hoan Tien (ORD-002)

#### Du lieu gui den (POST /api/orders/saga/start):
```bash
curl -X POST http://localhost:8080/api/orders/saga/start \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-002",
    "userId": "USER-102",
    "amount": 150.0,
    "simulateFailureStep": "INVENTORY"
  }'
```

#### Log xu ly he thong:
```text
[Saga] Starting Order: ORD-002 ...
[Saga] Event: START_ORDER -> State: PAYMENT_PROCESSING
[PaymentService] Processing payment for ORD-002 ... Success
[Saga] Event: PAYMENT_SUCCESS -> State: INVENTORY_PROCESSING
[InventoryService] Deducting inventory for ORD-002 ... Failed (Out of Stock)
[Saga] Event: INVENTORY_FAILED -> State: ORDER_CANCELLED
[Saga] Starting compensation for ORD-002 ...
[PaymentService] Refunding payment for ORD-002 ... Success
[Saga] Compensation completed. Order ORD-002 is CANCELLED.
```

#### Ket qua tra ve:
- currentState: ORDER_CANCELLED
- failureReason: Out of Stock
- paymentSuccessful: false (da hoan tien)
- inventorySuccessful: false

---

### 4.3. Kich Ban 3: Don Hang That Bai Tai Buoc Giao Hang - Bu Tru Hoan Kho va Hoan Tien (ORD-003)

#### Du lieu gui den (POST /api/orders/saga/start):
```bash
curl -X POST http://localhost:8080/api/orders/saga/start \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-003",
    "userId": "USER-103",
    "amount": 500.0,
    "simulateFailureStep": "SHIPPING"
  }'
```

#### Log xu ly he thong:
```text
[Saga] Starting Order: ORD-003 ...
[Saga] Event: START_ORDER -> State: PAYMENT_PROCESSING
[PaymentService] Processing payment for ORD-003 ... Success
[Saga] Event: PAYMENT_SUCCESS -> State: INVENTORY_PROCESSING
[InventoryService] Deducting inventory for ORD-003 ... Success
[Saga] Event: INVENTORY_SUCCESS -> State: SHIPPING_PROCESSING
[ShippingService] Creating shipment for ORD-003 ... Failed
[Saga] Event: SHIPPING_FAILED -> State: ORDER_CANCELLED
[Saga] Starting compensation for ORD-003 ...
[InventoryService] Compensating inventory for ORD-003 ... Success
[PaymentService] Refunding payment for ORD-003 ... Success
[Saga] Compensation completed. Order ORD-003 is CANCELLED.
```

#### Ket qua tra ve:
- currentState: ORDER_CANCELLED
- failureReason: Shipping provider unavailable
- shippingSuccessful: false
- inventorySuccessful: false (da hoan kho)
- paymentSuccessful: false (da hoan tien)

---

### 4.4. Kich Ban 4: Tra Cuu Trang Thai Don Hang

#### Du lieu gui den:
```bash
curl -X GET http://localhost:8080/api/orders/saga/ORD-001/status
```

#### Ket qua tra ve:
```json
{
  "orderId": "ORD-001",
  "userId": "USER-101",
  "amount": 250.0,
  "currentState": "ORDER_COMPLETED",
  "sagaHistory": [
    "[Saga] Starting Order: ORD-001 ...",
    "[Saga] Event: START_ORDER -> State: PAYMENT_PROCESSING",
    "[PaymentService] Processing payment for ORD-001 ... Success",
    "[Saga] Event: PAYMENT_SUCCESS -> State: INVENTORY_PROCESSING",
    "[InventoryService] Deducting inventory for ORD-001 ... Success",
    "[Saga] Event: INVENTORY_SUCCESS -> State: SHIPPING_PROCESSING",
    "[ShippingService] Creating shipment for ORD-001 ... Success",
    "[Saga] Event: SHIPPING_SUCCESS -> State: ORDER_COMPLETED",
    "[Saga] Order ORD-001 completed successfully!"
  ],
  "paymentSuccessful": true,
  "inventorySuccessful": true,
  "shippingSuccessful": true,
  "failureReason": null,
  "simulateFailureStep": null
}
```
