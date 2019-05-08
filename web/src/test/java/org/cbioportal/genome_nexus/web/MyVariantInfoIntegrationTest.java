package org.cbioportal.genome_nexus.web;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = {
        "spring.data.mongodb.uri=mongodb://localhost/integration",
        "server.port=38892"
    }
)
public class MyVariantInfoIntegrationTest
{
    private final static String BASE_URL = "http://localhost:38892/my_variant_info/variant/";

    private RestTemplate restTemplate = new RestTemplate();

    private Map<String, HashMap> fetchMyVariantInfoAnnotationGET(String variant)
    {
        String response = this.restTemplate.getForObject(BASE_URL + variant, String.class);
        // parse response string
        JsonParser springParser = JsonParserFactory.getJsonParser();
        // cast to Map<String, HashMap>
        Map<String, HashMap> map = (Map<String, HashMap>)(Map) springParser.parseMap(response);
        return map;

    }
    private List<Map<String, HashMap>> fetchMyVariantInfoAnnotationPOST(String[] variants)
    {
        String responses = this.restTemplate.postForObject(BASE_URL, variants, String.class);
        // parse response string
        JsonParser springParser = JsonParserFactory.getJsonParser();
        // cast to List<Map<String, HashMap>>
        List<Map<String, HashMap>> lists = (List<Map<String, HashMap>>)(List<?>) springParser.parseList(responses);
        return lists;
    }


    @Test
    public void testMyVariantInfoAnnotation()
    {
        String[] variants = {
            "7:g.140453136A>T",
            "rs12190874",
            "INVALID"
        };

        //////////////////
        // GET requests //
        //////////////////

        String chrom = this.fetchMyVariantInfoAnnotationGET(variants[0]).get("mutdb").get("chrom").toString();
        // the chrom shout be 7
        assertEquals("7", chrom);

        Object alleleNumber = ((HashMap) this.fetchMyVariantInfoAnnotationGET(variants[0]).get("gnomadExome").get("alleleNumber")).get("an");
        // the allele number should be 246028
        assertEquals(246028, alleleNumber);

        String alt = this.fetchMyVariantInfoAnnotationGET(variants[1]).get("vcf").get("alt").toString();
        // the alt should be A
        assertEquals("A", alt);

        Object alleleCount = ((HashMap) this.fetchMyVariantInfoAnnotationGET(variants[1]).get("gnomadGenome").get("alleleCount")).get("ac");
        // the allele count should be 3239
        assertEquals(3239, alleleCount);


        //////////////////
        // POST request //
        //////////////////

        List<Map<String, HashMap>> postResponses = this.fetchMyVariantInfoAnnotationPOST(variants);

        // for each pdbId we should have one matching PdbHeader instance, except the invalid one
        assertEquals(postResponses.size(), variants.length - 1);

        String chrom0 = postResponses.get(0).get("mutdb").get("chrom").toString();
        // the Get and Post should have same result
        assertEquals(chrom, chrom0);

        Object alleleNumber0 = ((HashMap) postResponses.get(0).get("gnomadExome").get("alleleNumber")).get("an");
        assertEquals(alleleNumber, alleleNumber0);

        String alt1 = postResponses.get(1).get("vcf").get("alt").toString();
        assertEquals(alt, alt1);

        Object alleleCount1 = ((HashMap) postResponses.get(1).get("gnomadGenome").get("alleleCount")).get("ac");
        assertEquals(alleleCount, alleleCount1);

    }
}
