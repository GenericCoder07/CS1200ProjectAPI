import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class ServerManager extends JFrame {
    // ---- DB CONFIG ----
    private static final String JDBC_URL = "jdbc:h2:./sqldb/mydb";
    private static final String DB_USER  = "sa";
    private static final String DB_PASS  = "";

    // ---- UI ----
    private final JComboBox<String> tableCombo = new JComboBox<>();
    private final JCheckBox autoRefresh = new JCheckBox("Auto-refresh (1.5s)", true);
    private final JButton refreshBtn = new JButton("Refresh");
    private final JTable table = new JTable(new DefaultTableModel());
    private final JLabel status = new JLabel(" ");

    // User editor
    private final JTextField fUsername   = new JTextField(16);
    private final JTextField fPassword   = new JTextField(16);
    private final JTextField fEmail      = new JTextField(20);
    private final JTextField fSessionId  = new JTextField(24);
    private final JCheckBox  fIsAdmin    = new JCheckBox("is_admin");
    private final JCheckBox  fIsVerified = new JCheckBox("is_verified");
    private final JButton    btnInsert   = new JButton("Insert User");
    private final JButton    btnUpdate   = new JButton("Update User");
    private final JButton    btnClear    = new JButton("Clear Form");

    private Timer timer = null;
    private Connection conn;

    public ServerManager() {
        super("ServerManager — Live DB Viewer (H2) + User Editor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8,8));

        // Top bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8,8));
        top.add(new JLabel("Table:"));
        top.add(tableCombo);
        top.add(refreshBtn);
        top.add(autoRefresh);
        add(top, BorderLayout.NORTH);

        // Center split: table (top) + user editor (bottom)
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.7);

        // Table panel
        JScrollPane tablePane = new JScrollPane(table);
        split.setTopComponent(tablePane);

        // User editor panel
        JPanel editor = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4,6,4,6);
        g.anchor = GridBagConstraints.WEST;

        int r=0;
        addRow(editor, g, r++, "username*", fUsername);
        addRow(editor, g, r++, "password", fPassword);
        addRow(editor, g, r++, "email",    fEmail);
        addRow(editor, g, r++, "session_id", fSessionId);

        g.gridx=1; g.gridy=r; editor.add(fIsAdmin, g);
        g.gridx=2; editor.add(fIsVerified, g); r++;

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6,0));
        btns.add(btnInsert);
        btns.add(btnUpdate);
        btns.add(btnClear);
        g.gridx=1; g.gridy=r; g.gridwidth=2; editor.add(btns, g); r++;

        split.setBottomComponent(editor);
        add(split, BorderLayout.CENTER);

        // Status
        status.setBorder(BorderFactory.createEmptyBorder(4,8,8,8));
        add(status, BorderLayout.SOUTH);

        // Events
        refreshBtn.addActionListener(e -> refreshNow());
        tableCombo.addActionListener(e -> refreshNow());
        autoRefresh.addActionListener(e -> { if (autoRefresh.isSelected()) timer.start(); else timer.stop(); });

        // Table row click -> load into form if table == users
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedRowIntoForm();
        });

        btnInsert.addActionListener(e -> insertUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnClear.addActionListener(e -> clearForm());

        // Timer
        timer = new Timer(1500, e -> refreshNow());

        // DB connect + ensure users table + load tables
        connect();
        ensureUsersTable();
        loadTables();
        selectUsersIfPresent();
        refreshNow();
        timer.start();

        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    private static void addRow(JPanel p, GridBagConstraints g, int y, String label, JComponent field) {
        g.gridx=0; g.gridy=y; p.add(new JLabel(label), g);
        g.gridx=1; g.gridy=y; g.gridwidth=2; g.fill=GridBagConstraints.HORIZONTAL; p.add(field, g);
        g.fill=GridBagConstraints.NONE; g.gridwidth=1;
    }

    private void connect() {
        try {
            conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
            status("Connected: " + JDBC_URL);
        } catch (Exception ex) {
            error("DB connect failed: " + ex.getMessage());
        }
    }

    private void ensureUsersTable() {
        // Create a simple users table with 6 fields; username is the PK
        String ddl = """
            CREATE TABLE IF NOT EXISTS users (
              username   VARCHAR(100) PRIMARY KEY,
              password   VARCHAR(255),
              email      VARCHAR(255),
              session_id VARCHAR(255),
              is_admin   BOOLEAN DEFAULT FALSE,
              is_verified BOOLEAN DEFAULT FALSE
            )
        """;
        try (Statement s = conn.createStatement()) {
            s.executeUpdate(ddl);
        } catch (SQLException ex) {
            error("Create users table failed: " + ex.getMessage());
        }
        // (Optional) if you’re migrating from an older shape, you could add columns defensively here.
    }

    private void loadTables() {
        if (conn == null) return;
        tableCombo.removeAllItems();
        String sql = """
            SELECT TABLE_NAME
            FROM INFORMATION_SCHEMA.TABLES
            WHERE TABLE_SCHEMA = 'PUBLIC'
            ORDER BY TABLE_NAME
        """;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                tableCombo.addItem(rs.getString(1));
                count++;
            }
            status(count + " tables found");
        } catch (SQLException ex) {
            error("Load tables failed: " + ex.getMessage());
        }
    }

    private void selectUsersIfPresent() {
        ComboBoxModel<String> m = tableCombo.getModel();
        for (int i=0;i<m.getSize();i++) {
            if ("USERS".equalsIgnoreCase(m.getElementAt(i))) {
                tableCombo.setSelectedIndex(i);
                return;
            }
        }
        if (m.getSize() > 0) tableCombo.setSelectedIndex(0);
    }

    private void refreshNow() {
        if (conn == null) return;
        String tableName = (String) tableCombo.getSelectedItem();
        if (tableName == null || tableName.isBlank()) return;

        String sql = "SELECT * FROM " + quoteIdent(tableName);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            DefaultTableModel model = buildTableModel(rs);
            table.setModel(model);
            status("Showing: " + tableName + "  (rows: " + model.getRowCount() + ")");
        } catch (SQLException ex) {
            error("Refresh failed: " + ex.getMessage());
        }
    }

    private void loadSelectedRowIntoForm() {
        String t = (String) tableCombo.getSelectedItem();
        if (t == null || !t.equalsIgnoreCase("users")) return;

        int row = table.getSelectedRow();
        if (row < 0) return;

        // Column names may be upper-cased by H2; rely on model indices:
        DefaultTableModel m = (DefaultTableModel) table.getModel();

        int cUser = findCol(m, "username");
        int cPass = findCol(m, "password");
        int cEmail= findCol(m, "email");
        int cSess = findCol(m, "session_id");
        int cAdm  = findCol(m, "is_admin");
        int cVer  = findCol(m, "is_verified");

        fUsername.setText(val(m, row, cUser));
        fPassword.setText(val(m, row, cPass));
        fEmail.setText(val(m, row, cEmail));
        fSessionId.setText(val(m, row, cSess));
        fIsAdmin.setSelected(boolVal(m, row, cAdm));
        fIsVerified.setSelected(boolVal(m, row, cVer));
    }

    private static int findCol(DefaultTableModel m, String name) {
        for (int i=0;i<m.getColumnCount();i++) {
            if (name.equalsIgnoreCase(m.getColumnName(i))) return i;
        }
        return -1;
    }
    private static String val(DefaultTableModel m, int r, int c) {
        if (c<0) return "";
        Object o = m.getValueAt(r, c);
        return o==null ? "" : String.valueOf(o);
    }
    private static boolean boolVal(DefaultTableModel m, int r, int c) {
        if (c<0) return false;
        Object o = m.getValueAt(r, c);
        if (o instanceof Boolean b) return b;
        if (o == null) return false;
        return "true".equalsIgnoreCase(o.toString()) || "1".equals(o.toString());
    }

    private void insertUser() {
        String username = fUsername.getText().trim();
        if (username.isEmpty()) { error("username is required for insert"); return; }

        String sql = "INSERT INTO users (username, password, email, session_id, is_admin, is_verified) " +
                     "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, emptyToNull(fPassword.getText()));
            ps.setString(3, emptyToNull(fEmail.getText()));
            ps.setString(4, emptyToNull(fSessionId.getText()));
            ps.setBoolean(5, fIsAdmin.isSelected());
            ps.setBoolean(6, fIsVerified.isSelected());
            ps.executeUpdate();
            status("Inserted user: " + username);
            refreshNow();
        } catch (SQLException ex) {
            error("Insert failed: " + ex.getMessage());
        }
    }

    private void clearForm() {
        // Clear text fields
        fUsername.setText("");
        fPassword.setText("");
        fEmail.setText("");
        fSessionId.setText("");

        // Reset checkboxes
        fIsAdmin.setSelected(false);
        fIsVerified.setSelected(false);

        // Clear any row selection in the table
        table.clearSelection();

        // Put cursor back on username for quick entry
        fUsername.requestFocusInWindow();

        // Optional: status message
        status("Form cleared");
    }

    
    private void updateUser() {
        String username = fUsername.getText().trim();
        if (username.isEmpty()) { error("username is required for update"); return; }

        String sql = """
            UPDATE users SET
              password=?,
              email=?,
              session_id=?,
              is_admin=?,
              is_verified=?
            WHERE username=?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emptyToNull(fPassword.getText()));
            ps.setString(2, emptyToNull(fEmail.getText()));
            ps.setString(3, emptyToNull(fSessionId.getText()));
            ps.setBoolean(4, fIsAdmin.isSelected());
            ps.setBoolean(5, fIsVerified.isSelected());
            ps.setString(6, username);
            int n = ps.executeUpdate();
            if (n == 0) {
                error("Update affected 0 rows (user not found)");
            } else {
                status("Updated user: " + username);
                refreshNow();
            }
        } catch (SQLException ex) {
            error("Update failed: " + ex.getMessage());
        }
    }

    private static String emptyToNull(String s) {
        String t = s==null ? null : s.trim();
        return (t==null || t.isEmpty()) ? null : t;
    }

    private static DefaultTableModel buildTableModel(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        Vector<String> cols = new Vector<>();
        for (int i = 1; i <= colCount; i++) cols.add(md.getColumnLabel(i));

        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> row = new Vector<>(colCount);
            for (int i = 1; i <= colCount; i++) row.add(rs.getObject(i));
            data.add(row);
        }
        return new DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }

    private void status(String msg) { status.setText(msg); }
    private void error(String msg)  { status.setText("⚠ " + msg); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new ServerManager().setVisible(true);
        });
    }
}
