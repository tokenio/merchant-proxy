package server;

import static io.grpc.Status.Code.NOT_FOUND;
import static io.token.TokenIO.TokenCluster.SANDBOX;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.EMAIL;
import static io.token.util.Util.generateNonce;

import com.google.protobuf.TextFormat;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.token.Member;
import io.token.TokenIO;
import io.token.proto.common.alias.AliasProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos;
import io.token.security.UnsecuredFileSystemKeyStore;
import server.proto.Proxy.GetMemberRequest;
import server.proto.Proxy.GetMemberResponse;
import server.proto.Proxy.GetTokenRequest;
import server.proto.Proxy.GetTokenResponse;
import server.proto.Proxy.RedeemTokenRequest;
import server.proto.Proxy.RedeemTokenResponse;
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

    public ProxyServer() throws IOException {
        TokenIO lib = initializeSdk();
        member = initializeMember(lib);
    }

    public TokenIO initializeSdk() throws IOException {
        Path keys = Files.createDirectories(Paths.get("./keys"));
        return TokenIO.builder()
                .connectTo(SANDBOX)
                // This KeyStore reads private keys from files.
                // Here, it's set up to read the ./keys dir.
                .withKeyStore(new UnsecuredFileSystemKeyStore(
                        keys.toFile()))
                .devKey("4qY7lqQw8NOl9gng0ZHgT4xdiDqxqoGVutuZwrUYQsI")
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
        logger.info("CREATING MEMBER");
        // Generate a random username.
        // If we try to create a member with an already-used name,
        // it will fail.
        String email = "merchant-sample-" + generateNonce().toLowerCase() + "+noverify@example.com";
        AliasProtos.Alias alias = AliasProtos.Alias.newBuilder()
                .setType(EMAIL)
                .setValue(email)
                .build();
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
            if (sre.getStatus().getCode() == NOT_FOUND) {
                // We think we have a member's ID and keys, but we can't log in.
                // In the sandbox testing environment, this can happen:
                // Sometimes, the member service erases the test members.
                throw new RuntimeException(
                        "Couldn't log in saved member, not found. Remove keys dir and try again.");
            } else {
                throw new RuntimeException(sre);
            }
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
        File keysDir = new File("./keys");
        String[] paths = keysDir.list();

        return Arrays.stream(paths)
                .filter(p -> p.contains("_")) // find dir names containing "_"
                .map(p -> p.replace("_", ":")) // member ID
                .findFirst()
                .map(memberId -> loginMember(tokenIO, memberId))
                .orElse(createMember(tokenIO));
    }

    @Override
    public void getMember(GetMemberRequest request,
                             StreamObserver<GetMemberResponse> responseObserver) {
        logger.info("Get({})", TextFormat.shortDebugString(request));
        responseObserver.onNext(GetMemberResponse.newBuilder()
                .setMemberId(member.memberId())
                .addAllAliases(member.aliases())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getToken(GetTokenRequest request,
                             StreamObserver<GetTokenResponse> responseObserver) {
        logger.info("Get({})", TextFormat.shortDebugString(request));
        responseObserver.onNext(GetTokenResponse.newBuilder()
                .setToken(member.getToken(request.getTokenId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void redeemToken(RedeemTokenRequest request,
                             StreamObserver<RedeemTokenResponse> responseObserver) {
        logger.info("Put({})", TextFormat.shortDebugString(request));
        responseObserver.onNext(RedeemTokenResponse.newBuilder()
                .setTransfer(member.redeemToken(
                        member.getToken(request.getTokenId()),
                        request.getAmount(),
                        request.getCurrency(),
                        TransferInstructionsProtos.TransferEndpoint.newBuilder()
                                .setAccount(request.getAccount())
                                .build()))
                .build());
        responseObserver.onCompleted();
    }
}

