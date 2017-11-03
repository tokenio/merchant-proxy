## Merchant Proxy

The merchant proxy is a wrapper server around the java SDK, that enables
merchants to accept and initiate payments from a user, using a simple HTTP API.

Once a tokenID is obtained (from the website), one can simply call GET /tokens/:id to 
get the details of that token, or PUT /tokens/:id to make a transfer on that token.

The steps to achieve this are the following:
1. Use the merchant-sample-js or merchant-sample-java to create a website with a Token
button, or create a custom website with the button, and the client side javascript code
explained in the[merchant checkout docs](https://developer.token.io/merchant-checkout/).
2. Start this proxy server locally
3. Call GET /member to get the alias of the member
4. Make sure the alias is the same one that is configured in the client side code
5. A user now checks out using the button, and a tokenId is received after the user approves
the transaction. (This will require a download of the Token app).
6. Call GET /tokens/:id to get details on that token
7. Call PUT /tokens/:id to initiate a payment using this token. This requires some data (amount, currency,
account), which is explained below. 


### Get member -  GET /member
```bash
curl -X GET "http://127.0.0.1:4567/member"
```

```json
{
  "memberId": "m:2EG28HPYkTkfbRyBFYiSoDjDaQRG:5zKtXEAq",
  "aliases": [{
    "type": "EMAIL",
    "value": "merchant-sample-svtvytaaydlkmvtvpmsg+noverify@example.com"
  }]
}

```

###  Get Token - GET /tokens/:id
```bash
curl -X GET "http://127.0.0.1:4567/tokens/tt:HuvAe7sY2WFh7ACP8f5LdL4muGCuWtupL7zGcMZoPhi:2gFuVeuQjGm"
```

```json
{
  "token": {
    "id": "tt:HuvAe7sY2WFh7ACP8f5LdL4muGCuWtupL7zGcMZoPhi:2gFuVeuQjGm",
    "payload": {
      "version": "1.0",
      "refId": "2sws1eu6bnd9iuloqo8h4n",
      "issuer": {
        "alias": {
          "type": "EMAIL",
          "value": "silver@token.io"
        }
      },
      "from": {
        "id": "m:3mYtK26VeJ9MD3AvK2ou1X9vnNe9:5zKtXEAq"
      },
      "expiresAtMs": "1509826707566",
      "transfer": {
        "redeemer": {
          "id": "m:2EG28HPYkTkfbRyBFYiSoDjDaQRG:5zKtXEAq",
          "alias": {
            "type": "EMAIL",
            "value": "merchant-sample-svtvytaaydlkmvtvpmsg+noverify@example.com"
          }
        },
        "instructions": {
          "source": {
            "account": {
              "token": {
                "memberId": "m:3mYtK26VeJ9MD3AvK2ou1X9vnNe9:5zKtXEAq",
                "accountId": "a:FcYJ48ccrc8JbMVpMPXnXpG71RfqNpfKqa5SyLZ4tt17:2gFuVeuQjGm"
              }
            }
          }
        },
        "currency": "EUR",
        "lifetimeAmount": "4.99",
        "pricing": {
          "sourceQuote": {
            "id": "54f2aec3f84a4ed1b3fffcd4c35231a8",
            "accountCurrency": "EUR",
            "feesTotal": "0.25",
            "fees": [{
              "amount": "0.17",
              "description": "Transaction Fee"
            }, {
              "amount": "0.08",
              "description": "Initiation Fee"
            }],
            "expiresAtMs": "1509826707566"
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
        "memberId": "m:3mYtK26VeJ9MD3AvK2ou1X9vnNe9:5zKtXEAq",
        "keyId": "sbKydDVML5zbUylC",
        "signature": "MFP4evLoKaywmWVRYxW6G_oDoFTYt-tq3Ok-HwQ6DBm3VUnt9YrtFmFvEmb6kLAqq4nIGMc59dcodaYUOrQSBA"
      }
    }, {
      "action": "ENDORSED",
      "signature": {
        "memberId": "m:2B6MCv4tsBcFAgidgd3jwkyibYuB:5zKtXEAq",
        "keyId": "CqSTHPvWY_dgVh-f",
        "signature": "QgqjFt5RCuTp0M6sYG1_ay-8NpnMlnuC9Q9ETW1bW3tKn9u0cKR-hzdcBF8jq0e-k-xHaPR3AqD99ti4lvOMCQ"
      }
    }]
  }
}
```

### Redeem Token - PUT /tokens/:id
```bash
curl -X PUT -H 'Content-Type: application/json' "http://127.0.0.1:4567/tokens/tt:HuvAe7sY2WFh7ACP8f5LdL4muGCuWtupL7zGcMZoPhi:2gFuVeuQjGm" -d '{amount:4, currency:"EUR", account:{sepa:{iban:"123"}}}'
```

```json
{
  "transfer": {
    "id": "t:3w711hRP97GxLrNbbrVsi5YgQVPMtiKnzT9tysN2NMVB:2gFuVeuQjGm",
    "transactionId": "913f1c919cbf40e9be4472e689340458",
    "createdAtMs": "1509740477516",
    "payload": {
      "refId": "duvSEtEPelMNEcpKaVkV",
      "tokenId": "tt:HuvAe7sY2WFh7ACP8f5LdL4muGCuWtupL7zGcMZoPhi:2gFuVeuQjGm",
      "amount": {
        "currency": "EUR",
        "value": "4.0"
      },
      "destinations": [{
        "account": {
          "sepa": {
            "iban": "123"
          }
        }
      }]
    },
    "payloadSignatures": [{
      "memberId": "m:2B6MCv4tsBcFAgidgd3jwkyibYuB:5zKtXEAq",
      "keyId": "CqSTHPvWY_dgVh-f",
      "signature": "63dLtA6yJXM3DUeCeBmjPxe8YQJLX2-MWpBtoxRjG2Ak43_KalBWfmETXIQLOydANWDhDwENFLPVN5MiXnWTBw"
    }, {
      "memberId": "m:2EG28HPYkTkfbRyBFYiSoDjDaQRG:5zKtXEAq",
      "keyId": "QIZPdrtxVLBfsrXk",
      "signature": "KtREmB5L0TEld1uVlPekybch3gxvHuXWLzwayA2tjH96XJsdIdag_k8IQ-lWqvWlo_6P8fIWl7o2xzh3lpQmDw"
    }],
    "status": "SUCCESS"
  }
}
```

