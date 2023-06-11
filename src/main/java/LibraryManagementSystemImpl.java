import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {
    private final DatabaseConnector connector;
    public boolean releaseConnection() {
        return connector.release();
    }
    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }
    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        String sql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            stmt.setDouble(6, book.getPrice());
            stmt.setInt(7, book.getStock());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Storing book failed, no rows affected.");
            }
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                book.setBookId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Storing book failed, no book_id obtained.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        String sql = "UPDATE book SET stock = stock + ? WHERE book_id = ? AND stock + ? >= 0";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, deltaStock);
            stmt.setInt(2, bookId);
            stmt.setInt(3, deltaStock);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Increasing book stock failed, no rows affected.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        String sql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (Book book : books) {
                stmt.setString(1, book.getCategory());
                stmt.setString(2, book.getTitle());
                stmt.setString(3, book.getPress());
                stmt.setInt(4, book.getPublishYear());
                stmt.setString(5, book.getAuthor());
                stmt.setDouble(6, book.getPrice());
                stmt.setInt(7, book.getStock());
                stmt.addBatch();
            }
            stmt.executeBatch();
            // Retrieve the generated keys and set the book IDs
            ResultSet rs = stmt.getGeneratedKeys();
            int i = 0;
            while (rs.next()) {
                //System.out.println("Before: " + books.get(i).toString());
                books.get(i++).setBookId(rs.getInt(1));
                //System.out.println("After: " + books.get(i - 1).toString());
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        String checkLentSql = "SELECT 1 FROM borrow WHERE book_id = ? AND return_time = 0";
        String deleteBookSql = "DELETE FROM book WHERE book_id = ?";
        try {
            PreparedStatement checkLentStmt = conn.prepareStatement(checkLentSql);
            checkLentStmt.setInt(1, bookId);
            ResultSet resultSet = checkLentStmt.executeQuery();
            if (resultSet.next()) {
                return new ApiResult(false, "Book has been lent and cannot be removed.");
            }
            PreparedStatement deleteBookStmt = conn.prepareStatement(deleteBookSql);
            deleteBookStmt.setInt(1, bookId);
            int affectedRows = deleteBookStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Removing book failed, no rows affected.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        String sql = "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? WHERE book_id = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, book.getCategory());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getPress());
            stmt.setInt(4, book.getPublishYear());
            stmt.setString(5, book.getAuthor());
            stmt.setDouble(6, book.getPrice());
            stmt.setInt(7, book.getBookId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Modifying book info failed, no rows affected.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        String sql = "SELECT * FROM book WHERE 1=1 ";
        StringBuilder queryBuilder = new StringBuilder(sql);
        List<Object> queryParams = new ArrayList<>();
        if (conditions.getCategory() != null) {
            queryBuilder.append(" AND category = ?");
            queryParams.add(conditions.getCategory());
        }
        if (conditions.getTitle() != null) {
            queryBuilder.append(" AND title LIKE ?");
            queryParams.add("%" + conditions.getTitle() + "%");
        }
        if (conditions.getPress() != null) {
            queryBuilder.append(" AND press LIKE ?");
            queryParams.add("%" + conditions.getPress() + "%");
        }
        if (conditions.getMinPublishYear() != null) {
            queryBuilder.append(" AND publish_year >= ?");
            queryParams.add(conditions.getMinPublishYear());
        }
        if (conditions.getMaxPublishYear() != null) {
            queryBuilder.append(" AND publish_year <= ?");
            queryParams.add(conditions.getMaxPublishYear());
        }
        if (conditions.getAuthor() != null) {
            queryBuilder.append(" AND author LIKE ?");
            queryParams.add("%" + conditions.getAuthor() + "%");
        }
        if (conditions.getMinPrice() != null) {
            queryBuilder.append(" AND price >= ?");
            queryParams.add(conditions.getMinPrice());
        }
        if (conditions.getMaxPrice() != null) {
            queryBuilder.append(" AND price <= ?");
            queryParams.add(conditions.getMaxPrice());
        }
        if (conditions.getSortBy() != null && conditions.getSortOrder() != null) {
            queryBuilder.append(" ORDER BY ").append(conditions.getSortBy().name().toLowerCase())
                    .append(" ").append(conditions.getSortOrder().name()).append(", ");
        }
        queryBuilder.append("book_id ASC");
        try {
            PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString());
            for (int i = 0; i < queryParams.size(); i++) {
                stmt.setObject(i + 1, queryParams.get(i));
            }
            ResultSet resultSet = stmt.executeQuery();
            List<Book> books = new ArrayList<>();
            while (resultSet.next()) {
                Book book = new Book();
                book.setBookId(resultSet.getInt("book_id"));
                book.setCategory(resultSet.getString("category"));
                book.setTitle(resultSet.getString("title"));
                book.setPress(resultSet.getString("press"));
                book.setPublishYear(resultSet.getInt("publish_year"));
                book.setAuthor(resultSet.getString("author"));
                book.setPrice(resultSet.getDouble("price"));
                book.setStock(resultSet.getInt("stock"));
                books.add(book);
            }
            BookQueryResults bookQueryResults = new BookQueryResults(books);
            return new ApiResult(true, bookQueryResults);
        } catch (Exception e) {
            return new ApiResult(false, e.getMessage());
        }
    }
    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        // Check if the user has already borrowed the book but hasn't returned it
        String checkBorrowedSql = "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0";
        String insertBorrowSql = "INSERT INTO borrow (card_id, book_id, borrow_time) VALUES (?, ?, ?)";
        String updateBookStockSql = "UPDATE book SET stock = stock - 1 WHERE book_id = ? AND stock > 0";
        try {
            // Check if the user has already borrowed the book but hasn't returned it
            PreparedStatement checkBorrowedStmt = conn.prepareStatement(checkBorrowedSql);
            checkBorrowedStmt.setInt(1, borrow.getCardId());
            checkBorrowedStmt.setInt(2, borrow.getBookId());
            ResultSet resultSet = checkBorrowedStmt.executeQuery();
            if (resultSet.next()) {
                return new ApiResult(false, "User has already borrowed the book and hasn't returned it yet.");
            }
            PreparedStatement insertBorrowStmt = conn.prepareStatement(insertBorrowSql);
            insertBorrowStmt.setInt(1, borrow.getCardId());
            insertBorrowStmt.setInt(2, borrow.getBookId());
            insertBorrowStmt.setLong(3, borrow.getBorrowTime());
            int affectedRows1 = insertBorrowStmt.executeUpdate();
            if (affectedRows1 == 0) {
                rollback(conn);
                return new ApiResult(false, "Borrowing book failed, no rows affected.");
            }
            PreparedStatement updateBookStockStmt = conn.prepareStatement(updateBookStockSql);
            updateBookStockStmt.setInt(1, borrow.getBookId());
            int affectedRows2 = updateBookStockStmt.executeUpdate();
            if (affectedRows2 == 0) {
                rollback(conn);
                return new ApiResult(false, "Borrowing book failed, book stock not updated.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        String checkBorrowedSql = "SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0";
        String updateReturnTimeSql = "UPDATE borrow SET return_time = ? WHERE card_id = ? AND book_id = ? AND return_time = 0";
        String updateBookStockSql = "UPDATE book SET stock = stock + 1 WHERE book_id = ?";
        try {
            PreparedStatement checkBorrowedStmt = conn.prepareStatement(checkBorrowedSql);
            checkBorrowedStmt.setInt(1, borrow.getCardId());
            checkBorrowedStmt.setInt(2, borrow.getBookId());
            ResultSet resultSet = checkBorrowedStmt.executeQuery();
            if (!resultSet.next()) {
                return new ApiResult(false, "User has not borrowed this book or has already returned it.");
            }
            long borrowTime = resultSet.getLong("borrow_time");
            long returnTime = borrow.getReturnTime();
            // Check if the return time is valid
            if (returnTime <= borrowTime) {
                return new ApiResult(false, "Invalid return time. Return time must be greater than borrow time.");
            }
            PreparedStatement updateReturnTimeStmt = conn.prepareStatement(updateReturnTimeSql);
            updateReturnTimeStmt.setLong(1, returnTime);
            updateReturnTimeStmt.setInt(2, borrow.getCardId());
            updateReturnTimeStmt.setInt(3, borrow.getBookId());
            int affectedRows1 = updateReturnTimeStmt.executeUpdate();
            if (affectedRows1 == 0) {
                rollback(conn);
                return new ApiResult(false, "Returning book failed, no rows affected.");
            }
            PreparedStatement updateBookStockStmt = conn.prepareStatement(updateBookStockSql);
            updateBookStockStmt.setInt(1, borrow.getBookId());
            int affectedRows2 = updateBookStockStmt.executeUpdate();
            if (affectedRows2 == 0) {
                rollback(conn);
                return new ApiResult(false, "Returning book failed, book stock not updated.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        String listBorrowHistoriesSql = "SELECT b.*, bk.category, bk.title, bk.press, bk.publish_year, bk.author, bk.price FROM borrow b INNER JOIN book bk ON b.book_id = bk.book_id WHERE b.card_id = ? ORDER BY b.borrow_time DESC, b.book_id ASC";
        try {
            PreparedStatement listBorrowHistoriesStmt = conn.prepareStatement(listBorrowHistoriesSql);
            listBorrowHistoriesStmt.setInt(1, cardId);
            ResultSet resultSet = listBorrowHistoriesStmt.executeQuery();

            List<BorrowHistories.Item> borrowHistoryItems = new ArrayList<>();
            while (resultSet.next()) {
                Borrow borrow = new Borrow();
                borrow.setCardId(resultSet.getInt("card_id"));
                borrow.setBookId(resultSet.getInt("book_id"));
                borrow.setBorrowTime(resultSet.getLong("borrow_time"));
                borrow.setReturnTime(resultSet.getLong("return_time"));
                Book book = new Book();
                book.setBookId(resultSet.getInt("book_id"));
                book.setCategory(resultSet.getString("category"));
                book.setTitle(resultSet.getString("title"));
                book.setPress(resultSet.getString("press"));
                book.setPublishYear(resultSet.getInt("publish_year"));
                book.setAuthor(resultSet.getString("author"));
                book.setPrice(resultSet.getDouble("price"));
                BorrowHistories.Item item = new BorrowHistories.Item(cardId, book, borrow);
                borrowHistoryItems.add(item);
            }
            BorrowHistories borrowHistories = new BorrowHistories(borrowHistoryItems);
            return new ApiResult(true, borrowHistories);
        } catch (Exception e) {
            return new ApiResult(false, e.getMessage());
        }
    }
    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        String checkCardSql = "SELECT * FROM card WHERE name = ? AND department = ? AND type = ?";
        try {
            PreparedStatement checkStmt = conn.prepareStatement(checkCardSql);
            checkStmt.setString(1, card.getName());
            checkStmt.setString(2, card.getDepartment());
            checkStmt.setString(3, card.getType().getStr());
            ResultSet checkResult = checkStmt.executeQuery();
            if (checkResult.next()) {
                return new ApiResult(false, "Card already exists.");
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        String sql = "INSERT INTO card (name, department, type) VALUES (?, ?, ?)";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, card.getName());
            stmt.setString(2, card.getDepartment());
            stmt.setString(3, card.getType().getStr());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Registering card failed, no rows affected.");
            }
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                card.setCardId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Registering card failed, no card_id obtained.");
            }
            //System.out.println("card id: " + card.getCardId());
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        // Check if there are any unreturned books for the card
        String checkUnreturnedBooksSql = "SELECT book_id FROM borrow WHERE card_id = ? AND return_time = 0";
        try {
            PreparedStatement checkUnreturnedBooksStmt = conn.prepareStatement(checkUnreturnedBooksSql);
            checkUnreturnedBooksStmt.setInt(1, cardId);
            ResultSet checkUnreturnedBooksResult = checkUnreturnedBooksStmt.executeQuery();
            if (checkUnreturnedBooksResult.next()) {
                return new ApiResult(false, "Cannot remove card, unreturned books exist.");
            }
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        // Delete the card
        String sql = "DELETE FROM card WHERE card_id = ?";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cardId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                rollback(conn);
                return new ApiResult(false, "Removing card failed, no rows affected.");
            }
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        //System.out.println("delete: " + cardId);
        return new ApiResult(true, null);
    }
    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        String sql = "SELECT * FROM card ORDER BY card_id ASC";
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet resultSet = stmt.executeQuery();
            List<Card> cards = new ArrayList<>();
            while (resultSet.next()) {
                Card card = new Card();
                card.setCardId(resultSet.getInt("card_id"));
                card.setName(resultSet.getString("name"));
                card.setDepartment(resultSet.getString("department"));
                card.setType(Card.CardType.values(resultSet.getString("type")));
                cards.add(card);
            }
            return new ApiResult(true, new CardList(cards));
        } catch (Exception e) {
            return new ApiResult(false, e.getMessage());
        }
    }
    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }
    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
