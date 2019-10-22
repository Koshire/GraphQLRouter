package by.lwo.graphqlrouter.utility;

import graphql.introspection.IntrospectionResultToSchema;
import graphql.language.Document;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.experimental.UtilityClass;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class GraphUtility {

    private final String QUERY = "Query";
    private final String MUTATION = "Mutation";
    private final String DATA = "data";

    @SuppressWarnings("unchecked")
    private static TypeDefinitionRegistry getTypeDefinitionRegistry(String url, RestTemplate restTemplate) {
        URI uri = URI.create(url);
        TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
        RequestEntity<String> requestEntity = RequestEntity.post(uri).body(GraphQlServiceQuery.INTROSPECTION_QUERY);
        ResponseEntity<Map<String, Object>> exchange = restTemplate
                .exchange(requestEntity, new ParameterizedTypeReference<Map<String, Object>>() {
                });
        if (exchange.getStatusCode().is2xxSuccessful() && exchange.hasBody()) {
            try {
                Map<String, Object> node = (Map) exchange.getBody().get(DATA);
                SchemaParser parser = new SchemaParser();
                IntrospectionResultToSchema resultToSchema = new IntrospectionResultToSchema();
                Document schemaDefinition = resultToSchema.createSchemaDefinition(node);
                typeDefinitionRegistry = parser.buildRegistry(schemaDefinition);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        return typeDefinitionRegistry;
    }

    private static List<TypeDefinitionRegistry> getListTypeDefRegistry(List<String> urls, RestTemplate restTemplate) {
        List<TypeDefinitionRegistry> list = new ArrayList<>();
        urls.forEach(s -> list.add(getTypeDefinitionRegistry(s, restTemplate)));
        return list;
    }

    @SuppressWarnings("unchecked")
    public static TypeDefinitionRegistry getMergedSchema(List<String> urls, RestTemplate restTemplate) {

        List<TypeDefinitionRegistry> list = getListTypeDefRegistry(urls, restTemplate);

        Map<String, List<FieldDefinition>> map = new HashMap<>();
        map.put(QUERY, new ArrayList<>());
        map.put(MUTATION, new ArrayList<>());

        TypeDefinitionRegistry typeDef = new TypeDefinitionRegistry();

        list.forEach(typeDefinitionRegistry -> {
            map.get(QUERY).addAll(typeDefinitionRegistry.types().get(QUERY).getChildren());
            map.get(MUTATION).addAll(typeDefinitionRegistry.types().get(MUTATION).getChildren());

            typeDefinitionRegistry.remove(typeDefinitionRegistry.types().get(QUERY));
            typeDefinitionRegistry.remove(typeDefinitionRegistry.types().get(MUTATION));

            typeDef.merge(typeDefinitionRegistry);
        });

        ObjectTypeDefinition typeQuery = new ObjectTypeDefinition(QUERY);
        ObjectTypeDefinition typeMutation = new ObjectTypeDefinition(MUTATION);

        typeQuery = typeQuery.transform(builder -> builder.fieldDefinitions(map.get(QUERY)));
        typeMutation = typeMutation.transform(builder -> builder.fieldDefinitions(map.get(MUTATION)));

        typeDef.add(typeQuery);
        typeDef.add(typeMutation);

        return typeDef;
    }
}
