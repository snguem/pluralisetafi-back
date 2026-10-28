package pluralisconseil.sn.pluralisEtatFin.data.entities;

public interface ActivableEntity {
    void setActive(Boolean active);
    Boolean getActive();
    Entreprise getEntreprise();
}
