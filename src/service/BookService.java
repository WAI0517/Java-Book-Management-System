package bookmanagement.service;

import bookmanagement.model.Book;

import java.io.*;
import java.util.*;

// 图书数据管理，用 books.txt 保存，每行: ID|书名|作者|总数量|剩余数量
public class BookService {

    private Map<String, Book> books = new LinkedHashMap<>();
    private int nextId = 1;

    public BookService() { loadBooks(); }

    public Book addBook(String title, String author, int totalCount) {
        String id = String.format("B%06d", nextId++);
        Book book = new Book(id, title, author, totalCount, totalCount);
        books.put(id, book);
        saveBooks();
        return book;
    }

    public boolean deleteBook(String id) {
        if (books.remove(id) == null) return false;
        saveBooks();
        return true;
    }

    public boolean updateBook(Book book) {
        if (book.getId() == null || !books.containsKey(book.getId())) return false;
        Book old = books.get(book.getId());
        // 馆藏总数不能小于当前已借出数量，否则还书时会库存溢出
        int borrowedCount = old.getTotalCount() - old.getRemainingCount();
        if (book.getTotalCount() < borrowedCount) return false;
        if (book.getRemainingCount() < 0 || book.getRemainingCount() > book.getTotalCount()) return false;
        // 剩余数量不能超过馆藏总数减去外借数量
        if (book.getRemainingCount() > book.getTotalCount() - borrowedCount) return false;
        books.put(book.getId(), book);
        saveBooks();
        return true;
    }

    public Book findById(String id) { return books.get(id); }

    public List<Book> getAllBooks() { return new ArrayList<>(books.values()); }

    public List<Book> search(String keyword) {
        List<Book> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) return getAllBooks();
        String kw = keyword.trim().toLowerCase();
        for (Book b : books.values()) {
            if (b.getId().toLowerCase().contains(kw)
                    || b.getTitle().toLowerCase().contains(kw)
                    || b.getAuthor().toLowerCase().contains(kw)) {
                result.add(b);
            }
        }
        return result;
    }

    public boolean borrowBook(String bookId, int quantity) {
        Book book = books.get(bookId);
        if (book == null || book.getRemainingCount() < quantity) return false;
        book.setRemainingCount(book.getRemainingCount() - quantity);
        saveBooks();
        return true;
    }

    public boolean returnBook(String bookId, int quantity) {
        Book book = books.get(bookId);
        if (book == null || book.getRemainingCount() + quantity > book.getTotalCount()) return false;
        book.setRemainingCount(book.getRemainingCount() + quantity);
        saveBooks();
        return true;
    }

    public int size() { return books.size(); }

    private void loadBooks() {
        File file = new File("books.txt");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line = reader.readLine();
            if (line != null) nextId = Integer.parseInt(line.trim());
            books.clear();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length < 5) continue;
                Book b = new Book();
                b.setId(parts[0]);
                b.setTitle(parts[1]);
                b.setAuthor(parts[2]);
                try { b.setTotalCount(Integer.parseInt(parts[3])); } catch (NumberFormatException ignored) { }
                try { b.setRemainingCount(Integer.parseInt(parts[4])); } catch (NumberFormatException ignored) { }
                books.put(b.getId(), b);
            }
        } catch (Exception e) {
            System.out.println("读取 books.txt 失败：" + e.getMessage());
        }
    }

    private void saveBooks() {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("books.txt"), "UTF-8"))) {
            writer.write(String.valueOf(nextId));
            writer.newLine();
            for (Book b : books.values()) {
                writer.write(b.getId() + "|" + b.getTitle() + "|" + b.getAuthor()
                        + "|" + b.getTotalCount() + "|" + b.getRemainingCount());
                writer.newLine();
            }
            writer.flush();
        } catch (Exception e) {
            System.out.println("保存 books.txt 失败：" + e.getMessage());
        }
    }
}
