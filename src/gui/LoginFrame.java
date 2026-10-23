package bookmanagement.gui;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private static final String ADMIN_PASSWORD = "123456";

    public LoginFrame() {
        setTitle("图书管理系统 —— 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 340);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JLabel titleLabel = new JLabel("欢迎使用图书管理系统", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(15));

        // 管理员入口
        JPanel adminPanel = new JPanel();
        adminPanel.setLayout(new BoxLayout(adminPanel, BoxLayout.Y_AXIS));
        adminPanel.setBorder(BorderFactory.createTitledBorder("管理员入口"));
        adminPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminPanel.setMaximumSize(new Dimension(420, 120));

        JPanel pwdRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        pwdRow.add(new JLabel("密码："));
        JPasswordField pwdField = new JPasswordField(18);
        pwdRow.add(pwdField);
        adminPanel.add(pwdRow);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        JButton adminBtn = new JButton("管理员登录");
        adminBtn.setPreferredSize(new Dimension(200, 36));
        adminBtn.setFont(adminBtn.getFont().deriveFont(Font.BOLD, 13f));
        btnRow.add(adminBtn);
        adminPanel.add(btnRow);

        mainPanel.add(adminPanel);
        mainPanel.add(Box.createVerticalStrut(12));

        // 借阅者入口
        JPanel borrowerPanel = new JPanel();
        borrowerPanel.setLayout(new BoxLayout(borrowerPanel, BoxLayout.Y_AXIS));
        borrowerPanel.setBorder(BorderFactory.createTitledBorder("借阅者入口"));
        borrowerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        borrowerPanel.setMaximumSize(new Dimension(420, 65));

        JPanel brRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        JButton borrowerBtn = new JButton("进入借阅界面");
        borrowerBtn.setPreferredSize(new Dimension(200, 36));
        borrowerBtn.setFont(borrowerBtn.getFont().deriveFont(Font.BOLD, 13f));
        brRow.add(borrowerBtn);
        borrowerPanel.add(brRow);

        mainPanel.add(borrowerPanel);
        setContentPane(mainPanel);

        adminBtn.addActionListener(e -> {
            if (ADMIN_PASSWORD.equals(new String(pwdField.getPassword()))) {
                dispose();
                new AdminFrame().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "密码错误！");
                pwdField.setText("");
                pwdField.requestFocus();
            }
        });
        pwdField.addActionListener(e -> adminBtn.doClick());
        borrowerBtn.addActionListener(e -> {
            dispose();
            new BorrowerFrame().setVisible(true);
        });

        getRootPane().setDefaultButton(adminBtn);
    }
}
