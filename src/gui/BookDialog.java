package bookmanagement.gui;

import bookmanagement.model.Book;

import javax.swing.*;
import java.awt.*;

public class BookDialog extends JDialog {

    private JTextField titleField;
    private JTextField authorField;
    private JSpinner totalSpinner;
    private JSpinner remainingSpinner;
    private boolean confirmed = false;
    private int oldTotalCount;
    private int minTotalCount;
    private int borrowedCount;

    public BookDialog(Frame owner, Book book) {
        super(owner, book == null ? "添加图书" : "编辑图书", true);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("书名 *："), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        titleField = new JTextField(22);
        formPanel.add(titleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("作者 *："), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        authorField = new JTextField(22);
        formPanel.add(authorField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("馆藏总数："), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        totalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        formPanel.add(totalSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("剩余数量："), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        remainingSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        formPanel.add(remainingSpinner, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton saveBtn = new JButton("保存");
        JButton cancelBtn = new JButton("取消");
        saveBtn.setPreferredSize(new Dimension(90, 32));
        cancelBtn.setPreferredSize(new Dimension(90, 32));
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        saveBtn.addActionListener(e -> {
            if (titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入书名！");
                return;
            }
            if (authorField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入作者！");
                return;
            }
            int total = (Integer) totalSpinner.getValue();
            int remaining = (Integer) remainingSpinner.getValue();
            if (remaining > total) {
                JOptionPane.showMessageDialog(this, "剩余数量不能大于总数！");
                return;
            }
            // 编辑模式下，馆藏总数不能小于已借出数量，否则还书时会库存溢出
            if (book != null) {
                int borrowedCount = book.getTotalCount() - book.getRemainingCount();
                if (total < borrowedCount) {
                    JOptionPane.showMessageDialog(this,
                            "馆藏总数不能小于当前已借出数量（" + borrowedCount + " 本）！\n"
                            + "请等待读者归还后再减少馆藏总数。");
                    return;
                }
                // 剩余数量不能超过馆藏总数减去外借数量
                int maxRemaining = total - borrowedCount;
                if (remaining > maxRemaining) {
                    JOptionPane.showMessageDialog(this,
                            "剩余数量不能超过 " + maxRemaining + " 本！\n"
                            + "当前外借 " + borrowedCount + " 本，馆藏总数 " + total + " 本。");
                    return;
                }
            }
            confirmed = true;
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());

        if (book != null) {
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            totalSpinner.setValue(book.getTotalCount());
            remainingSpinner.setValue(book.getRemainingCount());
            oldTotalCount = book.getTotalCount();
            // 馆藏总数不能小于当前已借出数量，否则还书时会库存溢出
            borrowedCount = book.getTotalCount() - book.getRemainingCount();
            minTotalCount = Math.max(1, borrowedCount);
            ((SpinnerNumberModel) totalSpinner.getModel()).setMinimum(minTotalCount);
            // 剩余数量上限 = 馆藏总数 - 外借数量
            int maxRemaining = book.getTotalCount() - borrowedCount;
            ((SpinnerNumberModel) remainingSpinner.getModel()).setMaximum(maxRemaining);
        } else {
            oldTotalCount = (Integer) totalSpinner.getValue();
            minTotalCount = 1;
            borrowedCount = 0;
        }

        // 编辑模式下，修改总数量时自动按差值联动剩余数量
        totalSpinner.addChangeListener(e -> {
            int newTotal = (Integer) totalSpinner.getValue();
            int delta = newTotal - oldTotalCount;
            if (delta != 0) {
                int maxRemaining = newTotal - borrowedCount;
                int newRemaining = (Integer) remainingSpinner.getValue() + delta;
                newRemaining = Math.max(0, Math.min(maxRemaining, newRemaining));
                // 临时放宽上限以确保 setValue 不报错
                SpinnerNumberModel model = (SpinnerNumberModel) remainingSpinner.getModel();
                model.setMaximum(Math.max(maxRemaining, (Integer) model.getMaximum()));
                remainingSpinner.setValue(newRemaining);
                model.setMaximum(maxRemaining);
                oldTotalCount = newTotal;
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(saveBtn);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isConfirmed() { return confirmed; }
    public String getBookTitle() { return titleField.getText().trim(); }
    public String getBookAuthor() { return authorField.getText().trim(); }
    public int getTotalCount() { return (Integer) totalSpinner.getValue(); }
    public int getRemainingCount() { return (Integer) remainingSpinner.getValue(); }
}
