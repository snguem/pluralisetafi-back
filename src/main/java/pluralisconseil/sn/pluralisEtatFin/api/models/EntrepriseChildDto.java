package pluralisconseil.sn.pluralisEtatFin.api.models;

/** DTO of a record owned by an {@code Entreprise} (dirigeant, banque, ...). */
public interface EntrepriseChildDto {
    Long getEntrepriseId();
    void setEntrepriseId(Long entrepriseId);
}
