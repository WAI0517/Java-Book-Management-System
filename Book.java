package bookmanagement.model;

public class Book {
    private String id;
    private String title;
    private String author;
    private int totalCount;
    private int remainingCount;

    public Book() {}

    public Book(String id, String title, String author, int totalCount, int remainingCount) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.totalCount = totalCount;
        this.remainingCount = remainingCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getRemainingCount() { return remainingCount; }
    public void setRemainingCount(int remainingCount) { this.remainingCount = remainingCount; }

    @Override
    public String toString() {
        return id + " - 《" + title + "》 " + author;
    }
}
