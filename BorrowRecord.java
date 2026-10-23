package bookmanagement.model;

public class BorrowRecord {
    private String recordId;
    private String borrowerName;
    private String bookId;
    private int quantity;
    private String borrowDate;
    private String returnDate;

    public BorrowRecord() {}

    public BorrowRecord(String recordId, String borrowerName, String bookId,
                        int quantity, String borrowDate, String returnDate) {
        this.recordId = recordId;
        this.borrowerName = borrowerName;
        this.bookId = bookId;
        this.quantity = quantity;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getBorrowDate() { return borrowDate; }
    public void setBorrowDate(String borrowDate) { this.borrowDate = borrowDate; }
    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public boolean isReturned() {
        return returnDate != null && !returnDate.isEmpty() && !"未归还".equals(returnDate);
    }

    @Override
    public String toString() {
        return recordId + " | " + borrowerName + " | 图书" + bookId
                + " ×" + quantity + " | " + borrowDate + " | "
                + (isReturned() ? returnDate : "未归还");
    }
}
