package bookmanagement.service;

import bookmanagement.model.Book;
import bookmanagement.model.BorrowRecord;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

// 借阅记录管理，用 borrow_records.txt 保存，每行: 记录ID|借阅者|图书ID|数量|借阅日期|归还日期
public class BorrowService {

    private Map<String, BorrowRecord> records = new LinkedHashMap<>();
    private int nextRecordId = 1;
    private BookService bookService;

    public BorrowService(BookService bookService) {
        this.bookService = bookService;
        loadRecords();
    }

    // 借阅图书，返回 null 表示成功，否则返回错误信息
    public String borrow(String borrowerName, String bookId, int quantity, String borrowDate) {
        if (borrowerName == null || borrowerName.trim().isEmpty()) return "借阅者姓名不能为空！";
        Book book = bookService.findById(bookId);
        if (book == null) return "图书 ID【" + bookId + "】不存在！";
        if (quantity <= 0) return "借阅数量必须大于 0！";
        if (book.getRemainingCount() < quantity)
            return "库存不足！《" + book.getTitle() + "》剩余 " + book.getRemainingCount() + " 本。";
        try {
            LocalDate bDate = LocalDate.parse(borrowDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (bDate.isAfter(LocalDate.now())) return "借阅日期不能晚于今天！";
        } catch (DateTimeParseException e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式！";
        }

        String recordId = String.format("R%06d", nextRecordId++);
        BorrowRecord r = new BorrowRecord(recordId, borrowerName.trim(), bookId,
                quantity, borrowDate, "未归还");
        records.put(recordId, r);
        bookService.borrowBook(bookId, quantity);
        saveRecords();
        return null;
    }

    // 归还图书，返回 null 表示成功，否则返回错误信息
    public String returnBook(String borrowerName, String bookId, String returnDate) {
        if (borrowerName == null || borrowerName.trim().isEmpty()) return "借阅者姓名不能为空！";
        try { LocalDate.parse(returnDate, DateTimeFormatter.ofPattern("yyyy-MM-dd")); }
        catch (DateTimeParseException e) { return "日期格式错误，请使用 yyyy-MM-dd 格式！"; }

        BorrowRecord matched = null;
        for (BorrowRecord r : records.values()) {
            if (!r.isReturned()
                    && r.getBorrowerName().equalsIgnoreCase(borrowerName.trim())
                    && r.getBookId().equalsIgnoreCase(bookId)) {
                matched = r;
                break;
            }
        }
        if (matched == null) return "未找到【" + borrowerName + "】借阅图书【" + bookId + "】的未归还记录！";

        String realBookId = matched.getBookId();
        Book book = bookService.findById(realBookId);
        if (book == null) return "图书【" + realBookId + "】已不存在！";
        if (book.getRemainingCount() + matched.getQuantity() > book.getTotalCount())
            return "归还失败：库存溢出！";

        matched.setReturnDate(returnDate);
        bookService.returnBook(realBookId, matched.getQuantity());
        saveRecords();
        return null;
    }

    public List<BorrowRecord> getAllRecords() { return new ArrayList<>(records.values()); }

    // 检查指定图书是否有未归还的借阅记录
    public boolean hasActiveBorrows(String bookId) {
        for (BorrowRecord r : records.values()) {
            if (!r.isReturned() && r.getBookId().equalsIgnoreCase(bookId)) {
                return true;
            }
        }
        return false;
    }

    public List<BorrowRecord> getRecordsByBorrower(String name) {
        List<BorrowRecord> result = new ArrayList<>();
        for (BorrowRecord r : records.values())
            if (r.getBorrowerName().equalsIgnoreCase(name))
                result.add(r);
        return result;
    }

    private void loadRecords() {
        File file = new File("borrow_records.txt");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line = reader.readLine();
            if (line != null) nextRecordId = Integer.parseInt(line.trim());
            records.clear();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length < 6) continue;
                BorrowRecord r = new BorrowRecord();
                r.setRecordId(parts[0]);
                r.setBorrowerName(parts[1]);
                r.setBookId(parts[2]);
                try { r.setQuantity(Integer.parseInt(parts[3])); } catch (NumberFormatException ignored) { }
                r.setBorrowDate(parts[4]);
                r.setReturnDate(parts[5]);
                records.put(r.getRecordId(), r);
            }
        } catch (Exception e) {
            System.out.println("读取 borrow_records.txt 失败：" + e.getMessage());
        }
    }

    private void saveRecords() {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("borrow_records.txt"), "UTF-8"))) {
            writer.write(String.valueOf(nextRecordId));
            writer.newLine();
            for (BorrowRecord r : records.values()) {
                writer.write(r.getRecordId() + "|" + r.getBorrowerName() + "|"
                        + r.getBookId() + "|" + r.getQuantity() + "|"
                        + r.getBorrowDate() + "|"
                        + (r.getReturnDate() != null ? r.getReturnDate() : "未归还"));
                writer.newLine();
            }
            writer.flush();
        } catch (Exception e) {
            System.out.println("保存 borrow_records.txt 失败：" + e.getMessage());
        }
    }
}
