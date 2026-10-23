# 图书管理系统 —— 项目实现过程报告

> **技术栈：** Java 25 + Swing + 文件持久化（纯JDK，零外部依赖）

---

## 一、项目背景与需求分析

### 1.1 项目背景

随着信息化建设的不断推进，传统的人工图书管理方式（纸质登记、手工台账）已无法满足效率与准确性的要求。设计并实现一套**图书管理系统**，可以帮助图书馆或小型图书室实现图书入库、借阅、归还等操作的数字化管理，提升工作效率，降低管理成本。

### 1.2 用户角色与功能需求

系统分为两种用户角色：

| 角色 | 功能需求 |
|------|----------|
| **管理员** | 登录系统（密码验证）、添加图书、编辑图书信息、删除图书、查看全部借阅记录 |
| **借阅者** | 浏览图书、按关键字搜索图书、借阅图书、归还图书、查看个人借阅记录 |

### 1.3 核心业务规则

1. 每本图书有唯一编号，记录馆藏总数和当前可借剩余数量
2. 借阅时需验证库存是否充足，借阅日期不能早于当天
3. 归还时自动恢复库存，记录归还日期
4. 删除图书前校验是否存在未归还的借阅记录
5. 编辑图书时不能将馆藏总数减少到当前已借出数量以下

---

## 二、系统架构设计

### 2.1 整体架构

本项目采用**经典三层架构**（MVC变体），将系统划分为三个逻辑层：

```
┌─────────────────────────────────────┐
│          表示层 (GUI Layer)          │
│  LoginFrame / AdminFrame /          │
│  BorrowerFrame / BookDialog         │
│  (Java Swing 图形界面)              │
├─────────────────────────────────────┤
│         业务逻辑层 (Service Layer)    │
│  BookService / BorrowService        │
│  (业务规则、数据验证、ID生成)         │
├─────────────────────────────────────┤
│         数据层 (Model Layer)         │
│  Book / BorrowRecord                │
│  (数据实体 + 文本文件持久化)           │
└─────────────────────────────────────┘
```

**设计优势：**
- **高内聚低耦合**：每层职责清晰，GUI 不直接操作文件，Service 不关心界面
- **易于扩展**：如需替换为数据库，只需修改 Service 层的持久化代码
- **可维护性**：修改界面布局不影响业务逻辑，反之亦然

### 2.2 包结构设计

```
bookmanagement/
├── Main.java                 # 程序入口，设置系统外观风格
├── gui/                      # 图形界面层
│   ├── LoginFrame.java       # 登录/角色选择窗口
│   ├── AdminFrame.java       # 管理员操作界面
│   ├── BorrowerFrame.java    # 借阅者操作界面
│   └── BookDialog.java       # 添加/编辑图书对话框
├── model/                    # 数据实体层
│   ├── Book.java             # 图书实体类
│   └── BorrowRecord.java     # 借阅记录实体类
└── service/                  # 业务逻辑层
    ├── BookService.java      # 图书管理服务
    └── BorrowService.java    # 借阅管理服务
```

---

## 三、数据存储设计

### 3.1 存储方案选型

考虑到项目定位为**轻量级桌面应用**，无需安装和配置数据库服务器，本系统采用**文本文件持久化**方案。数据以管道符 `|` 分隔的文本格式存储，使用 UTF-8 编码，结构清晰、便于调试和维护。

### 3.2 图书数据文件设计 (books.txt)

```
3                           ← 下一本图书的自增ID计数器
B000001|Java编程思想|Bruce Eckel|10|8
B000002|数据结构|严蔚敏|5|5
```

| 字段 | 说明 | 示例 |
|------|------|------|
| 第1行 | 自增ID计数器（整型） | `3` |
| 每行记录 | `ID\|书名\|作者\|馆藏总数\|剩余数量` | `B000001\|Java编程思想\|Bruce Eckel\|10\|8` |

**ID 生成规则**：格式为 `B` + 6位数字（如 `B000001`），通过读取第一行计数器自增生成，保证唯一性。

### 3.3 借阅记录数据文件设计 (borrow_records.txt)

```
3                           ← 下一条记录的自增ID计数器
R000001|张三|B000001|2|2026-06-08|未归还
R000002|李四|B000002|1|2026-06-10|2026-06-15
```

| 字段 | 说明 | 示例 |
|------|------|------|
| 第1行 | 自增ID计数器（整型） | `3` |
| ` | `R000001\|张三\|B000001\|2\|2026-06-08\|未归还` |

**设计要点：**
- 归还日期字段用 `"未归还"` 标识尚未归还的记录
- 记录ID格式为 `R` + 6位数字（如 `R000001`）
- `BorrowService` 中通过借阅者姓名 + 图书ID + 未归还状态来匹配归还记录

---

## 四、核心模块实现

### 4.1 数据实体层实现

#### Book 实体类

```java
public class Book {
    private String id;           // 图书编号，如 B000001
    private String title;        // 书名
    private String author;       // 作者
    private int totalCount;      // 馆藏总数
    private int remainingCount;  // 当前剩余可借数量

    // 提供完整的 getter/setter 方法
    // toString() 格式化输出："B000001 - 《Java编程思想》 Bruce Eckel"
}
```

**设计考量：**
- `totalCount` 和 `remainingCount` 分离设计，避免借阅操作丢失原始馆藏信息
- `remainingCount` 作为实时库存，借阅/归还时动态更新

#### BorrowRecord 实体类

```java
public class BorrowRecord {
    private String recordId;     // 记录编号，如 R000001
    private String borrowerName; // 借阅者姓名
    private String bookId;       // 图书编号
    private int quantity;        // 借阅数量
    private String borrowDate;   // 借阅日期 (yyyy-MM-dd)
    private String returnDate;   // 归还日期，未归还则为 "未归还"

    // isReturned() 方法：判断 returnDate 是否不为空且不等于 "未归还"
}
```

### 4.2 业务逻辑层实现

#### BookService —— 图书管理服务每行记录 | `记录ID\|借阅者\|图书ID\|数量\|借阅日期\|归还日期

**核心数据结构：** `LinkedHashMap<String, Book>` —— 保证插入顺序的同时提供 O(1) 的按ID查找

**关键方法实现：**

| 方法 | 功能 | 关键逻辑 |
|------|------|----------|
| `addBook()` | 添加图书 | 读取计数器 → 生成ID → 构造Book对象 → 写入Map → 持久化到文件 |
| `deleteBook()` | 删除图书 | 从Map中移除 → 重新序列化全部数据到文件 |
| `updateBook()` | 编辑图书 | **约束校验**：总数 ≥ 已借数量；剩余数 ≤ 总数 - 已借数 |
| `search()` | 搜索图书 | 线性扫描，不区分大小写匹配 ID/书名/作者 |
| `borrowBook()` | 库存扣减 | 校验库存≥借阅数量 → remainingCount -= quantity |
| `returnBook()` | 库存恢复 | remainingCount += quantity |

**文件读写机制：**
- 启动时：通过 `BufferedReader` 逐行读取，解析管道符分隔的字段
- 修改后：通过 `BufferedWriter` 全量写回文件（先写计数器，再写每条记录）
- 编码：统一使用 UTF-8，避免中文乱码

```java
// 核心：编辑图书时的约束校验逻辑
public void updateBook(Book book) {
    int borrowedCount = book.getTotalCount() - book.getRemainingCount();
    // 约束1：总数不能小于已借出数量
    if (book.getTotalCount() < borrowedCount) {
        throw new IllegalArgumentException("总数不能小于已借出数量");
    }
    // 约束2：剩余数量范围校验
    if (book.getRemainingCount() < 0 || book.getRemainingCount() > book.getTotalCount()) {
        throw new IllegalArgumentException("剩余数量不在有效范围");
    }
    // 约束3：剩余数量不能超过总数减去已借出的部分
    if (book.getRemainingCount() > book.getTotalCount() - borrowedCount) {
        throw new IllegalArgumentException("剩余数量超出限制");
    }
    // ... 更新Map并持久化
}
```

#### BorrowService —— 借阅管理服务

**依赖注入**：构造函数接收 `BookService` 引用，实现服务间协作

**关键方法实现：**

| 方法 | 功能 | 关键逻辑 |
|------|------|----------|
| `borrow()` | 借阅图书 | 验证姓名非空 → 验证图书存在 → 验证库存充足 → 验证日期格式且不早于当天 → 扣减库存 → 生成借阅记录 |
| `returnBook()` | 归还图书 | 通过姓名+图书ID匹配未归还记录 → 验证库存不溢出 → 恢复库存 → 更新归还日期 |
| `hasActiveBorrows()` | 检查未归还记录 | 删除图书前的安全检查 |
| `getRecordsByBorrower()` | 按借阅者筛选 | 遍历所有记录，按姓名过滤返回列表 |

```java
// 核心：借阅方法的业务校验流程
public String borrow(String borrowerName, String bookId, int quantity, String borrowDate) {
    // 1. 借阅者姓名校验
    if (borrowerName == null || borrowerName.trim().isEmpty()) {
        return "借阅者姓名不能为空";
    }
    // 2. 图书存在性校验
    Book book = bookService.findById(bookId);
    if (book == null) {
        return "图书不存在";
    }
    // 3. 库存校验
    if (book.getRemainingCount() < quantity) {
        return "库存不足，当前剩余：" + book.getRemainingCount();
    }
    // 4. 日期格式 + 不早于今天校验
    try {
        LocalDate date = LocalDate.parse(borrowDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (date.isBefore(LocalDate.now())) {
            return "借阅日期不能早于今天";
        }
    } catch (DateTimeParseException e) {
        return "日期格式错误，请使用 yyyy-MM-dd 格式";
    }
    // 5. 执行借阅
    bookService.borrowBook(bookId, quantity);
    // ... 生成记录并持久化
    return null; // null 表示成功
}
```

### 4.3 图形界面层实现

#### 系统外观设置

```java
// Main.java - 程序入口
UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
SwingUtilities.invokeLater(() -> new LoginFrame());
```

- 使用 `getSystemLookAndFeelClassName()` 使程序外观与操作系统一致
- `SwingUtilities.invokeLater()` 确保所有GUI操作在事件调度线程(EDT)上执行，避免线程安全问题

#### LoginFrame —— 登录界面

- **布局**：使用 `BorderLayout` + `Box` 垂直布局
- **交互逻辑**：
  - 点击"管理员入口" → 弹出密码输入框，密码为 `123456`，验证通过后进入 AdminFrame
  - 点击"借阅者入口" → 直接进入 BorrowerFrame（无需密码）
- **窗口管理**：登录成功后 `dispose()` 当前窗口，新建目标窗口

#### AdminFrame —— 管理员后台

**界面布局：**
- **顶部工具栏**：JToolBar 包含添加、编辑、删除、刷新、查看借阅记录、返回按钮
- **搜索栏**：JTextField + "搜索"按钮，支持按书名/作者/ID实时搜索
- **中央区域**：JScrollPane + JTable，5列（ID、书名、作者、总数量、剩余数量）
- **JTable 特性**：启用 `setAutoCreateRowSorter(true)` 支持点击表头排序

**关键交互逻辑：**

| 操作 | 处理流程 |
|------|----------|
| 添加图书 | 弹出 BookDialog（添加模式）→ 确认后调用 `bookService.addBook()` → 刷新表格 |
| 编辑图书 | 获取选中行 → 弹出 BookDialog（编辑模式，预填数据）→ 确认后调用 `bookService.updateBook()` |
| 删除图书 | 确认对话框 → 检查 `hasActiveBorrows()` → 调用 `bookService.deleteBook()` |
| 查看记录 | 弹出 JOptionPane 展示全部借阅记录（JTable + JScrollPane） |

```java
// 核心：删除前的安全检查
if (borrowService.hasActiveBorrows(selectedBook.getId())) {
    JOptionPane.showMessageDialog(this, "该图书有未归还的借阅记录，无法删除！", "删除失败", JOptionPane.WARNING_MESSAGE);
    return;
}
```

#### BorrowerFrame —— 借阅者界面

**界面布局：**
- 搜索栏 + 图书表格（与管理员界面类似，但最后一列标题改为"剩余可借"）
- 底部操作按钮：借阅、归还、我的借阅记录
- 表格双击事件绑定借阅操作

**借阅流程（核心交互）：**
1. 用户在表格中选择一本图书
2. 弹出输入面板（GridBagLayout 布局），输入姓名、数量（JSpinner，上限为当前库存）、日期
3. 系统验证后调用 `borrowService.borrow()`
4. 成功则刷新表格显示最新库存

**归还流程：**
1. 弹出输入面板，输入姓名、图书ID、归还日期
2. 若表格中有选中行，图书ID自动填充
3. 调用 `borrowService.returnBook()` 执行归还

**我的借阅记录：**
1. 弹窗要求输入姓名
2. 调用 `borrowService.getRecordsByBorrower(name)` 获取记录
3. 在表格对话框中展示，包含"状态"列（已归还/未归还）

#### BookDialog —— 添加/编辑图书对话框

**设计模式：Modal Dialog（模态对话框）**
- 继承 `JDialog`，设置 `setModal(true)` 阻塞父窗口
- 使用 `isConfirmed()` + getter 方法与调用方通信

**添加模式与编辑模式的差异化处理：**
| 特性 | 添加模式 | 编辑模式 |
|------|----------|----------|
| 预填数据 | 空 | 填充选中图书的当前值 |
| 总数下限 | 1 | 已借出数量（borrowedCount），防止数据不一致 |
| 联动逻辑 | 无 | 修改总数时自动调整剩余数量 |

**联动逻辑实现：**
```java
// 总数JSpinner的值变更监听
totalCountSpinner.addChangeListener(e -> {
    int newTotal = (int) totalCountSpinner.getValue();
    int oldTotal = originalTotalCount;  // 编辑前的总数
    int currentRemaining = (int) remainingCountSpinner.getValue();
    int delta = newTotal - oldTotal;
    int newRemaining = currentRemaining + delta;
    // 约束：剩余数量在合理范围内
    newRemaining = Math.max(0, Math.min(newRemaining, newTotal - borrowedCount));
    remainingCountSpinner.setValue(newRemaining);
    oldTotal = newTotal;
});
```

---

## 五、关键技术难点与解决方案

### 5.1 数据一致性保障

**难点：** 文本文件没有数据库的事务机制，如何在多步操作中保持数据一致？

**解决方案：**
1. **内存优先策略**：所有操作先在内存中的 `LinkedHashMap` 完成，验证通过后再一次性写回文件
2. **全量写回**：每次写操作都将整个数据集序列化到文件，避免增量写入导致的不一致
3. **约束前置校验**：所有业务规则在修改内存数据前完成校验，校验失败则直接返回错误，不触碰文件

### 5.2 图书编辑时的库存约束

**难点：** 编辑图书时，不能将馆藏总数减少到已借出数量以下，否则会产生逻辑错误

**解决方案：**
- 计算 `borrowedCount = totalCount - remainingCount`（当前已借出数量）
- 编辑模式下，总数下限设置为 `borrowedCount`（确保至少能覆盖已借出的书）
- 联动逻辑：总数变更时，剩余数量按差值自动增减，并做边界校验

### 5.3 借阅与归还的匹配逻辑

**难点：** 同一借阅者可以多次借阅同一本书，归还时如何准确匹配？

**解决方案：**
- 归还时按 `借阅者姓名 + 图书ID + 未归还状态` 三个条件匹配
- 找到第一条匹配的未归还记录即执行归还
- 避免了一次性归还所有同书记录的问题

### 5.4 文件读写的中文编码

**难点：** 图书信息和用户输入均包含中文，编码不当会导致乱码

**解决方案：**
- 所有文件读写统一使用 `UTF-8` 编码
- GUI 字体统一设置为 `"微软雅黑"`（Windows 系统原生中文字体）

### 5.5 GUI 线程安全

**难点：** Swing 组件不是线程安全的，非 EDT 线程操作可能导致界面异常

**解决方案：**
- 程序入口使用 `SwingUtilities.invokeLater()` 启动
- 所有事件处理器（按钮点击、表格选择等）天然在 EDT 中执行
- 文件 I/O 操作在事件线程中同步执行，数据量小不会有性能问题

---

## 六、系统界面展示与功能验证

### 6.1 登录界面
- 管理员入口（需密码验证，密码：123456）
- 借阅者入口（无需密码）
- 居中显示，不可调整大小

### 6.2 管理员功能验证

| 测试项 | 测试步骤 | 预期结果 |
|--------|----------|----------|
| 添加图书 | 点击添加 → 填写信息 → 确认 | 表格新增一行，文件自动更新 |
| 编辑图书 | 选中行 → 点击编辑 → 修改数据 → 确认 | 表格更新，库存约束校验通过 |
| 删除图书 | 选中行 → 点击删除 → 确认 | 无未归还记录则删除成功 |
| 删除有借阅的图书 | 选中已借出的书 → 点击删除 | 提示"无法删除"，数据保持不变 |
| 查看借阅记录 | 点击"查看借阅记录" | 弹窗展示全量记录表格 |
| 搜索功能 | 输入关键字 → 搜索 | 表格实时过滤，支持 ID/书名/作者 |

### 6.3 借阅者功能验证

| 测试项 | 测试步骤 | 预期结果 |
|--------|----------|----------|
| 借阅图书 | 选中书 → 借阅 → 输入信息 → 确认 | 库存减1，生成借阅记录 |
| 库存不足 | 借阅数量 > 剩余数量 | 提示"库存不足" |
| 归还图书 | 归还 → 输入姓名/书ID/日期 → 确认 | 库存恢复，记录更新为已归还 |
| 查看我的记录 | 点击"我的借阅记录" → 输入姓名 | 展示该读者的借阅历史 |

---

## 七、项目总结与展望

### 7.1 项目成果

本项目成功实现了一套**功能完整的图书管理系统**，具备以下特点：

1. **功能完备**：覆盖图书管理（增删改查）、借阅管理（借出/归还）、记录查询等核心业务
2. **用户友好**：基于 Swing 的图形界面，操作直观，支持搜索和排序
3. **数据可靠**：完善的数据校验机制和业务约束，保证数据一致性
4. **部署简单**：纯 Java 实现，零外部依赖，复制即可运行
5. **代码规范**：清晰的三层架构，良好的命名规范和注释

### 7.2 技术收获

- 掌握了 Java Swing 图形界面编程（JTable、JDialog、JSpinner 等组件）
- 深入理解了文件 I/O 与数据持久化方案的设计
- 实践了三层架构（表示层-业务层-数据层）的代码组织方式
- 积累了数据一致性校验和约束设计经验
- 学会了模态对话框、事件监听、值联动等 GUI 交互模式

### 7.3 改进方向

| 方向 | 方案 |
|------|------|
| 数据存储升级 | 引入 SQLite 或 MySQL 替代文本文件，提升数据安全性和查询效率 |
| 用户认证增强 | 实现多用户注册登录、密码加密存储、权限分级管理 |
| 功能扩展 | 增加图书分类、逾期罚款、预约排队、借阅统计报表等功能 |
| 界面美化 | 引入 FlatLaf 等现代化 Swing 主题库 |
| 架构优化 | 引入 Maven/Gradle 构建工具，使用依赖注入框架管理组件 |

---

## 附录：项目文件清单

```
D:\Study\Java\com.Ahstu.practical\
├── books.txt                    # 图书数据文件
├── borrow_records.txt           # 借阅记录数据文件（运行时自动创建）
└── src\bookmanagement\
    ├── Main.java                # 程序入口（系统外观 + EDT启动）
    ├── gui\
    │   ├── LoginFrame.java      # 登录界面（400行+）
    │   ├── AdminFrame.java      # 管理员后台（450行+）
    │   ├── BorrowerFrame.java   # 借阅者界面（550行+）
    │   └── BookDialog.java      # 图书编辑对话框（400行+）
    ├── model\
    │   ├── Book.java            # 图书实体
    │   └── BorrowRecord.java    # 借阅记录实体
    └── service\
        ├── BookService.java     # 图书管理服务（200行+）
        └── BorrowService.java   # 借阅管理服务（250行+）
```

---

> **报告撰写日期：** 2026年6月15日
>
> **开发环境：** IntelliJ IDEA + JDK 25 + Git Bash (Windows 11)
