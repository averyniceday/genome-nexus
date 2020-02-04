package org.cbioportal.genome_nexus.service.cached;

import com.mongodb.DBObject;
import org.cbioportal.genome_nexus.model.VariantAnnotation;
import org.cbioportal.genome_nexus.persistence.VariantAnnotationRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cbioportal.genome_nexus.persistence.GenericMongoRepository;
import org.cbioportal.genome_nexus.service.CachedExternalResourceFetcher;
import org.cbioportal.genome_nexus.service.ExternalResourceFetcher;
import org.cbioportal.genome_nexus.service.ResourceTransformer;
import org.cbioportal.genome_nexus.service.exception.ResourceMappingException;
import org.cbioportal.genome_nexus.util.NaturalOrderComparator;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class BaseCachedVariantAnnotationFetcher
    extends BaseCachedExternalResourceFetcher<VariantAnnotation, VariantAnnotationRepository>
{
    private static final Log LOG = LogFactory.getLog(BaseCachedVariantAnnotationFetcher.class);

    protected String collection;
    protected VariantAnnotationRepository repository;
    protected Class<VariantAnnotation> type;
    protected ExternalResourceFetcher<VariantAnnotation> fetcher;
    protected ResourceTransformer<VariantAnnotation> transformer;
    protected Integer maxPageSize;

    public BaseCachedVariantAnnotationFetcher(String collection,
                                             VariantAnnotationRepository repository,
                                             Class<VariantAnnotation> type,
                                             ExternalResourceFetcher<VariantAnnotation> fetcher,
                                             ResourceTransformer<VariantAnnotation> transformer)
    {
        super(collection, repository, type, fetcher, transformer, Integer.MAX_VALUE);
    }

    public BaseCachedVariantAnnotationFetcher(String collection,
                                             VariantAnnotationRepository repository,
                                             Class<VariantAnnotation> type,
                                             ExternalResourceFetcher<VariantAnnotation> fetcher,
                                             ResourceTransformer<VariantAnnotation> transformer,
                                             Integer maxPageSize)
    {
        super(collection, repository, type, fetcher, transformer, maxPageSize);
    }
   
    @Override 
    public VariantAnnotation fetchAndCache(String id) throws ResourceMappingException
    {
        System.out.println("HERE I AM BOINGO");
        VariantAnnotation variantAnnotation = super.fetchAndCache(id);
        if (variantAnnotation == null) {
            System.out.println("HERE I AM DOINGO");
            variantAnnotation = new VariantAnnotation(id);
        } else {
            variantAnnotation.setSuccessfullyAnnotated(true);
        }
        return variantAnnotation;
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
/*
    protected void fetchAndCache(Set<String> needToFetch,
                                 Map<String, T> idToInstance,
                                 boolean saveValues) throws ResourceMappingException, HttpClientErrorException
    {
        // send up to maxPageSize entities per request
        for (Set<String> subSet: this.generateChunks(needToFetch))
        {
            DBObject rawValue = null;

            try {
                // get the raw annotation string from the web service
                rawValue = this.fetcher.fetchRawValue(this.buildRequestBody(subSet));
            } catch (HttpClientErrorException e) {
                LOG.error("HTTP ERROR " + e.getStatusCode() + " for " + subSet.toString() + ": " + e.getResponseBodyAsString());
            }

            if (rawValue != null) {
                try {
                    // fetch instances to return:
                    // this does not contain all the information obtained from the web service
                    // only the fields mapped to the VariantAnnotation model will be returned
                    List<T> fetched = this.transformer.transform(rawValue, this.type);
                    fetched.forEach(t -> idToInstance.put(this.extractId(t), t));

                    // save everything to the cache as a properly parsed JSON
                    if (saveValues) {
                        this.saveToDb(rawValue);
                    }
                } catch (DataIntegrityViolationException e) {
                    // in case of data integrity violation exception, do not bloat the logs
                    // this is thrown when the annotationJSON can't be stored by mongo
                    // due to the variant annotation key being too large to index
                    LOG.info(e.getLocalizedMessage());
                }
            }
        }
    }
*/
}
