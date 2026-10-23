package bookmanagement.gui;

import bookmanagement.model.Book;
import bookmanagement.model.BorrowRecord;
import bookmanagement.service.BookService;
import bookmanagement.service.BorrowService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminFrame extends JFrame {

    private final BookService bookService;
    private final BorrowService borrowService;
    private final JTable bookTable;
    private final DefaultTableModel tableModel;
    private JLabel statusLabel;

    public AdminFrame() {
        bookService = new BookService();
        borrowService = new BorrowService(bookService);

        setTitle("图书管理系统 —— 管理员后台");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // 工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));

        JButton addBtn = new JButton("＋ 添加图书");
        addBtn.setFont(addBtn.getFont().deriveFont(Font.BOLD, 12f));
        addBtn.addActionListener(e -> addBook());
        toolBar.add(addBtn);

        JButton editBtn = new JButton("✎ 编辑");
        editBtn.setFont(editBtn.getFont().deriveFont(Font.BOLD, 12f));
        editBtn.addActionListener(e -> editBook());
        toolBar.add(editBtn);

        JButton delBtn = new JButton("✕ 删除");
        delBtn.setFont(delBtn.getFont().deriveFont(Font.BOLD, 12f));
        delBtn.addActionListener(e -> deleteBook());
        toolBar.add(delBtn);

        toolBar.add(Box.createHorizontalStrut(20));

        JButton refreshBtn = new JButton("⟳ 刷新");
        refreshBtn.addActionListener(e -> refreshTable());
        toolBar.add(refreshBtn);

        toolBar.add(Box.createHorizontalStrut(20));

        JButton recordsBtn = new JButton("📋 查看借阅记录");
        recordsBtn.addActionListener(e -> showRecords());
        toolBar.add(recordsBtn);

        toolBar.add(Box.createHorizontalStrut(20));

        JButton backBtn = new JButton("↩ 返回登录");
        backBtn.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        toolBar.add(backBtn);

        // 搜索栏
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchBar.add(new JLabel("搜索（书名/作者/ID）："));
        JTextField searchField = new JTextField(18);
        searchBar.add(searchField);
        JButton searchBtn = new JButton("搜索");
        searchBar.add(searchBtn);
        JButton clearBtn = new JButton("显示全部");
        searchBar.add(clearBtn);

        searchBtn.addActionListener(e -> {
            List<Book> result = bookService.search(searchField.getText().trim());
            updateTableData(result);
            statusLabel.setText("  搜索到 " + result.size() + " 条记录");
        });
        clearBtn.addActionListener(e -> { searchField.setText(""); refreshTable(); });
        searchField.addActionListener(e -> searchBtn.doClick());

        // 表格
        String[] colNames = {"ID", "书名", "作者", "总数量", "剩余数量"};
        tableModel = new DefaultTableModel(colNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        bookTable = new JTable(tableModel);
        bookTable.setRowHeight(26);
        bookTable.getTableHeader().setFont(bookTable.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.setAutoCreateRowSorter(true);
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("图书列表"));

        statusLabel = new JLabel("  就绪");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolBar, BorderLayout.NORTH);
        topPanel.add(searchBar, BorderLayout.SOUTH);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        updateTableData(bookService.getAllBooks());
        statusLabel.setText("  共 " + bookService.size() + " 本图书");
    }

    private void updateTableData(List<Book> books) {
        tableModel.setRowCount(0);
        for (Book b : books) {
            tableModel.addRow(new Object[]{
                    b.getId(), b.getTitle(), b.getAuthor(),
                    b.getTotalCount(), b.getRemainingCount()
            });
        }
    }

    private Book getSelectedBook() {
        int row = bookTable.getSelectedRow();
        if (row < 0) return null;
        String id = (String) tableModel.getValueAt(bookTable.convertRowIndexToModel(row), 0);
        return bookService.findById(id);
    }

    private void addBook() {
        BookDialog d = new BookDialog(this, null);
        d.setVisible(true);
        if (d.isConfirmed()) {
            Book added = bookService.addBook(d.getBookTitle(), d.getBookAuthor(), d.getTotalCount());
            int remain = d.getRemainingCount();
            if (remain != d.getTotalCount()) {
                added.setRemainingCount(remain);
                bookService.updateBook(added);
            }
            refreshTable();
            statusLabel.setText("  添加成功 —— 《" + added.getTitle() + "》");
        }
    }

    private void editBook() {
        Book sel = getSelectedBook();
        if (sel == null) { JOptionPane.showMessageDialog(this, "请先选择一本图书！"); return; }
        BookDialog d = new BookDialog(this, sel);
        d.setVisible(true);
        if (d.isConfirmed()) {
            sel.setTitle(d.getBookTitle());
            sel.setAuthor(d.getBookAuthor());
            sel.setTotalCount(d.getTotalCount());
            sel.setRemainingCount(d.getRemainingCount());
            if (!bookService.updateBook(sel)) {
                JOptionPane.showMessageDialog(this,
                        "更新失败！馆藏总数不能小于当前已借出数量，请等待读者归还后再试。");
                return;
            }
            refreshTable();
            statusLabel.setText("  更新成功 —— 《" + sel.getTitle() + "》");
        }
    }

    private void deleteBook() {
        Book sel = getSelectedBook();
        if (sel == null) { JOptionPane.showMessageDialog(this, "请先选择一本图书！"); return; }

        // 检查是否有未归还的借阅记录
        if (borrowService.hasActiveBorrows(sel.getId())) {
            JOptionPane.showMessageDialog(this,
                    "无法删除《" + sel.getTitle() + "》！\n该书还有未归还的借阅记录，请等待全部归还后再删除。",
                    "删除失败", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int c = JOptionPane.showConfirmDialog(this,
                "确定要删除《" + sel.getTitle() + "》吗？",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            bookService.deleteBook(sel.getId());
            refreshTable();
            statusLabel.setText("  已删除 —— 《" + sel.getTitle() + "》");
        }
    }

    private void showRecords() {
        List<BorrowRecord> records = borrowService.getAllRecords();
        if (records.isEmpty()) { JOptionPane.showMessageDialog(this, "暂无借阅记录。"); return; }

        String[] cols = {"记录ID", "借阅者", "图书ID", "数量", "借阅日期", "归还日期"};
        String[][] data = new String[records.size()][6];
        for (int i = 0; i < records.size(); i++) {
            BorrowRecord r = records.get(i);
            data[i][0] = r.getRecordId();
            data[i][1] = r.getBorrowerName();
            data[i][2] = r.getBookId();
            data[i][3] = String.valueOf(r.getQuantity());
            data[i][4] = r.getBorrowDate();
            data[i][5] = r.isReturned() ? r.getReturnDate() : "未归还";
        }

        JTable recTable = new JTable(data, cols);
        recTable.setAutoCreateRowSorter(true);
        JScrollPane sp = new JScrollPane(recTable);
        sp.setPreferredSize(new Dimension(750, 350));
        JOptionPane.showMessageDialog(this, sp,
                "借阅记录（共 " + records.size() + " 条）", JOptionPane.INFORMATION_MESSAGE);
    }
}
