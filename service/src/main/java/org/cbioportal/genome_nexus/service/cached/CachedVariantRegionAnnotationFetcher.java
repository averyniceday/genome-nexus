
package org.cbioportal.genome_nexus.service.cached;

import com.mongodb.DBObject;
import org.cbioportal.genome_nexus.model.VariantAnnotation;
import org.cbioportal.genome_nexus.persistence.VariantAnnotationRepository;
import org.cbioportal.genome_nexus.persistence.internal.VariantAnnotationRepositoryImpl;
import org.cbioportal.genome_nexus.service.exception.ResourceMappingException;
import org.cbioportal.genome_nexus.service.transformer.ExternalResourceTransformer;
import org.cbioportal.genome_nexus.service.remote.VEPIdDataFetcher;
import org.cbioportal.genome_nexus.service.remote.VEPRegionDataFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CachedVariantRegionAnnotationFetcher extends BaseCachedExternalResourceFetcher<VariantAnnotation, VariantAnnotationRepository>
{
    @Autowired
    public CachedVariantRegionAnnotationFetcher(ExternalResourceTransformer<VariantAnnotation> transformer,
                                                VariantAnnotationRepository repository,
                                                VEPRegionDataFetcher fetcher,
                                                @Value("${vep.max_page_size:200}") Integer maxPageSize)
    {
        super(VariantAnnotationRepositoryImpl.COLLECTION,
            repository,
            VariantAnnotation.class,
            fetcher,
            transformer,
            maxPageSize);
    }

    @Override
    protected Boolean isValidId(String id) 
    {
        // e.g.
        // 17:36002278-36002277:1/A
        //  1:206811015-206811016:1/-
        return (
            !id.contains("N") && !id.contains("undefined") && !id.contains("g.") &&
            id.contains("-") &&
            // should have two :
            (id.length() - id.replace(":","").length() == 2) &&
            // should have one /
            (id.length() - id.replace("/","").length() == 1) &&
            // should be (digit|X|Y|MT):digit-digit:1/character(s)
            (id.split(":")[0].matches("\\d+") || id.split(":")[0].contains("X") || id.split(":")[0].contains("Y") || id.split(":")[0].contains("MT")) &&
            id.split(":")[1].split("-")[0].matches("\\d+") &&
            id.split(":")[1].split("-")[1].matches("\\d+") &&
            id.split(":")[2].startsWith("1/")
        );
    }

    @Override
    protected String extractId(VariantAnnotation instance)
    {
        return instance.getVariantId();
    }

    @Override
    protected String extractId(DBObject dbObject)
    {
        return (String)dbObject.get("input");
    }

    @Override
    public List<VariantAnnotation> fetchAndCache(List<String> ids) throws ResourceMappingException
    {
        Map<String, VariantAnnotation> variantResponse = super.testFunction(ids); 
        System.out.println("operating in variant region fetch and cache override");
        for (String variantId : variantResponse.keySet()) {
            System.out.println(variantId);
            if (variantResponse.get(variantId) == null) {
                // TODO:
                // need a way to set ID to match the format of all other variants passed in
                // also there's some weird thing with setting hgvs based on id needing g.* in front or something like that
                // in the VariantAnnotation model
                VariantAnnotation fakeVariant = new VariantAnnotation(variantId);
                variantResponse.put(variantId, fakeVariant);
            } else {
                variantResponse.get(variantId).setSuccessfullyAnnotated(true);
            }
        }
        // some util function to return response (values) with duplicates
        // need to pass in a list representing indexes of original request
        List<VariantAnnotation> values = new ArrayList();
        for (String id : ids) {
           values.add(variantResponse.get(id));
        } 
        //List<VariantAnnotation> values = new ArrayList(variantResponse.values());
        //values.removeIf(Objects::isNull);
        //List<VariantAnnotation> annotations = super.fetchAndCache(ids);
        return values;
    }
}
