package pluralisconseil.sn.pluralisEtatFin.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetModelPageValues {

    public static List<Map<String, Object>> model1ConfigEntreprise(){
        List<Map<String, Object>> configs = new ArrayList<>();
//        name config
        Map<String, Object> name_config = new HashMap<>();
//        sigle_usel config
        Map<String, Object> sigle_usel = new HashMap<>();
//        contact config
        Map<String, Object> contact = new HashMap<>();
//        mail config
        Map<String, Object> mail = new HashMap<>();
//        adresse config
        Map<String, Object> adresse = new HashMap<>();
//        n_identification_fiscal config
        Map<String, Object> n_identification_fiscal = new HashMap<>();
//        registre_commerce config
        Map<String, Object> registre_commerce = new HashMap<>();
//        repertoire_entreprise config
        Map<String, Object> repertoire_entreprise = new HashMap<>();
//        caisse_social config
        Map<String, Object> caisse_social = new HashMap<>();
//        code_importeur config
        Map<String, Object> code_importeur = new HashMap<>();
//        code_activite_principale config
        Map<String, Object> code_activite_principale = new HashMap<>();

        name_config.put("page", "Page de garde");
        name_config.put("field", "name");
        name_config.put("code", "L35");
        name_config.put("row", 35);
        name_config.put("col", 12);

        sigle_usel.put("page", "Page de garde");
        sigle_usel.put("code", "H41");
        sigle_usel.put("field", "sigle_usel");
        sigle_usel.put("row", 41);
        sigle_usel.put("col", 8);
        adresse.put("page", "Page de garde");
        adresse.put("field", "adresse");
        adresse.put("code", "J46");
        adresse.put("row", 46);
        adresse.put("col", 10);
        n_identification_fiscal.put("page", "Page de garde");
        n_identification_fiscal.put("field", "n_identification_fiscal");
        n_identification_fiscal.put("code", "N51");
        n_identification_fiscal.put("row", 51);
        n_identification_fiscal.put("col", 14);
        registre_commerce.put("page", "Fiche de renseignement R1");
        registre_commerce.put("field", "registre_commerce");
        registre_commerce.put("code", "I24");
        registre_commerce.put("row", 24);
        registre_commerce.put("col", 9);
        repertoire_entreprise.put("page", "Fiche de renseignement R1");
        repertoire_entreprise.put("field", "repertoire_entreprise");
        repertoire_entreprise.put("code", "W24");
        repertoire_entreprise.put("row", 24);
        repertoire_entreprise.put("col", 23);
        caisse_social.put("page", "Fiche de renseignement R1");
        caisse_social.put("field", "caisse_social");
        caisse_social.put("code", "G28");
        caisse_social.put("row", 28);
        caisse_social.put("col", 7);
        code_importeur.put("page", "Fiche de renseignement R1");
        code_importeur.put("field", "code_importeur");
        code_importeur.put("code", "S28");
        code_importeur.put("row", 28);
        code_importeur.put("col", 19);
        code_activite_principale.put("page", "Fiche de renseignement R1");
        code_activite_principale.put("field", "code_activite_principale");
        code_activite_principale.put("code", "AE28");
        code_activite_principale.put("row", 28);
        code_activite_principale.put("col", 31);
        contact.put("page", "Fiche de renseignement R1");
        contact.put("field", "contact");
        contact.put("code", "G36");
        contact.put("row", 36);
        contact.put("col", 7);
        mail.put("page", "Fiche de renseignement R1");
        mail.put("field", "mail");
        mail.put("code", "Q36");
        mail.put("row", 36);
        mail.put("col", 17);


        configs.add(name_config);
        configs.add(sigle_usel);
        configs.add(contact);
        configs.add(mail);
        configs.add(adresse);
        configs.add(n_identification_fiscal);
        configs.add(registre_commerce);
        configs.add(repertoire_entreprise);
        configs.add(caisse_social);
        configs.add(code_importeur);
        configs.add(code_activite_principale);
        return configs;
    }
}
