# Merchant Proxy

The merchant proxy is a HTTP wrapper server around the [Java SDK](https://github.com/tokenio/sdk-java),
which enables merchants to initiate payments from a user, and access user's account
information.

Following the [Token Request Flow](https://developer.token.io/token-request), a merchant can obtain 
an id of a token that is endorsed by a user. Then it can fetch the details of that token, and
use the token to create a payment or access the user's account information, depending on the
type of the requested resources.

The definitions of the HTTP endpoints can be found [here](src/main/proto/proxy.proto). 

## Configuration
The configuration options can be found in [application.conf](src/main/resources/application.conf).
Make sure to configure your own domain name. It will serve as the alias of your token account. Your
[keys](https://developer.token.io/overview/#key-management) will be stored in the specified key
directory. Remember to take good care of your keys.

Note that we will need to verify your domain name in the production environment, but not in the
sandbox environment.

## Usage
The steps to initiate a payment:
1. Create a custom web app that implements the [Token Request Flow](https://developer.token.io/token-request),
or use the [merchant-sample-proxy](https://github.com/tokenio/merchant-sample-proxy) to see a simple example.
2. Start this proxy server: `./gradlew build run`
3. Call GET /member to get the alias of the member. Make sure the alias is the same one that
is configured.
4. A user now checks out using the button. Call POST /token-requests to create a token request.
5. Call GET /token-request-url to get a url and redirect user to the url. A callback will
occur once the payment is approved by the user (This will require a download of the Token app for
most of the banks).
6. Call GET /parse-token-request-callback to retrieve the token id.
7. Call GET /tokens/:id to get details on that token.
8. Call POST /transfers to initiate a transfer using this token.

The steps to access account information:
1. Create a custom web app that implements the [Token Request Flow](https://developer.token.io/token-request),
or use the [pfm-sample-proxy](https://github.com/tokenio/pfm-sample-proxy) to see a simple example.
2. Start this proxy server: `./gradlew build run`
3. Call GET /member to get the alias of the member. Make sure the alias is the same one that
is configured.
4. A user now checks out using the button. Call POST /access-token-requests to create a token request.
5. Call GET /token-request-url to get a url and redirect user to the url. A callback will
occur once the payment is approved by the user (This will require a download of the Token app for
most of the banks).
6. Call GET /parse-token-request-callback to retrieve the token id.
7. Call GET /tokens/{token_id} to get details on that token.
8. Put the token id in the Authorization header to fetch account, balance and transactions data via
the corresponding endpoints.

## Payment Flow Walk-through

#### Get member -  GET /member
```bash
curl -X GET "http://127.0.0.1:4567/member"
```

```json
{
  "memberId": "m:m2hkupUjtJRFKvpfD3vBKvTXM4J:5zKtXEAq",
  "aliases": [{
    "type": "DOMAIN",
    "value": "example.com"
  }]
}
```

####  Create Token Request - POST /token-requests
```bash
curl -X POST -H 'Content-Type: application/json' "http://127.0.0.1:4567/transfer-token-requests" -d '{"amount":"4.99","currency":"EUR","description":"Book Purchase","destination":{"sepa":{"iban":"DE16700222000072880129"}}, "callbackUrl":"http://localhost:3000/redeem"}'
```

```json
{
  "tokenRequestId": "rq:21UaTmPruCDVPKJfv9peZ7Juv7Ck:5zKtXEAq"
}
```

####  Generates Token Request URL - GET /token-request-url?requestId={request_id}&state={state}&csrfToken={csrf_token}
```bash
curl -X GET "http://127.0.0.1:4567/token-request-url?requestId=rq:21UaTmPruCDVPKJfv9peZ7Juv7Ck:5zKtXEAq&state=123&csrfToken=456"
```

```json
{
  "url": "https://web-app.sandbox.token.io/request-token/rq:21UaTmPruCDVPKJfv9peZ7Juv7Ck:5zKtXEAq?state=%7B%22csrfTokenHash%22%3A%22b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0%22%2C%22innerState%22%3A%22123%22%7D"
}
```

####  Parse Callback URL - GET /parse-token-request-callback?url={url}&csrfToken={csrf_token}
```bash
curl -X GET "http://127.0.0.1:4567/parse-token-request-callback?csrfToken=456&url=http%3A%2F%2Flocalhost%3A3000%2Fredeem%3Fsignature%3D%257B%2522memberId%2522%253A%2522m%253A3rKtsoKaE1QUti3KCWPrcSQYvJj9%253A5zKtXEAq%2522%252C%2522keyId%2522%253A%2522lgf2Mn0G4kkcXd5m%2522%252C%2522signature%2522%253A%2522dkd52gYVCFZhETUPWHO1sCogkpzIjagXrNnUvtgVDxs9eMQg6_oRDYqMkFOpET4GoPpJywGYwipKVHpH_M7LAA%2522%257D%26state%3D%257B%2522csrfTokenHash%2522%253A%2522b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0%2522%252C%2522innerState%2522%253A%2522123%2522%257D%26tokenId%3Dtt%253ADkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh%253A5zKcENpV"
```

```json
{
  "tokenId": "tt:Dkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh:5zKcENpV",
  "state": "123"
}
```

####  Get Token - GET /tokens/{token_id}
```bash
curl -X GET "http://127.0.0.1:4567/tokens/tt:Dkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh:5zKcENpV"
```

```json
{
  "token": {
    "id": "tt:Dkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh:5zKcENpV",
    "payload": {
      "version": "1.0",
      "refId": "3529zeyo56u5kp7l65gj69",
      "issuer": {
        "alias": {
          "type": "BANK",
          "value": "iron"
        }
      },
      "from": {
        "id": "m:3EV6R3dpjRhKMJcDbFo24iUygBFa:5zKtXEAq"
      },
      "to": {
        "id": "m:2ZooBovcBG9zLTPMMgkQFVrK9YLf:5zKtXEAq",
        "alias": {
          "type": "DOMAIN",
          "value": "example.com"
        }
      },
      "expiresAtMs": "1525384419767",
      "transfer": {
        "redeemer": {
          "id": "m:2ZooBovcBG9zLTPMMgkQFVrK9YLf:5zKtXEAq",
          "alias": {
            "type": "DOMAIN",
            "value": "example.com"
          }
        },
        "instructions": {
          "source": {
            "account": {
              "token": {
                "memberId": "m:3EV6R3dpjRhKMJcDbFo24iUygBFa:5zKtXEAq",
                "accountId": "a:9UNkvJ4DFZjAWMepYFAnvFCBu9MrVACMXWMTAFuqvkj8:5zKcENpV"
              }
            }
          },
          "destinations": [{
            "account": {
              "sepa": {
                "iban": "DE16700222000072880129"
              }
            }
          }],
          "metadata": {
          }
        },
        "currency": "EUR",
        "lifetimeAmount": "4.9900",
        "pricing": {
          "sourceQuote": {
            "id": "0ef77dc033034d2c8ae9588f15bfbe23",
            "accountCurrency": "USD",
            "feesTotal": "0.25",
            "fees": [{
              "amount": "0.17",
              "description": "Transaction Fee"
            }, {
              "amount": "0.08",
              "description": "Initiation Fee"
            }],
            "rates": [{
              "baseCurrency": "EUR",
              "quoteCurrency": "USD",
              "rate": "1.0668"
            }],
            "expiresAtMs": "1525384419767"
          },
          "instructions": {
            "feesPaidBy": "SHARED_FEE",
            "fxPerformedBy": "SHARED_FX"
          }
        }
      }
    },
    "payloadSignatures": [{
      "action": "ENDORSED",
      "signature": {
        "memberId": "m:3EV6R3dpjRhKMJcDbFo24iUygBFa:5zKtXEAq",
        "keyId": "bxDQn2F81970-c-Y",
        "signature": "aBH6PSSn0Cdxtr7tEkW9NfxcHQvcUWueUr7i-1qxbl-AuHheCktdITtP2H190kvWnRzasZOSFg-Y7tnZ_ByvBQ"
      }
    }, {
      "action": "ENDORSED",
      "signature": {
        "memberId": "m:2sTarMXhSBPaDpCbvVK6GTMgJqvT:5zKtXEAq",
        "keyId": "CqSTHPvWY_dgVh-f",
        "signature": "WavCXUN1hp7uUp83lK9g4MyGkUE_4G_7c2PoN4pmXRZWq2JlpT8JnzuE67afyUz14dAH-Lojfc6cWcthSrEMDA"
      }
    }]
  }
}
```

#### Create Transfer - POST /transfers
```bash
curl -X POST -H 'Content-Type: application/json' "http://127.0.0.1:4567/transfers" -d '{tokenId:"tt:Dkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh:5zKcENpV"}'
```

```json
{
  "transfer": {
    "id": "t:y9aKTnWwLD2qmb3mvF5sNzLGQNj8dMEGK9aB3n9Dv1y:5zKcENpV",
    "transactionId": "96ba8b65067b45f0be8334bf31e9bdb0",
    "createdAtMs": "1525298132076",
    "payload": {
      "refId": "b86ABqdy4Lf4fnjcnnzSVEVgg6d",
      "tokenId": "tt:Dkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh:5zKcENpV"
    },
    "payloadSignatures": [{
      "memberId": "m:2sTarMXhSBPaDpCbvVK6GTMgJqvT:5zKtXEAq",
      "keyId": "CqSTHPvWY_dgVh-f",
      "signature": "H9hKfMznHeli0LOgG8cDgh_Cqx_q4fcX654ZgVeGIyj8dkVd63105yojr7MHO5IJaABZK0kWuMuTol2oBv5WCQ"
    }, {
      "memberId": "m:2ZooBovcBG9zLTPMMgkQFVrK9YLf:5zKtXEAq",
      "keyId": "3TdbZVoDYhxpH1qb",
      "signature": "SZKffABNEiPP7CFJA9f7sKlyEe09sJw9XtbGkhqKGLV06cDZuXWY05KStthw5PoZ4uY1Vzu65nGcA0ZZo0xwCg"
    }],
    "status": "SUCCESS"
  }
}
```

## Information Access Flow Walk-through

#### Create Token Request - Post /access-token-requests
```bash
curl -X POST -H 'Content-Type: application/json' "http://127.0.0.1:4567/access-token-requests" -d '{"callbackUrl":"http://localhost:3000/redeem"}'
```

```json
{
  "tokenRequestId": "rq:22s7CGgjSbHHYkneVBSHdaGKCUcW:5zKtXEAq"
}
```
####  Generates Token Request URL - GET /token-request-url?requestId={request_id}&state={state}&csrfToken={csrf_token}
```bash
curl -X GET "http://127.0.0.1:4567/token-request-url?requestId=rq:22s7CGgjSbHHYkneVBSHdaGKCUcW:5zKtXEAq&state=123&csrfToken=456"
```

```json
{
  "url": "https://web-app.sandbox.token.io/request-token/rq:22s7CGgjSbHHYkneVBSHdaGKCUcW:5zKtXEAq?state=%7B%22csrfTokenHash%22%3A%22b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0%22%2C%22innerState%22%3A%22123%22%7D"
}
```

####  Parse Callback URL - GET /parse-token-request-callback?url={url}&csrfToken={csrf_token}
```bash
curl -X GET "http://127.0.0.1:4567/parse-token-request-callback?csrfToken=456&url=http%3A%2F%2Flocalhost%3A3000%2Fredeem%3Fsignature%3D%257B%2522memberId%2522%253A%2522m%253A3rKtsoKaE1QUti3KCWPrcSQYvJj9%253A5zKtXEAq%2522%252C%2522keyId%2522%253A%2522lgf2Mn0G4kkcXd5m%2522%252C%2522signature%2522%253A%2522j1zv_XAIJ5GsixDmbICvVxHFcUEPaRxY8mmITOxsb2WjlMQRZaZ8Khw8ViDHgTNeO8eSarADUwDBxjx9ce7-BA%2522%257D%26state%3D%257B%2522csrfTokenHash%2522%253A%2522b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0%2522%252C%2522innerState%2522%253A%2522123%2522%257D%26tokenId%3Dta%253A4yr8Aow193um9EJ8SE231Aud6JKGf5xyTHGHknar15QF%253A5zKtXEAq"
```

```json
{
  "tokenId": "ta:4yr8Aow193um9EJ8SE231Aud6JKGf5xyTHGHknar15QF:5zKtXEAq",
  "state": "123"
}
```

####  Get Accounts - GET /accounts
```bash
curl -H "Authorization: ta:4yr8Aow193um9EJ8SE231Aud6JKGf5xyTHGHknar15QF:5zKtXEAq" -X GET "http://127.0.0.1:4567/accounts"
```

```json
{
  "accounts": [{
    "id": "a:6VAYc1RooMSaDjVkfCV22e4FYB4sTxhDRbfQ9JtUnuCw:8QSLX5njxscQ",
    "name": "xxxx0001",
    "bankId": "obozone",
    "accountFeatures": {
      "supportsInformation": true,
      "requiresExternalAuth": true,
      "supportsSendPayment": true,
      "supportsReceivePayment": true
    }
  }]
}
```

####  Get Account - GET /accounts/{account_id}
```bash
curl -H "Authorization: ta:4yr8Aow193um9EJ8SE231Aud6JKGf5xyTHGHknar15QF:5zKtXEAq" -X GET "http://127.0.0.1:4567/accounts/a:6VAYc1RooMSaDjVkfCV22e4FYB4sTxhDRbfQ9JtUnuCw:8QSLX5njxscQ"
```

```json
{
  "accounts": [{
    "id": "a:6VAYc1RooMSaDjVkfCV22e4FYB4sTxhDRbfQ9JtUnuCw:8QSLX5njxscQ",
    "name": "xxxx0001",
    "bankId": "obozone",
    "accountFeatures": {
      "supportsInformation": true,
      "requiresExternalAuth": true,
      "supportsSendPayment": true,
      "supportsReceivePayment": true
    }
  }]
}
```

####  Get Transactions - GET /accounts/{account_id}/transactions?offset={offset}&limit={limit}
```bash
curl -H "Authorization: ta:4yr8Aow193um9EJ8SE231Aud6JKGf5xyTHGHknar15QF:5zKtXEAq" -X GET "http://127.0.0.1:4567/accounts/a:6VAYc1RooMSaDjVkfCV22e4FYB4sTxhDRbfQ9JtUnuCw:8QSLX5njxscQ/transactions?limit=2"
```

```json
{
  "transactions": [{
    "id": "5185dd59-0d22-419a-9c38-a2de8165f76a",
    "type": "DEBIT",
    "status": "SUCCESS",
    "amount": {
      "currency": "GBP",
      "value": "6.00"
    },
    "description": "Payment Id: pmt-a94446ce-ab2a-4fae-aa9f-05f23fa9a06a",
    "createdAtMs": "1530523392939",
    "metadata": {
      "transactionSecondaryReference": "",
      "balanceType": "ClosingAvailable",
      "balanceCreditDebitIndicator": "Debit",
      "providerAccountId": "500000000000000000000007",
      "bookingStatus": "Booked",
      "proprietaryBankTransactionCode": "PMT",
      "valueDateTime": "2018-07-02T09:23:12.939Z",
      "balanceAmount": "6.00",
      "dataSource": "CMA9",
      "balanceCurrency": "GBP"
    }
  }, {
    "id": "802cfc7d-0a49-4b2d-878e-7a01e486620c",
    "type": "DEBIT",
    "status": "SUCCESS",
    "amount": {
      "currency": "GBP",
      "value": "6.00"
    },
    "description": "Payment Id: pmt-2069c76a-ef19-4024-828e-34a1abe51e4c",
    "createdAtMs": "1530523924650",
    "metadata": {
      "transactionSecondaryReference": "",
      "balanceType": "ClosingAvailable",
      "balanceCreditDebitIndicator": "Credit",
      "providerAccountId": "500000000000000000000007",
      "bookingStatus": "Booked",
      "proprietaryBankTransactionCode": "PMT",
      "valueDateTime": "2018-07-02T09:32:04.65Z",
      "balanceAmount": "0.00",
      "dataSource": "CMA9",
      "balanceCurrency": "GBP"
    }
  }],
  "offset": "CSX7tB4nXfs6z6fsT2NcJJACyTyJG42ftZghBrjrmq8mA4X6K9ENprLBFiFsRbMQ8BZ3"
}
```

####  Get Transaction - GET /accounts/{account_id}/transactions/{transaction_id}
```bash
curl -H "Authorization: ta:4yr8Aow193um9EJ8SE231Aud6JKGf5xyTHGHknar15QF:5zKtXEAq" -X GET "http://127.0.0.1:4567/accounts/a:6VAYc1RooMSaDjVkfCV22e4FYB4sTxhDRbfQ9JtUnuCw:8QSLX5njxscQ/transactions/5185dd59-0d22-419a-9c38-a2de8165f76a"
```

```json
{
  "transaction": {
    "id": "5185dd59-0d22-419a-9c38-a2de8165f76a",
    "type": "DEBIT",
    "status": "SUCCESS",
    "amount": {
      "currency": "GBP",
      "value": "6.00"
    },
    "description": "Payment Id: pmt-a94446ce-ab2a-4fae-aa9f-05f23fa9a06a",
    "createdAtMs": "1530523392939",
    "metadata": {
      "transactionSecondaryReference": "",
      "balanceType": "ClosingAvailable",
      "balanceCreditDebitIndicator": "Debit",
      "providerAccountId": "500000000000000000000007",
      "bookingStatus": "Booked",
      "proprietaryBankTransactionCode": "PMT",
      "valueDateTime": "2018-07-02T09:23:12.939Z",
      "balanceAmount": "6.00",
      "dataSource": "CMA9",
      "balanceCurrency": "GBP"
    }
  }
}

```

####  Get Balance - GET /accounts/{account_id}/balance
```bash
curl -H "Authorization: ta:4yr8Aow193um9EJ8SE231Aud6JKGf5xyTHGHknar15QF:5zKtXEAq" -X GET "http://127.0.0.1:4567/accounts/a:6VAYc1RooMSaDjVkfCV22e4FYB4sTxhDRbfQ9JtUnuCw:8QSLX5njxscQ/balance"
```

```json
{
  "balance": {
    "accountId": "a:6VAYc1RooMSaDjVkfCV22e4FYB4sTxhDRbfQ9JtUnuCw:8QSLX5njxscQ",
    "current": {
      "currency": "GBP",
      "value": "0.0000"
    },
    "available": {
      "currency": "GBP",
      "value": "0.0000"
    }
  }
}
```
