import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/** Misma idea que JdbcDemo, usando entidades y transacciones JPA. */
public class JpaDemo {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("basededatosMySQL");
        //EntityManagerFactory emf = Persistence.createEntityManagerFactory("basededatosPSQL");
        EntityManager em = emf.createEntityManager();
        try {
            // ===== Ejemplo: crear cliente + pedido en una transacción =====
            System.out.println("\n===== EJEMPLO: nuevo cliente con un pedido =====");
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            // codigo es UNIQUE: le añado un número aleatorio para poder ejecutar varias veces
            Cliente cliente = new Cliente("CLI-JPA-" + new Random().nextInt(100000), TipoCliente.PERSONA, LocalDate.now());
            em.persist(cliente);
            em.persist(new Pedido(LocalDate.now(), new BigDecimal("64.90"), cliente));
            tx.commit();

            List<Pedido> pedidos = em.createQuery(
                    "SELECT p FROM Pedido p JOIN FETCH p.cliente ORDER BY p.fecha DESC", Pedido.class)
                    .getResultList();
            pedidos.forEach(p -> System.out.println(p.getId() + " | " + p.getCliente().getCodigo() + " | " + p.getTotal()));

            reto1InsertarPedido(em, 2L, new BigDecimal("75.90"));
            reto2PedidosPrimerCliente(em);
            reto3FiltrarPorFecha(em, LocalDate.of(2026, 10, 5));
            reto4BorrarCliente(em, "CLI-004");
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ex;
        } finally { em.close(); emf.close(); }
    }

    /** Reto 1: insertar un nuevo pedido para un cliente EXISTENTE (lo buscamos por su PK con find). */
    static void reto1InsertarPedido(EntityManager em, long clienteId, BigDecimal total) {
        System.out.println("\n===== RETO 1: nuevo pedido para el cliente con id " + clienteId + " =====");
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        Cliente cliente = em.find(Cliente.class, clienteId);   // SELECT ... WHERE id = ?
        if (cliente == null) {
            System.out.println("No existe el cliente " + clienteId);
            tx.rollback();
            return;
        }
        Pedido pedido = new Pedido(LocalDate.now(), total, cliente);
        em.persist(pedido);                                     // INSERT INTO pedido ...
        tx.commit();
        System.out.println("Pedido creado con id " + pedido.getId() + " para " + cliente.getCodigo()
                + " | " + pedido.getFecha() + " | " + pedido.getTotal());
    }

    /** Reto 2: consultar la lista de pedidos del primer cliente de la lista. */
    static void reto2PedidosPrimerCliente(EntityManager em) {
        System.out.println("\n===== RETO 2: pedidos del primer cliente =====");
        Cliente primero = em.createQuery("SELECT c FROM Cliente c ORDER BY c.id", Cliente.class)
                .setMaxResults(1)
                .getSingleResult();
        // Navegamos por la relación @OneToMany: JPA lanza el SELECT de pedidos por nosotros
        em.refresh(primero);   // recarga desde la BBDD por si la lista en memoria estaba desactualizada
        System.out.println("Cliente " + primero.getId() + " (" + primero.getCodigo() + ") tiene "
                + primero.getPedidos().size() + " pedidos:");
        for (Pedido p : primero.getPedidos()) {
            System.out.println("  " + p.getId() + " | " + p.getFecha() + " | " + p.getTotal());
        }
    }

    /** Reto 3: filtrar pedidos por fecha con un parámetro con nombre (:fecha). */
    static void reto3FiltrarPorFecha(EntityManager em, LocalDate limite) {
        System.out.println("\n===== RETO 3: pedidos con fecha >= " + limite + " =====");
        List<Pedido> pedidos = em.createQuery(
                        "SELECT p FROM Pedido p JOIN FETCH p.cliente WHERE p.fecha >= :fecha ORDER BY p.fecha", Pedido.class)
                .setParameter("fecha", limite)   // igual que ps.setDate(1, ...) en JDBC, pero por nombre
                .getResultList();
        pedidos.forEach(p -> System.out.println(p.getId() + " | " + p.getFecha() + " | "
                + p.getCliente().getCodigo() + " | " + p.getTotal()));
    }

    /**
     * Reto 4: borrar el cliente CLI-004 con remove().
     * La entidad Empresa no está mapeada: su fila se borra sola porque la FK de empresa
     * tiene ON DELETE CASCADE en la BBDD. No hace falta código extra en Java.
     */
    static void reto4BorrarCliente(EntityManager em, String codigo) {
        System.out.println("\n===== RETO 4: remove() del cliente " + codigo + " =====");
        List<Cliente> encontrados = em.createQuery("SELECT c FROM Cliente c WHERE c.codigo = :codigo", Cliente.class)
                .setParameter("codigo", codigo)
                .getResultList();
        if (encontrados.isEmpty()) {
            System.out.println("El cliente " + codigo + " ya no existe (borrado en una ejecución anterior).");
            return;
        }
        Cliente cliente = encontrados.get(0);
        System.out.println("Filas en empresa de " + codigo + " ANTES: " + contarEmpresas(em, cliente.getId()));

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        em.remove(cliente);   // marca la entidad para borrar
        tx.commit();          // aquí se lanza el DELETE FROM cliente WHERE id = ?

        System.out.println("Filas en empresa de " + codigo + " DESPUÉS: " + contarEmpresas(em, cliente.getId())
                + " -> la BBDD las borró en cascada (ON DELETE CASCADE)");
    }

    // Consulta SQL nativa porque Empresa no es una entidad JPA en este proyecto
    private static long contarEmpresas(EntityManager em, long clienteId) {
        Number n = (Number) em.createNativeQuery("SELECT COUNT(*) FROM empresa WHERE cliente_id = ?")
                .setParameter(1, clienteId)
                .getSingleResult();
        return n.longValue();
    }
}
