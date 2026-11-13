import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "cliente")
public class Cliente {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TipoCliente tipo;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos = new ArrayList<>();

    public Cliente(){}

    public Cliente(String codigo, TipoCliente tipo, LocalDate fechaAlta) {
        this.codigo=codigo; this.tipo=tipo; this.fechaAlta=fechaAlta;
    }

    public Long getId(){return id;} public String getCodigo(){
        return codigo;
    }

    public TipoCliente getTipo(){
        return tipo;
    }

    public LocalDate getFechaAlta(){
        return fechaAlta;
    }

    public boolean isActivo(){
        return activo;
    }

    public void setActivo(boolean a){
        activo=a;
    }
    public List<Pedido> getPedidos(){
        return pedidos;
    }
}
