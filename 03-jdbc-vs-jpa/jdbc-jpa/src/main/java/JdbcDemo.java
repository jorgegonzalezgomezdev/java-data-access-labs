import java.math.BigDecimal;
import java.sql.*;

/** Ejemplos JDBC completos sobre el modelo cliente/persona/empresa/pedido. */
public class JdbcDemo {
    private static final String URL = "jdbc:mysql://localhost:3307/basededatos"
            + "?serverTimezone=Europe/Madrid&useSSL=false&allowPublicKeyRetrieval=true";
    //private static final String URL = "jdbc:postgresql://localhost:5432/basededatos";
    private static final String USER = "bbdd";
    private static final String PASSWORD = "bbddadmin";

    public static void main(String[] args) throws SQLException {
        try (Connection cn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT id, codigo, tipo, fecha_alta, activo FROM cliente ORDER BY id";
            try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%d | %s | %s | %s | activo=%s%n",
                            rs.getLong("id"), rs.getString("codigo"), rs.getString("tipo"),
                            rs.getDate("fecha_alta"), rs.getBoolean("activo"));
                }
            }
            actualizarEstado(cn, 3L, true);
            insertarPedido(cn, Date.valueOf("2026-10-27"), new BigDecimal("75.90"), 1L);
            transaccionClienteYPedido(cn);
        }
    }

    static void actualizarEstado(Connection cn, long id, boolean activo) throws SQLException {
        String sql = "UPDATE cliente SET activo = ? WHERE id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setLong(2, id);
            System.out.println("Clientes actualizados: " + ps.executeUpdate());
        }
    }

    static void insertarPedido(Connection cn, Date fecha, BigDecimal total, long clienteId) throws SQLException {
        String sql = "INSERT INTO pedido(fecha, total, cliente_id) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, fecha); ps.setBigDecimal(2, total); ps.setLong(3, clienteId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) System.out.println("Pedido creado con id " + keys.getLong(1));
            }
        }
    }

    static void transaccionClienteYPedido(Connection cn) throws SQLException {
        String clienteSql = "INSERT INTO cliente(codigo, tipo, fecha_alta, activo) VALUES (?, 'PERSONA', ?, true)";
        String personaSql = "INSERT INTO persona(cliente_id, nombre, apellidos, dni, email, telefono) VALUES (?, ?, ?, ?, ?, ?)";
        String pedidoSql = "INSERT INTO pedido(fecha, total, cliente_id) VALUES (?, ?, ?)";
        boolean anterior = cn.getAutoCommit();
        try {
            cn.setAutoCommit(false);
            long clienteId;
            try (PreparedStatement ps = cn.prepareStatement(clienteSql, Statement.RETURN_GENERATED_KEYS)) {
                // codigo es VARCHAR(20): "CLI-T-" (6) + milisegundos (13) = 19 caracteres
                ps.setString(1, "CLI-T-" + System.currentTimeMillis());
                ps.setDate(2, Date.valueOf("2026-10-27"));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); clienteId = keys.getLong(1); }
            }
            try (PreparedStatement ps = cn.prepareStatement(personaSql)) {
                ps.setLong(1, clienteId); ps.setString(2, "Cliente"); ps.setString(3, "Transaccional");
                ps.setString(4, "99999999Z"); ps.setString(5, "transaccion@example.com"); ps.setString(6, "600000000");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = cn.prepareStatement(pedidoSql)) {
                ps.setDate(1, Date.valueOf("2026-10-27")); ps.setBigDecimal(2, new BigDecimal("99.90")); ps.setLong(3, clienteId);
                ps.executeUpdate();
            }
            cn.commit();
            System.out.println("Transacción confirmada: cliente, persona y pedido creados.");
        } catch (SQLException ex) {
            cn.rollback();
            System.out.println("Transacción cancelada: " + ex.getMessage());
        } finally {
            cn.setAutoCommit(anterior);
        }
    }
}
