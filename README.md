## Merchant Proxy

The merchant proxy is a wrapper server around the [Java SDK](https://github.com/tokenio/sdk-java),
that enables merchants to initiate and accept payments from a user, using a simple HTTP API.

Following the Token Request flow, a merchant can obtain an id of a token that is endorsed
by a user. Then it can simply call GET /tokens/:id to get the details of that token, or POST
/transfers to make a transfer on that token. More details about the Token Request flow can
be found [here](http://developer.token.io/token-request).

The steps to achieve this are the following:
1. Use the [merchant-sample-proxy](https://github.com/tokenio/merchant-sample-proxy) to create
a website with a Token button, or create a custom web app that implements the Token Request flow.
2. Configure the application.conf file with an email address.
3. Start this proxy server: `./gradlew build run`
4. Call GET /member to get the alias of the member. Make sure the alias is the same one that
is configured.
5. A user now checks out using the button. Call POST /token-requests to create a token request.
6. Call GET /token-request-url to get a url and redirect user to the url. A callback will
occur once the payment is approved by the user (This will require a download of the Token app for
most of the banks).
7. Call GET /parse-token-request-callback to retrieve the token id.
8. Call GET /tokens/:id to get details on that token.
9. Call POST /transfers to initiate a transfer using this token.


See an example below:
### Get member -  GET /member
```bash
curl -X GET "http://127.0.0.1:4567/member"
```

```json
{
  "memberId": "m:m2hkupUjtJRFKvpfD3vBKvTXM4J:5zKtXEAq",
  "aliases": [{
    "type": "EMAIL",
    "value": "merchanta@+noverify@example.com"
  }]
}
```

###  Create Token Request - POST /token-requests
```bash
curl -X POST -H 'Content-Type: application/json' "http://127.0.0.1:4567/token-requests" -d '{"amount":"4.99","currency":"EUR","description":"Book Purchase","destination":{"sepa":{"iban":"DE16700222000072880129"}}, "callbackUrl":"http://localhost:3000/redeem"}'
```

```json
{
  "tokenRequestId": "rq:21UaTmPruCDVPKJfv9peZ7Juv7Ck:5zKtXEAq"
}
```

###  Generates Token Request URL - GET /token-request-url
```bash
curl -X GET "http://127.0.0.1:4567/token-request-url?requestId=rq:21UaTmPruCDVPKJfv9peZ7Juv7Ck:5zKtXEAq&state=123&csrfToken=456"
```

```json
{
  "url": "https://web-app.sandbox.token.io/request-token/rq:21UaTmPruCDVPKJfv9peZ7Juv7Ck:5zKtXEAq?state=%7B%22csrfTokenHash%22%3A%22b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0%22%2C%22innerState%22%3A%22123%22%7D"
}
```

###  Pasre Callback URL - GET /parse-token-request-callback
```bash
curl -X GET "http://127.0.0.1:4567/parse-token-request-callback?csrfToken=456&url=http%3A%2F%2Flocalhost%3A3000%2Fredeem%3Fsignature%3D%257B%2522memberId%2522%253A%2522m%253A3rKtsoKaE1QUti3KCWPrcSQYvJj9%253A5zKtXEAq%2522%252C%2522keyId%2522%253A%2522lgf2Mn0G4kkcXd5m%2522%252C%2522signature%2522%253A%2522dkd52gYVCFZhETUPWHO1sCogkpzIjagXrNnUvtgVDxs9eMQg6_oRDYqMkFOpET4GoPpJywGYwipKVHpH_M7LAA%2522%257D%26state%3D%257B%2522csrfTokenHash%2522%253A%2522b3a8e0e1f9ab1bfe3a36f231f676f78bb30a519d2b21e6c530c0eee8ebb4a5d0%2522%252C%2522innerState%2522%253A%2522123%2522%257D%26tokenId%3Dtt%253ADkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh%253A5zKcENpV"
```

```json
{
  "tokenId": "tt:Dkjm8ysbkWxP6CBV8WbffJrZS6AGoBHTfoBwnU6erFDh:5zKcENpV",
  "state": "123"
}
```

###  Get Token - GET /tokens/:id
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
          "type": "EMAIL",
          "value": "merchanta@+noverify@example.com"
        }
      },
      "expiresAtMs": "1525384419767",
      "transfer": {
        "redeemer": {
          "id": "m:2ZooBovcBG9zLTPMMgkQFVrK9YLf:5zKtXEAq",
          "alias": {
            "type": "EMAIL",
            "value": "merchanta@+noverify@example.com"
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

### Create Transfer - POST /transfers
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

