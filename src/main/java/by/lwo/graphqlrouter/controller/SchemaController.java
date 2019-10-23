package by.lwo.graphqlrouter.controller;

import by.lwo.graphqlrouter.utility.GraphUtility;
import by.lwo.graphqlrouter.utility.RestMediaType;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
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

    @PostMapping(value = "/schema", produces = RestMediaType.APLICATION_JSON_VALUE_UTF8)
    public Object test() {
        List<String> urls = Arrays.asList(urlArray);
        TypeDefinitionRegistry t = GraphUtility.getMergedSchema(urls, restTemplate);
        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring().build();
        SchemaGenerator generator = new SchemaGenerator();
        GraphQLSchema schema = generator.makeExecutableSchema(t, runtimeWiring);
        GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        ExecutionResult execute = graphQL.execute(IntrospectionQuery.INTROSPECTION_QUERY);
        return execute.getData();
    }

    @PostMapping("/query")
    public String getQuery(@RequestBody(required = false) String body) {
        int routerCalue;
        String route = request.getHeader(HEADER_NAME);
        try {
            routerCalue = Integer.parseInt(route);
        } catch (NumberFormatException e) {
            routerCalue = -1;
        }
        ResponseEntity<String> response = ResponseEntity.badRequest().build();
        if (Optional.ofNullable(body).isPresent() && routerCalue >= 0 && routerCalue < urlArray.length) {
            URI url = URI.create(urlArray[routerCalue]);
            RequestEntity<String> request = RequestEntity.post(url).body(body);
            response = restTemplate
                    .exchange(request, String.class);
        }
        return response.getBody();
    }
}
