package service;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.data.neo4j.rest.SpringCypherRestGraphDatabase;

import service.data.domain.entity.User;

@SpringBootApplication
@EnableNeo4jRepositories
@EnableZuulProxy
@EnableHystrix
public class Application extends Neo4jConfiguration {

    // Used to bootstrap the Neo4j database with demo data
    @Value("${aws.s3.url}")
    String datasetUrl;

    @Value("${neo4j.uri}")
    private String url;

    @Value("${neo4j.username}")
    private String username;

    @Value("${neo4j.password}")
    private String password;
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    
    public Application() {
        setBasePackage("service");
    }

    @Autowired(required = true)
    @Override
    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        super.setGraphDatabaseService(graphDatabaseService);
        
    }
    
    @Bean(destroyMethod = "shutdown")
    public GraphDatabaseService graphDatabaseService() {
       // return new GraphDatabaseFactory().newEmbeddedDatabase("user.db");
    	setGraphDatabaseService(new SpringCypherRestGraphDatabase(url, username, password));
    	return getGraphDatabaseService();
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);

        RepositoryRestConfiguration restConfiguration = ctx.getBean("config", RepositoryRestConfiguration.class);
        restConfiguration.exposeIdsFor(User.class);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return strings -> {
            // Import graph data for users
            String userImport = String.format("LOAD CSV WITH HEADERS FROM \"%s/users.csv\" AS csvLine\n" +
                    "MERGE (user:User:_User { id: csvLine.id, age: toInt(csvLine.age), gender: csvLine.gender, occupation: csvLine.occupation, zipcode: csvLine.zipcode })", datasetUrl);

            neo4jTemplate().query(userImport, null).finish();
        };
    }

    @Bean
    public ResourceProcessor<Resource<User>> movieProcessor() {
        return new ResourceProcessor<Resource<User>>() {
            @Override
            public Resource<User> process(Resource<User> resource) {

                resource.add(new Link("/movie/movies", "movies"));
                return resource;
            }
        };
    }
}
