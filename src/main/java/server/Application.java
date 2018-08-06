package server;

import static io.grpc.Status.Code.ALREADY_EXISTS;
import static io.grpc.Status.Code.DEADLINE_EXCEEDED;
import static io.grpc.Status.Code.FAILED_PRECONDITION;
import static io.grpc.Status.Code.INTERNAL;
import static io.grpc.Status.Code.INVALID_ARGUMENT;
import static io.grpc.Status.Code.NOT_FOUND;
import static io.grpc.Status.Code.OUT_OF_RANGE;
import static io.grpc.Status.Code.PERMISSION_DENIED;
import static io.grpc.Status.Code.RESOURCE_EXHAUSTED;
import static io.grpc.Status.Code.UNAUTHENTICATED;
import static io.grpc.Status.Code.UNAVAILABLE;
import static io.grpc.Status.Code.UNIMPLEMENTED;
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
import grpcbridge.http.HttpMethod;
import grpcbridge.http.HttpRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

/**
 * A sample application that demonstrates setting up gRPC {@link Bridge} for
 * bridging HTTP RESTful API the corresponding gRPC implementation.
 *
 * <p>
 * The HTTP endpoint is implemented using <a href="http://sparkjava.com/">Spark</a>.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final Map<Status.Code, Integer> errorMap =
            new HashMap<Status.Code, Integer>() {
                {
                    put(INVALID_ARGUMENT, 400);
                    put(FAILED_PRECONDITION, 400);
                    put(OUT_OF_RANGE, 400);
                    put(UNAUTHENTICATED, 401);
                    put(PERMISSION_DENIED, 403);
                    put(NOT_FOUND, 404);
                    put(ALREADY_EXISTS, 409);
                    put(RESOURCE_EXHAUSTED, 429);
                    put(INTERNAL, 500);
                    put(UNIMPLEMENTED, 501);
                    put(UNAVAILABLE, 503);
                    put(DEADLINE_EXCEEDED, 504);
                }
            };

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

        // Set port number
        port(config.getInt("port"));

        // Map Spark HTTP endpoints to the Bridge.
        get("/*", (req, res) -> handle(bridge, req));
        post("/*", (req, res) -> handle(bridge, req));
        put("/*", (req, res) -> handle(bridge, req));
        delete("/*", (req, res) -> handle(bridge, req));
        patch("/*", (req, res) -> handle(bridge, req));
        exception(
                StatusRuntimeException.class,
                Application::handleStatusError);
        exception(
                Exception.class,
                (error, req, res) -> {
                    logger.error("Unhandled exception", error);
                    res.status(500);
                    res.body(error.toString());
                });
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

    private static void handleStatusError(
            Exception exception,
            Request request,
            Response response) {
        logger.error("Status runtime exception: ", exception);
        StatusRuntimeException ex = (StatusRuntimeException) exception;
        Status status = ex.getStatus();
        int code = errorMap.getOrDefault(status.getCode(), 500);
        String error = Optional.ofNullable(status.getDescription()).orElse("Unknown");

        response.status(code);
        response.body(error);
    }
}
