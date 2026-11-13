import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad Pedido: cada atributo cuenta la misma historia que la tabla pedido.
 *   id BIGINT AUTO_INCREMENT PK | fecha DATE NOT NULL | total DECIMAL(10,2) NOT NULL | cliente_id BIGINT NOT NULL FK
 */
@Entity @Table(name = "pedido")
public class Pedido {

    // PK autoincremental: la genera MySQL (AUTO_INCREMENT) -> IDENTITY
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    // DECIMAL(10,2): 10 dígitos en total, 2 decimales
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    // Relación N:1 -> muchos pedidos son de un cliente.
    // Pedido es la PARTE PROPIETARIA: su tabla tiene la FK (cliente_id), por eso lleva @JoinColumn.
    // En Cliente, la lista de pedidos es la parte inversa (mappedBy = "cliente").
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    // JPA exige un constructor vacío
    public Pedido() {}

    public Pedido(LocalDate fecha, BigDecimal total, Cliente cliente) {
        this.fecha = fecha;
        this.total = total;
        this.cliente = cliente;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
}
