package org.meveo.service.custom;

import org.apache.commons.collections.CollectionUtils;
import org.meveo.model.customEntities.CustomEntityReference;
import org.meveo.model.customEntities.CustomEntityTemplate;
import org.meveo.service.base.PersistenceService;

import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hien Bach
 */
@Stateless
public class CustomEntityReferenceService extends PersistenceService<CustomEntityReference> {

    /**
     * Get a list of custom entity templates for cache
     *
     * @return A list of custom entity templates
     */
    public List<CustomEntityTemplate> getCETFromReference() {
        List<CustomEntityTemplate> list = new ArrayList<>();
        List<CustomEntityReference> entityReferences = getEntityManager().createNamedQuery("CustomEntityReference.getCER", CustomEntityReference.class).getResultList();
        if (CollectionUtils.isNotEmpty(entityReferences)) {
            for (CustomEntityReference customEntityReference : entityReferences) {
                list.add(customEntityReference.getCustomEntityTemplate());
            }
        }
        return list;
    }
}
