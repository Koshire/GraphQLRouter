package by.lwo.graphqlrouter.controller;

import by.lwo.graphqlrouter.utility.GraphUtility;
import by.lwo.graphqlrouter.utility.interfaces.RestMediaType;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SchemaController {

    @Value("${string.urls.graphqls}")
    private String[] urlArray;

    @Value("${string.router.header}")
    private String HEADER_NAME;

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    private Object getSchema() {
        List<String> urls = Arrays.asList(urlArray);
        TypeDefinitionRegistry mergedSchema = GraphUtility.getMergedSchema(urls, restTemplate);
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        SchemaGenerator generator = new SchemaGenerator();
        GraphQLSchema schema = generator.makeExecutableSchema(mergedSchema, runtimeWiring);
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionResult execute = graphQL.execute(IntrospectionQuery.INTROSPECTION_QUERY);
        return execute.getData();
    }

    private Object getQuery (String body) {
        int routerValue;
        String route = request.getHeader(HEADER_NAME);
        try {
            routerValue = Integer.parseInt(route);
        } catch (NumberFormatException e) {
            routerValue = -1;
        }
        ResponseEntity<String> response = ResponseEntity.badRequest().build();
        if (Optional.ofNullable(body).isPresent() && routerValue >= 0 && routerValue < urlArray.length) {
            URI url = URI.create(urlArray[routerValue]);
            RequestEntity<String> request = RequestEntity.post(url).body(body);
            response = restTemplate
                    .exchange(request, String.class);
        }
        return response.getBody();
    }

    @PostMapping(value = "/query", produces = RestMediaType.APLICATION_JSON_VALUE_UTF8)
    public Object getQuery(@RequestBody(required = false) String body,
                           @RequestParam(value = "schema", required = false) boolean schema) {
        return schema ? getSchema(): getQuery(body);
    }
}
