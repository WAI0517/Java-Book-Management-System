package bookmanagement.gui;

import bookmanagement.model.Book;
import bookmanagement.model.BorrowRecord;
import bookmanagement.service.BookService;
import bookmanagement.service.BorrowService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BorrowerFrame extends JFrame {

    private final BookService bookService;
    private final BorrowService borrowService;
    private final JTable bookTable;
    private final DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JTextField searchField;
    private DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BorrowerFrame() {
        bookService = new BookService();
        borrowService = new BorrowService(bookService);

        setTitle("图书管理系统 —— 借阅者界面");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // 搜索栏
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchBar.add(new JLabel("搜索图书："));
        searchField = new JTextField(18);
        searchBar.add(searchField);
        JButton searchBtn = new JButton("搜索");
        searchBar.add(searchBtn);
        JButton showAllBtn = new JButton("显示全部");
        searchBar.add(showAllBtn);
        searchBar.add(Box.createHorizontalStrut(20));
        JButton backBtn = new JButton("↩ 返回登录");
        backBtn.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        searchBar.add(backBtn);

        searchBtn.addActionListener(e -> {
            List<Book> result = bookService.search(searchField.getText().trim());
            updateTableData(result);
            statusLabel.setText("  搜索到 " + result.size() + " 本");
        });
        showAllBtn.addActionListener(e -> { searchField.setText(""); refreshTable(); });
        searchField.addActionListener(e -> searchBtn.doClick());

        // 表格
        String[] colNames = {"ID", "书名", "作者", "总数量", "剩余可借"};
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
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(280);
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(80);

        bookTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) borrowBook();
            }
        });

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("图书列表（双击可借阅）"));

        // 底部按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));

        JButton borrowBtn = new JButton("借阅图书");
        borrowBtn.setPreferredSize(new Dimension(130, 36));
        borrowBtn.addActionListener(e -> borrowBook());
        btnPanel.add(borrowBtn);

        JButton returnBtn = new JButton("归还图书");
        returnBtn.setPreferredSize(new Dimension(130, 36));
        returnBtn.addActionListener(e -> returnBook());
        btnPanel.add(returnBtn);

        JButton myRecBtn = new JButton("我的借阅记录");
        myRecBtn.setPreferredSize(new Dimension(150, 36));
        myRecBtn.addActionListener(e -> showMyRecords());
        btnPanel.add(myRecBtn);

        statusLabel = new JLabel("  就绪");
        statusLabel.setBorder(BorderFactory.createEtchedBorder());

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(btnPanel, BorderLayout.NORTH);
        southPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(searchBar, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        refreshTable();
    }

    private void refreshTable() {
        updateTableData(bookService.getAllBooks());
        statusLabel.setText("  共 " + bookService.size() + " 本图书可借阅");
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

    private void borrowBook() {
        Book sel = getSelectedBook();
        if (sel == null) { JOptionPane.showMessageDialog(this, "请先选择一本图书（双击也可借阅）！"); return; }
        if (sel.getRemainingCount() <= 0) {
            JOptionPane.showMessageDialog(this, "《" + sel.getTitle() + "》已全部借出！");
            return;
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("借阅图书："), g);
        g.gridx = 1;
        panel.add(new JLabel(sel.getTitle() + "（" + sel.getId() + "） 剩余 " + sel.getRemainingCount() + " 本"), g);

        g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("借阅者姓名 *："), g);
        g.gridx = 1;
        JTextField nameField = new JTextField(15);
        panel.add(nameField, g);

        g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("借阅数量 *："), g);
        g.gridx = 1;
        JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, sel.getRemainingCount(), 1));
        panel.add(qtySpinner, g);

        g.gridx = 0; g.gridy = 3;
        panel.add(new JLabel("借阅日期："), g);
        g.gridx = 1;
        JTextField dateField = new JTextField(LocalDate.now().format(dateFmt), 12);
        panel.add(dateField, g);

        int opt = JOptionPane.showConfirmDialog(this, panel, "借阅图书",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        int qty = (Integer) qtySpinner.getValue();
        String err = borrowService.borrow(name, sel.getId(), qty, dateField.getText().trim());
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "借阅失败", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "借阅成功！\n" + name + " 借阅《" + sel.getTitle() + "》×" + qty + " 本");
            refreshTable();
        }
    }

    private void returnBook() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        panel.add(new JLabel("借阅者姓名 *："), g);
        g.gridx = 1;
        JTextField nameField = new JTextField(15);
        panel.add(nameField, g);

        g.gridx = 0; g.gridy = 1;
        panel.add(new JLabel("归还图书 ID *："), g);
        g.gridx = 1;
        JTextField bookIdField = new JTextField(12);
        panel.add(bookIdField, g);

        g.gridx = 0; g.gridy = 2;
        panel.add(new JLabel("归还日期："), g);
        g.gridx = 1;
        JTextField dateField = new JTextField(LocalDate.now().format(dateFmt), 12);
        panel.add(dateField, g);

        Book sel = getSelectedBook();
        if (sel != null) bookIdField.setText(sel.getId());

        int opt = JOptionPane.showConfirmDialog(this, panel, "归还图书",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String bookId = bookIdField.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "请输入借阅者姓名！"); return; }
        if (bookId.isEmpty()) { JOptionPane.showMessageDialog(this, "请输入图书 ID！"); return; }

        String err = borrowService.returnBook(name, bookId, dateField.getText().trim());
        if (err != null) {
            JOptionPane.showMessageDialog(this, err, "归还失败", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "归还成功！\n" + name + " 归还图书 " + bookId);
            refreshTable();
        }
    }

    private void showMyRecords() {
        String name = JOptionPane.showInputDialog(this, "请输入您的姓名：", "查询借阅记录",
                JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        List<BorrowRecord> records = borrowService.getRecordsByBorrower(name.trim());
        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(this, "未找到【" + name.trim() + "】的借阅记录。");
            return;
        }

        String[] cols = {"记录ID", "图书ID", "数量", "借阅日期", "归还日期", "状态"};
        String[][] data = new String[records.size()][6];
        for (int i = 0; i < records.size(); i++) {
            BorrowRecord r = records.get(i);
            data[i][0] = r.getRecordId();
            data[i][1] = r.getBookId();
            data[i][2] = String.valueOf(r.getQuantity());
            data[i][3] = r.getBorrowDate();
            data[i][4] = r.isReturned() ? r.getReturnDate() : "-";
            data[i][5] = r.isReturned() ? "已归还" : "未归还";
        }

        JTable recTable = new JTable(data, cols);
        recTable.setAutoCreateRowSorter(true);
        JScrollPane sp = new JScrollPane(recTable);
        sp.setPreferredSize(new Dimension(650, 300));
        JOptionPane.showMessageDialog(this, sp,
                name.trim() + " 的借阅记录（共 " + records.size() + " 条）",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
