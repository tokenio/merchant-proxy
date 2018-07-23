package server;

import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.patch;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import grpcbridge.Bridge;
import grpcbridge.BridgeBuilder;
import grpcbridge.Exceptions.BridgeException;
import grpcbridge.http.HttpMethod;
import grpcbridge.http.HttpRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

/**
 * A sample application that demonstrates setting up gRPC {@link Bridge} for
 * bridging HTTP RESTful API the corresponding gRPC implementation.
 *
 * <p>
 * The HTTP endpoint is implemented using <a href="http://sparkjava.com/">Spark</a>.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        // Setup logging
        setupLogging();

        // Create service implementation instance.
        Config config = ConfigFactory.load();
        ProxyServer proxyServer = new ProxyServer(config);

        // Create gRPC server, bind the service implementation and start the server.
        Server rpcServer = ServerBuilder
                .forPort(9000)
                .addService(proxyServer.bindService())
                .build();
        rpcServer.start();
        // Create new HTTP to gRPC bridge.
        Bridge bridge = new BridgeBuilder()
                .addFile(server.proto.Proxy.getDescriptor())
                .addService(proxyServer.bindService())
                .build();

        // Set port numnber
        port(config.getInt("port"));

        // Map Spark HTTP endpoints to the Bridge.
        get("/*", (req, res) -> handle(bridge, req));
        post("/*", (req, res) -> handle(bridge, req));
        put("/*", (req, res) -> handle(bridge, req));
        delete("/*", (req, res) -> handle(bridge, req));
        patch("/*", (req, res) -> handle(bridge, req));
        exception(
                BridgeException.class,
                (error, req, res) -> logger.warn("Bridge error: " + error.getMessage()));
        exception(
                RuntimeException.class,
                (error, req, res) -> logger.error("Unhandled exception", error));
    }

    private static String handle(Bridge bridge, Request req) {
        String pathInfo = (req.queryString() == null || req.queryString().isEmpty())
                ? req.pathInfo()
                : req.pathInfo() + "?" + req.queryString();
        HttpRequest httpRequest = HttpRequest
                .builder(HttpMethod.valueOf(req.requestMethod()), pathInfo)
                .body(req.body())
                .build();
        return bridge.handle(httpRequest).getBody();
    }

    private static void setupLogging() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger("root").setLevel(Level.INFO);
        context.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
        context.getLogger("io.grpc.netty.NettyClientHandler").setLevel(Level.WARN);
        context.getLogger("io.netty").setLevel(Level.WARN);
        context.getLogger("io.token").setLevel(Level.WARN);
    }
}
