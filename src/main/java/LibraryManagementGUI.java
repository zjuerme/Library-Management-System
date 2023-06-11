import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import entities.Book;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import entities.Borrow;
import entities.Card;
import queries.ApiResult;
import queries.CardList;
import queries.BookQueryConditions;
import queries.BookQueryResults;
import queries.BorrowHistories;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JCheckBox;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.text.ParseException;

public class LibraryManagementGUI extends JFrame{
    private final LibraryManagementSystem libraryManagementSystem;
    private final JPanel mainPanel;
    private final JTabbedPane tabbedPane;
    private final JPanel booksPanel;
    private final JPanel cardsPanel;
    private final JPanel borrowsPanel;
    private final JTextField[] bookFields;
    private final JTextField[] cardFields;
    private final JTextField[] borrowFields;
    private final JButton[] bookButtons;
    private final JButton[] cardButtons;
    private final JButton[] borrowButtons;
    private final JTable bookTable;
    private final JTable cardTable;
    private final JTable borrowTable;
    private DefaultTableModel cardTableModel;
    private boolean isAdmin;
    private int cardID;
    private JTable borrowsTable = new JTable();
    private JLabel usernameLabel;
    private void refreshGUI() {
        tabbedPane.removeAll();
        initializeBookPanel();
        tabbedPane.addTab("Books", booksPanel);
        if (isAdmin) {
            initializeCardPanel();
            tabbedPane.addTab("Cards", cardsPanel);
        } else {
            initializeBorrowsPanel();
        }
    }
    private void showLoginWindow() {
        JDialog loginDialog = new JDialog(this, "Login", true);
        loginDialog.setLayout(new FlowLayout());
        loginDialog.setSize(300, 100);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setLocationRelativeTo(null);
        JLabel nameLabel = new JLabel("Enter ID:");
        JTextField nameField = new JTextField(15);
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            String enteredName = nameField.getText();
            if (enteredName.equalsIgnoreCase("root")) {
                isAdmin = true;
            } else {
                isAdmin = false;
                try {
                    cardID = Integer.parseInt(enteredName);
                    ApiResult result = libraryManagementSystem.showCards();
                    if (result.ok) {
                        CardList cardList = (CardList) result.payload;
                        boolean cardExists = false;
                        for (Card card : cardList.getCards()) {
                            if (card.getCardId() == cardID) {
                                cardExists = true;
                                break;
                            }
                        }
                        if (!cardExists) {
                            JOptionPane.showMessageDialog(loginDialog, "Card ID not found. Please Re-enter your ID.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } else {
                        JOptionPane.showMessageDialog(loginDialog, "Error fetching cards: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(loginDialog, "Invalid input. Please enter a valid card ID.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            loginDialog.dispose();
            refreshGUI(); // refresh the GUI after the user has logged in
        });
        loginDialog.add(nameLabel);
        loginDialog.add(nameField);
        loginDialog.add(okButton);
        loginDialog.setVisible(true);
    }
    public LibraryManagementGUI(LibraryManagementSystem libraryManagementSystem) {
        this.libraryManagementSystem = libraryManagementSystem;
        this.usernameLabel = new JLabel();
        this.mainPanel = new JPanel(new BorderLayout());
        this.tabbedPane = new JTabbedPane();
        this.booksPanel = new JPanel(new BorderLayout());
        this.cardsPanel = new JPanel(new BorderLayout());
        this.borrowsPanel = new JPanel(new BorderLayout());
        this.bookFields = new JTextField[7];
        this.cardFields = new JTextField[3];
        this.borrowFields = new JTextField[3];
        this.bookButtons = new JButton[6];
        this.cardButtons = new JButton[4];
        this.borrowButtons = new JButton[3];
        this.bookTable = new JTable();
        this.cardTable = new JTable();
        this.borrowTable = new JTable();
        showLoginWindow();
        createAndShowGUI();
    }
    public class ReturnButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int cardId;
        private int bookId;
        private LibraryManagementSystem libraryManagementSystem;
        public ReturnButtonEditor(JCheckBox checkBox, LibraryManagementSystem libraryManagementSystem) {
            super(checkBox);
            this.libraryManagementSystem = libraryManagementSystem;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(UIManager.getColor("Button.background"));
            }
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            cardId = (int) table.getValueAt(row, 0);
            bookId = (int) table.getValueAt(row, 1);
            return button;
        }
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                long returnTime = System.currentTimeMillis();
                Borrow borrow = new Borrow();
                borrow.setCardId(cardId);
                borrow.setBookId(bookId);
                borrow.setReturnTime(returnTime);
                ApiResult result = libraryManagementSystem.returnBook(borrow);
                if (result.ok) {
                    System.out.println("Book returned successfully");
                    refreshBorrowsTable();
                } else {
                    System.out.println("Error returning book: " + result.message);
                }
            }
            isPushed = false;
            return label;
        }
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }
    private void initializeBorrowsPanel() {
        JPanel borrowsPanel = new JPanel(new BorderLayout());
        DefaultTableModel borrowsTableModel = new DefaultTableModel();
        String[] columnNames = {"Card ID", "Book ID", "Category", "Title", "Press", "Publish Year", "Author", "Price", "Borrow Time", "Return Time"};
        borrowsTableModel.setColumnIdentifiers(columnNames);
        borrowsTable = new JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 9; // Only make the "Return Time" column editable
            }
        };
        borrowsTable.setModel(borrowsTableModel);
        // Add the custom cell editor to the "Return Time" column
        TableColumn returnTimeColumn = borrowsTable.getColumnModel().getColumn(9);
        returnTimeColumn.setCellEditor(new ReturnButtonEditor(new JCheckBox(), libraryManagementSystem));
        // Add custom cell renderer for the "Return Time" column
        returnTimeColumn.setCellRenderer(new DefaultTableCellRenderer() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    long borrowTime = dateFormat.parse((String) table.getValueAt(row, 8)).getTime(); // Assuming "Borrow Time" is at column 8
                    long returnTime = dateFormat.parse((String) value).getTime();

                    if (returnTime < borrowTime) {
                        c.setBackground(Color.RED);
                        setText("Return");
                    } else {
                        c.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return c;
            }
        });
        borrowsPanel.add(new JScrollPane(borrowsTable), BorderLayout.CENTER);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshBorrowsTable();
            }
        });
        borrowsPanel.add(refreshButton, BorderLayout.SOUTH);
        tabbedPane.addTab("Borrows", borrowsPanel);
    }
    private void refreshBorrowsTable() {
        DefaultTableModel borrowsTableModel = (DefaultTableModel) borrowsTable.getModel();
        borrowsTableModel.setRowCount(0);
        ApiResult result = libraryManagementSystem.showBorrowHistory(cardID);
        if (result.ok) {
            BorrowHistories borrowHistories = (BorrowHistories) result.payload;
            List<BorrowHistories.Item> items = borrowHistories.getItems();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (BorrowHistories.Item item : items) {
                Object[] row = new Object[]{
                        item.getCardId(),
                        item.getBookId(),
                        item.getCategory(),
                        item.getTitle(),
                        item.getPress(),
                        item.getPublishYear(),
                        item.getAuthor(),
                        item.getPrice(),
                        dateFormat.format(item.getBorrowTime()),
                        dateFormat.format(item.getReturnTime())
                };
                borrowsTableModel.addRow(row);
            }
        } else {
            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error loading borrows: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.out.println("Refreshing borrows table");
    }
    private void refreshCardTable() {
        ApiResult result = libraryManagementSystem.showCards();
        if (result.ok) {
            CardList cardList = (CardList) result.payload;
            List<Card> cards = cardList.getCards();
            cardTableModel.setRowCount(0);
            for (Card card : cards) {
                cardTableModel.addRow(new Object[]{
                        card.getCardId(),
                        card.getName(),
                        card.getDepartment(),
                        card.getType().toString()
                });
            }
        } else {
            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error loading cards: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private void initializeCardPanel() {
        JPanel cardPanel = new JPanel(new BorderLayout());
        cardTableModel = new DefaultTableModel(new Object[]{"ID", "Name", "Department", "Type"}, 0);
        JTable cardTable = new JTable(cardTableModel);
        JScrollPane cardTableScrollPane = new JScrollPane(cardTable);
        cardPanel.add(cardTableScrollPane, BorderLayout.CENTER);
        refreshCardTable();
        cardTable.setModel(cardTableModel);
        // Add a button for adding a new card
        JButton addCardButton = new JButton("Add Card");
        addCardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Implement adding a new card and call refreshCardTable()
                JPanel addCardPanel = new JPanel(new GridLayout(0, 1));
                JTextField nameField = new JTextField(20);
                JTextField departmentField = new JTextField(20);
                JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"S", "T"});
                addCardPanel.add(new JLabel("Name:"));
                addCardPanel.add(nameField);
                addCardPanel.add(new JLabel("Department:"));
                addCardPanel.add(departmentField);
                addCardPanel.add(new JLabel("Type:"));
                addCardPanel.add(typeComboBox);
                int result = JOptionPane.showConfirmDialog(null, addCardPanel, "Add Card", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String name = nameField.getText();
                    String department = departmentField.getText();
                    Card.CardType type = Card.CardType.values(((String) typeComboBox.getSelectedItem()).toUpperCase());
                    if (type == null) {
                        JOptionPane.showMessageDialog(null, "Error adding card: Invalid card type.", "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        Card newCard = new Card(0, name, department, type);
                        ApiResult apiResult = libraryManagementSystem.registerCard(newCard);
                        if (apiResult.ok) {
                            JOptionPane.showMessageDialog(null, "Card added successfully.");
                            refreshCardTable();
                        } else {
                            JOptionPane.showMessageDialog(null, "Error adding card: " + apiResult.message, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
        cardPanel.add(addCardButton, BorderLayout.NORTH);
        // Add a button for deleting a card
        JButton deleteCardButton = new JButton("Delete Card");
        deleteCardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = cardTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int cardId = (int) cardTable.getValueAt(selectedRow, 0);
                    ApiResult apiResult = libraryManagementSystem.removeCard(cardId);
                    if (apiResult.ok) {
                        JOptionPane.showMessageDialog(null, "Card deleted successfully.");
                        refreshCardTable();
                    } else {
                        JOptionPane.showMessageDialog(null, "Error deleting card: " + apiResult.message, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a card to delete.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        cardPanel.add(deleteCardButton, BorderLayout.SOUTH);
        cardsPanel.add(cardPanel, BorderLayout.CENTER);
    }
    private void initializeBookPanel() {
        JPanel bookPanel = new JPanel(new BorderLayout());
        DefaultTableModel bookTableModel = new DefaultTableModel();
        String[] columnNames = isAdmin ? new String[]{"ID", "Category", "Title", "Press", "Publish Year", "Author", "Price", "Stock", "Delete"}
                : new String[]{"ID", "Category", "Title", "Press", "Publish Year", "Author", "Price", "Stock", "Borrow"};
        bookTableModel.setColumnIdentifiers(columnNames);
        bookTable.setModel(bookTableModel);
        bookTable.getColumnModel().getColumn(7).setCellEditor(new StockCellEditor(new JTextField()));
        bookTable.getColumnModel().getColumn(8).setCellRenderer(new ButtonRenderer());
        bookTable.getColumnModel().getColumn(8).setCellEditor(new ButtonEditor(new JCheckBox(), bookTable, isAdmin));
        bookPanel.add(new JScrollPane(bookTable), BorderLayout.CENTER);
        if(isAdmin) {
            JPanel storeBookPanel = createStoreBookPanel();
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bookPanel, storeBookPanel);
            splitPane.setDividerLocation(0.5);
            booksPanel.add(splitPane, BorderLayout.CENTER);
        }
        else{
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            JLabel helloLabel = new JLabel("             hello!   nice to meeet you      ");
            JLabel welcomeLabel = new JLabel("              welcome to this library       ");
            JLabel noticeLabel = new JLabel("       You can borrow books by clicking √       ");
            rightPanel.add(Box.createVerticalStrut(100));
            rightPanel.add(helloLabel);
            rightPanel.add(Box.createVerticalStrut(10)); // add some spacing between the labels
            rightPanel.add(welcomeLabel);
            rightPanel.add(Box.createVerticalStrut(10)); // add some spacing between the labels
            rightPanel.add(noticeLabel);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, bookPanel, rightPanel);
            splitPane.setDividerLocation(0.7);
            booksPanel.add(splitPane, BorderLayout.CENTER);
        }
        refreshBookTable();
        addBookTableListener();
        bookTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = bookTable.getSelectedRow();
                    int bookId = (int) bookTable.getValueAt(row, 0);
                    if (isAdmin) {
                        int result = JOptionPane.showConfirmDialog(LibraryManagementGUI.this, "Are you sure you want to delete the selected book?", "Delete Book", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            ApiResult apiResult = libraryManagementSystem.removeBook(bookId);
                            if (apiResult.ok) {
                                JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Book deleted successfully.");
                                refreshBookTable();
                            } else {
                                JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error deleting book: " + apiResult.message, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } else {
                        // Borrow book functionality
                        int cardId = cardID;
                        Borrow borrow = new Borrow(bookId, cardId);
                        borrow.resetBorrowTime();
                        ApiResult apiResult = libraryManagementSystem.borrowBook(borrow);
                        if (apiResult.ok) {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Book borrowed successfully.");
                            refreshBookTable();
                        } else {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error borrowing book: " + apiResult.message, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
    }
    private JPanel createStoreBookPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        //book's title:
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Title: "), c);
        JTextField titleField = new JTextField(20);
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(titleField, c);
        //book's category
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Category: "), c);
        JTextField categoryField = new JTextField(20);
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(categoryField, c);
        //book's press
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Press: "), c);
        JTextField pressField = new JTextField(20);
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(pressField, c);
        //book's publishYear
        c.gridx = 0;
        c.gridy = 3;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("PublishYear: "), c);
        JTextField publishYearField = new JTextField(20);
        c.gridx = 1;
        c.gridy = 3;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(publishYearField, c);
        //book's author
        c.gridx = 0;
        c.gridy = 4;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Author: "), c);
        JTextField authorField = new JTextField(20);
        c.gridx = 1;
        c.gridy = 4;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(authorField, c);
        //book's price
        c.gridx = 0;
        c.gridy = 5;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Price: "), c);
        JTextField priceField = new JTextField(20);
        c.gridx = 1;
        c.gridy = 5;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(priceField, c);
        //book's stock
        c.gridx = 0;
        c.gridy = 6;
        c.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Stock: "), c);
        JTextField stockField = new JTextField(20);
        c.gridx = 1;
        c.gridy = 6;
        c.anchor = GridBagConstraints.LINE_START;
        panel.add(stockField, c);
            JButton storeBookButton = new JButton("Store Book");
            storeBookButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (isAdmin) {
                        int publishYear = Integer.parseInt(publishYearField.getText());
                        double price = Double.parseDouble(priceField.getText());
                        int stock = Integer.parseInt(stockField.getText());
                        String author = authorField.getText();
                        String title = titleField.getText();
                        String category = categoryField.getText();
                        String press = pressField.getText();
                        Book book = new Book(category, title, press, publishYear, author, price, stock);
                        ApiResult result = libraryManagementSystem.storeBook(book);
                        if (result.ok) {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Book stored successfully.");
                            refreshBookTable();
                        } else {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error storing book: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(LibraryManagementGUI.this, "You don't have permission to store a book.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            c.gridx = 1;
            c.gridy = 8;
            c.anchor = GridBagConstraints.LINE_START;
            panel.add(storeBookButton, c);
        return panel;
    }
    private void refreshBookTable() {
        DefaultTableModel bookTableModel = (DefaultTableModel) bookTable.getModel();
        bookTableModel.setRowCount(0);
        BookQueryConditions emptyConditions = new BookQueryConditions();
        ApiResult result = libraryManagementSystem.queryBook(emptyConditions);
        if (result.ok) {
            BookQueryResults bookQueryResults = (BookQueryResults) result.payload;
            List<Book> books = bookQueryResults.getResults();
            for (Book book : books) {
                Object[] row = new Object[]{
                        book.getBookId(),
                        book.getCategory(),
                        book.getTitle(),
                        book.getPress(),
                        book.getPublishYear(),
                        book.getAuthor(),
                        book.getPrice(),
                        book.getStock(),
                        isAdmin ? "x" : "√"
                };
                bookTableModel.addRow(row);
            }
        } else {
            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error loading books: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
        }
        System.out.println("Refreshing book table");
    }
    private class StockCellEditor extends DefaultCellEditor {
        private int oldStockValue = -1;
        public StockCellEditor(final JTextField textField) {
            super(textField);
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    oldStockValue = Integer.parseInt(textField.getText());
                }
            });
        }
        public int getOldStockValue() {
            return oldStockValue;
        }
    }
    private void addBookTableListener() {
        bookTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (isAdmin){
                    if (e.getType() == TableModelEvent.UPDATE) {
                        int row = e.getFirstRow();
                        int column = e.getColumn();
                        // Ignore the ID column and the Delete colunmn
                        if (column == 0 || column == 8 ) {
                            return;
                        }
                        int bookId = (int) bookTable.getValueAt(row, 0);
                        String category = (String) bookTable.getValueAt(row, 1);
                        String title = (String) bookTable.getValueAt(row, 2);
                        String press = (String) bookTable.getValueAt(row, 3);
                        int publishYear = (int) bookTable.getValueAt(row, 4);
                        String author = (String) bookTable.getValueAt(row, 5);
                        double price = Double.parseDouble(bookTable.getValueAt(row, 6).toString());
                        int newStock;
                        try {
                            newStock = Integer.parseInt(bookTable.getValueAt(row, 7).toString());
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Invalid stock value. Please enter a valid integer.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        if (column == 7) {
                            int oldStock = ((StockCellEditor) bookTable.getColumnModel().getColumn(7).getCellEditor()).getOldStockValue();
                            int deltaStock = newStock - oldStock;
                            ApiResult result = libraryManagementSystem.incBookStock(bookId, deltaStock);

                            if (result.ok) {
                                JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Book stock updated successfully.");
                            } else {
                                JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error updating book stock: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            Book modifiedBook = new Book(category, title, press, publishYear, author, price, newStock);
                            modifiedBook.setBookId(bookId);
                            ApiResult result = libraryManagementSystem.modifyBookInfo(modifiedBook);

                            if (result.ok) {
                                JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Book information updated successfully.");
                            } else {
                                JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error updating book information: " + result.message, "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                } else {
                    if (e.getType() == TableModelEvent.UPDATE){
                        int column = e.getColumn();
                        if (column != 8 ) {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "You don't have permission to modify book information.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
    }
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private JTable table;
        private boolean isAdmin;
        public ButtonEditor(JCheckBox checkBox, JTable table, boolean isAdmin) {
            super(checkBox);
            this.table = table;
            this.isAdmin = isAdmin;
            button = new JButton();
            button.setOpaque(true);
        }
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            if (isSelected) {
                button.setForeground(table.getSelectionForeground());
                button.setBackground(table.getSelectionBackground());
            } else {
                button.setForeground(table.getForeground());
                button.setBackground(table.getBackground());
            }
            if (value == null || "".equals(value)) {
                button.setText("");
            } else {
                button.setText(value.toString());
            }
            if (isAdmin) {
                button.setIcon(null);
            } else {
                button.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
            }
            int bookId = (int) table.getValueAt(row, 0);
            for (ActionListener al : button.getActionListeners()) {
                button.removeActionListener(al);
            }
            // Add the appropriate action listener based on isAdmin
            if (isAdmin) {
                button.addActionListener(e -> {
                    fireEditingStopped();
                    int result = JOptionPane.showConfirmDialog(LibraryManagementGUI.this, "Are you sure you want to delete the selected book?", "Delete Book", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        ApiResult apiResult = libraryManagementSystem.removeBook(bookId);
                        if (apiResult.ok) {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Book deleted successfully.");
                            refreshBookTable();
                        } else {
                            JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error deleting book: " + apiResult.message, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            } else {
                button.addActionListener(e -> {
                    fireEditingStopped();
                    int cardId = cardID;
                    Borrow borrow = new Borrow(bookId, cardId);
                    borrow.resetBorrowTime();
                    ApiResult apiResult = libraryManagementSystem.borrowBook(borrow);
                    if (apiResult.ok) {
                        JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Book borrowed successfully.");
                        refreshBookTable();
                    } else {
                        JOptionPane.showMessageDialog(LibraryManagementGUI.this, "Error borrowing book: " + apiResult.message, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            return button;
        }

        public Object getCellEditorValue() {
            return label;
        }
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
    private void createAndShowGUI() {
        ImageIcon icon = new ImageIcon("icon.png");
        JFrame frame = new JFrame("Zhou Wei's Library Management System");
        frame.setIconImage(icon.getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 600);
        refreshGUI();
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        // Add a JPanel at the bottom of the main panel to hold the small characters
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.LIGHT_GRAY);
        JLabel label = new JLabel("Developed by 3210103790 with heart");
        bottomPanel.add(label);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        // Add a JLabel at the top of the main panel to display "user mode"
        JLabel userModeLabel = new JLabel("Develop Mode");
        if(isAdmin) {
            userModeLabel.setText("Admin Mode");
        } else {
            userModeLabel.setText("User Mode");
        }
        userModeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userModeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        mainPanel.add(userModeLabel, BorderLayout.NORTH);
        frame.setContentPane(mainPanel);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (libraryManagementSystem.releaseConnection()) {
                    System.out.println("Success to release connection.");
                } else {
                    System.out.println("Failed to release connection.");
                }
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}