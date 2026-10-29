package pluralisconseil.sn.pluralisEtatFin.data.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * How one sheet of an Excel model is printed to PDF: the area to print and the orientation, so the
 * sheet fills the page. A sheet the model does not list falls back on {@code LegacyPdfPageRules}.
 */
@Entity
@Table(name = "model_pdf_page", uniqueConstraints = @UniqueConstraint(columnNames = {"model_id", "sheet_name"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "model")
public class ModelPdfPage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_name", nullable = false)
    private String sheetName;

    /** e.g. {@code $A$1:$H$38}; null prints the whole sheet. */
    private String printArea;

    private boolean landscape;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private ModelExcel model;
}
