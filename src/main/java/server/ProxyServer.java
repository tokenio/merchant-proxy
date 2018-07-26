package server;

import static io.token.TokenRequest.TokenRequestOptions.REDIRECT_URL;
import static io.token.proto.common.alias.AliasProtos.Alias.Type.DOMAIN;
import static io.token.proto.common.security.SecurityProtos.Key.Level.STANDARD;
import static io.token.proto.common.transaction.TransactionProtos.Transaction;
import static io.token.rpc.util.Converters.execute;
import static server.proto.Proxy.GetAccountRequest;
import static server.proto.Proxy.GetAccountResponse;
import static server.proto.Proxy.GetAccountsRequest;
import static server.proto.Proxy.GetAccountsResponse;
import static server.proto.Proxy.GetBalanceRequest;
import static server.proto.Proxy.GetBalanceResponse;
import static server.proto.Proxy.GetTransactionRequest;
import static server.proto.Proxy.GetTransactionResponse;
import static server.proto.Proxy.GetTransactionsRequest;
import static server.proto.Proxy.GetTransactionsResponse;
import static server.proto.Proxy.RequestAccessTokenRequest;
import static server.proto.Proxy.RequestAccessTokenResponse;
import static server.proto.Proxy.UseAccessTokenRequest;
import static server.proto.Proxy.UseAccessTokenResponse;

import com.google.common.base.Preconditions;
import com.google.protobuf.TextFormat;
import com.typesafe.config.Config;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.token.AccessTokenBuilder;
import io.token.Account;
import io.token.Member;
import io.token.TokenIO;
import io.token.TokenIO.TokenCluster;
import io.token.TokenRequest;
import io.token.TokenRequestCallback;
import io.token.TransferTokenBuilder;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.alias.AliasProtos.Alias;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.security.UnsecuredFileSystemKeyStore;
import server.proto.Proxy.CreateTransferRequest;
import server.proto.Proxy.CreateTransferResponse;
import server.proto.Proxy.GenerateTokenRequestUrlRequest;
import server.proto.Proxy.GenerateTokenRequestUrlResponse;
import server.proto.Proxy.GetMemberRequest;
import server.proto.Proxy.GetMemberResponse;
import server.proto.Proxy.GetTokenRequest;
import server.proto.Proxy.GetTokenResponse;
import server.proto.Proxy.ParseTokenRequestCallbackRequest;
import server.proto.Proxy.ParseTokenRequestCallbackResponse;
import server.proto.Proxy.StoreTokenRequestRequest;
import server.proto.Proxy.StoreTokenRequestResponse;
import server.proto.ProxyServiceGrpc.ProxyServiceImplBase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A sample gRPC service implementation. Echos requests back.
 */
public class ProxyServer extends ProxyServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);
    private boolean usingAccessToken = false;
    private Member member;
    private Config config;
    private TokenIO tokenIO;

    ProxyServer(Config config) throws IOException {
        this.config = config;
        tokenIO = initializeSdk();
        member = initializeMember(tokenIO);

        // TODO(RD-581): Deprecate
        new Timer().schedule(
                new TimerTask() {
                    public void run() {
                        tokenIO.getMember(member.memberId());
                    }
                },
                0,
                2 * 60 * 1000);
    }

    @Override
    public void getMember(
            GetMemberRequest request,
            StreamObserver<GetMemberResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get member: ({})", TextFormat.shortDebugString(request));

            return GetMemberResponse.newBuilder()
                    .setMemberId(member.memberId())
                    .addAllAliases(member.aliases())
                    .build();
        });
    }

    @Override
    public void getToken(
            GetTokenRequest request,
            StreamObserver<GetTokenResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get token: ({})", TextFormat.shortDebugString(request));

            return GetTokenResponse.newBuilder()
                    .setToken(member.getToken(request.getTokenId()))
                    .build();
        });
    }

    @Override
    public void createTransfer(
            CreateTransferRequest request,
            StreamObserver<CreateTransferResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Create transfer: ({})", TextFormat.shortDebugString(request));

            return CreateTransferResponse.newBuilder()
                    .setTransfer(member.redeemToken(member.getToken(request.getTokenId())))
                    .build();
        });
    }

    @Override
    public void storeTokenRequest(
            StoreTokenRequestRequest request,
            StreamObserver<StoreTokenRequestResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Store token request: ({})", TextFormat.shortDebugString(request));

            TransferTokenBuilder transferTokenBuilder = new TransferTokenBuilder(
                    request.getAmount(),
                    request.getCurrency())
                    .setToAlias(member.firstAlias())
                    .setToMemberId(member.memberId())
                    .setRefId(request.getRefId())
                    .setDescription(request.getDescription())
                    .addDestination(TransferEndpoint.newBuilder()
                            .setAccount(request.getDestination())
                            .build());

            String tokenRequestId = member.storeTokenRequest(TokenRequest
                    .create(transferTokenBuilder)
                    .setOption(REDIRECT_URL, request.getCallbackUrl()));

            return StoreTokenRequestResponse.newBuilder()
                    .setTokenRequestId(tokenRequestId)
                    .build();
        });
    }

    @Override
    public void requestAccess(
            RequestAccessTokenRequest request,
            StreamObserver<RequestAccessTokenResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Request access: ({})", TextFormat.shortDebugString(request));

            AccessTokenBuilder accessTokenBuilder = AccessTokenBuilder.create(member.firstAlias())
                    .forAllAccounts()
                    .forAllBalances()
                    .forAllTransactions();

            String tokenRequestId = member.storeTokenRequest(TokenRequest
                    .create(accessTokenBuilder)
                    .setOption(REDIRECT_URL, request.getCallbackUrl()));

            return RequestAccessTokenResponse.newBuilder()
                    .setTokenRequestId(tokenRequestId)
                    .build();
        });
    }

    @Override
    public void generateTokenRequestUrl(
            GenerateTokenRequestUrlRequest request,
            StreamObserver<GenerateTokenRequestUrlResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Generate token request url: ({})", TextFormat.shortDebugString(request));

            String url = tokenIO.generateTokenRequestUrl(
                    request.getRequestId(),
                    request.getState(),
                    request.getCsrfToken());
            return GenerateTokenRequestUrlResponse.newBuilder()
                    .setUrl(url)
                    .build();
        });
    }

    @Override
    public void parseTokenRequestCallback(
            ParseTokenRequestCallbackRequest request,
            StreamObserver<ParseTokenRequestCallbackResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info(
                    "Parse token request call back: ({})",
                    TextFormat.shortDebugString(request));

            TokenRequestCallback callback = tokenIO.parseTokenRequestCallbackUrl(
                    request.getUrl(),
                    request.getCsrfToken());
            return ParseTokenRequestCallbackResponse.newBuilder()
                    .setTokenId(callback.getTokenId())
                    .setState(callback.getState())
                    .build();
        });
    }

    @Override
    public void useAccessToken(
            UseAccessTokenRequest request,
            StreamObserver<UseAccessTokenResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Use access token: ({})", TextFormat.shortDebugString(request));

            member.useAccessToken(request.getTokenId(), false);
            usingAccessToken = true;
            return UseAccessTokenResponse.getDefaultInstance();
        });
    }

    @Override
    public void getAccounts(
            GetAccountsRequest request,
            StreamObserver<GetAccountsResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get accounts: ({})", TextFormat.shortDebugString(request));
            Preconditions.checkArgument(usingAccessToken, "Access token not set!");

            List<AccountProtos.Account> accounts = member.getAccounts()
                    .stream()
                    .map(Account::protoAccount)
                    .collect(Collectors.toList());
            return GetAccountsResponse.newBuilder()
                    .addAllAccounts(accounts)
                    .build();
        });
    }

    @Override
    public void getAccount(
            GetAccountRequest request,
            StreamObserver<GetAccountResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get account: ({})", TextFormat.shortDebugString(request));
            Preconditions.checkArgument(usingAccessToken, "Access token not set!");

            return GetAccountResponse.newBuilder()
                    .setAccount(member.getAccount(request.getAccountId()).protoAccount())
                    .build();
        });
    }

    @Override
    public void getBalance(
            GetBalanceRequest request,
            StreamObserver<GetBalanceResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get balance: ({})", TextFormat.shortDebugString(request));
            Preconditions.checkArgument(usingAccessToken, "Access token not set!");

            return GetBalanceResponse.newBuilder()
                    .setBalance(member.getBalance(request.getAccountId(), STANDARD))
                    .build();
        });
    }

    @Override
    public void getTransaction(
            GetTransactionRequest request,
            StreamObserver<GetTransactionResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get transaction: ({})", TextFormat.shortDebugString(request));
            Preconditions.checkArgument(usingAccessToken, "Access token not set!");

            return GetTransactionResponse.newBuilder()
                    .setTransaction(member.getTransaction(
                            request.getAccountId(),
                            request.getTransactionId(),
                            STANDARD))
                    .build();
        });
    }

    @Override
    public void getTransactions(
            GetTransactionsRequest request,
            StreamObserver<GetTransactionsResponse> responseObserver) {
        execute(responseObserver, () -> {
            logger.info("Get transactions: ({})", TextFormat.shortDebugString(request));
            Preconditions.checkArgument(usingAccessToken, "Access token not set!");
            Preconditions.checkArgument(request.getLimit() > 0, "Limit not set properly!");

            PagedList<Transaction, String> transactions = member.getTransactions(
                    request.getAccountId(),
                    request.getOffset(),
                    request.getLimit(),
                    STANDARD);
            return GetTransactionsResponse.newBuilder()
                    .addAllTransactions(transactions.getList())
                    .setOffset(transactions.getOffset())
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
        // If we try to create a member with an already-used alias,
        // it will fail.
        String domain = config.getString("domain").toLowerCase();
        Alias alias = Alias.newBuilder()
                .setType(DOMAIN)
                .setValue(domain)
                .build();
        if (tokenIO.aliasExists(alias)) {
            throw new IllegalArgumentException(
                    "Domain already taken. Change domain and try again.");
        }
        return tokenIO.createBusinessMember(alias);
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
            return tokenIO.getMember(memberId);
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
                                        config.getString("domain").toLowerCase())))
                .findFirst()
                .orElseGet(() -> createMember(tokenIO));
    }
}

