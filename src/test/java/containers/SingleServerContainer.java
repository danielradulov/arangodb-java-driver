package containers;


import com.arangodb.next.connection.HostDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.concurrent.CompletableFuture;

public enum SingleServerContainer {

    INSTANCE;

    private final Logger log = LoggerFactory.getLogger(SingleServerContainer.class);

    private final int PORT = 8529;
    private final String DOCKER_IMAGE = "docker.io/arangodb/arangodb:3.5.1";
    private final String PASSWORD = "test";

    private final GenericContainer container =
            new GenericContainer(DOCKER_IMAGE)
                    .withExposedPorts(PORT)
                    .withEnv("ARANGO_ROOT_PASSWORD", PASSWORD)
                    .withLogConsumer(new Slf4jLogConsumer(log).withPrefix("[DB_LOG]"))
                    .waitingFor(Wait.forHttp("/_api/version")
                            .withBasicCredentials("root", "test")
                            .forStatusCode(200));

    public HostDescription getHostDescription() {
        return HostDescription.of(container.getContainerIpAddress(), container.getFirstMappedPort());
    }

    public CompletableFuture<SingleServerContainer> start() {
        return CompletableFuture.runAsync(container::start).thenAccept((v) -> log.info("Ready!")).thenApply((v) -> this);
    }

    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(container::stop).thenAccept((v) -> log.info("Stopped!"));
    }

}