package server;

import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.rpc.util.Converters.execute;

import com.google.protobuf.TextFormat;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.token.Member;
import io.token.TokenIO;
import io.token.TokenIO.TokenCluster;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.BankAccount.Sepa;
import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.security.UnsecuredFileSystemKeyStore;
import server.proto.Proxy.CreateTransferRequest;
import server.proto.Proxy.CreateTransferResponse;
import server.proto.Proxy.GetMemberRequest;
import server.proto.Proxy.GetMemberResponse;
import server.proto.Proxy.GetTokenRequest;
import server.proto.Proxy.GetTokenResponse;
import server.proto.ProxyServiceGrpc.ProxyServiceImplBase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sample gRPC service implementation. Echos requests back.
 */
public class ProxyServer extends ProxyServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private Member member;
    private Config config;

    ProxyServer() throws IOException {
        config = ConfigFactory.load();
        TokenIO lib = initializeSdk();
        member = initializeMember(lib);
    }

    @Override
    public void getMember(GetMemberRequest request,
                             StreamObserver<GetMemberResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get({})", TextFormat.shortDebugString(request));
            return GetMemberResponse.newBuilder()
                    .setMemberId(member.memberId())
                    .addAllAliases(member.aliases())
                    .build();
        });
    }

    @Override
    public void getToken(GetTokenRequest request,
                             StreamObserver<GetTokenResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get({})", TextFormat.shortDebugString(request));
            return GetTokenResponse.newBuilder()
                .setToken(member.getToken(request.getTokenId()))
                .build();
        });
    }

    @Override
    public void createTransfer(CreateTransferRequest request,
                             StreamObserver<CreateTransferResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Put({})", TextFormat.shortDebugString(request));
            return CreateTransferResponse.newBuilder()
                .setTransfer(member.redeemToken(
                        member.getToken(request.getTokenId()),
                        request.getAmount(),
                        request.getCurrency(),
                        TransferEndpoint.newBuilder()
                                .setAccount(BankAccount.newBuilder()
                                        .setSepa(Sepa.newBuilder()
                                                .setIban(config.getString("destinationIban"))))
                                .build()))
                .build();
            });
    }

    /**
     * Initializes the SDK by connecting to Token, and loading or creating a member
     *
     * @return initialized SDK object
     */
    private TokenIO initializeSdk() throws IOException {
        Path keys = Files.createDirectories(Paths.get(config.getString("keysDir")));
        return TokenIO.builder()
                .connectTo(TokenCluster.valueOf(config.getString("environment")))
                // This KeyStore reads private keys from files.
                // Here, it's set up to read the ./keys dir.
                .withKeyStore(new UnsecuredFileSystemKeyStore(
                        keys.toFile()))
                .devKey(config.getString("devKey"))
                .build();
    }

    /**
     * Using a TokenIO SDK client, create a new Member.
     * This has the side effect of storing the new Member's private
     * keys in the ./keys directory.
     *
     * @param tokenIO Token SDK client
     * @return newly-created member
     */
    private Member createMember(TokenIO tokenIO) {
        // Generate a random username.
        // If we try to create a member with an already-used name,
        // it will fail.
        String email = config.getString("email").toLowerCase();
        AliasProtos.Alias alias = AliasProtos.Alias.newBuilder()
                .setType(EMAIL)
                .setValue(email)
                .build();
        if (tokenIO.aliasExists(alias)) {
            throw new RuntimeException(
                    "Email already taken. Change email and try again.");
        }
        return tokenIO.createMember(alias);
        // The newly-created member is automatically logged in.
    }

    /**
     * Using a TokenIO SDK client and the member ID of a previously-created
     * Member (whose private keys we have stored locally), log in as that member.
     *
     * @param tokenIO SDK
     * @param memberId ID of member
     * @return Logged-in member.
     */
    private Member loginMember(TokenIO tokenIO, String memberId) {
        try {
            return tokenIO.login(memberId);
        } catch (StatusRuntimeException sre) {
            // We think we have a member's ID and keys, but we can't log in.
            // In the sandbox testing environment, this can happen:
            // Sometimes, the member service erases the test members.
            throw new RuntimeException(
                    "Couldn't log in saved member, not found. Remove keys dir and try again.");
        }
    }

    /**
     * Log in existing member or create new member.
     *
     * @param tokenIO Token SDK client
     * @return Logged-in member
     */
    private Member initializeMember(TokenIO tokenIO) {
        // The UnsecuredFileSystemKeyStore stores keys in a directory
        // named on the member's memberId, but with ":" replaced by "_".
        // Look for such a directory.
        //   If found, try to log in with that memberId
        //   If not found, create a new member.
        File keysDir = new File(config.getString("keysDir"));
        String[] paths = keysDir.list();

        return Arrays.stream(paths)
                .filter(p -> p.contains("_")) // find dir names containing "_"
                .map(p -> p.replace("_", ":")) // member ID
                .map(memberId -> loginMember(tokenIO, memberId))
                .filter(member -> member.aliases().stream()
                                .anyMatch(alias ->
                                        alias.getValue().equals(
                                                config.getString("email").toLowerCase())))
                .findFirst()
                .orElseGet(() -> createMember(tokenIO));
    }
}

