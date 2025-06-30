// SplitExpensesApp.java
// Improved Java Swing-based Split Expenses GUI Application


import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.PriorityQueue;
import javax.swing.border.TitledBorder;


class User {
    String name;
    public User(String name) { this.name = name; }
    public String toString() { return name; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return name.equals(((User) o).name);
    }
    @Override public int hashCode() { return name.hashCode(); }
}

class ExpenseManager {
    ArrayList<User> users = new ArrayList<>();
    Map<User, Map<User, Double>> balances = new HashMap<>();
    List<String> transactionHistory = new ArrayList<>();

    public void addUser(String name) {
        User user = new User(name);
        users.add(user);
        balances.put(user, new HashMap<>());
    }

    public ArrayList<User> getUsers() { return users; }

    public void addExpense(User paidBy, double amount, List<User> sharedWith, boolean isEqual, List<Double> customShares) {
        if (isEqual) {
            double share = amount / sharedWith.size();
            for (User user : sharedWith) {
                if (!user.equals(paidBy)) {
                    balances.get(user).put(paidBy, balances.get(user).getOrDefault(paidBy, 0.0) + share);
                    transactionHistory.add(user.name + " owes " + paidBy.name + " ₹" + String.format("%.2f", share));
                }
            }
        } else {
            double total = 0;
            for (double val : customShares) total += val;
            if (Math.abs(total - amount) > 0.01) {
                JOptionPane.showMessageDialog(null, "Error: Custom shares must sum up to the total amount.");
                return;
            }
            for (int i = 0; i < sharedWith.size(); i++) {
                User user = sharedWith.get(i);
                if (!user.equals(paidBy)) {
                    double share = customShares.get(i);
                    balances.get(user).put(paidBy, balances.get(user).getOrDefault(paidBy, 0.0) + share);
                    transactionHistory.add(user.name + " owes " + paidBy.name + " ₹" + String.format("%.2f", share));
                }
            }
        }
    }

    public List<String> getBalances() {
        List<String> output = new ArrayList<>();
        for (User u1 : balances.keySet()) {
            for (User u2 : balances.get(u1).keySet()) {
                double amt = balances.get(u1).get(u2);
                if (amt > 0.0) {
                    output.add(u1.name + " owes " + u2.name + " ₹" + String.format("%.2f", amt));
                }
            }
        }
        return output;
    }

    public List<String> getMinimizedBalances() {
        Map<User, Double> netBalance = new HashMap<>();
        for (User u : users) netBalance.put(u, 0.0);

        for (User u1 : balances.keySet()) {
            for (User u2 : balances.get(u1).keySet()) {
                double amt = balances.get(u1).get(u2);
                netBalance.put(u1, netBalance.get(u1) - amt);
                netBalance.put(u2, netBalance.get(u2) + amt);
            }
        }

        PriorityQueue<Map.Entry<User, Double>> credit = new PriorityQueue<>((a, b) -> Double.compare(b.getValue(), a.getValue()));
        PriorityQueue<Map.Entry<User, Double>> debit = new PriorityQueue<>((a, b) -> Double.compare(a.getValue(), b.getValue()));

        for (Map.Entry<User, Double> entry : netBalance.entrySet()) {
            double val = entry.getValue();
            if (val > 0) credit.add(entry);
            else if (val < 0) debit.add(entry);
        }

        List<String> settlements = new ArrayList<>();
        while (!credit.isEmpty() && !debit.isEmpty()) {
            Map.Entry<User, Double> cr = credit.poll();
            Map.Entry<User, Double> dr = debit.poll();

            double min = Math.min(cr.getValue(), -dr.getValue());
            settlements.add(dr.getKey().name + " pays " + cr.getKey().name + " ₹" + String.format("%.2f", min));

            double crRemaining = cr.getValue() - min;
            double drRemaining = dr.getValue() + min;

            if (crRemaining > 0) credit.add(new AbstractMap.SimpleEntry<>(cr.getKey(), crRemaining));
            if (drRemaining < 0) debit.add(new AbstractMap.SimpleEntry<>(dr.getKey(), drRemaining));
        }

        return settlements;
    }

    public List<String> getAllTransactions() {
        return new ArrayList<>(transactionHistory);
    }
}

public class SplitExpense {
    private JFrame frame;
    private ExpenseManager manager = new ExpenseManager();
    private JComboBox<User> payerDropdown = new JComboBox<>();
    private JPanel checkPanel = new JPanel(new GridLayout(5, 2, 5, 5));
    private JCheckBox[] userCheckboxes = new JCheckBox[10];

    public SplitExpense() {
        UIManager.put("control", new Color(250, 250, 255));
        UIManager.put("info", new Color(240, 240, 255));
        UIManager.put("nimbusBase", new Color(176, 224, 230));
        UIManager.put("nimbusBlueGrey", new Color(216, 191, 216));
        UIManager.put("nimbusLightBackground", new Color(255, 250, 240));
        UIManager.put("text", Color.DARK_GRAY);

        frame = new JFrame("Split Expenses App");
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabs.add("Add Users", userPanel());
        tabs.add("Add Expense", expensePanel());
        tabs.add("View Balances", balancePanel());

        frame.add(tabs);
        frame.setVisible(true);
    }

    private JPanel userPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel inputPanel = new JPanel(new FlowLayout());
        JTextField nameField = new JTextField(15);
        JButton addButton = new JButton("Add User");

        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(listModel);
        userList.setFont(new Font("Monospaced", Font.PLAIN, 13));

        addButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                manager.addUser(name);
                listModel.addElement(name);
                nameField.setText("");
                refreshUserDropdownAndCheckboxes();
            }
        });

        inputPanel.add(new JLabel("Enter name:"));
        inputPanel.add(nameField);
        inputPanel.add(addButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(userList), BorderLayout.CENTER);
        return panel;
    }

    private JPanel expensePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField amountField = new JTextField(10);
        checkPanel.setBorder(new TitledBorder("Split Between"));

        JRadioButton equalSplit = new JRadioButton("Equal Split", true);
        JRadioButton manualSplit = new JRadioButton("Manual Split");
        ButtonGroup splitGroup = new ButtonGroup();
        splitGroup.add(equalSplit);
        splitGroup.add(manualSplit);

        JTextField customShares = new JTextField("(comma-separated: 20,30,...)");
        customShares.setToolTipText("Enter share values if not equal");

        JButton addExpenseBtn = new JButton("Add Expense");
        addExpenseBtn.addActionListener(e -> {
            User payer = (User) payerDropdown.getSelectedItem();
            double amount = Double.parseDouble(amountField.getText());
            List<User> selectedUsers = new ArrayList<>();
            List<Double> shares = new ArrayList<>();

            for (int i = 0; i < userCheckboxes.length; i++) {
                if (userCheckboxes[i] != null && userCheckboxes[i].isSelected()) {
                    selectedUsers.add(manager.getUsers().get(i));
                }
            }

            boolean isEqual = equalSplit.isSelected();
            if (!isEqual) {
                double total = 0;
                for (String s : customShares.getText().split(",")) {
                    double val = Double.parseDouble(s.trim());
                    shares.add(val);
                    total += val;
                }
                if (Math.abs(total - amount) > 0.01) {
                    JOptionPane.showMessageDialog(frame, "Error: Custom shares must sum up to the total amount.");
                    return;
                }
            }

            manager.addExpense(payer, amount, selectedUsers, isEqual, shares);
            JOptionPane.showMessageDialog(frame, "Expense added successfully!");
        });

        panel.add(new JLabel("Payer:"));
        panel.add(payerDropdown);
        panel.add(new JLabel("Amount:"));
        panel.add(amountField);
        panel.add(checkPanel);
        panel.add(equalSplit);
        panel.add(manualSplit);
        panel.add(customShares);
        panel.add(addExpenseBtn);
        refreshUserDropdownAndCheckboxes();
        return panel;
    }

    private void refreshUserDropdownAndCheckboxes() {
        payerDropdown.removeAllItems();
        checkPanel.removeAll();
        List<User> users = manager.getUsers();

        for (int i = 0; i < users.size(); i++) {
            payerDropdown.addItem(users.get(i));
            userCheckboxes[i] = new JCheckBox(users.get(i).name);
            checkPanel.add(userCheckboxes[i]);
        }
        checkPanel.revalidate();
        checkPanel.repaint();
    }

    private JPanel balancePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea balanceArea = new JTextArea();
        balanceArea.setEditable(false);
        balanceArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JButton rawBtn = new JButton("Show Raw Balances");
        JButton optBtn = new JButton("Show Minimized Transactions");
        JButton allBtn = new JButton("Show All Transactions");

        rawBtn.addActionListener(e -> {
            balanceArea.setText("");
            for (String b : manager.getBalances()) balanceArea.append(b + "\n");
        });

        optBtn.addActionListener(e -> {
            balanceArea.setText("");
            for (String b : manager.getMinimizedBalances()) balanceArea.append(b + "\n");
        });

        allBtn.addActionListener(e -> {
            balanceArea.setText("");
            for (String b : manager.getAllTransactions()) balanceArea.append(b + "\n");
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(rawBtn);
        btnPanel.add(optBtn);
        btnPanel.add(allBtn);
        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(balanceArea), BorderLayout.CENTER);
        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SplitExpense::new);
    }
}
